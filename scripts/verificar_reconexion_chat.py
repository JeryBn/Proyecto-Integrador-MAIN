"""Real Spring/Django/MySQL with disposable users and the production JS read poller.

Explicit local test. MAIN_TEST_ADMIN_PASSWORD is required; secrets go to child stdin,
never a file, argument, log or evidence. Does not interrupt the user's servers/network.
"""
import concurrent.futures
import http.cookiejar
import json
import os
from pathlib import Path
import subprocess
import time
import uuid
import urllib.request
from verificar_arquitectura_compartida import Session

def main():
    admin=Session('http://127.0.0.1:8090');_,csrf=admin.call('/api/admin/csrf')
    assert admin.call('/api/admin/login','POST',dict(username='android_test_admin',password=os.environ['MAIN_TEST_ADMIN_PASSWORD']),csrf,True)[0]==200
    _,csrf=admin.call('/api/admin/csrf')
    stamp=int(time.time()*1000);names=['reconnect_'+str(stamp)+'_'+str(i) for i in range(2)]
    created=[];checks=[]
    def check(name,condition):
        assert condition,name
        checks.append(dict(name=name,passed=True))
    def login(name):
        client=Session('http://127.0.0.1:8088');_,token=client.call('/login-token')
        assert client.call('/login','POST',dict(username=name,password='SyntheticReconnect2026!',**{token['parameterName']:token['token']}),form=True)[0]==200
        return client,client.call('/api/csrf')[1]
    try:
        for i,name in enumerate(names):
            check('create_synthetic_'+str(i),admin.call('/api/admin/v1/users','POST',dict(username=name,name='PRUEBA SINTETICA reconexión',role='ALUMNO',password='SyntheticReconnect2026!',alumnoId=stamp+i),csrf)[0]==201)
            created.append(name)
        check('contact',admin.call('/api/admin/v1/contacts','POST',dict(first=names[0],second=names[1]),csrf)[0]==200)
        sender,token=login(names[0]);receiver,receiver_token=login(names[1])
        code,group=sender.call('/api/v1/conversations/groups','POST',dict(title='PRUEBA SINTETICA reconexión prolongada'),token);check('group',code==201)
        path='/api/v1/conversations/'+group['id']
        check('invite',sender.call(path+'/invitations','POST',dict(username=names[1]),token)[0]==200)
        check('accept',receiver.call(path+'/invitation','POST',dict(accept=True),receiver_token)[0]==200)
        def send_and_review(label):
            body=dict(clientKey=str(uuid.uuid4()),text='PRUEBA SINTETICA '+label)
            code,message=sender.call(path+'/messages','POST',body,token);check('pending_'+label,code==202)
            check('retry_same_id_'+label,sender.call(path+'/messages','POST',body,token)[1]['id']==message['id'])
            check('human_review_'+label,admin.call('/api/admin/v1/moderation/messages/'+message['id']+'/review','POST',dict(revision=1,contentHash=message['contentHash'],approved=True,reason=''),csrf)[0]==200)
        send_and_review('before_outage')
        jar=next(h.cookiejar for h in receiver.opener.handlers if isinstance(h,urllib.request.HTTPCookieProcessor))
        fixture=dict(path=path+'/messages',cookie='; '.join(c.name+'='+c.value for c in jar))
        def during_outage():
            time.sleep(30);send_and_review('during_outage')
        with concurrent.futures.ThreadPoolExecutor(max_workers=1) as pool:
            future=pool.submit(during_outage)
            run=subprocess.run(['node',str(Path(__file__).with_suffix('.mjs'))],input=json.dumps(fixture),text=True,capture_output=True,timeout=100)
            future.result()
        if run.returncode:
            Path('var/reconnect-js-error.log').write_text(run.stderr.replace(fixture['cookie'],'[SESSION REDACTED]'),encoding='utf-8')
            raise RuntimeError('Falló verificador JS de reconexión; no se publican sesiones ni entradas privadas.')
        result=json.loads(run.stdout)
        check('backend_exactly_two_messages',len(receiver.call(path+'/messages')[1])==2)
        check('backend_exactly_two_notices',len(receiver.call('/api/v1/notifications')[1])==2)
    finally:
        for name in created:
            check('disable_'+name.rsplit('_',1)[-1],admin.call('/api/admin/v1/users/'+name+'/disable','POST',{},csrf)[0]==200)
    result.update(synthetic=True,backend='Spring/Django shared local MySQL',httpChecks=checks,httpPassed=len(checks),syntheticUsersDisabled=True)
    output=Path('docs/informes/2026-10-30-reconexion/results.json');output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps(result,indent=2),encoding='utf-8')
    print(f'Reconexión: {result["passed"]} condiciones JS y {len(checks)} HTTP aprobadas; cuentas sintéticas deshabilitadas.')

if __name__=='__main__':main()
