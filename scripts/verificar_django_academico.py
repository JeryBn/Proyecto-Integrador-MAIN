"""Comprobación real con identidades sintéticas; conserva el curso de prueba creado."""
import json
import os
import time
from pathlib import Path
from verificar_arquitectura_compartida import Session

admin = Session('http://127.0.0.1:8090')
user = Session('http://127.0.0.1:8088')
checks = []
def check(label, actual, expected):
    assert actual == expected, (label, actual, expected)
    checks.append(dict(name=label, passed=True))
_, token = admin.call('/api/admin/csrf')
check('login_django', admin.call('/api/admin/login', 'POST', dict(username='android_test_admin',
      password=os.environ['MAIN_TEST_ADMIN_PASSWORD']), token=token, form=True)[0], 200)
_, token = admin.call('/api/admin/csrf')
cid = int(time.time()*1000)
course = dict(id=cid, name='Curso sintético Django académico', teacherUsername='docente_revision')
check('create_course', admin.call('/api/admin/v1/courses', 'POST', course, token)[0], 201)
course.update(name='Curso sintético Django editado', expectedVersion=0)
check('edit_course', admin.call(f'/api/admin/v1/courses/{cid}', 'PUT', course, token)[0], 200)
check('stale_course_conflict', admin.call(f'/api/admin/v1/courses/{cid}', 'PUT', course, token)[0], 409)
enrollment = dict(username='vacio', enrolled=True)
path = f'/api/admin/v1/courses/{cid}/students'
check('enroll', admin.call(path, 'PUT', enrollment, token)[0], 200)
check('enroll_idempotent', admin.call(path, 'PUT', enrollment, token)[0], 200)
check('roster', admin.call(path)[1][0]['username'], 'vacio')
_, csrf = user.call('/login-token')
check('login_spring', user.call('/login', 'POST', dict(username='vacio', password=os.environ['MAIN_TEST_STUDENT_PASSWORD'],
      **{csrf['parameterName']:csrf['token']}), form=True)[0], 200)
status, agenda = user.call('/api/v1/agenda')
check('spring_agenda', status, 200)
check('spring_reads_django_enrollment', cid in [c['id'] for c in agenda['courses']], True)
enrollment['enrolled'] = False
check('revoke', admin.call(path, 'PUT', enrollment, token)[0], 200)
check('spring_revokes_access', cid in [c['id'] for c in user.call('/api/v1/agenda')[1]['courses']], False)
check('audit_metadata', any(a['subject']==str(cid) and a['action']=='COURSE_UPDATED'
                         for a in admin.call('/api/admin/v1/audit?limit=100')[1]), True)
destination = Path(os.environ['MAIN_ACADEMIC_EVIDENCE'])
destination.parent.mkdir(parents=True, exist_ok=True)
destination.write_text(json.dumps(dict(courseId=cid, passed=len(checks), checks=checks), indent=2), encoding='utf-8')
print(f'{len(checks)} comprobaciones HTTP reales correctas.')
