"""Add the approved Stitch demonstration without deleting existing school records.

Default: read-only preview. Use --apply to load; repeated runs do not duplicate rows.
Only this seed's activity/publication dates are refreshed on repeated runs.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import sys
import uuid
from datetime import datetime, timedelta, timezone

LOCAL = Path.home() / 'Documents/Codex/2026-10-22/ho/work'
sys.path.insert(0, str(LOCAL / 'main-python-libs'))
import bcrypt
import MySQLdb

NS = uuid.UUID('6a7cc77f-8476-4bfe-a445-8addf551c76b')
def uid(name):
    return str(uuid.uuid5(NS, name))


def build():
    today = datetime.now(timezone(timedelta(hours=-5))).date()
    now = datetime.combine(today, datetime.min.time()).replace(hour=8)
    rows = {}
    def add(table, key, value):
        rows.setdefault(table, []).append((key, value))
    accounts = [
        ('mateo.morales', 'Mateo Morales', 'ALUMNO', 880001, None),
        ('valentina.morales', 'Valentina Morales', 'ALUMNO', 880002, None),
        ('ana.morales', 'Ana Morales', 'FAMILIAR', None, None),
        ('ana.torres', 'Ana Torres', 'FAMILIAR', None, None),
        ('carlos.mendoza', 'Prof. Carlos Mendoza', 'DOCENTE', None, 880101),
        ('monica.silva', 'Lic. Mónica Silva', 'DOCENTE', None, 880102),
        ('elena.ramos', 'Prof. Elena Ramos', 'DOCENTE', None, 880103),
        ('diego.salazar', 'Prof. Diego Salazar', 'DOCENTE', None, 880104),
        ('laura.vega', 'Prof. Laura Vega', 'DOCENTE', None, 880105),
        ('admin.monica', 'Lic. Mónica Silva', 'ADMIN', None, None),
    ]
    for username, name, role, student, teacher in accounts:
        if student:
            add('alumno', ['id'], dict(id=student, usuario=username, nombre=name))
        if teacher:
            add('docente', ['id'], dict(id=teacher, nombre=name))
        add('main_user', ['username'], dict(username=username, display_name=name, role=role, enabled=True,
            archived=False, password_change_required=False, alumno_id=student, docente_id=teacher,
            dark_theme=False, notifications=True, status_audience='CONTACTS'))
    courses = [
        (881001, 'Matemáticas', 880101, 'carlos.mendoza', 'secondary'),
        (881002, 'Comunicación', 880103, 'elena.ramos', 'secondary'),
        (881003, 'Ciencias', 880101, 'carlos.mendoza', 'secondary'),
        (881004, 'Historia', 880103, 'elena.ramos', 'secondary'),
        (881005, 'Música', 880104, 'diego.salazar', 'secondary'),
        (881006, 'Física Teórica', 880105, 'laura.vega', 'secondary'),
        (882001, 'Matemática', 880102, 'monica.silva', 'primary'),
        (882002, 'Comunicación', 880102, 'monica.silva', 'primary'),
        (882003, 'Ciencias', 880102, 'monica.silva', 'primary'),
        (882004, 'Historia', 880102, 'monica.silva', 'primary'),
        (882005, 'Música', 880102, 'monica.silva', 'primary'),
    ]
    students = {'secondary': 880001, 'primary': 880002}
    for level, student in students.items():
        room, enrollment = uid('room-' + level), uid('enrollment-' + level)
        add('school_classroom', ['id'], dict(id=room, year_id=today.year,
            level='SECUNDARIA' if level == 'secondary' else 'PRIMARIA', grade=3 if level == 'secondary' else 5,
            section='A', grading_mode='LITERAL', maximum=20, passing_score=11, passing_achievement='A'))
        add('school_enrollment', ['id'], dict(id=enrollment, year_id=today.year, classroom_id=room, student_id=student, created_at=now))
        add('guardian_link', ['guardian', 'alumno_id'], dict(guardian='ana.morales', alumno_id=student,
            verified_by='admin.monica', verified_at=now))
        days = []
        day = today
        while len(days) < 41:
            if day.weekday() < 5:
                days.append(day)
            day -= timedelta(days=1)
        for i, day in enumerate(days):
            status = 'LATE' if i in (4, 13) else 'JUSTIFIED' if i == 20 else 'PRESENT'
            add('school_attendance', ['id'], dict(id=uid(f'attendance-{student}-{i}'), enrollment_id=enrollment,
                attendance_date=day, status=status, note='Cita médica comunicada al colegio.' if status == 'JUSTIFIED' else '',
                recorded_by='admin.monica', version=0))
    for i, (course, name, teacher, teacher_user, level) in enumerate(courses):
        student, room, enrollment = students[level], uid('room-' + level), uid('enrollment-' + level)
        assignment = uid('assignment-' + str(course))
        add('curso', ['id'], dict(id=course, nombre=name, docente_id=teacher, admin_version=0))
        add('matricula', ['alumno_id', 'curso_id'], dict(alumno_id=student, curso_id=course))
        add('school_course', ['id'], dict(id=assignment, classroom_id=room, course_id=course, teacher_username=teacher_user))
        for period in range(1, 4):
            for competency in range(2):
                achievement = ['AD', 'A', 'AD', 'A', 'B', 'C'][(i + competency + period) % 6]
                if level == 'primary' and i % 3 == 0:
                    achievement = 'AD'
                label = ('Resuelve problemas de cantidad', 'Comunica sus procedimientos') if 'Matem' in name else ('Comprende y explica', 'Aplica lo aprendido')
                add('school_mark', ['id'], dict(id=uid(f'mark-{course}-{period}-{competency}'), enrollment_id=enrollment,
                    assignment_id=assignment, period_id=f'@period:{period}', assessment=f'Evaluación {competency + 1} · Trimestre {period}',
                    competency=label[competency], score=None, achievement=achievement, recorded_by=teacher_user,
                    recorded_at=now - timedelta(days=(3-period)*60 + 2-competency), version=0))
        if (level == 'secondary' and name == 'Matemáticas') or (level == 'primary' and name == 'Comunicación'):
            add('school_recovery', ['id'], dict(id=uid('recovery-' + level), enrollment_id=enrollment, assignment_id=assignment,
                period_id='@period:3', plan='Reforzar resolución de problemas con práctica guiada y acompañamiento de la familia.',
                due_date=today + timedelta(days=14), status='IN_PROGRESS', result_score=None, result_achievement=None,
                recorded_by=teacher_user, updated_at=now, version=0))
    # Three classes every weekday; school course ownership prevents cross-student visibility.
    for level, base in [('secondary', 881000), ('primary', 882000)]:
        schedules = [[1, 6, 4], [2, 3, 5], [1, 6, 3], [1, 6, 4], [2, 3, 5]] if level == 'secondary' else [[1, 2, 3], [4, 1, 5], [1, 2, 3], [1, 4, 5], [2, 3, 1]]
        for day, course_slots in enumerate(schedules, 1):
            for slot, suffix in enumerate(course_slots):
                add('timetable', ['id'], dict(id=uid(f'lesson-{level}-{day}-{slot}'), curso_id=base+suffix, day_of_week=day,
                    starts_at=['08:00:00', '09:45:00', '11:30:00'][slot], ends_at=['09:30:00', '11:15:00', '13:00:00'][slot],
                    classroom=(['Aula 402', 'Lab. Ciencias 01', 'Pabellón B-12'] if level == 'secondary' else ['Aula 205', 'Aula 205', 'Sala de Primaria'])[slot]))
    tasks = [
        (881001, 'TASK', 'Taller 04: Derivadas', 'Resuelve los ejercicios y explica cada procedimiento. Adjunta tu desarrollo.', 2),
        (881006, 'TASK', 'Informe Termodinámica', 'Describe un experimento de transferencia de calor con conclusiones y fuentes.', 4),
        (881001, 'EXAM', 'Evaluación Escrita II', 'Repasa los temas trabajados y trae tus materiales.', 7),
        (881003, 'TASK', 'Exploramos los ecosistemas', 'Presenta un esquema de un ecosistema de tu región.', 5),
        (881004, 'EXAM', 'Evaluación de Historia', 'Relaciona los procesos históricos estudiados.', 9),
        (881002, 'TASK', 'Lectura y comentario crítico', 'Lee el texto asignado y argumenta tus respuestas.', 6),
        (882001, 'TASK', 'Resolvemos problemas cotidianos', 'Resuelve las situaciones con dibujos y operaciones.', 2),
        (882002, 'TASK', 'Mi cuento favorito', 'Escribe un cuento breve con inicio, desarrollo y final.', 3),
        (882003, 'EXAM', 'Evaluación de Ciencias', 'Repasa los seres vivos y sus características.', 8),
        (882004, 'TASK', 'Conocemos nuestra historia', 'Prepara una línea de tiempo con ilustraciones.', 5),
        (882005, 'TASK', 'Ritmos y sonidos', 'Prepara una secuencia rítmica con materiales del hogar.', 6),
        (882001, 'EXAM', 'Evaluación de Matemática', 'Repasa las operaciones y la resolución de problemas.', 10),
    ]
    course_lookup = {c[0]: c for c in courses}
    publication_ids = []
    for i, (course, kind, title, instructions, days) in enumerate(tasks):
        _, _, _, teacher_user, level = course_lookup[course]
        activity = 883001 + i
        due = datetime.combine(today + timedelta(days=days), datetime.min.time()).replace(hour=18)
        add('actividad', ['id'], dict(id=activity, curso_id=course, titulo=title, descripcion=instructions, fecha_limite=due))
        publication = uid('publication-' + str(i));publication_ids.append(publication)
        add('school_publication', ['id'], dict(id=publication, course_id=course, classroom_id=uid('room-'+level), teacher_username=teacher_user,
            kind=kind, title=title, instructions=instructions, due_at=due, created_at=now-timedelta(hours=i)))
        if i in (3, 10):
            add('entrega', ['alumno_id', 'actividad_id'], dict(alumno_id=students[level], actividad_id=activity, fecha=now))
    for i, (course, title, message) in enumerate([
        (881001, 'Calendario de evaluaciones 2026', 'Consulta las próximas evaluaciones y organiza tu agenda de estudio.'),
        (881003, 'Reunión de familias y docentes', 'El colegio invita a las familias a revisar los avances y acordar el acompañamiento.'),
        (882001, 'Acompañamos el aprendizaje en casa', 'Reserva un momento diario para conversar y revisar las actividades.'),
        (882003, 'Feria escolar de Ciencias', 'Prepararemos una exposición con los trabajos del aula y la participación de las familias.'),
    ]):
        _, _, _, teacher_user, level = course_lookup[course]
        publication=uid('notice-'+str(i));publication_ids.append(publication)
        add('school_publication', ['id'], dict(id=publication, course_id=course, classroom_id=uid('room-'+level), teacher_username=teacher_user,
            kind='NOTICE', title=title, instructions=message, due_at=None, created_at=now-timedelta(days=2,hours=i)))
    for i, publication in enumerate(publication_ids[:8]+publication_ids[-2:]):
        add('school_publication_read', ['publication_id', 'guardian'], dict(publication_id=publication, guardian='ana.morales', read_at=now+timedelta(minutes=10+i)))
    return today.year, accounts, rows


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', type=Path, default=LOCAL/'main-review-private.json')
    parser.add_argument('--apply', action='store_true')
    args=parser.parse_args()
    year, accounts, rows=build()
    config=json.loads(args.config.read_text(encoding='utf-8-sig'))
    db=MySQLdb.connect(host='127.0.0.1',port=int(config['port']),user='main_django',passwd=config['django_password'],db=config['database'],charset='utf8mb4')
    try:
        with db.cursor() as q:
            q.execute('SET TRANSACTION READ ONLY' if not args.apply else 'SET TRANSACTION ISOLATION LEVEL SERIALIZABLE')
            q.execute('START TRANSACTION')
            q.execute('SELECT id,name,position FROM school_period WHERE year_id=%s ORDER BY position',[year])
            periods={r[2]:r[0] for r in q.fetchall()}
            if set(periods)!={1,2,3}:
                raise RuntimeError('Se requieren los tres trimestres existentes del año actual. No se modificaron periodos.')
            q.execute('SELECT id FROM administrative_audit WHERE id=%s',[uid('seed-marker')]);owned=q.fetchone() is not None
            order = ['docente','alumno','main_user','curso','school_classroom','school_enrollment','guardian_link',
                     'matricula','school_course','timetable','actividad','entrega','school_publication',
                     'school_publication_read','school_mark','school_attendance','school_recovery']
            for table in order:
                entries = rows[table]
                for keys, value in entries:
                    for field, val in list(value.items()):
                        if isinstance(val,str) and val.startswith('@period:'):
                            value[field]=periods[int(val.split(':')[1])]
                    where=' AND '.join('`'+key+'`=%s' for key in keys)
                    q.execute('SELECT COUNT(*) FROM `'+table+'` WHERE '+where,[value[k] for k in keys]);exists=q.fetchone()[0]>0
                    if exists and not owned:
                        raise RuntimeError('Colisión con un registro anterior en '+table+'. No se reemplazó ningún dato.')
            print(json.dumps({'mode':'apply' if args.apply else 'preview','records':{t:len(v) for t,v in rows.items()},'existingReplacements':[],
                'schoolYearUpdate':f'{year}: activar Trimestre 3','alreadyLoaded':owned},ensure_ascii=False))
            if not args.apply:
                db.rollback();return
            for table in order:
                entries = rows[table]
                for keys, value in entries:
                    where=' AND '.join('`'+key+'`=%s' for key in keys)
                    q.execute('SELECT COUNT(*) FROM `'+table+'` WHERE '+where,[value[k] for k in keys]);exists=q.fetchone()[0]>0
                    if exists:
                        if table in ('actividad','school_publication'):
                            fields=[f for f in ('fecha_limite','due_at','created_at') if f in value]
                            q.execute('UPDATE `'+table+'` SET '+','.join('`'+f+'`=%s' for f in fields)+' WHERE '+where,[value[f] for f in fields]+[value[k] for k in keys])
                        continue
                    if table=='main_user':
                        value['password_hash']=bcrypt.hashpw(b'ColegiaDemo2026!',bcrypt.gensalt(rounds=10)).decode()
                    fields=list(value)
                    q.execute('INSERT INTO `'+table+'` ('+','.join('`'+f+'`' for f in fields)+') VALUES ('+','.join(['%s']*len(fields))+')',[value[f] for f in fields])
            q.execute('UPDATE school_year SET active_period_id=%s WHERE id=%s AND active_period_id IS NULL',[periods[3],year])
            if not owned:
                q.execute('INSERT INTO administrative_audit(id,actor,action,subject,occurred_at) VALUES(%s,%s,%s,%s,%s)',
                          [uid('seed-marker'),'admin.monica','STITCH_DEMO_LOADED','stitch-demo-v1: additive, no replacements',datetime.now(timezone(timedelta(hours=-5))).replace(tzinfo=None)])
            db.commit()
            print('Carga confirmada. Cuentas ficticias nuevas:')
            for username, name, role, _, _ in accounts:
                print(f'{role}: {username} · {name}')
            print('Contraseña inicial de prueba: ColegiaDemo2026! (no cambia al repetir la carga).')
    except Exception:
        db.rollback();raise
    finally:
        db.close()


if __name__=='__main__':
    main()
