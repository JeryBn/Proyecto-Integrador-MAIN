import uuid
from datetime import datetime
from decimal import Decimal, InvalidOperation
from zoneinfo import ZoneInfo
from django.conf import settings
from django.db import connection
from django.http import JsonResponse
from django.views.decorators.http import require_http_methods
from .models import Course
from .views import admin_only, error
from .academic import body, positive, record
from .identity import mutation, text


def timestamp(value):
    # java.sql.Timestamp.toString conserva al menos un decimal.
    return value.strftime('%Y-%m-%d %H:%M:%S') + '.' + (f'{value.microsecond:06d}'.rstrip('0') or '0')


@require_http_methods(['PUT'])
@admin_only
@mutation
def save(request, item_id):
    value = body(request, {'studentId','assessment','score','maximum','expectedRecordedAt'})
    if not positive(value.get('studentId')):
        raise ValueError('Estudiante positivo requerido.')
    assessment = text(value.get('assessment'),120)
    numbers = []
    for key in ('score','maximum'):
        raw = value.get(key)
        if type(raw) not in (int,float):
            raise ValueError('Nota y escala numéricas requeridas.')
        try:
            number = Decimal(str(raw))
            if not number.is_finite() or number != number.quantize(Decimal('.01')):
                raise ValueError('Máximo dos decimales.')
            numbers.append(number)
        except InvalidOperation:
            raise ValueError('Nota o escala inválida.')
    score,maximum = numbers
    if not 0 < maximum <= 1000 or not 0 <= score <= maximum:
        raise ValueError('Nota entre cero y la escala máxima, hasta 1000.')
    if not Course.objects.select_for_update().filter(pk=item_id).first():
        return error(404,'NOT_FOUND')
    with connection.cursor() as cursor:
        cursor.execute('SELECT COUNT(*) FROM matricula WHERE curso_id=%s AND alumno_id=%s', [item_id,value['studentId']])
        if not cursor.fetchone()[0]:
            return error(404,'NOT_FOUND')
        cursor.execute('SELECT id,recorded_at FROM course_grade WHERE course_id=%s AND student_id=%s AND assessment=%s',
                       [item_id,value['studentId'],assessment])
        previous = cursor.fetchone()
        expected = value.get('expectedRecordedAt')
        if expected is not None and expected != (timestamp(previous[1]) if previous else 'NEW'):
            return error(409,'CONFLICT','La nota cambió. Actualiza antes de guardarla.')
        gid = previous[0] if previous else str(uuid.uuid4())
        now = datetime.now(ZoneInfo(settings.TIME_ZONE)).replace(tzinfo=None)
        if previous:
            cursor.execute('UPDATE course_grade SET score=%s,maximum=%s,recorded_by=%s,recorded_at=%s WHERE id=%s',
                           [score,maximum,request.admin_actor,now,gid])
        else:
            cursor.execute('INSERT INTO course_grade VALUES(%s,%s,%s,%s,%s,%s,%s,%s)',
                           [gid,item_id,value['studentId'],assessment,score,maximum,request.admin_actor,now])
        # Recuperar la precisión realmente persistida, no la del reloj Python.
        cursor.execute('SELECT recorded_at FROM course_grade WHERE id=%s',[gid])
        saved = cursor.fetchone()[0]
    record(request,'GRADE_UPDATED' if previous else 'GRADE_CREATED',gid)
    return JsonResponse(dict(id=gid,assessment=assessment,score=float(score),maximum=float(maximum),recordedAt=timestamp(saved)))
