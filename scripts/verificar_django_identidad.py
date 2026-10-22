"""Recorrido sintético de cuentas, parentescos, contactos y revocación entre backends."""
import os, time, json
from pathlib import Path
from verificar_arquitectura_compartida import Session
admin=Session('http://127.0.0.1:8090')
_,token=admin.call('/api/admin/csrf')
assert admin.call('/api/admin/login','POST',dict(username='android_test_admin',password=os.environ['MAIN_TEST_ADMIN_PASSWORD']),token,True)[0]==200
_,token=admin.call('/api/admin/csrf')
checks=[]
def check(name,actual,expected):
    assert actual==expected,(name,actual,expected)
    checks.append(dict(name=name,passed=True))
suffix=str(int(time.time()*1000))
name='family_'+suffix
password='SyntheticFamily2026!'
check('create_family',admin.call('/api/admin/v1/users','POST',dict(username=name,name='Familiar sintético Django',role='FAMILIAR',password=password),token)[0],201)
check('no_admin_creation',admin.call('/api/admin/v1/users','POST',dict(username='forbidden_'+suffix,name='Prueba',role='ADMIN',password=password),token)[0],400)
def login(session,credential):
    _,csrf=session.call('/login-token')
    return session.call('/login','POST',dict(username=name,password=credential,**{csrf['parameterName']:csrf['token']}),form=True)[0]
family=Session('http://127.0.0.1:8088')
check('spring_login_django_account',login(family,password),200)
check('family_initially_private',family.call('/api/v1/agenda')[1]['students'],[])
check('guardian_granted',admin.call('/api/admin/v1/guardians','POST',dict(guardian=name,studentId=1),token)[0],200)
check('guardian_visible_in_spring',family.call('/api/v1/agenda')[1]['students'][0]['id'],1)
check('guardian_revoked',admin.call(f'/api/admin/v1/guardians/{name}/1','DELETE',{},token)[0],200)
check('guardian_revocation_immediate',family.call('/api/v1/agenda')[1]['students'],[])
check('approve_contact',admin.call('/api/admin/v1/contacts','POST',dict(first=name,second='alumno'),token)[0],200)
check('contact_visible',any(c['username']=='alumno' for c in family.call('/api/v1/contacts')[1]),True)
replacement='ReplacementSynthetic2026!'
check('password_reset',admin.call(f'/api/admin/v1/users/{name}/reset-password','POST',dict(password=replacement),token)[0],200)
check('old_spring_cookie_revoked',family.call('/api/perfil')[0],401)
fresh=Session('http://127.0.0.1:8088')
check('replacement_login',login(fresh,replacement),200)
check('disable_django',admin.call(f'/api/admin/v1/users/{name}/disable','POST',{},token)[0],200)
check('disabled_cookie_revoked',fresh.call('/api/v1/agenda')[0],401)
check('admin_account_protected',admin.call('/api/admin/v1/users/android_test_admin/disable','POST',{},token)[0],403)
check('reports_available',admin.call('/api/admin/v1/reports')[0],200)
destination=Path(os.environ['MAIN_IDENTITY_EVIDENCE']);destination.parent.mkdir(parents=True,exist_ok=True)
destination.write_text(json.dumps(dict(syntheticAccount=name,passed=len(checks),checks=checks),indent=2),encoding='utf-8')
print(f'{len(checks)} comprobaciones de identidad reales correctas.')
