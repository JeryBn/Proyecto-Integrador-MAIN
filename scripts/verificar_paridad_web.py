"""Verificación local tras reinicio; solo fixtures generados por preparar_paridad_web.py."""
import argparse,hashlib,json,os,urllib.request,urllib.error
from pathlib import Path
from datetime import datetime,timezone
from verificar_arquitectura_compartida import Session

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--fixtures',type=Path,required=True);p.add_argument('--result',type=Path,required=True);p.add_argument('--base',default='http://127.0.0.1:8088');a=p.parse_args()
 fixture=json.loads((a.fixtures/'fixtures.json').read_text(encoding='utf-8'));bridge=json.loads((a.fixtures/'bridge.json').read_text(encoding='utf-8'))
 assert all(name.startswith('web_') for name in fixture['accounts'].values()),'Solo identidades sintéticas'
 checks=[]
 def expect(name,actual,expected=True):
  checks.append(dict(name=name,passed=actual==expected));assert actual==expected,name
 def raw(client,path,method='GET',body=None,headers=None):
  request=urllib.request.Request(client.base+path,data=body if isinstance(body,bytes) else json.dumps(body).encode() if body is not None else None,method=method,headers={'Content-Type':'application/json',**(headers or {})})
  try:response=client.opener.open(request,timeout=20)
  except urllib.error.HTTPError as error:response=error
  return response.status,response.read()
 sessions={}
 for key,name in fixture['accounts'].items():
  client=Session(a.base);_,token=client.call('/login-token');code,_=client.call('/login','POST',dict(username=name,password=os.environ['MAIN_TEST_WEB_PASSWORD'],**{token['parameterName']:token['token']}),form=True)
  expect(key+'_login',code,200);sessions[key]=client
 student=sessions['student'];peer=sessions['peer'];outsider=sessions['outsider'];family=sessions['family'];conversation=bridge['conversationId']
 messages=peer.call(f'/api/v1/conversations/{conversation}/messages?limit=100')[1]
 message=next(m for m in messages if m['id']==bridge['webMessageId'])
 expect('corrected_revision_persists',message['revision'],2);expect('correction_mark_persists',message['correctedBeforeDelivery'])
 expect('cancelled_message_not_delivered',not any(m['text']=='WEB-PARITY mensaje para cancelar antes de entrega' for m in messages))
 expect('android_photo_persists',any(m['text'].startswith('WEB-PARITY foto enviada desde Android ') and m['attachmentKind']=='IMAGE' for m in messages))
 expect('outsider_chat_denied',outsider.call('/api/v1/conversations/'+conversation)[0],404)
 expect('outsider_messages_denied',outsider.call('/api/v1/conversations/'+conversation+'/messages')[0],404)
 code,data=raw(peer,f"/api/v1/messages/{bridge['textAttachmentId']}/attachment?revision=1")
 expect('txt_download_authorized',code,200);expect('txt_bytes_persist',b'WEB-PARITY documento' in data)
 expect('outsider_file_denied',raw(outsider,f"/api/v1/messages/{bridge['textAttachmentId']}/attachment?revision=1")[0],404)
 expect('revoked_group_denied',student.call('/api/v1/conversations/'+bridge['groupId'])[0],404)
 expect('promoted_group_admin_persists',peer.call('/api/v1/conversations/'+bridge['groupId'])[1]['administrator'])
 own=student.call('/api/v1/statuses')[1];other=peer.call('/api/v1/statuses')[1]
 private=next(s for s in own if s['text']=='WEB-PARITY imagen privada creada desde web')
 expect('private_status_not_shared',not any(s['id']==private['id'] for s in other))
 expect('private_status_media_denied',raw(peer,f"/api/v1/statuses/{private['id']}/media")[0],404)
 for kind in ('TEXT','VIDEO','AUDIO'):
  expect('shared_'+kind.lower()+'_persists',any(s['owner']==fixture['accounts']['student'] and s['kind']==kind for s in other))
 expect('outsider_status_empty',outsider.call('/api/v1/statuses')[1],[])
 _,csrf=family.call('/api/csrf')
 boundary='MAINWebParityRoleCheck';fields=dict(clientKey='family-cannot-publish',text='WEB-PARITY prueba negativa',background='#087f68',audience='PRIVATE')
 body=(''.join('--'+boundary+'\r\nContent-Disposition: form-data; name=\"'+key+'\"\r\n\r\n'+value+'\r\n' for key,value in fields.items())+'--'+boundary+'--\r\n').encode()
 expect('family_status_publish_denied',raw(family,'/api/v1/statuses','POST',body,{'Content-Type':'multipart/form-data; boundary='+boundary,csrf['headerName']:csrf['token']})[0],403)
 grade=next(g for c in student.call('/api/v1/grades')[1] if c['courseId']==fixture['courseId'] for g in c['grades'] if g['assessment']=='Evaluación web de prueba')
 expect('web_grade_persists',grade['score'],19)
 expect('family_verified_student',len(family.call('/api/v1/agenda')[1]['students']),1)
 expect('outsider_has_no_courses',outsider.call('/api/v1/agenda')[1]['courses'],[])
 _,csrf=student.call('/api/csrf');headers={csrf['headerName']:csrf['token'],'X-MAIN-Expected-User':fixture['accounts']['peer']}
 expect('stale_identity_read_rejected',raw(student,'/api/perfil',headers=headers)[0],409)
 expect('stale_identity_write_rejected',raw(student,'/api/v1/preferences','PUT',dict(darkTheme=True,notifications=False,statusAudience='PRIVATE'),headers)[0],409)
 expect('stale_identity_logout_rejected',raw(student,'/logout','POST',{},headers)[0],409)
 expect('correct_session_survives_stale_logout',student.call('/api/perfil')[0],200)
 expect('preferences_not_changed_by_stale_tab',student.call('/api/v1/preferences')[1],dict(darkTheme=False,notifications=True,statusAudience='CONTACTS'))
 asset=Path(__file__).resolve().parents[1]/'src/main/resources/static/assets/main.js'
 code,data=raw(student,'/assets/main.js');expect('packaged_asset_http_ok',code,200)
 expect('packaged_asset_equals_build',hashlib.sha256(data).hexdigest(),hashlib.sha256(asset.read_bytes()).hexdigest())
 a.result.parent.mkdir(parents=True,exist_ok=True);a.result.write_text(json.dumps(dict(timestampUTC=datetime.now(timezone.utc).isoformat(),checks=checks,passed=len(checks),limitation='Solo entorno local y datos sintéticos; no incluye aceptación ni éxito del proveedor IA.'),ensure_ascii=False,indent=2),encoding='utf-8')
 print(f'{len(checks)} comprobaciones posteriores al reinicio aprobadas.')
if __name__=='__main__':main()
