"""Metadata-only local verification. Real initial passwords arrive only via environment.
Real accounts are never activated or assigned a personal password by this test.
"""
import argparse,json,os,sys,time,uuid
from io import StringIO
from pathlib import Path
from unittest.mock import patch
from verificar_arquitectura_compartida import Session
parser=argparse.ArgumentParser();parser.add_argument('--configuration',type=Path,required=True);parser.add_argument('--cleanup',action='store_true');parser.add_argument('--real-only',action='store_true');args=parser.parse_args()
config=json.loads(args.configuration.read_text(encoding='utf-8-sig'))
assert config['database']=='main_architecture_review' and config['port']==3307,'Solo el entorno local aislado'
os.environ.update(MAIN_DB_NAME=config['database'],MAIN_DB_HOST='127.0.0.1',MAIN_DB_PORT=str(config['port']),MAIN_DB_USER='main_migration',MAIN_DB_PASSWORD=config['migration_password'],MAIN_DJANGO_SECRET_KEY=config['django_secret'],DJANGO_SETTINGS_MODULE='main_admin.settings')
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'admin-django'))
import django
django.setup()
from django.core.management import call_command
from administration.models import SharedUser
from administration import moderation as human
fixture=Path('var/reviewer-http-fixture.json');evidence=Path('var/reviewer-http-results.json')
if args.cleanup:
    value=json.loads(fixture.read_text())
    names=[value['first'],value['second'],value['initial']]
    assert all(name.startswith('review_test_') for name in names)
    SharedUser.objects.filter(username__in=names,role='REVIEWER').update(enabled=False)
    assert not SharedUser.objects.filter(username__in=names,enabled=True).exists()
    report=json.loads(evidence.read_text());report['syntheticReviewersDisabled']=True;evidence.write_text(json.dumps(report,indent=2))
    print('Tres revisores sintéticos deshabilitados; cuentas de Jery/Michael sin cambiar.');raise SystemExit

checks=[]
def check(name,value):
    assert value,name
    checks.append(dict(name=name,passed=True))
def operator(name,password):
    client=Session('http://127.0.0.1:8090');_,token=client.call('/api/admin/csrf')
    code,value=client.call('/api/admin/login','POST',dict(username=name,password=password),token,True)
    assert code==200,'operator_login'
    return client,client.call('/api/admin/csrf')[1],value
for name,key in [('jery.revision','MAIN_JERY_REVIEWER_PASSWORD'),('michael.revision','MAIN_MICHAEL_REVIEWER_PASSWORD')]:
    client,token,profile=operator(name,os.environ[key])
    check(name+'_initial_login',profile['rol']=='REVIEWER' and profile['mustChangePassword'])
    check(name+'_content_locked_until_personal_password',client.call('/api/admin/v1/moderation/messages')[0]==403)
    for path in ('users','courses','audit'):
        check(name+'_denied_'+path,client.call('/api/admin/v1/'+path)[0]==403)
    check(name+'_denial_does_not_destroy_session',client.call('/api/admin/perfil')[0]==200)
    check(name+'_password_still_initial',SharedUser.objects.get(username=name).password_change_required)
    user_side=Session('http://127.0.0.1:8088');_,spring_csrf=user_side.call('/login-token')
    status,_=user_side.call('/login','POST',dict(username=name,password=os.environ[key],**{spring_csrf['parameterName']:spring_csrf['token']}),form=True)
    check(name+'_student_portal_denied',status==403)
    check(name+'_student_api_denied',user_side.call('/api/v1/contacts')[0]==403)
    client.call('/api/admin/logout','POST',{},token)

if args.real_only:
    report=json.loads(evidence.read_text()) if evidence.exists() else dict(realAccountsPasswordChanged=False,syntheticReviewWorkflow=False,syntheticReviewersDisabled=False,checks=[])
    by_name={value['name']:value for value in report['checks']}
    by_name.update({value['name']:value for value in checks})
    report.update(checks=list(by_name.values()),passed=len(by_name));evidence.write_text(json.dumps(report,indent=2))
    print(str(len(by_name))+' comprobaciones acumuladas; permisos reales verificados sin cambiar contraseñas.');raise SystemExit

stamp=int(time.time()*1000);initial='SyntheticReviewInitial2026!';personal='SyntheticReviewPersonal2026!'
names=['review_test_'+str(stamp)+'_'+letter for letter in ('a','b','initial')]
for name in names:
    with patch.dict(os.environ,{'MAIN_REVIEWER_INITIAL_PASSWORD':initial}):call_command('provision_reviewer',username=name,name='Revisor SINTETICO '+name[-1],stdout=StringIO())
first,t1,_=operator(names[0],initial);old_session,_,_=operator(names[0],initial)
check('first_password_requires_csrf',first.call('/api/admin/reviewer/password','POST',dict(currentPassword=initial,newPassword=personal))[0]==403)
check('synthetic_personal_password_saved',first.call('/api/admin/reviewer/password','POST',dict(currentPassword=initial,newPassword=personal),t1)[0]==200)
_,t1=first.call('/api/admin/csrf')
check('old_session_revoked',old_session.call('/api/admin/perfil')[0]==401)
second,t2,_=operator(names[1],initial)
check('second_synthetic_personal_password_saved',second.call('/api/admin/reviewer/password','POST',dict(currentPassword=initial,newPassword=personal),t2)[0]==200)
_,t2=second.call('/api/admin/csrf')
for name,client in [('first',first),('second',second)]:
    check(name+'_can_read_review',client.call('/api/admin/v1/moderation/messages')[0]==200)
    check(name+'_no_account_creation',client.call('/api/admin/v1/users','POST',dict(username='not_created',role='ADMIN'),client.call('/api/admin/csrf')[1])[0]==403)
    check(name+'_no_grade_mutation',client.call('/api/admin/v1/courses/1/grades','PUT',{},client.call('/api/admin/csrf')[1])[0]==403)
    check(name+'_no_general_audit',client.call('/api/admin/v1/audit')[0]==403)
    check(name+'_denial_preserves_profile',client.call('/api/admin/perfil')[0]==200)
admin,admin_token,_=operator('android_test_admin','SyntheticAndroidTest2026!')
owner='review_owner_'+str(stamp)
check('synthetic_student_created',admin.call('/api/admin/v1/users','POST',dict(username=owner,name='Estudiante SINTETICO revisión limitada',role='ALUMNO',password='SyntheticReviewOwner2026!',alumnoId=stamp),admin_token)[0]==201)
student=Session('http://127.0.0.1:8088');_,login=student.call('/login-token')
assert student.call('/login','POST',dict(username=owner,password='SyntheticReviewOwner2026!',**{login['parameterName']:login['token']}),form=True)[0]==200
_,student_token=student.call('/api/csrf')
code,group=student.call('/api/v1/conversations/groups','POST',dict(title='Revisores limitados SINTETICO'),student_token);assert code==201
code,message=student.call('/api/v1/conversations/'+group['id']+'/messages','POST',dict(clientKey=str(uuid.uuid4()),text='Mensaje SINTETICO para probar revisores limitados'),student_token);assert code==202
review=dict(revision=1,contentHash=message['contentHash'],approved=False,reason='Regla sintética de revisión')
check('limited_reviewer_can_reject',first.call('/api/admin/v1/moderation/messages/'+message['id']+'/review','POST',review,t1)[0]==200)
code,appeal=student.call('/api/v1/appeals','POST',dict(kind='messages',subjectId=message['id'],revision=1,contentHash=message['contentHash'],reason='Contexto educativo sintético'),student_token)
check('student_can_appeal_limited_review',code==200 and appeal['state']=='OPEN')
decision=dict(outcome='RETURNED',contentHash=message['contentHash'],reason='Segunda revisión independiente sintética')
check('original_limited_reviewer_cannot_resolve',first.call('/api/admin/v1/appeals/'+appeal['id']+'/resolve','POST',decision,t1)[0]==403)
check('second_limited_reviewer_can_resolve',second.call('/api/admin/v1/appeals/'+appeal['id']+'/resolve','POST',decision,t2)[0]==200)
check('return_keeps_unpublished',human.rows('SELECT published_revision FROM message WHERE id=%s',[message['id']])[0]['published_revision'] is None)
check('human_approval_still_required',first.call('/api/admin/v1/moderation/messages/'+message['id']+'/review','POST',dict(review,approved=True,reason=''),t1)[0]==200)
check('published_after_human_approval',human.rows('SELECT published_revision FROM message WHERE id=%s',[message['id']])[0]['published_revision']==1)
check('student_history_private_resolution',student.call('/api/v1/appeals')[1][0]['state']=='RETURNED')
fixture.write_text(json.dumps(dict(first=names[0],second=names[1],initial=names[2]),indent=2))
result=dict(syntheticReviewWorkflow=True,realAccountsPasswordChanged=False,syntheticReviewersDisabled=False,passed=len(checks),checks=checks)
evidence.write_text(json.dumps(result,indent=2));print(str(len(checks))+' comprobaciones HTTP/MySQL de cuentas y revisión limitada aprobadas; claves reales sin cambiar.')
