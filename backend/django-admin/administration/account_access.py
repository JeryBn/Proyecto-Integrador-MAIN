"""School-issued activation. Flyway owns both new tables; Django never creates them."""
import hashlib
import secrets
import uuid
from datetime import datetime, timedelta
from functools import wraps
from zoneinfo import ZoneInfo
from django.conf import settings
from django.core.exceptions import ValidationError as EmailValidationError
from django.core.validators import validate_email
from django.core.mail import send_mail
from django.db import connection, transaction, IntegrityError
from django.http import JsonResponse
from django.views.decorators.http import require_http_methods, require_POST, require_GET
from .models import SharedUser
from .views import admin_only, error, fingerprint, pagination
from .academic import body, ValidationError, record
from .identity import text


def now():
    return datetime.now(ZoneInfo(settings.TIME_ZONE)).replace(tzinfo=None)


def revoke_tokens(username):
    # Older isolated Django tests intentionally have only the legacy schema.
    if 'account_access_token' in connection.introspection.table_names():
        with connection.cursor() as cursor:
            cursor.execute('UPDATE account_access_token SET used_at=%s WHERE username=%s AND used_at IS NULL', [now(), username])


def atomic_access(view):
    @wraps(view)
    def execute(request, *args, **kwargs):
        try:
            with transaction.atomic():
                return view(request, *args, **kwargs)
        except (ValidationError, EmailValidationError) as exc:
            return error(400, 'VALIDATION_ERROR', str(exc))
        except IntegrityError:
            return error(409, 'DATA_CONFLICT', 'El correo ya pertenece a otra cuenta.')
        except MailUnavailable:
            return error(503, 'MAIL_UNAVAILABLE', 'El correo del colegio no está disponible. Puedes entregar una invitación por el canal del colegio.')
    return execute


class MailUnavailable(Exception):
    pass


@require_GET
@admin_only
def requests(request):
    page = pagination(request)
    if page is None:
        return error(400, 'VALIDATION_ERROR', 'Paginación inválida.')
    offset, limit = page
    with connection.cursor() as cursor:
        cursor.execute("SELECT u.username,u.display_name,c.email,c.created_at FROM main_user u "
                       "JOIN account_contact c ON c.username=u.username "
                       "WHERE u.role='FAMILIAR' AND u.enabled=FALSE AND u.archived=FALSE AND c.approved_by IS NULL "
                       "ORDER BY c.created_at DESC,u.username LIMIT %s OFFSET %s", [limit, offset])
        values = [dict(username=row[0], name=row[1], email=row[2], createdAt=row[3].isoformat()) for row in cursor.fetchall()]
    response = JsonResponse(values, safe=False)
    response['Cache-Control'] = 'no-store'
    return response


@require_http_methods(['GET', 'POST'])
@admin_only
@atomic_access
def contact(request, username):
    user = SharedUser.objects.select_for_update().filter(pk=username, archived=False).first()
    if not user:
        return error(404, 'NOT_FOUND')
    with connection.cursor() as cursor:
        if request.method == 'POST':
            value = body(request, {'email', 'verified'})
            address = text(value.get('email'), 254).lower()
            validate_email(address)
            if not isinstance(value.get('verified'), bool):
                raise ValidationError('Indica si el colegio verificó el correo.')
            verified = now() if value['verified'] else None
            revoke_tokens(username)
            cursor.execute('SELECT username FROM account_contact WHERE username=%s', [username])
            if cursor.fetchone():
                cursor.execute('UPDATE account_contact SET email=%s,verified_at=%s,approved_by=%s,updated_at=%s WHERE username=%s',
                               [address, verified, request.admin_actor, now(), username])
            else:
                cursor.execute('INSERT INTO account_contact(username,email,verified_at,approved_by,created_at,updated_at) VALUES(%s,%s,%s,%s,%s,%s)',
                               [username, address, verified, request.admin_actor, now(), now()])
            record(request, 'ACCOUNT_CONTACT_UPDATED', username)
        cursor.execute('SELECT email,verified_at FROM account_contact WHERE username=%s', [username])
        row = cursor.fetchone()
    return JsonResponse({'username': username, 'email': row[0] if row else '', 'verified': bool(row and row[1])})


@require_POST
@admin_only
@atomic_access
def invitation(request, username):
    user = SharedUser.objects.select_for_update().filter(pk=username, archived=False).first()
    if not user:
        return error(404, 'NOT_FOUND')
    if user.role not in ('ALUMNO', 'DOCENTE', 'FAMILIAR'):
        return error(403, 'FORBIDDEN', 'Las cuentas administrativas no se activan mediante invitaciones públicas.')
    value = body(request, {'delivery'})
    delivery = value.get('delivery')
    if delivery not in ('SCHOOL', 'EMAIL'):
        raise ValidationError('Selecciona correo o entrega por el colegio.')
    code = secrets.token_urlsafe(24)
    created = now()
    revoke_tokens(username)
    with connection.cursor() as cursor:
        if delivery == 'EMAIL':
            cursor.execute('SELECT email FROM account_contact WHERE username=%s', [username])
            contact_row = cursor.fetchone()
            if not contact_row:
                raise ValidationError('Registra primero el correo de esta cuenta.')
            if not settings.EMAIL_HOST or not settings.DEFAULT_FROM_EMAIL:
                raise MailUnavailable()
        cursor.execute('INSERT INTO account_access_token(id,username,token_hash,purpose,delivery,credential_version,expires_at,created_at) VALUES(%s,%s,%s,%s,%s,%s,%s,%s)',
                       [str(uuid.uuid4()), username, hashlib.sha256(code.encode()).hexdigest(), 'ACTIVATE', delivery,
                        fingerprint(user), created + timedelta(minutes=30), created])
        cursor.execute('UPDATE account_contact SET approved_by=%s,updated_at=%s WHERE username=%s AND approved_by IS NULL',
                       [request.admin_actor, created, username])
    if delivery == 'EMAIL':
        try:
            send_mail('COLEGIA · Activa tu cuenta', f'Tu código de activación es:\n\n{code}\n\nCaduca en 30 minutos y solo se puede usar una vez. No lo compartas.',
                      settings.DEFAULT_FROM_EMAIL, [contact_row[0]], fail_silently=False)
        except Exception as exc:
            raise MailUnavailable() from exc
    record(request, 'ACCOUNT_INVITATION_CREATED', username)
    result = {'username': username, 'delivery': delivery, 'expiresAt': (created + timedelta(minutes=30)).isoformat(),
              'message': 'Invitación enviada.' if delivery == 'EMAIL' else 'Entrega este código únicamente a la persona cuya identidad verificó el colegio.'}
    if delivery == 'SCHOOL':
        result['code'] = code
    response = JsonResponse(result, status=201)
    response['Cache-Control'] = 'no-store'
    return response
