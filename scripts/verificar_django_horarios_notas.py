import json,os,time
from pathlib import Path
from verificar_arquitectura_compartida import Session
admin=Session('http://127.0.0.1:8090');_,token=admin.call('/api/admin/csrf')
assert admin.call('/api/admin/login','POST',dict(username='android_test_admin',password=os.environ['MAIN_TEST_ADMIN_PASSWORD']),token,True)[0]==200
_,token=admin.call('/api/admin/csrf');checks=[]
def check(name,actual,expected):
    assert actual==expected,(name,actual,expected)
    checks.append(dict(name=name,passed=True))
cid=int(time.time()*1000);teacher='teacher_'+str(cid)
check('teacher_created',admin.call('/api/admin/v1/users','POST',dict(username=teacher,name='Docente sintético horario',role='DOCENTE',password='SyntheticTeacher2026!',docenteId=cid),token)[0],201)
check('course_created',admin.call('/api/admin/v1/courses','POST',dict(id=cid,name='Horario y notas Django sintéticos',teacherUsername=teacher),token)[0],201)
path=f'/api/admin/v1/courses/{cid}/students'
check('enroll',admin.call(path,'PUT',dict(username='alumno',enrolled=True),token)[0],200)
value=dict(courseId=cid,dayOfWeek=7,startsAt='22:17',endsAt='22:27',classroom='SYN-'+str(cid))
status,slot=admin.call('/api/admin/v1/timetable','POST',value,token);check('timetable_create',status,201)
check('overlap_rejected',admin.call('/api/admin/v1/timetable','POST',value,token)[0],400)
student=Session('http://127.0.0.1:8088');_,csrf=student.call('/login-token')
check('student_login',student.call('/login','POST',dict(username='alumno',password=os.environ['MAIN_TEST_STUDENT_PASSWORD'],**{csrf['parameterName']:csrf['token']}),form=True)[0],200)
check('schedule_shared',any(s['id']==slot['id'] for s in student.call('/api/v1/agenda')[1]['timetable']),True)
grade=dict(studentId=1,assessment='Evaluación sintética Django',score=18.5,maximum=20,expectedRecordedAt='NEW')
gradepath=f'/api/admin/v1/courses/{cid}/grades'
status,saved=admin.call(gradepath,'PUT',grade,token);check('grade_create',status,200)
check('stale_grade_conflict',admin.call(gradepath,'PUT',grade,token)[0],409)
grade.update(score=19,expectedRecordedAt=saved['recordedAt'])
check('grade_edit',admin.call(gradepath,'PUT',grade,token)[0],200)
own=next(c for c in student.call('/api/v1/grades')[1] if c['courseId']==cid)
check('student_reads_django_grade',own['grades'][0]['score'],19)
check('timetable_remove',admin.call('/api/admin/v1/timetable/'+slot['id'],'DELETE',{},token)[0],200)
check('schedule_revocation_shared',any(s['id']==slot['id'] for s in student.call('/api/v1/agenda')[1]['timetable']),False)
check('enrollment_cleanup',admin.call(path,'PUT',dict(username='alumno',enrolled=False),token)[0],200)
check('grade_hidden_after_revocation',any(c['courseId']==cid for c in student.call('/api/v1/grades')[1]),False)
out=Path(os.environ['MAIN_SCHEDULING_EVIDENCE']);out.parent.mkdir(parents=True,exist_ok=True)
out.write_text(json.dumps(dict(courseId=cid,passed=len(checks),checks=checks),indent=2),encoding='utf-8')
print(f'{len(checks)} comprobaciones reales de horarios y notas correctas.')
