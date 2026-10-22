"""Ensayo aislado de moderación Django y publicación Spring con cuentas sintéticas nuevas."""
import json,os,time,uuid,urllib.request,urllib.error,struct,zlib
from pathlib import Path
from verificar_arquitectura_compartida import Session

checks=[]
def check(name,actual,expected):
    assert actual==expected,(name,actual,expected)
    checks.append(dict(name=name,passed=True))
def multipart(session,path,fields,token,file=None):
    boundary='MAIN'+uuid.uuid4().hex;parts=[]
    for key,value in fields.items():
        parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"\r\n\r\n{value}\r\n'.encode())
    if file:
        name,mime,data=file
        parts.extend([f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{name}"\r\nContent-Type: {mime}\r\n\r\n'.encode(),data,b'\r\n'])
    parts.append(f'--{boundary}--\r\n'.encode())
    req=urllib.request.Request(session.base+path,data=b''.join(parts),headers={'Content-Type':'multipart/form-data; boundary='+boundary,token['headerName']:token['token']})
    try:r=session.opener.open(req,timeout=30)
    except urllib.error.HTTPError as ex:r=ex
    return r.status,json.loads(r.read())
def download(session,path):
    try:r=session.opener.open(session.base+path,timeout=20)
    except urllib.error.HTTPError as ex:r=ex
    return r.status,r.read(),r.headers
def png():
    def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',2,2,8,2,0,0,0))+chunk(b'IDAT',zlib.compress((b'\x00'+b'\x00\x88\x99'*2)*2))+chunk(b'IEND',b'')
admin=Session('http://127.0.0.1:8090');_,token=admin.call('/api/admin/csrf')
check('admin_login',admin.call('/api/admin/login','POST',dict(username='android_test_admin',password=os.environ['MAIN_TEST_ADMIN_PASSWORD']),token,True)[0],200)
_,token=admin.call('/api/admin/csrf')
seed=int(time.time()*1000);names=['modsender_'+str(seed),'modrecipient_'+str(seed)]
users=[]
for i,name in enumerate(names):
    check('synthetic_account_'+str(i),admin.call('/api/admin/v1/users','POST',dict(username=name,name='Moderación sintética '+str(i),role='ALUMNO',password='SyntheticModeration2026!',alumnoId=seed+i),token)[0],201)
    session=Session('http://127.0.0.1:8088');_,csrf=session.call('/login-token')
    check('spring_login_'+str(i),session.call('/login','POST',dict(username=name,password='SyntheticModeration2026!',**{csrf['parameterName']:csrf['token']}),form=True)[0],200)
    _,csrf=session.call('/login-token');csrf['headerName']='X-CSRF-TOKEN';users.append((session,csrf))
check('contact_authorized',admin.call('/api/admin/v1/contacts','POST',dict(first=names[0],second=names[1]),token)[0],200)
sender,csrf=users[0];recipient,_=users[1]
status,conv=sender.call('/api/v1/conversations/direct','POST',dict(username=names[1]),csrf);check('private_conversation',status,201)
path='/api/v1/conversations/'+conv['id']+'/messages'
status,msg=sender.call(path,'POST',dict(clientKey=str(uuid.uuid4()),text='Texto sintético pendiente'),csrf);check('pending_message',status,202)
check('pending_hidden',recipient.call(path)[1],[])
reviewpath='/api/admin/v1/moderation/messages/'+msg['id']+'/review'
decision=dict(revision=msg['revision'],contentHash=msg['contentHash'],approved=False,reason='Rechazo sintético para verificar corrección')
check('reject_django',admin.call(reviewpath,'POST',decision,token)[0],200)
check('rejected_hidden',recipient.call(path)[1],[])
status,msg=sender.call('/api/v1/messages/'+msg['id'],'PUT',dict(expectedRevision=msg['revision'],text='Texto sintético corregido'),csrf);check('correct_message',status,200)
check('old_review_conflict',admin.call(reviewpath,'POST',decision,token)[0],409)
decision.update(revision=msg['revision'],contentHash=msg['contentHash'],approved=True,reason='')
check('approve_current_revision',admin.call(reviewpath,'POST',decision,token)[0],200)
visible=recipient.call(path)[1]
check('corrected_message_visible',visible[0]['correctedBeforeDelivery'],True)
check('notification_generated',len(recipient.call('/api/v1/notifications')[1]),1)
status,attached=multipart(sender,path,dict(clientKey=str(uuid.uuid4()),text='Archivo sintético'),csrf,('prueba.txt','text/plain',b'Documento sintetico MAIN\n'))
check('pending_attachment',status,202)
attachment='/api/admin/v1/moderation/messages/'+attached['id']+'/attachment?revision=1'
code,data,headers=download(admin,attachment);check('admin_download',code,200);check('attachment_bytes',data,b'Documento sintetico MAIN\n')
check('no_store','no-store' in headers.get('Cache-Control',''),True)
check('anonymous_no_attachment',download(Session(admin.base),attachment)[0],401)
decision=dict(revision=1,contentHash=attached['contentHash'],approved=True,attachmentReviewed=False)
reviewpath='/api/admin/v1/moderation/messages/'+attached['id']+'/review'
check('explicit_attachment_required',admin.call(reviewpath,'POST',decision,token)[0],400)
decision['attachmentReviewed']=True
check('approve_attachment',admin.call(reviewpath,'POST',decision,token)[0],200)
check('recipient_attachment_visible',download(recipient,'/api/v1/messages/'+attached['id']+'/attachment?revision=1')[0],200)
for kind,file in [('TEXT',None),('IMAGE',('pixel.png','image/png',png()))]:
    status,update=multipart(sender,'/api/v1/statuses',dict(clientKey=str(uuid.uuid4()),text='Estado sintético '+kind,background='#008899',audience='CONTACTS'),csrf,file)
    check('pending_status_'+kind,status,200)
    check('pending_status_hidden_'+kind,any(s['id']==update['id'] for s in recipient.call('/api/v1/statuses')[1]),False)
    statepath='/api/admin/v1/moderation/statuses/'+update['id']
    decision=dict(contentHash=update['contentHash'],approved=True)
    if file:
        check('status_media_download',download(admin,statepath+'/media')[0],200)
        check('status_explicit_review_required',admin.call(statepath+'/review','POST',decision,token)[0],400)
        decision['attachmentReviewed']=True
    check('status_approve_'+kind,admin.call(statepath+'/review','POST',decision,token)[0],200)
    check('approved_status_shared_'+kind,any(s['id']==update['id'] for s in recipient.call('/api/v1/statuses')[1]),True)
    check('repeat_status_conflict_'+kind,admin.call(statepath+'/review','POST',decision,token)[0],409)
    check('review_media_hidden_after_decision_'+kind,download(admin,statepath+'/media')[0],404)
out=Path(os.environ['MAIN_MODERATION_EVIDENCE']);out.parent.mkdir(parents=True,exist_ok=True)
out.write_text(json.dumps(dict(accounts=names,conversationId=conv['id'],passed=len(checks),checks=checks),indent=2),encoding='utf-8')
print(f'{len(checks)} comprobaciones reales de moderación correctas.')
