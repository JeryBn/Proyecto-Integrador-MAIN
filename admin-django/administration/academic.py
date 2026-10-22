"""Administración académica sobre el esquema compartido propiedad de Flyway."""
import json
import uuid
from datetime import datetime
from zoneinfo import ZoneInfo
from django.conf import settings
from django.db import connection, transaction, IntegrityError
from django.http import JsonResponse
from django.views.decorators.http import require_http_methods, require_GET
from .models import Course, SharedUser, Audit
from .views import admin_only, error, pagination


def record(request, action, subject):
    Audit.objects.create(id=str(uuid.uuid4()), actor=request.admin_actor, action=action,
                         subject=str(subject), occurred_at=datetime.now(ZoneInfo(settings.TIME_ZONE)).replace(tzinfo=None))


def body(request, allowed):
    if request.content_type != 'application/json' or len(request.body) > 32768:
        raise ValueError('Se requiere un objeto JSON de tamaño válido.')
    value = json.loads(request.body)
    if not isinstance(value, dict) or value.keys() - allowed:
        raise ValueError('Objeto JSON con campos conocidos requerido.')
    return value


def positive(value):
    return type(value) is int and 1 <= value <= 9007199254740991


def course_data(item):
    return dict(id=item.id, name=item.nombre, teacherId=item.docente_id,
                teacher=item.docente.nombre, version=item.admin_version)


@require_http_methods(['GET', 'POST', 'PUT'])
@admin_only
def courses(request, item_id=None):
    if request.method == 'GET':
        if item_id is not None:
            return error(405, 'METHOD_NOT_ALLOWED')
        page = pagination(request)
        if page is None:
            return error(400, 'VALIDATION_ERROR', 'Paginación inválida.')
        offset, limit = page
        return JsonResponse([course_data(c) for c in Course.objects.select_related('docente').order_by('id')[offset:offset+limit]], safe=False)
    create = request.method == 'POST'
    if create != (item_id is None):
        return error(405, 'METHOD_NOT_ALLOWED')
    try:
        value = body(request, {'id', 'name', 'teacherUsername', 'expectedVersion'})
        cid = value.get('id')
        if not positive(cid) or (not create and cid != item_id):
            raise ValueError('Identificador positivo y coincidente requerido.')
        name, teacher_name = value.get('name'), value.get('teacherUsername')
        if not isinstance(name, str) or not 1 <= len(name.strip()) <= 100:
            raise ValueError('Nombre de curso de 1 a 100 caracteres requerido.')
        if not isinstance(teacher_name, str) or not 1 <= len(teacher_name.strip()) <= 60:
            raise ValueError('Usuario docente requerido.')
        version = value.get('expectedVersion')
        if (create and version is not None) or (not create and (type(version) is not int or version < 0)):
            raise ValueError('Versión previa inválida. Actualiza antes de editar.')
        with transaction.atomic():
            teacher = SharedUser.objects.filter(username=teacher_name.strip(), enabled=True, role='DOCENTE').first()
            if not teacher or teacher.docente_id is None:
                raise ValueError('El responsable debe ser una cuenta docente activa.')
            if create:
                item = Course.objects.create(id=cid, nombre=name.strip(), docente_id=teacher.docente_id, admin_version=0)
            else:
                item = Course.objects.select_for_update().filter(pk=cid).first()
                if item is None:
                    return error(404, 'NOT_FOUND')
                if item.admin_version != version:
                    return error(409, 'CONFLICT', 'El curso cambió. Actualiza antes de guardarlo.')
                with connection.cursor() as cursor:
                    cursor.execute('SELECT COUNT(*) FROM timetable WHERE curso_id=%s', [cid])
                    if cursor.fetchone()[0] and item.docente_id != teacher.docente_id:
                        raise ValueError('Retira o ajusta primero el horario antes de cambiar de docente.')
                # Comparación atómica adicional, incluso en motores sin SELECT FOR UPDATE.
                if not Course.objects.filter(pk=cid, admin_version=version).update(
                        nombre=name.strip(), docente_id=teacher.docente_id, admin_version=version+1):
                    return error(409, 'CONFLICT', 'El curso cambió. Actualiza antes de guardarlo.')
            record(request, 'COURSE_CREATED' if create else 'COURSE_UPDATED', cid)
            result = course_data(Course.objects.select_related('docente').get(pk=cid))
        return JsonResponse(result, status=201 if create else 200)
    except (ValueError, UnicodeError) as exc:
        return error(400, 'VALIDATION_ERROR', str(exc))
    except IntegrityError:
        return error(409, 'DATA_CONFLICT', 'Conflicto de datos; no se guardaron cambios.')


@require_http_methods(['GET', 'PUT'])
@admin_only
def students(request, item_id):
    if request.method == 'GET':
        if not Course.objects.filter(pk=item_id).exists():
            return error(404, 'NOT_FOUND')
        page = pagination(request)
        if page is None:
            return error(400, 'VALIDATION_ERROR', 'Paginación inválida.')
        offset, limit = page
        with connection.cursor() as cursor:
            cursor.execute('SELECT u.username,a.id,a.nombre FROM alumno a JOIN matricula m ON m.alumno_id=a.id JOIN main_user u ON u.alumno_id=a.id WHERE m.curso_id=%s ORDER BY a.id LIMIT %s OFFSET %s', [item_id, limit, offset])
            return JsonResponse([dict(username=r[0], studentId=r[1], name=r[2]) for r in cursor.fetchall()], safe=False)
    try:
        value = body(request, {'username', 'enrolled'})
        username, enrolled = value.get('username'), value.get('enrolled')
        if not isinstance(username, str) or not 1 <= len(username.strip()) <= 60 or type(enrolled) is not bool:
            raise ValueError('Usuario y decisión de matrícula explícita requeridos.')
        with transaction.atomic():
            if not Course.objects.select_for_update().filter(pk=item_id).first():
                return error(404, 'NOT_FOUND')
            student = SharedUser.objects.filter(pk=username.strip(), role='ALUMNO').first()
            if not student or student.alumno_id is None or (enrolled and not student.enabled):
                raise ValueError('La matrícula requiere una cuenta de estudiante activa.')
            with connection.cursor() as cursor:
                if enrolled:
                    cursor.execute('SELECT COUNT(*) FROM matricula WHERE curso_id=%s AND alumno_id=%s', [item_id, student.alumno_id])
                    changed = cursor.fetchone()[0] == 0
                    if changed:
                        cursor.execute('INSERT INTO matricula(alumno_id,curso_id) VALUES(%s,%s)', [student.alumno_id, item_id])
                else:
                    cursor.execute('DELETE FROM matricula WHERE curso_id=%s AND alumno_id=%s', [item_id, student.alumno_id])
                    changed = cursor.rowcount > 0
            if changed:
                record(request, 'ENROLLMENT_GRANTED' if enrolled else 'ENROLLMENT_REVOKED', f'{item_id}:{student.username}')
        return JsonResponse({}, status=200)
    except (ValueError, UnicodeError) as exc:
        return error(400, 'VALIDATION_ERROR', str(exc))
    except IntegrityError:
        return error(409, 'DATA_CONFLICT', 'Conflicto de datos; no se guardaron cambios.')


@require_GET
@admin_only
def audit(request):
    page = pagination(request)
    if page is None:
        return error(400, 'VALIDATION_ERROR', 'Paginación inválida.')
    offset, limit = page
    return JsonResponse([dict(id=a.id, actor=a.actor, action=a.action, subject=a.subject,
                             occurredAt=a.occurred_at.isoformat(timespec='seconds'))
                         for a in Audit.objects.order_by('-occurred_at', '-id')[offset:offset+limit]], safe=False)
