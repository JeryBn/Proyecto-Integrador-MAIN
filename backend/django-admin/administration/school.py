"""Aulas, seguimiento escolar y recuperación. El esquema lo administra Flyway."""
from collections import defaultdict
from datetime import date, datetime
from decimal import Decimal, InvalidOperation
from uuid import uuid4
from zoneinfo import ZoneInfo

from django.conf import settings
from django.db import connection
from django.http import JsonResponse
from django.views.decorators.http import require_http_methods

from .academic import ValidationError, body, positive, record
from .identity import mutation, text
from .models import Course, SharedUser
from .views import admin_only, error


ACHIEVEMENTS = {'C': 0, 'B': 1, 'A': 2, 'AD': 3}


def school_now():
    return datetime.now(ZoneInfo(settings.TIME_ZONE)).replace(tzinfo=None)


def rows(sql, params=()):
    with connection.cursor() as cursor:
        cursor.execute(sql, params)
        names = [column[0] for column in cursor.description]
        return [dict(zip(names, row)) for row in cursor.fetchall()]


def execute(sql, params=()):
    with connection.cursor() as cursor:
        cursor.execute(sql, params)


def required_row(sql, params, message):
    result = rows(sql, params)
    if not result:
        raise ValidationError(message)
    return result[0]


def lock_year(year_id):
    # Serializa las escrituras académicas del año y sus eventos de auditoría.
    suffix = '' if connection.vendor == 'sqlite' else ' FOR UPDATE'
    return required_row('SELECT id FROM school_year WHERE id=%s' + suffix,
                        [year_id], 'Selecciona un año escolar registrado.')


def number(value, label):
    if type(value) not in (int, float):
        raise ValidationError(f'{label}: usa un número válido.')
    try:
        result = Decimal(str(value))
        if not result.is_finite() or result != result.quantize(Decimal('.01')):
            raise ValidationError(f'{label}: admite hasta dos decimales.')
        return result
    except InvalidOperation:
        raise ValidationError(f'{label}: usa un número válido.') from None


def calendar_date(value):
    try:
        return date.fromisoformat(value)
    except (ValueError, TypeError):
        raise ValidationError('Usa una fecha válida con formato año-mes-día.') from None


def classroom(item_id):
    return required_row('SELECT * FROM school_classroom WHERE id=%s', [item_id],
                        'Selecciona un aula registrada.')


def context(value):
    enrollment = required_row('SELECT * FROM school_enrollment WHERE id=%s',
                              [value.get('enrollmentId')], 'Selecciona una matrícula registrada.')
    lock_year(enrollment['year_id'])
    room = classroom(enrollment['classroom_id'])
    assignment = required_row('SELECT * FROM school_course WHERE id=%s',
                              [value.get('assignmentId')], 'Selecciona un curso asignado al aula.')
    period = required_row('SELECT * FROM school_period WHERE id=%s',
                          [value.get('periodId')], 'Selecciona un periodo registrado.')
    if assignment['classroom_id'] != room['id'] or period['year_id'] != room['year_id']:
        raise ValidationError('Estudiante, curso y periodo deben pertenecer a la misma aula y año.')
    return enrollment, room, assignment, period


def grade_value(value, room, score_key='score', achievement_key='achievement'):
    if room['grading_mode'] == 'NUMERIC':
        score = number(value.get(score_key), 'Calificación')
        if not 0 <= score <= Decimal(str(room['maximum'])):
            raise ValidationError(f'La nota debe estar entre 0 y {room["maximum"]}.')
        return score, None
    achievement = value.get(achievement_key)
    if achievement not in ACHIEVEMENTS:
        raise ValidationError('Selecciona AD, A, B o C.')
    return None, achievement


def add_course_access(student_id, course_id):
    execute('INSERT INTO matricula(alumno_id,curso_id) SELECT %s,%s '
            'WHERE NOT EXISTS (SELECT 1 FROM matricula WHERE alumno_id=%s AND curso_id=%s)',
            [student_id, course_id, student_id, course_id])


@require_http_methods(['GET'])
@admin_only
def catalog(request):
    return JsonResponse(dict(
        years=rows('SELECT * FROM school_year ORDER BY id DESC'),
        periods=rows('SELECT * FROM school_period ORDER BY year_id DESC,position'),
        classrooms=rows('SELECT c.*,COUNT(e.id) AS students FROM school_classroom c '
                        'LEFT JOIN school_enrollment e ON e.classroom_id=c.id '
                        'GROUP BY c.id,c.year_id,c.level,c.grade,c.section,c.grading_mode,'
                        'c.maximum,c.passing_score,c.passing_achievement '
                        'ORDER BY c.year_id DESC,c.level,c.grade,c.section'),
        courses=list(Course.objects.order_by('nombre').values('id', 'nombre')),
        teachers=list(SharedUser.objects.filter(role='DOCENTE', enabled=True)
                      .order_by('display_name').values('username', 'display_name')),
    ))


@require_http_methods(['POST'])
@admin_only
@mutation
def years(request):
    value = body(request, {'year', 'periodCount'})
    year = value.get('year')
    count = value.get('periodCount')
    if type(year) is not int or not 1000 <= year <= 9999 or type(count) is not int or count not in (3, 4):
        raise ValidationError('Indica un año de cuatro cifras y tres trimestres o cuatro bimestres.')
    execute('INSERT INTO school_year(id,name) VALUES(%s,%s)', [year, f'Año escolar {year}'])
    label = 'Trimestre' if count == 3 else 'Bimestre'
    for position in range(1, count + 1):
        execute('INSERT INTO school_period(id,year_id,name,position) VALUES(%s,%s,%s,%s)',
                [str(uuid4()), year, f'{label} {position}', position])
    record(request, 'SCHOOL_YEAR_CREATED', year)
    return JsonResponse({'id': year}, status=201)


@require_http_methods(['POST'])
@admin_only
@mutation
def active_period(request):
    value = body(request, {'yearId', 'periodId'})
    lock_year(value.get('yearId'))
    period = required_row('SELECT id FROM school_period WHERE id=%s AND year_id=%s',
                          [value.get('periodId'), value.get('yearId')], 'El periodo debe pertenecer al año seleccionado.')
    execute('UPDATE school_year SET active_period_id=%s WHERE id=%s', [period['id'], value['yearId']])
    record(request, 'ACTIVE_PERIOD_UPDATED', str(value['yearId']))
    return JsonResponse({'id': period['id']})


@require_http_methods(['POST'])
@admin_only
@mutation
def classrooms(request):
    value = body(request, {'yearId', 'level', 'grade', 'section', 'gradingMode',
                           'maximum', 'passingScore', 'passingAchievement'})
    lock_year(value.get('yearId'))
    level, grade = value.get('level'), value.get('grade')
    if level not in ('PRIMARIA', 'SECUNDARIA') or type(grade) is not int:
        raise ValidationError('Selecciona un nivel y grado válidos.')
    if not 1 <= grade <= (6 if level == 'PRIMARIA' else 5):
        raise ValidationError('Primaria tiene grados de 1 a 6 y secundaria de 1 a 5.')
    mode = value.get('gradingMode')
    if mode not in ('NUMERIC', 'LITERAL'):
        raise ValidationError('Selecciona calificación numérica o AD/A/B/C.')
    maximum = number(value.get('maximum'), 'Escala máxima')
    passing = number(value.get('passingScore'), 'Umbral de seguimiento')
    achievement = value.get('passingAchievement')
    if not 0 < maximum <= 1000 or not 0 <= passing <= maximum or achievement not in ACHIEVEMENTS:
        raise ValidationError('Revisa la escala y el criterio mínimo del aula.')
    item_id = str(uuid4())
    execute('INSERT INTO school_classroom VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s)',
            [item_id, value['yearId'], level, grade, text(value.get('section'), 20).upper(),
             mode, maximum, passing, achievement])
    record(request, 'CLASSROOM_CREATED', item_id)
    return JsonResponse({'id': item_id}, status=201)


@require_http_methods(['GET', 'POST'])
@admin_only
@mutation
def assignments(request):
    if request.method == 'GET':
        return JsonResponse(rows('SELECT s.*,c.nombre AS course_name,u.display_name AS teacher_name '
                                 'FROM school_course s JOIN curso c ON c.id=s.course_id '
                                 'JOIN main_user u ON u.username=s.teacher_username '
                                 'WHERE s.classroom_id=%s ORDER BY c.nombre',
                                 [request.GET.get('classroomId')]), safe=False)
    value = body(request, {'classroomId', 'courseId', 'teacherUsername'})
    room = classroom(value.get('classroomId'))
    lock_year(room['year_id'])
    if not positive(value.get('courseId')) or not Course.objects.filter(pk=value['courseId']).exists():
        raise ValidationError('Selecciona un curso registrado.')
    teacher = SharedUser.objects.filter(username=value.get('teacherUsername'), role='DOCENTE',
                                         enabled=True, docente_id__isnull=False).first()
    if teacher is None:
        raise ValidationError('El responsable debe ser una cuenta docente activa.')
    item_id = str(uuid4())
    execute('INSERT INTO school_course VALUES(%s,%s,%s,%s)',
            [item_id, room['id'], value['courseId'], teacher.username])
    for enrollment in rows('SELECT student_id FROM school_enrollment WHERE classroom_id=%s', [room['id']]):
        add_course_access(enrollment['student_id'], value['courseId'])
    record(request, 'CLASSROOM_COURSE_ASSIGNED', item_id)
    return JsonResponse({'id': item_id}, status=201)


@require_http_methods(['GET', 'POST'])
@admin_only
@mutation
def enrollments(request):
    if request.method == 'GET':
        return JsonResponse(rows('SELECT e.*,a.nombre AS student_name,a.usuario AS username '
                                 'FROM school_enrollment e JOIN alumno a ON a.id=e.student_id '
                                 'WHERE e.classroom_id=%s ORDER BY a.nombre',
                                 [request.GET.get('classroomId')]), safe=False)
    value = body(request, {'classroomId', 'username'})
    room = classroom(value.get('classroomId'))
    lock_year(room['year_id'])
    student = SharedUser.objects.filter(username=text(value.get('username'), 60), role='ALUMNO',
                                         enabled=True, alumno_id__isnull=False).first()
    if student is None:
        raise ValidationError('Usa el usuario de una cuenta de alumno activa.')
    existing = rows('SELECT id,classroom_id FROM school_enrollment WHERE year_id=%s AND student_id=%s',
                    [room['year_id'], student.alumno_id])
    if existing:
        if existing[0]['classroom_id'] != room['id']:
            return error(409, 'CONFLICT', 'El alumno ya está matriculado en otra aula de este año.')
        return JsonResponse({'id': existing[0]['id']})
    item_id = str(uuid4())
    execute('INSERT INTO school_enrollment VALUES(%s,%s,%s,%s,%s)',
            [item_id, room['year_id'], room['id'], student.alumno_id, school_now()])
    for assignment in rows('SELECT course_id FROM school_course WHERE classroom_id=%s', [room['id']]):
        add_course_access(student.alumno_id, assignment['course_id'])
    record(request, 'SCHOOL_ENROLLMENT_CREATED', item_id)
    return JsonResponse({'id': item_id}, status=201)


@require_http_methods(['GET', 'POST'])
@admin_only
@mutation
def marks(request):
    if request.method == 'GET':
        return JsonResponse(rows('SELECT m.*,a.nombre AS student_name,c.nombre AS course_name,'
                                 'p.name AS period_name FROM school_mark m '
                                 'JOIN school_enrollment e ON e.id=m.enrollment_id '
                                 'JOIN alumno a ON a.id=e.student_id JOIN school_course s ON s.id=m.assignment_id '
                                 'JOIN curso c ON c.id=s.course_id JOIN school_period p ON p.id=m.period_id '
                                 'WHERE e.classroom_id=%s AND m.period_id=%s '
                                 'ORDER BY a.nombre,c.nombre,m.recorded_at DESC,m.id',
                                 [request.GET.get('classroomId'), request.GET.get('periodId')]), safe=False)
    value = body(request, {'enrollmentId', 'assignmentId', 'periodId', 'assessment',
                           'score', 'achievement', 'expectedVersion', 'competency'})
    enrollment, room, assignment, period = context(value)
    assessment = text(value.get('assessment'), 80)
    competency = text(value.get('competency', 'General'), 120)
    score, achievement = grade_value(value, room)
    previous = rows('SELECT id,version FROM school_mark WHERE enrollment_id=%s AND assignment_id=%s '
                    'AND period_id=%s AND assessment=%s',
                    [enrollment['id'], assignment['id'], period['id'], assessment])
    expected = value.get('expectedVersion')
    actual = previous[0]['version'] if previous else -1
    if type(expected) is not int or expected != actual:
        return error(409, 'CONFLICT', 'La evaluación ya existe o cambió. Actualiza y usa Editar.')
    item_id = previous[0]['id'] if previous else str(uuid4())
    now = school_now()
    if previous:
        execute('UPDATE school_mark SET score=%s,achievement=%s,recorded_by=%s,recorded_at=%s,competency=%s,'
                'version=version+1 WHERE id=%s', [score, achievement, request.admin_actor, now, competency, item_id])
    else:
        execute('INSERT INTO school_mark(id,enrollment_id,assignment_id,period_id,assessment,score,achievement,recorded_by,recorded_at,version,competency) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,0,%s)',
                [item_id, enrollment['id'], assignment['id'], period['id'], assessment,
                 score, achievement, request.admin_actor, now, competency])
    if score is not None:
        # Mantiene disponibles las notas numéricas para los lectores existentes de cursos/notas.
        if rows('SELECT id FROM course_grade WHERE id=%s', [item_id]):
            execute('UPDATE course_grade SET score=%s,maximum=%s,recorded_by=%s,recorded_at=%s WHERE id=%s',
                    [score, room['maximum'], request.admin_actor, now, item_id])
        else:
            label = f'{room["year_id"]}/{period["position"]} - {assessment}'
            execute('INSERT INTO course_grade VALUES(%s,%s,%s,%s,%s,%s,%s,%s)',
                    [item_id, assignment['course_id'], enrollment['student_id'], label,
                     score, room['maximum'], request.admin_actor, now])
    record(request, 'SCHOOL_MARK_UPDATED' if previous else 'SCHOOL_MARK_CREATED', item_id)
    return JsonResponse({'id': item_id, 'version': actual + 1}, status=200 if previous else 201)


@require_http_methods(['GET', 'POST'])
@admin_only
@mutation
def attendance(request):
    if request.method == 'GET':
        day = calendar_date(request.GET.get('date'))
        return JsonResponse(rows('SELECT a.*,s.nombre AS student_name FROM school_attendance a '
                                 'JOIN school_enrollment e ON e.id=a.enrollment_id '
                                 'JOIN alumno s ON s.id=e.student_id WHERE e.classroom_id=%s '
                                 'AND a.attendance_date=%s ORDER BY s.nombre',
                                 [request.GET.get('classroomId'), day]), safe=False)
    value = body(request, {'enrollmentId', 'date', 'status', 'note', 'expectedVersion'})
    enrollment = required_row('SELECT * FROM school_enrollment WHERE id=%s',
                              [value.get('enrollmentId')], 'Selecciona un alumno matriculado.')
    lock_year(enrollment['year_id'])
    day = calendar_date(value.get('date'))
    if day.year != enrollment['year_id']:
        raise ValidationError('La asistencia debe corresponder al año de matrícula.')
    status = value.get('status')
    if status not in ('PRESENT', 'LATE', 'ABSENT', 'JUSTIFIED'):
        raise ValidationError('Selecciona un estado de asistencia válido.')
    note = value.get('note', '')
    if not isinstance(note, str) or len(note) > 500:
        raise ValidationError('La observación admite hasta 500 caracteres.')
    previous = rows('SELECT id,version FROM school_attendance WHERE enrollment_id=%s AND attendance_date=%s',
                    [enrollment['id'], day])
    actual = previous[0]['version'] if previous else -1
    if type(value.get('expectedVersion')) is not int or value['expectedVersion'] != actual:
        return error(409, 'CONFLICT', 'La asistencia cambió. Actualiza antes de registrarla.')
    item_id = previous[0]['id'] if previous else str(uuid4())
    if previous:
        execute('UPDATE school_attendance SET status=%s,note=%s,recorded_by=%s,version=version+1 WHERE id=%s',
                [status, note.strip(), request.admin_actor, item_id])
    else:
        execute('INSERT INTO school_attendance VALUES(%s,%s,%s,%s,%s,%s,0)',
                [item_id, enrollment['id'], day, status, note.strip(), request.admin_actor])
    record(request, 'ATTENDANCE_RECORDED', item_id)
    return JsonResponse({'id': item_id}, status=200 if previous else 201)


@require_http_methods(['GET', 'POST'])
@admin_only
@mutation
def recovery(request):
    if request.method == 'GET':
        return JsonResponse(rows('SELECT r.*,a.nombre AS student_name,c.nombre AS course_name,p.name AS period_name '
                                 'FROM school_recovery r JOIN school_enrollment e ON e.id=r.enrollment_id '
                                 'JOIN alumno a ON a.id=e.student_id JOIN school_course s ON s.id=r.assignment_id '
                                 'JOIN curso c ON c.id=s.course_id JOIN school_period p ON p.id=r.period_id '
                                 'WHERE e.classroom_id=%s ORDER BY r.due_date,a.nombre',
                                 [request.GET.get('classroomId')]), safe=False)
    value = body(request, {'enrollmentId', 'assignmentId', 'periodId', 'plan', 'dueDate', 'status',
                           'score', 'achievement', 'expectedVersion'})
    enrollment, room, assignment, period = context(value)
    plan = text(value.get('plan'), 2000)
    due = calendar_date(value.get('dueDate'))
    status = value.get('status')
    if status not in ('PENDING', 'IN_PROGRESS', 'COMPLETED'):
        raise ValidationError('Selecciona un estado de recuperación válido.')
    score, achievement = None, None
    if status == 'COMPLETED':
        score, achievement = grade_value(value, room)
    previous = rows('SELECT id,version FROM school_recovery WHERE enrollment_id=%s AND assignment_id=%s AND period_id=%s',
                    [enrollment['id'], assignment['id'], period['id']])
    actual = previous[0]['version'] if previous else -1
    if type(value.get('expectedVersion')) is not int or value['expectedVersion'] != actual:
        return error(409, 'CONFLICT', 'El plan ya existe o cambió. Actualiza y usa Editar.')
    item_id = previous[0]['id'] if previous else str(uuid4())
    if previous:
        execute('UPDATE school_recovery SET plan=%s,due_date=%s,status=%s,result_score=%s,'
                'result_achievement=%s,recorded_by=%s,updated_at=%s,version=version+1 WHERE id=%s',
                [plan, due, status, score, achievement, request.admin_actor, school_now(), item_id])
    else:
        execute('INSERT INTO school_recovery VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,0)',
                [item_id, enrollment['id'], assignment['id'], period['id'], plan, due, status,
                 score, achievement, request.admin_actor, school_now()])
    record(request, 'RECOVERY_UPDATED' if previous else 'RECOVERY_CREATED', item_id)
    return JsonResponse({'id': item_id}, status=200 if previous else 201)


@require_http_methods(['GET'])
@admin_only
@mutation
def overview(request):
    room_id = request.GET.get('classroomId')
    room = classroom(room_id)
    period = required_row('SELECT * FROM school_period WHERE id=%s AND year_id=%s',
                          [request.GET.get('periodId'), room['year_id']], 'Selecciona un periodo del año escolar.')
    students = rows('SELECT e.id,e.student_id,a.nombre AS name,a.usuario AS username '
                    'FROM school_enrollment e JOIN alumno a ON a.id=e.student_id '
                    'WHERE e.classroom_id=%s ORDER BY a.nombre', [room_id])
    selected = request.GET.get('studentId', '')
    if selected:
        students = [student for student in students if str(student['student_id']) == selected]
        if not students:
            raise ValidationError('El estudiante seleccionado debe estar matriculado en esta aula.')
    marks_data = rows('SELECT m.*,e.student_id FROM school_mark m '
                      'JOIN school_enrollment e ON e.id=m.enrollment_id '
                      'WHERE e.classroom_id=%s AND m.period_id=%s ORDER BY m.recorded_at,m.id',
                      [room_id, period['id']])
    by_student = defaultdict(list)
    for mark in marks_data:
        by_student[(mark['student_id'], mark['assignment_id'])].append(mark)
    chart = []
    support_by_student = defaultdict(int)
    evaluated_by_student = defaultdict(int)
    for assignment in rows('SELECT s.id,c.nombre AS name FROM school_course s '
                           'JOIN curso c ON c.id=s.course_id WHERE s.classroom_id=%s ORDER BY c.nombre', [room_id]):
        values = []
        distribution = dict.fromkeys(ACHIEVEMENTS, 0)
        support = 0
        for student in students:
            marks_for_student = by_student[(student['student_id'], assignment['id'])]
            if not marks_for_student:
                continue
            evaluated_by_student[student['student_id']] += 1
            if room['grading_mode'] == 'NUMERIC':
                average = sum(float(mark['score']) for mark in marks_for_student) / len(marks_for_student)
                values.append(average)
                needs_support = average < float(room['passing_score'])
            else:
                latest = marks_for_student[-1]['achievement']
                distribution[latest] += 1
                needs_support = ACHIEVEMENTS[latest] < ACHIEVEMENTS[room['passing_achievement']]
            if needs_support:
                support += 1
                support_by_student[student['student_id']] += 1
        chart.append(dict(id=assignment['id'], name=assignment['name'],
                          average=round(sum(values) / len(values), 2) if values else None,
                          maximum=float(room['maximum']), evaluated=sum(distribution.values()) if room['grading_mode'] == 'LITERAL' else len(values),
                          needsSupport=support, distribution=distribution))
    for student in students:
        student['evaluatedCourses'] = evaluated_by_student[student['student_id']]
        student['supportCourses'] = support_by_student[student['student_id']]
    return JsonResponse(dict(classroom=room, period=period, courses=chart, students=students,
                              evaluatedStudents=sum(bool(s['evaluatedCourses']) for s in students),
                              supportStudents=sum(bool(s['supportCourses']) for s in students)))
