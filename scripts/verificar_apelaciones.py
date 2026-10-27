"""Loopback synthetic appeal workflow; browser drives request and independent resolution."""
import argparse,json,time,uuid,urllib.request
from pathlib import Path
from verificar_arquitectura_compartida import Session
parser=argparse.ArgumentParser();parser.add_argument('mode',choices=['prepare','open','resolved','statuses']);args=parser.parse_args()
state=Path('var/appeal-web-fixture.json'); evidence=Path('var/appeal-http.json')
PASSWORD='SyntheticAppealOwner2026!'
admin=Session('http://127.0.0.1:8090');_,csrf=admin.call('/api/admin/csrf')
assert admin.call('/api/admin/login','POST',dict(username='android_test_admin',password='SyntheticAndroidTest2026!'),csrf,True)[0]==200
_,csrf=admin.call('/api/admin/csrf')
def login(name):
    client=Session('http://127.0.0.1:8088');_,token=client.call('/login-token')
    assert client.call('/login','POST',dict(username=name,password=PASSWORD,**{token['parameterName']:token['token']}),form=True)[0]==200
    return client,client.call('/api/csrf')[1]
if args.mode=='prepare':
    stamp=int(time.time()*1000);owner='appeal_web_'+str(stamp);peer='appeal_peer_'+str(stamp)
    for index,name in enumerate([owner,peer]):
        assert admin.call('/api/admin/v1/users','POST',dict(username=name,name='Estudiante SINTETICO apelaciones',role='ALUMNO',password=PASSWORD,alumnoId=stamp+index),csrf)[0]==201
    assert admin.call('/api/admin/v1/contacts','POST',dict(first=owner,second=peer),csrf)[0]==200
    client,token=login(owner);receiver,receiver_token=login(peer)
    code,group=client.call('/api/v1/conversations/groups','POST',dict(title='Apelación WEB sintética'),token);assert code==201
    conversation=group['id']
    assert client.call('/api/v1/conversations/'+conversation+'/invitations','POST',dict(username=peer),token)[0]==200
    assert receiver.call('/api/v1/conversations/'+conversation+'/invitation','POST',dict(accept=True),receiver_token)[0]==200
    code,message=client.call('/api/v1/conversations/'+conversation+'/messages','POST',dict(clientKey=str(uuid.uuid4()),text='Mensaje SINTETICO para probar apelaciones web.'),token);assert code==202
    assert admin.call('/api/admin/v1/moderation/messages/'+message['id']+'/review','POST',dict(revision=1,contentHash=message['contentHash'],approved=False,reason='Regla sintética de prueba'),csrf)[0]==200
    state.write_text(json.dumps(dict(owner=owner,peer=peer,conversation=conversation,message=message['id'],hash=message['contentHash'])),encoding='utf-8')
    print('Fixture web sintético preparado. El navegador debe enviar la apelación.');raise SystemExit
value=json.loads(state.read_text());client,token=login(value['owner']);receiver,receiver_token=login(value['peer'])
code,history=client.call('/api/v1/appeals');assert code==200;appeal=next(a for a in history if a['subjectId']==value['message'])
checks=[] if not evidence.exists() else json.loads(evidence.read_text())['checks']
def check(name,condition):
    assert condition,name
    if not any(c['name']==name for c in checks):checks.append(dict(name=name,passed=True))
if args.mode=='open':
    check('browser_request_open',appeal['state']=='OPEN')
    check('peer_history_private',receiver.call('/api/v1/appeals')[1]==[])
    check('peer_cannot_appeal_others',receiver.call('/api/v1/appeals','POST',dict(kind='messages',subjectId=value['message'],revision=1,contentHash=value['hash'],reason='No soy propietario'),receiver_token)[0]==404)
    check('anonymous_denied',Session('http://127.0.0.1:8088').call('/api/v1/appeals')[0]==401)
    check('csrf_required',client.call('/api/v1/appeals','POST',dict(kind='messages',subjectId=value['message'],revision=1,contentHash=value['hash'],reason='Prueba sin CSRF'))[0]==403)
    check('retention_protected',admin.call('/api/admin/v1/moderation/messages/'+value['message']+'/retention-hold')[1]['active'])
    check('manual_release_cannot_override_appeal',admin.call('/api/admin/v1/moderation/messages/'+value['message']+'/retention-hold','POST',dict(active=False,reason='Prueba sintética'),csrf)[0]==409)
    check('original_reviewer_cannot_resolve',admin.call('/api/admin/v1/appeals/'+appeal['id']+'/resolve','POST',dict(outcome='RETURNED',contentHash=value['hash'],reason='Debe resolver otro revisor'),csrf)[0]==403)
    check('not_delivered_while_open',receiver.call('/api/v1/conversations/'+value['conversation']+'/messages')[1]==[])
elif args.mode=='resolved':
    check('independent_browser_resolution_visible_to_sender',appeal['state']=='RETURNED' and bool(appeal['decisionReason']))
    check('returned_does_not_publish',receiver.call('/api/v1/conversations/'+value['conversation']+'/messages')[1]==[])
    check('returned_is_pending',client.call('/api/v1/conversations/'+value['conversation']+'/messages')[1][0]['state']=='PENDING')
    check('duplicate_does_not_reopen',client.call('/api/v1/appeals','POST',dict(kind='messages',subjectId=value['message'],revision=1,contentHash=value['hash'],reason='Reintento'),token)[1]['state']=='RETURNED')
    check('second_human_review_succeeds',admin.call('/api/admin/v1/moderation/messages/'+value['message']+'/review','POST',dict(revision=1,contentHash=value['hash'],approved=True,reason=''),csrf)[0]==200)
    check('only_then_delivered',receiver.call('/api/v1/conversations/'+value['conversation']+'/messages')[1][0]['state']=='APPROVED')
    check('approval_retry_refused',admin.call('/api/admin/v1/moderation/messages/'+value['message']+'/review','POST',dict(revision=1,contentHash=value['hash'],approved=True,reason=''),csrf)[0]==409)
    notices=receiver.call('/api/v1/notifications')[1]
    check('single_notification',len(notices)==1)
else:
    reviewer=Session('http://127.0.0.1:8090');_,review_csrf=reviewer.call('/api/admin/csrf')
    reviewer_name=json.loads(Path('var/appeal-fixture.json').read_text())['reviewer']
    assert reviewer.call('/api/admin/login','POST',dict(username=reviewer_name,password='SyntheticAppealReviewer2026!'),review_csrf,True)[0]==200
    _,review_csrf=reviewer.call('/api/admin/csrf')
    boundary='MAIN'+uuid.uuid4().hex
    fields=dict(clientKey=str(uuid.uuid4()),text='Estado SINTETICO de prueba de apelación',background='#008800',audience='CONTACTS')
    payload=b''.join(f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"\r\n\r\n{value}\r\n'.encode() for key,value in fields.items())+f'--{boundary}--\r\n'.encode()
    request=urllib.request.Request(client.base+'/api/v1/statuses',data=payload,headers={'Content-Type':'multipart/form-data; boundary='+boundary,token['headerName']:token['token']})
    with client.opener.open(request,timeout=20) as response:status=json.loads(response.read())
    sid=status['id'];digest=status['contentHash']
    check('status_rejected',admin.call('/api/admin/v1/moderation/statuses/'+sid+'/review','POST',dict(contentHash=digest,approved=False,reason='Regla sintética'),csrf)[0]==200)
    code,sa=client.call('/api/v1/appeals','POST',dict(kind='statuses',subjectId=sid,revision=0,contentHash=digest,reason='Solicito otra revisión del estado sintético'),token)
    check('status_appeal_open',code==200 and sa['state']=='OPEN')
    check('status_appeal_protects_retention',admin.call('/api/admin/v1/moderation/statuses/'+sid+'/retention-hold')[1]['active'])
    check('status_private_while_appealed',not any(s['id']==sid for s in receiver.call('/api/v1/statuses')[1]))
    check('status_independent_resolution',reviewer.call('/api/admin/v1/appeals/'+sa['id']+'/resolve','POST',dict(outcome='RETURNED',contentHash=digest,reason='Revisar contexto sintético'),review_csrf)[0]==200)
    check('status_returned_stays_private',not any(s['id']==sid for s in receiver.call('/api/v1/statuses')[1]))
    check('status_requires_new_human_approval',admin.call('/api/admin/v1/moderation/statuses/'+sid+'/review','POST',dict(contentHash=digest,approved=True),csrf)[0]==200)
    check('status_only_visible_after_human_approval',any(s['id']==sid and s['state']=='APPROVED' for s in receiver.call('/api/v1/statuses')[1]))
evidence.write_text(json.dumps(dict(synthetic=True,realProviderUsed=False,passed=len(checks),checks=checks),indent=2),encoding='utf-8')
print(str(len(checks))+' comprobaciones HTTP reales de apelaciones aprobadas.')
