import csv
import io

from django.db import connection
from django.http import JsonResponse
from django.views.decorators.http import require_POST

from .academic import ValidationError, body, record
from .identity import mutation
from .models import Course, SharedUser
from .views import admin_only, error


def parse_batch(source):
    if not isinstance(source, str) or not source.strip():
        raise ValidationError('Pega un CSV con la columna usuario.')
    reader = csv.reader(io.StringIO(source.lstrip('\ufeff'), newline=''), strict=True)
    entries = []
    try:
        header = next(reader, [])
        if len(header) != 1 or header[0].strip().casefold() != 'usuario':
            raise ValidationError('La primera fila debe contener solo el encabezado usuario.')
        for fields in reader:
            if not fields or all(not field.strip() for field in fields):
                continue
            if len(entries) == 300:
                raise ValidationError('El lote admite hasta 300 filas con datos.')
            username = fields[0].strip() if len(fields) == 1 else ''
            entries.append((reader.line_num, username))
    except csv.Error:
        raise ValidationError('El CSV contiene comillas o campos inválidos.') from None
    if not entries:
        raise ValidationError('Agrega al menos un estudiante después del encabezado.')
    return entries


@require_POST
@admin_only
@mutation
def import_students(request, item_id):
    value = body(request, {'csv', 'apply'})
    if type(value.get('apply')) is not bool:
        raise ValidationError('Selecciona vista previa o confirmación.')
    applying = value['apply']
    entries = parse_batch(value.get('csv'))
    courses = Course.objects.select_for_update() if applying else Course.objects
    if courses.filter(pk=item_id).first() is None:
        return error(404, 'NOT_FOUND', 'El curso no está disponible.')
    names = {name for _, name in entries if 1 <= len(name) <= 60}
    accounts = SharedUser.objects.filter(username__in=names, role='ALUMNO', enabled=True).order_by('username')
    if applying:
        accounts = accounts.select_for_update()
    students = {student.username.casefold(): student for student in accounts if student.alumno_id is not None}
    with connection.cursor() as cursor:
        cursor.execute('SELECT alumno_id FROM matricula WHERE curso_id=%s', [item_id])
        enrolled = {row[0] for row in cursor.fetchall()}
        seen, result, pending = set(), [], []
        for line, username in entries:
            student = students.get(username.casefold())
            if not student:
                state, detail = 'INVALID', 'Ingresa el usuario de un estudiante activo.'
            elif student.alumno_id in seen:
                state, detail = 'INVALID', 'El estudiante se repite en el archivo.'
            elif student.alumno_id in enrolled:
                state, detail = 'EXISTS', 'Ya está matriculado.'
            else:
                state, detail = 'READY', 'Listo para matricular.'
                pending.append(student)
            if student:
                seen.add(student.alumno_id)
            result.append(dict(line=line, username=username[:60], state=state, detail=detail))
        valid = all(row['state'] != 'INVALID' for row in result)
        if applying and valid:
            cursor.executemany('INSERT INTO matricula(alumno_id,curso_id) VALUES(%s,%s)',
                               [(student.alumno_id, item_id) for student in pending])
            for student in pending:
                record(request, 'ENROLLMENT_GRANTED', f'{item_id}:{student.username}')
    return JsonResponse(dict(rows=result, valid=valid, applied=applying and valid,
                             added=len(pending) if applying and valid else 0),
                        status=409 if applying and not valid else 200)
