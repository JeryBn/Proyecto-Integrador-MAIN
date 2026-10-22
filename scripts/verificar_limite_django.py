import json,os
from pathlib import Path
from verificar_arquitectura_compartida import Session
spring=Session('http://127.0.0.1:8088');django=Session('http://127.0.0.1:8090')
checks=[]
for path in ('/api/admin/csrf','/api/admin/v1/users','/api/admin/v1/moderation/messages','/moderation.html'):
    code,_=spring.call(path)
    assert code==410,(path,code)
    checks.append(dict(name='spring_closed_'+path,passed=True))
assert django.call('/api/admin/csrf')[0]==200
checks.append(dict(name='django_csrf_available',passed=True))
assert django.call('/api/admin/v1/users')[0]==401
checks.append(dict(name='django_accounts_require_admin',passed=True))
Path(os.environ['MAIN_BOUNDARY_EVIDENCE']).write_text(json.dumps(dict(passed=len(checks),checks=checks),indent=2),encoding='utf-8')
print(f'{len(checks)} comprobaciones del límite Django/Spring correctas.')
