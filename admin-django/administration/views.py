import hashlib
import hmac
import json
import re
import uuid
from datetime import datetime
from functools import wraps
from zoneinfo import ZoneInfo
import bcrypt
from django.conf import settings
from django.core.cache import cache
from django.db import IntegrityError, transaction
from django.http import FileResponse, HttpResponse, JsonResponse
from django.middleware.csrf import get_token, rotate_token
from django.views.decorators.cache import never_cache
from django.views.decorators.http import require_GET, require_POST, require_http_methods
from .models import Activity, Audit, Course, SharedUser

STATIC = settings.BASE_DIR.parent / 'src/main/resources/static'
DUMMY_HASH = bcrypt.hashpw(b'not-a-user-password', bcrypt.gensalt(rounds=10))

def error(status, code, message='Acceso rechazado.', fields=None):
    value = {'code': code, 'message': message}
    if fields is not None:
        value['fields'] = fields
    return JsonResponse({'error': value}, status=status)

def csrf_failure(request, reason=''):
    return error(403, 'CSRF_REJECTED', 'Renueva la sesión e intenta nuevamente.')

def fingerprint(user):
    return hashlib.sha256(user.password_hash.encode()).hexdigest()

def admin_only(view):
    @wraps(view)
    def protected(request, *args, **kwargs):
        name = request.session.get('main_admin')
        user = SharedUser.objects.filter(username=name).first() if name else None
        if not user or not user.enabled or user.role != 'ADMIN' or not hmac.compare_digest(
                request.session.get('password_version', ''), fingerprint(user)):
            request.session.flush()
            return error(401, 'UNAUTHENTICATED')
        request.admin_actor = user.username
        return view(request, *args, **kwargs)
    return never_cache(protected)

@never_cache
@require_GET
def csrf(request):
    return JsonResponse(dict(parameterName='csrfmiddlewaretoken', headerName='X-CSRFToken', token=get_token(request)))

@never_cache
@require_POST
def login(request):
    name, password = request.POST.get('username', ''), request.POST.get('password', '')
    # Límite local al proceso; el despliegue añade límite compartido en proxy/cache.
    key = 'login:' + hashlib.sha256((request.META.get('REMOTE_ADDR', '') + ':' + name[:60]).encode()).hexdigest()
    attempts = cache.get(key, 0)
    if attempts >= 10:
        return error(429, 'LOGIN_LIMIT', 'Espera unos minutos antes de intentar nuevamente.')
    user = SharedUser.objects.filter(username=name).first() if 0 < len(name) <= 60 else None
    try:
        valid = bcrypt.checkpw(password.encode(), user.password_hash.encode() if user else DUMMY_HASH)
    except (ValueError, UnicodeError):
        valid = False
    if not valid or not user or not user.enabled:
        cache.set(key, attempts + 1, 300)
        return error(401, 'INVALID_CREDENTIALS', 'Usuario o contraseña incorrectos.')
    if user.role != 'ADMIN':
        request.session.flush()
        return error(403, 'FORBIDDEN', 'Esta cuenta no tiene acceso de administración.')
    request.session.cycle_key()
    request.session['main_admin'] = user.username
    request.session['password_version'] = fingerprint(user)
    rotate_token(request)
    cache.delete(key)
    return JsonResponse({'rol': 'ADMIN'})

@require_POST
@admin_only
def logout(request):
    request.session.flush()
    return JsonResponse({'message': 'Sesión finalizada'})

@require_GET
@admin_only
def profile(request):
    return JsonResponse(dict(usuario=request.admin_actor, rol='ADMIN'))

def pagination(request):
    try:
        raw_offset, raw_limit = request.GET.get('offset', '0'), request.GET.get('limit', '20')
        if not re.fullmatch(r'[0-9]+', raw_offset) or not re.fullmatch(r'[0-9]+', raw_limit):
            raise ValueError()
        offset, limit = int(raw_offset), int(raw_limit)
        if not 0 <= offset <= 1_000_000 or not 1 <= limit <= 100:
            raise ValueError()
        return offset, limit
    except ValueError:
        return None

def activity_data(item):
    return dict(id=item.id, cursoId=item.curso_id, curso=item.curso.nombre,
                docente=item.curso.docente.nombre, titulo=item.titulo, descripcion=item.descripcion,
                fechaLimite=item.fecha_limite.isoformat(timespec='seconds'))

@require_GET
@admin_only
def courses(request):
    page = pagination(request)
    if page is None:
        return error(400, 'VALIDATION_ERROR', 'Paginación inválida.')
    offset, limit = page
    values = Course.objects.select_related('docente').order_by('id')
    return JsonResponse(dict(items=[dict(id=c.id, nombre=c.nombre, docente=c.docente.nombre)
                                    for c in values[offset:offset+limit]], total=values.count(), offset=offset, limit=limit))

@require_http_methods(['GET', 'POST'])
@admin_only
def activities(request):
    if request.method == 'POST':
        return create_activity(request)
    page = pagination(request)
    if page is None:
        return error(400, 'VALIDATION_ERROR', 'Paginación inválida.')
    offset, limit = page
    values = Activity.objects.select_related('curso__docente').order_by('fecha_limite', 'id')
    return JsonResponse(dict(items=[activity_data(a) for a in values[offset:offset+limit]],
                             total=values.count(), offset=offset, limit=limit))

def create_activity(request):
    if request.content_type != 'application/json':
        return error(415, 'UNSUPPORTED_MEDIA_TYPE', 'Se requiere JSON.')
    if len(request.body) > 32768:
        return error(400, 'INVALID_REQUEST', 'Solicitud demasiado grande.')
    try:
        body = json.loads(request.body)
    except (ValueError, UnicodeError):
        return error(400, 'INVALID_REQUEST', 'JSON inválido.')
    if not isinstance(body, dict):
        return error(400, 'INVALID_REQUEST', 'Se requiere objeto JSON.')
    allowed = {'id', 'cursoId', 'titulo', 'descripcion', 'fechaLimite'}
    fields = {key: 'Campo desconocido.' for key in body.keys() - allowed}
    for key in ('id', 'cursoId'):
        if type(body.get(key)) is not int or not 1 <= body[key] <= 9_007_199_254_740_991:
            fields[key] = 'Entero positivo seguro para JavaScript requerido.'
    for key, limit in (('titulo', 150), ('descripcion', 2000)):
        value = body.get(key)
        if not isinstance(value, str) or not 1 <= len(value.strip()) <= limit:
            fields[key] = f'Texto de 1 a {limit} caracteres requerido.'
    date = None
    try:
        value = body.get('fechaLimite')
        if not isinstance(value, str) or not re.fullmatch(r'[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}', value):
            raise ValueError()
        date = datetime.strptime(value, '%Y-%m-%dT%H:%M:%S')
        if date.year < 1970 or date.year > 2037:
            raise ValueError()
    except ValueError:
        fields['fechaLimite'] = 'Fecha Lima YYYY-MM-DDTHH:mm:ss, sin offset; años 1970–2037.'
    if fields:
        return error(400, 'VALIDATION_ERROR', 'Revisa los campos.', fields)
    if not Course.objects.filter(pk=body['cursoId']).exists():
        return error(400, 'VALIDATION_ERROR', 'Curso inexistente.', {'cursoId': 'Selecciona un curso existente.'})
    try:
        with transaction.atomic():
            item = Activity.objects.create(id=body['id'], curso_id=body['cursoId'], titulo=body['titulo'].strip(),
                                           descripcion=body['descripcion'].strip(), fecha_limite=date)
            Audit.objects.create(id=str(uuid.uuid4()), actor=request.admin_actor, action='ACTIVITY_CREATED',
                                 subject=str(item.id), occurred_at=datetime.now(ZoneInfo(settings.TIME_ZONE)).replace(tzinfo=None))
    except IntegrityError:
        return error(409, 'DATA_CONFLICT', 'Identificador duplicado o conflicto de curso. No se sobrescribió la actividad.')
    response = JsonResponse(activity_data(Activity.objects.select_related('curso__docente').get(pk=item.id)), status=201)
    response['Location'] = f'/api/admin/actividades/{item.id}'
    return response

@require_GET
@admin_only
def detail(request, item_id):
    item = Activity.objects.select_related('curso__docente').filter(pk=item_id).first()
    return JsonResponse(activity_data(item)) if item else error(404, 'NOT_FOUND', 'Actividad no disponible.')

@require_GET
def page(request):
    # La interfaz React consume directamente todos los módulos administrativos de Django.
    html = (STATIC / 'admin.html').read_text(encoding='utf-8')
    return HttpResponse(html)

@require_GET
def asset(request, name):
    if name not in ('admin.js', 'admin.css'):
        return error(404, 'NOT_FOUND')
    return FileResponse((STATIC / 'assets' / name).open('rb'), content_type='text/javascript' if name.endswith('.js') else 'text/css')
