"""Explicit synthetic pagination and privacy check over real Django/Spring/MySQL.

Creates three disposable users and 35 text messages, human-approves 33, checks
pages and revocation, and disables all users. No AI, FCM or real user data.
"""
import json
import os
from pathlib import Path
import time
import uuid
from datetime import datetime
from verificar_arquitectura_compartida import Session

def main():
    checks=[];created=[];stamp=int(time.time()*1000)
    names=['pagination_'+str(stamp)+'_'+str(i) for i in range(3)]
    def check(name,condition):
        assert condition,name
        checks.append(dict(name=name,passed=True))
    admin=Session('http://127.0.0.1:8090');_,csrf=admin.call('/api/admin/csrf')
    assert admin.call('/api/admin/login','POST',dict(username='android_test_admin',password=os.environ['MAIN_TEST_ADMIN_PASSWORD']),csrf,True)[0]==200
    _,csrf=admin.call('/api/admin/csrf')
    def login(name):
        session=Session('http://127.0.0.1:8088');_,token=session.call('/login-token')
        assert session.call('/login','POST',dict(username=name,password='SyntheticPagination2026!',**{token['parameterName']:token['token']}),form=True)[0]==200
        return session,session.call('/api/csrf')[1]
    def pages(session,path,limit=7):
        values=[];count=0
        for offset in range(0,100,limit):
            code,batch=session.call(path+f'?offset={offset}&limit={limit}')
            assert code==200 and isinstance(batch,list) and len(batch)<=limit,'page contract'
            count+=1;values+=batch
            if len(batch)<limit:return values,count
        raise AssertionError('Unexpected unbounded pagination')
    try:
        for index,name in enumerate(names):
            code,_=admin.call('/api/admin/v1/users','POST',dict(username=name,name='PRUEBA SINTETICA paginación',role='ALUMNO',password='SyntheticPagination2026!',alumnoId=stamp+index),csrf)
            check('create_synthetic_'+str(index),code==201);created.append(name)
        check('approve_contact',admin.call('/api/admin/v1/contacts','POST',dict(first=names[0],second=names[1]),csrf)[0]==200)
        sender,token=login(names[0]);receiver,receiver_token=login(names[1]);outsider,_=login(names[2])
        code,group=sender.call('/api/v1/conversations/groups','POST',dict(title='PRUEBA SINTETICA paginación 35 mensajes'),token);check('create_group',code==201)
        path='/api/v1/conversations/'+group['id']
        check('invite_receiver',sender.call(path+'/invitations','POST',dict(username=names[1]),token)[0]==200)
        check('accept_invitation',receiver.call(path+'/invitation','POST',dict(accept=True),receiver_token)[0]==200)
        messages=[]
        for index in range(35):
            code,message=sender.call(path+'/messages','POST',dict(clientKey=str(uuid.uuid4()),text=f'PRUEBA SINTETICA de paginación número {index}'),token)
            assert code==202;messages.append(message)
        check('35_distinct_pending_messages',len({m['id'] for m in messages})==35)
        for message in messages[:33]:
            assert admin.call('/api/admin/v1/moderation/messages/'+message['id']+'/review','POST',dict(revision=1,contentHash=message['contentHash'],approved=True,reason=''),csrf)[0]==200
        seen,page_count=pages(receiver,path+'/messages')
        check('all_33_approved_across_pages',len(seen)==33 and {m['id'] for m in seen}=={m['id'] for m in messages[:33]})
        check('no_duplicates_or_pending_leak',len({m['id'] for m in seen})==33 and all(m['state']=='APPROVED' for m in seen))
        # The API selects newest windows, then returns each window chronologically
        # for chat display. Concatenating pages must not be treated as one DESC list.
        ordered=sorted(messages[:33],key=lambda m:(datetime.fromisoformat(m['createdAt']),m['id']),reverse=True)
        expected_ids=[m['id'] for offset in range(0,len(ordered),7) for m in reversed(ordered[offset:offset+7])]
        check('stable_newest_windows_chronological_inside_each_page',[m['id'] for m in seen]==expected_ids)
        check('private_review_fields_hidden',all(not m.get('reason') and not m.get('contentHash') for m in seen))
        sender_rows,_=pages(sender,path+'/messages')
        check('author_sees_own_35_including_two_pending',len(sender_rows)==35 and sum(m['state']=='PENDING' for m in sender_rows)==2)
        notices,notice_pages=pages(receiver,'/api/v1/notifications')
        check('33_unique_message_notices',len(notices)==33 and len({n['id'] for n in notices})==33 and all(n['kind']=='MESSAGE' for n in notices))
        check('outsider_denied_first_page',outsider.call(path+'/messages?offset=0&limit=7')[0]==404)
        check('outsider_denied_later_page',outsider.call(path+'/messages?offset=28&limit=7')[0]==404)
        for query in ('offset=-1&limit=7','offset=0&limit=0','offset=0&limit=101'):
            check('invalid_page_'+query,receiver.call(path+'/messages?'+query)[0]==400)
        check('remove_receiver',sender.call(path+'/members/'+names[1],'PUT',dict(administrator=False,remove=True),token)[0]==200)
        check('revoked_first_and_later_pages',all(receiver.call(path+'/messages?offset='+str(offset)+'&limit=7')[0]==404 for offset in (0,28)))
        check('revoked_message_notices_hidden',receiver.call('/api/v1/notifications')[1]==[])
    finally:
        for index,name in enumerate(created):
            check('disable_synthetic_'+str(index),admin.call('/api/admin/v1/users/'+name+'/disable','POST',{},csrf)[0]==200)
    result=dict(date='2026-10-30',synthetic=True,database='isolated local MySQL',realHttp=True,
                messagesCreated=35,messagesApproved=33,messagePages=page_count,notificationPages=notice_pages,
                syntheticUsersDisabled=True,realProviderUsed=False,passed=len(checks),checks=checks)
    target=Path('docs/informes/2026-10-30-carga/pagination.json');target.parent.mkdir(parents=True,exist_ok=True)
    target.write_text(json.dumps(result,indent=2),encoding='utf-8')
    print(f'{len(checks)} comprobaciones HTTP/MySQL de paginación y privacidad aprobadas; tres usuarios sintéticos deshabilitados.')

if __name__=='__main__':main()
