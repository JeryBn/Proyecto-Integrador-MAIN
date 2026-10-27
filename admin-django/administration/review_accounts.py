"""Self-service password setup; no account creation or privilege changes on HTTP."""
import bcrypt
import hashlib
from django.core.cache import cache
from django.http import JsonResponse
from django.views.decorators.http import require_POST
from .views import operator_only, error, fingerprint, operator_data
from .identity import mutation, password
from .academic import body, record
from .models import SharedUser

@require_POST
@operator_only
@mutation
def change_password(request):
    value=body(request,{'currentPassword','newPassword'})
    current=value.get('currentPassword');new=value.get('newPassword')
    key='review-password:'+hashlib.sha256((request.admin_actor+':'+request.META.get('REMOTE_ADDR','')).encode()).hexdigest()
    attempts=cache.get(key,0)
    if attempts>=8:return error(429,'PASSWORD_CHANGE_LIMIT','Espera unos minutos antes de intentarlo nuevamente.')
    user=SharedUser.objects.select_for_update().get(username=request.admin_actor)
    # Recheck under lock: two concurrent changes cannot both use the old password.
    if not user.enabled or fingerprint(user)!=request.session.get('password_version'):
        request.session.flush();return error(401,'UNAUTHENTICATED')
    if not isinstance(current,str) or len(current.encode())>72 or not bcrypt.checkpw(current.encode(),user.password_hash.encode()):
        cache.set(key,attempts+1,900)
        return error(400,'CURRENT_PASSWORD_INVALID','La contraseña actual no coincide.')
    if current==new:raise ValueError('Elige una contraseña distinta de la temporal o anterior.')
    user.password_hash=password(new);user.password_change_required=False
    user.save(update_fields=['password_hash','password_change_required'])
    record(request,'REVIEWER_PASSWORD_CHANGED' if user.role=='REVIEWER' else 'PASSWORD_CHANGED_SELF',user.username)
    cache.delete(key)
    request.session.cycle_key();request.session['password_version']=fingerprint(user)
    return JsonResponse(operator_data(user))
