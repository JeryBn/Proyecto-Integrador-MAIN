import re
import uuid
from datetime import time
from django.db import connection
from django.http import JsonResponse
from django.views.decorators.http import require_http_methods
from .models import Course, Teacher
from .views import admin_only, error
from .academic import body, positive, record
from .identity import mutation, text


@require_http_methods(['POST','DELETE'])
@admin_only
@mutation
def timetable(request, slot_id=None):
    if request.method == 'DELETE':
        if slot_id is None:
            return error(405,'METHOD_NOT_ALLOWED')
        with connection.cursor() as cursor:
            cursor.execute('DELETE FROM timetable WHERE id=%s', [slot_id])
            if cursor.rowcount == 0:
                return error(404,'NOT_FOUND')
        record(request,'TIMETABLE_REMOVED',slot_id)
        return JsonResponse({})
    if slot_id is not None:
        return error(405,'METHOD_NOT_ALLOWED')
    value = body(request, {'courseId','dayOfWeek','startsAt','endsAt','classroom'})
    if not positive(value.get('courseId')) or type(value.get('dayOfWeek')) is not int or not 1 <= value['dayOfWeek'] <= 7:
        raise ValueError('Curso positivo y día de 1 (lunes) a 7 (domingo) requeridos.')
    classroom = text(value.get('classroom'),100)
    times = []
    for key in ('startsAt','endsAt'):
        raw = value.get(key)
        if not isinstance(raw,str) or not re.fullmatch(r'[0-9]{2}:[0-9]{2}(:[0-9]{2})?',raw):
            raise ValueError('Horas HH:mm o HH:mm:ss requeridas.')
        times.append(time.fromisoformat(raw))
    start,end = times
    if start >= end:
        raise ValueError('La hora final debe ser posterior a la inicial.')
    course = Course.objects.select_for_update().filter(pk=value['courseId']).first()
    if not course:
        return error(404,'NOT_FOUND')
    Teacher.objects.select_for_update().get(pk=course.docente_id)
    # Replica la protección de cruces del contrato compartido Spring.
    with connection.cursor() as cursor:
        cursor.execute('SELECT COUNT(*) FROM timetable t JOIN curso c ON c.id=t.curso_id WHERE t.day_of_week=%s AND t.starts_at<%s AND t.ends_at>%s AND (c.docente_id=%s OR t.classroom=%s)',
                       [value['dayOfWeek'],end.isoformat(),start.isoformat(),course.docente_id,classroom])
        if cursor.fetchone()[0]:
            raise ValueError('El horario se cruza con otra clase del docente o aula.')
        slot_id = str(uuid.uuid4())
        cursor.execute('INSERT INTO timetable(id,curso_id,day_of_week,starts_at,ends_at,classroom) VALUES(%s,%s,%s,%s,%s,%s)',
                       [slot_id,course.id,value['dayOfWeek'],start.isoformat(),end.isoformat(),classroom])
    record(request,'TIMETABLE_CREATED',slot_id)
    return JsonResponse({'id':slot_id},status=201)
