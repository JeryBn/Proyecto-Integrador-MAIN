"""Cuentas y vínculos verificados. Sin autorregistro ni creación de administradores."""
import re
import bcrypt
from datetime import datetime
from zoneinfo import ZoneInfo
from django.conf import settings
from django.db import connection, transaction, IntegrityError
from django.http import JsonResponse
from django.views.decorators.http import require_http_methods, require_POST, require_GET
from .models import SharedUser
from .views import admin_only, error, pagination
from .academic import body, positive, record


def text(value, maximum):
    if not isinstance(value, str) or not 1 <= len(value.strip()) <= maximum:
        raise ValueError(f'Texto de 1 a {maximum} caracteres requerido.')
    return value.strip()


def password(value):
    if not isinstance(value, str) or len(value) < 12 or len(value.encode()) > 72:
        raise ValueError('Contraseña: al menos 12 caracteres y máximo 72 bytes UTF-8.')
    return bcrypt.hashpw(value.encode(), bcrypt.gensalt(rounds=10)).decode()


def account_data(user):
    return dict(username=user.username, name=user.display_name, role=user.role, enabled=user.enabled,
                alumnoId=user.alumno_id, docenteId=user.docente_id)


def mutation(view):
    # Toda escritura y su evento se revierten juntos, incluso ante colisiones únicas.
    def execute(request, *args, **kwargs):
        try:
            with transaction.atomic():
                return view(request, *args, **kwargs)
        except (ValueError, UnicodeError) as exc:
            return error(400, 'VALIDATION_ERROR', str(exc))
        except IntegrityError:
            return error(409, 'DATA_CONFLICT', 'Conflicto de datos; no se guardaron cambios.')
    return execute


@require_http_methods(['GET', 'POST'])
@admin_only
@mutation
def users(request):
    if request.method == 'GET':
        page = pagination(request)
        if page is None:
            raise ValueError('Paginación inválida.')
        offset, limit = page
        return JsonResponse([account_data(u) for u in SharedUser.objects.order_by('username')[offset:offset+limit]], safe=False)
    value = body(request, {'username','name','role','password','alumnoId','docenteId'})
    username, name = text(value.get('username'), 60), text(value.get('name'), 100)
    if not re.fullmatch(r'[a-zA-Z0-9._-]{3,60}', username):
        raise ValueError('Usuario: 3 a 60 letras, números, punto o guion.')
    role = value.get('role')
    if role not in ('ALUMNO', 'DOCENTE', 'FAMILIAR'):
        raise ValueError('Rol inválido.')
    hashed = password(value.get('password'))
    if SharedUser.objects.filter(pk=username).exists():
        return error(409, 'CONFLICT', 'Usuario existente.')
    student, teacher = value.get('alumnoId'), value.get('docenteId')
    with connection.cursor() as cursor:
        if role == 'ALUMNO':
            if not positive(student) or teacher is not None:
                raise ValueError('Identidad de alumno positiva y sin identidad docente requerida.')
            cursor.execute('SELECT usuario FROM alumno WHERE id=%s', [student])
            row = cursor.fetchone()
            if row is None:
                cursor.execute('INSERT INTO alumno(id,usuario,nombre) VALUES(%s,%s,%s)', [student, username, name])
            elif row[0] != username:
                raise ValueError('El registro académico pertenece a otro usuario.')
        elif role == 'DOCENTE':
            if not positive(teacher) or student is not None:
                raise ValueError('Identidad de docente positiva y sin identidad de alumno requerida.')
            cursor.execute('SELECT id FROM docente WHERE id=%s', [teacher])
            if cursor.fetchone() is None:
                cursor.execute('INSERT INTO docente(id,nombre) VALUES(%s,%s)', [teacher, name])
        elif student is not None or teacher is not None:
            raise ValueError('Los parentescos se verifican por separado.')
    user = SharedUser.objects.create(username=username, display_name=name, role=role, password_hash=hashed,
                                    enabled=True, alumno_id=student, docente_id=teacher)
    record(request, 'ACCOUNT_CREATED', username)
    return JsonResponse(account_data(user), status=201)


@require_POST
@admin_only
@mutation
def change_account(request, username, action):
    user = SharedUser.objects.select_for_update().filter(pk=username).first()
    if not user:
        return error(404, 'NOT_FOUND')
    if user.role in ('ADMIN','REVIEWER'):
        return error(403, 'FORBIDDEN', 'No se modifican cuentas administrativas desde este módulo.')
    if action == 'reset-password':
        if not user.enabled:
            return error(403, 'FORBIDDEN')
        user.password_hash = password(body(request, {'password'}).get('password'))
        user.save(update_fields=['password_hash'])
        record(request, 'PASSWORD_RESET', username)
    else:
        user.enabled = False
        user.save(update_fields=['enabled'])
        record(request, 'ACCOUNT_DISABLED', username)
    return JsonResponse({})


@require_http_methods(['POST','DELETE'])
@admin_only
@mutation
def guardians(request, guardian=None, student_id=None):
    if request.method == 'POST':
        if guardian is not None:
            return error(405, 'METHOD_NOT_ALLOWED')
        value = body(request, {'guardian','studentId'})
        guardian, student_id = text(value.get('guardian'),60), value.get('studentId')
        if not positive(student_id):
            raise ValueError('Identificador de estudiante positivo requerido.')
    elif guardian is None:
        return error(405, 'METHOD_NOT_ALLOWED')
    with connection.cursor() as cursor:
        if request.method == 'POST':
            # Serializa altas idempotentes de un mismo familiar.
            user = SharedUser.objects.select_for_update().filter(pk=guardian, role='FAMILIAR', enabled=True).first()
            if not user:
                raise ValueError('La cuenta debe ser familiar activa.')
            cursor.execute('SELECT id FROM alumno WHERE id=%s', [student_id])
            if cursor.fetchone() is None:
                return error(404, 'NOT_FOUND')
            cursor.execute('SELECT COUNT(*) FROM guardian_link WHERE guardian=%s AND alumno_id=%s', [guardian,student_id])
            changed = cursor.fetchone()[0] == 0
            if changed:
                cursor.execute('INSERT INTO guardian_link VALUES(%s,%s,%s,%s)', [guardian,student_id,request.admin_actor,
                               datetime.now(ZoneInfo(settings.TIME_ZONE)).replace(tzinfo=None)])
        else:
            cursor.execute('DELETE FROM guardian_link WHERE guardian=%s AND alumno_id=%s', [guardian,student_id])
            changed = cursor.rowcount > 0
    if changed:
        record(request, 'GUARDIAN_VERIFIED' if request.method == 'POST' else 'GUARDIAN_REVOKED', f'{guardian}:{student_id}')
    return JsonResponse({'verified': request.method == 'POST'})


@require_POST
@admin_only
@mutation
def contacts(request):
    value = body(request, {'first','second'})
    first, second = sorted([text(value.get('first'),60), text(value.get('second'),60)])
    if first == second:
        raise ValueError('Contacto inválido.')
    users = list(SharedUser.objects.select_for_update().filter(username__in=[first,second],enabled=True).order_by('username'))
    if len(users) != 2 or any(u.role in ('ADMIN','REVIEWER') for u in users):
        raise ValueError('Dos cuentas activas no administrativas requeridas.')
    with connection.cursor() as cursor:
        cursor.execute('SELECT COUNT(*) FROM contact_link WHERE user_a=%s AND user_b=%s', [first,second])
        if cursor.fetchone()[0] == 0:
            cursor.execute('INSERT INTO contact_link VALUES(%s,%s,%s)', [first,second,request.admin_actor])
            record(request, 'CONTACT_APPROVED', f'{first}:{second}')
    return JsonResponse({'approved':True})


@require_GET
@admin_only
def reports(request):
    page = pagination(request)
    if page is None:
        return error(400, 'VALIDATION_ERROR', 'Paginación inválida.')
    offset, limit = page
    with connection.cursor() as cursor:
        cursor.execute('SELECT id,reporter,subject,reason,created_at FROM contact_report ORDER BY created_at DESC,id LIMIT %s OFFSET %s', [limit,offset])
        return JsonResponse([dict(zip(('id','reporter','subject','reason','created_at'),r)) for r in cursor.fetchall()], safe=False)
