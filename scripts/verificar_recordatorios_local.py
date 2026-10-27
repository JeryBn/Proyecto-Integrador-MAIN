"""Synthetic HTTP check of the real MySQL scheduler; no FCM/provider call."""
import json,os,time
from pathlib import Path
from datetime import datetime,timedelta
from zoneinfo import ZoneInfo
from verificar_arquitectura_compartida import Session
admin=Session('http://127.0.0.1:8090');student=Session('http://127.0.0.1:8088');other=Session('http://127.0.0.1:8088')
checks=[]
def check(name,condition):
    assert condition,name
    checks.append(dict(name=name,passed=True))
_,csrf=admin.call('/api/admin/csrf')
check('admin_login',admin.call('/api/admin/login','POST',dict(username='android_test_admin',password=os.environ['MAIN_TEST_ADMIN_PASSWORD']),csrf,True)[0]==200)
_,csrf=admin.call('/api/admin/csrf');stamp=int(time.time()*1000);name='reminder_student_'+str(stamp);teacher='reminder_teacher_'+str(stamp)
for username,role,field,identity in [(name,'ALUMNO','alumnoId',stamp),(teacher,'DOCENTE','docenteId',stamp+1)]:
    check('create_'+role.lower(),admin.call('/api/admin/v1/users','POST',dict(username=username,name='Recordatorio sintético',role=role,password='SyntheticReminder2026!',**{field:identity}),csrf)[0]==201)
check('course',admin.call('/api/admin/v1/courses','POST',dict(id=stamp,name='Curso sintético de recordatorios',teacherUsername=teacher),csrf)[0]==201)
roster='/api/admin/v1/courses/'+str(stamp)+'/students'
check('enroll',admin.call(roster,'PUT',dict(username=name,enrolled=True),csrf)[0]==200)
due=(datetime.now(ZoneInfo('America/Lima'))+timedelta(minutes=10)).strftime('%Y-%m-%dT%H:%M:%S')
check('activity',admin.call('/api/admin/actividades','POST',dict(id=stamp,cursoId=stamp,titulo='Recordatorio sintético',descripcion='Solo prueba aislada',fechaLimite=due),csrf)[0]==201)
def login(session,username,password):
    _,token=session.call('/login-token')
    check('login_'+('student' if session is student else 'other'),session.call('/login','POST',dict(username=username,password=password,**{token['parameterName']:token['token']}),form=True)[0]==200)
    return session.call('/api/csrf')[1]
token=login(student,name,'SyntheticReminder2026!');login(other,'vacio','MainDemo2026!')
check('default_off',student.call('/api/v1/reminders/preferences')[1]['enabled'] is False)
check('csrf_required',student.call('/api/v1/reminders/preferences','PUT',dict(enabled=True,minutesBefore=15))[0]==403)
check('enable',student.call('/api/v1/reminders/preferences','PUT',dict(enabled=True,minutesBefore=15),token)[0]==200)
deadline=time.monotonic()+75;notices=[]
while time.monotonic()<deadline:
    notices=student.call('/api/v1/notifications')[1]
    if any(n.get('activityId')==stamp for n in notices):break
    time.sleep(2)
items=[n for n in notices if n.get('activityId')==stamp]
check('real_scheduler_once',len(items)==1)
check('activity_destination',items[0]['kind']=='REMINDER' and items[0]['conversationId'] is None)
check('other_account_isolated',not any(n.get('activityId')==stamp for n in other.call('/api/v1/notifications')[1]))
check('read',student.call('/api/v1/notifications/'+items[0]['id']+'/read','POST',{},token)[0]==200)
check('revoke',admin.call(roster,'PUT',dict(username=name,enrolled=False),csrf)[0]==200)
check('revoked_notice_hidden',not any(n.get('activityId')==stamp for n in student.call('/api/v1/notifications')[1]))
check('disable',student.call('/api/v1/reminders/preferences','PUT',dict(enabled=False,minutesBefore=15),token)[0]==200)
out=Path(os.environ['MAIN_REMINDER_EVIDENCE']);out.parent.mkdir(parents=True,exist_ok=True)
out.write_text(json.dumps(dict(synthetic=True,realMySqlScheduler=True,fcmSent=False,passed=len(checks),checks=checks),indent=2),encoding='utf-8')
print(str(len(checks))+' comprobaciones reales de recordatorios aprobadas; sin envío FCM. Se conservan las entidades sintéticas identificadas.')

