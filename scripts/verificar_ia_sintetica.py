"""Explicit, bounded provider recheck with synthetic educational content only.

Reuses the configured server key. Does not create keys, change billing, activate
moderation workers, process queues or include real academic records.
"""
import json
import os
from pathlib import Path
import subprocess
import sys
import uuid
from verificar_arquitectura_compartida import Session

root=Path(__file__).resolve().parents[1]
client=Session('http://127.0.0.1:8088');_,token=client.call('/login-token')
assert client.call('/login','POST',dict(username='vacio',password=os.environ['MAIN_TEST_STUDENT_PASSWORD'],**{token['parameterName']:token['token']}),form=True)[0]==200
_,csrf=client.call('/api/csrf')
code,reply=client.call('/api/v1/assistant','POST',dict(clientKey=str(uuid.uuid4()),question='Prueba sintética: explica brevemente cuánto es dos más dos, sin usar datos personales.'),csrf)
assistant=dict(status=code,success=code==200 and bool(reply.get('answer','').strip()),
               errorCode=reply.get('error',{}).get('code'),model=reply.get('model'),answerCharacters=len(reply.get('answer','')))
client.call('/logout','POST',{},csrf)
child_env=dict(os.environ)
if not child_env.get('OPENAI_API_KEY'):
    source=root/'.env.local'
    if source.is_file():
        for line in source.read_text(encoding='utf-8-sig').splitlines():
            if line.startswith('OPENAI_API_KEY='):
                child_env['OPENAI_API_KEY']=line.split('=',1)[1].strip().strip('"\'')
                break
moderation=dict(attempted=False)
if child_env.get('OPENAI_API_KEY'):
    run=subprocess.run([sys.executable,'manage.py','moderate_pending','--synthetic-probe','--settings=main_admin.test_settings'],cwd=root/'admin-django',env=child_env,capture_output=True,text=True,timeout=45)
    moderation=dict(attempted=True,success=run.returncode==0)
    if run.returncode==0:
        value=json.loads(run.stdout.strip().splitlines()[-1]);moderation.update(outcome=value['outcome'],model=value['model'])
    else:
        # Provider exceptions are translated to bounded, sanitized service codes.
        import re
        match=re.search(r'(?:QUOTA|RATE_OR_QUOTA|RATE_LIMIT|PROVIDER|AUTH)[A-Z_]*(?::[0-9]+)?',run.stderr)
        moderation['errorCode']=match.group(0) if match else 'PROVIDER_PROBE_FAILED'
result=dict(date='2026-10-30',synthetic=True,existingKeyReused=True,realProvider=True,
            realAcademicRecordsIncluded=False,moderationQueueProcessed=False,assistant=assistant,moderation=moderation)
out=root/'docs/informes/2026-10-30-ia/results.json';out.parent.mkdir(parents=True,exist_ok=True)
out.write_text(json.dumps(result,indent=2),encoding='utf-8')
print(json.dumps(result))
