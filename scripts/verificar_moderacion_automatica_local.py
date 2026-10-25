"""Ensayo sintético MySQL/Django/Spring con proveedor CONTROLADO, nunca contenido real.

No habilita política de producción ni llama a OpenAI. La prueba real del proveedor
es moderate_pending --synthetic-probe; este script comprueba la publicación cruzada.
"""
import json,os,sys,time,uuid
from pathlib import Path
from types import SimpleNamespace
repo=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(repo/'admin-django'));os.environ.setdefault('DJANGO_SETTINGS_MODULE','main_admin.settings')
import django
django.setup()
from administration.automatic_moderation import Assessment,process
from verificar_arquitectura_compartida import Session

admin=Session('http://127.0.0.1:8090');_,csrf=admin.call('/api/admin/csrf')
assert admin.call('/api/admin/login','POST',dict(username='android_test_admin',password=os.environ['MAIN_TEST_ADMIN_PASSWORD']),csrf,True)[0]==200
_,csrf=admin.call('/api/admin/csrf')
stamp=int(time.time()*1000);names=['automod_sender_'+str(stamp),'automod_recipient_'+str(stamp)]
for index,name in enumerate(names):
    assert name.startswith('automod_')
    assert admin.call('/api/admin/v1/users','POST',dict(username=name,name='Prueba automática sintética',role='ALUMNO',password='SyntheticAutoModeration2026!',alumnoId=stamp+index),csrf)[0]==201
assert admin.call('/api/admin/v1/contacts','POST',dict(first=names[0],second=names[1]),csrf)[0]==200
sessions=[]
for name in names:
    client=Session('http://127.0.0.1:8088');_,token=client.call('/login-token')
    assert client.call('/login','POST',dict(username=name,password='SyntheticAutoModeration2026!',**{token['parameterName']:token['token']}),form=True)[0]==200
    _,token=client.call('/api/csrf');sessions.append((client,token))
sender,token=sessions[0];recipient,_=sessions[1]
status,conversation=sender.call('/api/v1/conversations/direct','POST',dict(username=names[1]),token);assert status==201
path='/api/v1/conversations/'+conversation['id']+'/messages'
status,message=sender.call(path,'POST',dict(clientKey=str(uuid.uuid4()),text='Contenido sintético para comprobar revisión automática'),token);assert status==202
checks=[]
def check(name,condition):
    assert condition,name
    checks.append(dict(name=name,passed=True))
check('pending_hidden',recipient.call(path)[1]==[])
provider=SimpleNamespace(assess=lambda *args:Assessment('CLEAR','controlled-test-only'))
check('shadow_clear',process('messages',message['id'],provider,False,'synthetic-test-only')=='CLEAR_SHADOW')
check('shadow_not_published',recipient.call(path)[1]==[])
check('apply_shared_path',process('messages',message['id'],provider,True,'synthetic-test-only')=='APPROVED')
check('spring_sees_approved',len(recipient.call(path)[1])==1)
check('notification_once',len(recipient.call('/api/v1/notifications')[1])==1)
check('repeated_apply_stale',process('messages',message['id'],provider,True,'synthetic-test-only')=='STALE')
check('notification_not_duplicated',len(recipient.call('/api/v1/notifications')[1])==1)
result=dict(controlledProvider=True,realOpenAiRequest=False,productionPolicyEnabled=False,passed=len(checks),checks=checks)
out=Path(os.environ['MAIN_AUTO_MODERATION_EVIDENCE']);out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(result,indent=2),encoding='utf-8')
print(str(len(checks))+' comprobaciones sintéticas MySQL/Django/Spring aprobadas con proveedor controlado.')
