"""Metadata-only reviewer summary with synthetic video and controlled provider."""
import json,os,sys,time,uuid,urllib.request
from pathlib import Path
from datetime import timedelta
from types import SimpleNamespace
from unittest.mock import patch
repo=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(repo/'admin-django'));os.environ.setdefault('DJANGO_SETTINGS_MODULE','main_admin.settings')
import django
django.setup()
from administration.automatic_moderation import Assessment,process
from administration import moderation as human
from verificar_arquitectura_compartida import Session
admin=Session('http://127.0.0.1:8090');_,csrf=admin.call('/api/admin/csrf')
assert admin.call('/api/admin/login','POST',dict(username='android_test_admin',password=os.environ['MAIN_TEST_ADMIN_PASSWORD']),csrf,True)[0]==200
_,csrf=admin.call('/api/admin/csrf');stamp=int(time.time()*1000);owner='review_summary_'+str(stamp)
assert admin.call('/api/admin/v1/users','POST',dict(username=owner,name='Prueba resumen CONTROLADO',role='ALUMNO',password='SyntheticSummary2026!',alumnoId=stamp),csrf)[0]==201
media=(repo/'mobile/app/src/androidTest/assets/status-synthetic.mp4').read_bytes()
caption='DATO SINTETICO — evaluación con proveedor CONTROLADO, no resultado real de IA'
sender=Session('http://127.0.0.1:8088');_,login_token=sender.call('/login-token')
assert sender.call('/login','POST',dict(username=owner,password='SyntheticSummary2026!',**{login_token['parameterName']:login_token['token']}),form=True)[0]==200
_,sender_csrf=sender.call('/api/csrf');boundary='MAIN'+uuid.uuid4().hex
fields=dict(clientKey=str(uuid.uuid4()),text=caption,background='#008800',audience='PRIVATE')
parts=[f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"\r\n\r\n{value}\r\n'.encode() for key,value in fields.items()]
parts.extend([f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="synthetic.mp4"\r\nContent-Type: video/mp4\r\n\r\n'.encode(),media,f'\r\n--{boundary}--\r\n'.encode()])
request=urllib.request.Request(sender.base+'/api/v1/statuses',data=b''.join(parts),headers={'Content-Type':'multipart/form-data; boundary='+boundary,sender_csrf['headerName']:sender_csrf['token']})
with sender.opener.open(request,timeout=30) as response:created=json.loads(response.read())
item=created['id']
provider=SimpleNamespace(assess_media=lambda *args:Assessment('HUMAN_REQUIRED','controlled-only',('violence',),'VIDEO_SAMPLED_PARTIAL',1,2.0))
checks=[]
def check(name,condition):
    assert condition,name
    checks.append(dict(name=name,passed=True))
with patch.dict(os.environ,{'MAIN_MODERATION_MEDIA_ENABLED':'true'}):
    check('controlled_assessment',process('statuses',item,provider,True,'synthetic-summary-only')=='HUMAN_REQUIRED')
code,queue=admin.call('/api/admin/v1/moderation/statuses?limit=100')
entry=next(value for value in queue if value['id']==item)
summary=entry['automaticReview']
check('admin_queue',code==200)
check('partial_coverage',summary['coverage']=='VIDEO_SAMPLED_PARTIAL' and summary['humanRequired'])
check('metadata_only',summary['categories']==['violence'] and summary['sampledFrames']==1 and 'transcript' not in summary and 'model' not in summary)
check('not_published',human.rows('SELECT state FROM status_update WHERE id=%s',[item])[0]['state']=='PENDING')
check('no_notice',human.rows('SELECT COUNT(*) AS total FROM notification WHERE sender=%s',[owner])[0]['total']==0)
check('anonymous_denied',Session('http://127.0.0.1:8090').call('/api/admin/v1/moderation/statuses')[0]==401)
check('stale_hash_not_reused',human.assessment_summary('statuses',item,'different-version') is None)
target=Path(os.environ['MAIN_SUMMARY_EVIDENCE']);target.parent.mkdir(parents=True,exist_ok=True)
target.write_text(json.dumps(dict(synthetic=True,controlledProvider=True,realOpenAiRequest=False,passed=len(checks),checks=checks),indent=2),encoding='utf-8')
print(str(len(checks))+' comprobaciones MySQL/Django del resumen orientativo aprobadas; estado privado sin publicar.')
