"""School-issued password activation using the shared Spring identity."""
import hashlib
import secrets
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo

from django.conf import settings
from django.db import connection
from django.http import JsonResponse
from django.views.decorators.http import require_POST

from .academic import record
from .identity import mutation
from .models import SharedUser
from .views import admin_only, error, fingerprint


@require_POST
@admin_only
@mutation
def invitation(request, username):
    user = SharedUser.objects.select_for_update().filter(pk=username).first()
    if user is None:
        return error(404, 'NOT_FOUND')
    if not user.enabled or user.role not in ('ALUMNO', 'DOCENTE', 'FAMILIAR'):
        return error(409, 'INVITATION_UNAVAILABLE', 'La cuenta debe estar habilitada y pertenecer a la comunidad escolar.')

    code = secrets.token_urlsafe(24)
    expires = datetime.now(ZoneInfo(settings.TIME_ZONE)).replace(tzinfo=None) + timedelta(minutes=30)
    with connection.cursor() as cursor:
        cursor.execute('DELETE FROM account_activation_invitation WHERE username=%s', [username])
        cursor.execute('''INSERT INTO account_activation_invitation
            (username, token_hash, credential_version, expires_at)
            VALUES (%s,%s,%s,%s)''', [username, hashlib.sha256(code.encode()).hexdigest(), fingerprint(user), expires])
    record(request, 'ACCOUNT_INVITATION_CREATED', username)
    response = JsonResponse({'username': username, 'code': code, 'expiresAt': expires.isoformat()}, status=201)
    response['Cache-Control'] = 'no-store'
    return response
