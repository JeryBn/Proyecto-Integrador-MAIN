"""Build the importable, credential-free Postman smoke collection."""
import json
from pathlib import Path
root=Path(__file__).resolve().parent.parent
out=root/'postman';out.mkdir(exist_ok=True)
items=[]
def request(name,base,path,status,method='GET',body=None,headers=None,extra=()):
    checks=[f"pm.test('Estado HTTP {status}', () => pm.response.to.have.status({status}));",*extra]
    req=dict(method=method,header=[dict(key=k,value=v) for k,v in (headers or {}).items()],url='{{'+base+'}}'+path)
    if body is not None:
        req['body']=dict(mode='urlencoded',urlencoded=[dict(key=k,value=v,type='text') for k,v in body.items()])
    items.append(dict(name=name,request=req,protocolProfileBehavior={'followRedirects':False},event=[dict(listen='test',script=dict(type='text/javascript',exec=checks))]))
def token(prefix):
    return [f"const t=pm.response.json(); pm.expect(t.token).to.be.a('string').and.not.empty; pm.collectionVariables.set('{prefix}Token',t.token); pm.collectionVariables.set('{prefix}Parameter',t.parameterName);"]
def secure_cookie(name):
    return ["pm.test('Cookie de sesión segura', () => {const cookies=pm.response.headers.all().filter(h=>h.key.toLowerCase()==='set-cookie'); const c=cookies.find(h=>h.value.startsWith('"+name+"=')); pm.expect(c,'cookie de sesión').to.exist; pm.expect(c.value).to.match(/;\\s*Secure/i); pm.expect(c.value).to.match(/;\\s*HttpOnly/i); pm.expect(c.value).to.match(/;\\s*SameSite=Lax/i);});"]
request('01 Acceso alumno público','userBase','/login.html',200)
request('02 Administración React pública','adminBase','/admin.html',200)
request('03 Sin sesión alumno','userBase','/api/actividades/pendientes',401)
request('04 Sin sesión administrativa','adminBase','/api/admin/perfil',401)
request('05 CSRF alumno','userBase','/login-token',200,extra=token('user'))
request('06 Acceso alumno','userBase','/login',302,'POST',dict(username='{{username}}',password='{{password}}',**{'{{userParameter}}':'{{userToken}}'}),extra=secure_cookie('JSESSIONID')+["pm.test('Redirección permanece HTTPS',()=>{pm.expect(pm.response.headers.get('Location')).not.to.match(/^http:\\/\\//i);});"])
request('07 Identidad autenticada','userBase','/api/v1/profile',200)
request('08 Pendientes autorizados','userBase','/api/actividades/pendientes',200,extra=["pm.test('Listado válido',()=>pm.expect(pm.response.json()).to.be.an('array')); "])
request('09 Detalle inexistente privado','userBase','/api/actividades/9223372036854775806',404)
request('10 Avisos del usuario','userBase','/api/v1/notifications',200)
request('11 Escritura sin CSRF rechazada','userBase','/api/v1/reminders/preferences',403,'PUT')
request('12 CSRF sesión alumno','userBase','/api/csrf',200,extra=token('user'))
request('13 Salida alumno','userBase','/logout',302,'POST',headers={'X-CSRF-TOKEN':'{{userToken}}'})
request('14 Sesión alumno revocada','userBase','/api/actividades/pendientes',401)
request('15 CSRF administración Django','adminBase','/api/admin/csrf',200,extra=token('admin')+secure_cookie('MAIN_ADMIN_SESSION'))
request('16 Alumno no puede administrar','adminBase','/api/admin/login',403,'POST',dict(username='{{username}}',password='{{password}}'),{'X-CSRFToken':'{{adminToken}}','Origin':'{{adminBase}}'})
request('17 Renovar CSRF administración','adminBase','/api/admin/csrf',200,extra=token('admin'))
request('18 Acceso administrativo','adminBase','/api/admin/login',200,'POST',dict(username='{{adminUsername}}',password='{{adminPassword}}'),{'X-CSRFToken':'{{adminToken}}','Origin':'{{adminBase}}'})
request('19 Perfil administrativo','adminBase','/api/admin/perfil',200,extra=["pm.test('Rol administrativo',()=>pm.expect(pm.response.json().rol).to.equal('ADMIN'));"])
request('20 Actividades Django','adminBase','/api/admin/actividades',200)
request('21 Alta sin CSRF rechazada','adminBase','/api/admin/actividades',403,'POST',headers={'Origin':'{{adminBase}}'})
request('22 CSRF administrativo actual','adminBase','/api/admin/csrf',200,extra=token('admin'))
request('23 Origen ajeno rechazado','adminBase','/api/admin/logout',403,'POST',headers={'X-CSRFToken':'{{adminToken}}','Origin':'https://foreign.invalid'})
request('24 Salida administrativa','adminBase','/api/admin/logout',200,'POST',headers={'X-CSRFToken':'{{adminToken}}','Origin':'{{adminBase}}'})
request('25 Sesión administrativa revocada','adminBase','/api/admin/perfil',401)
collection=dict(info=dict(name='MAIN HTTPS sesiones y permisos',schema='https://schema.getpostman.com/json/collection/v2.1.0/collection.json',description='Smoke local HTTPS o servidor futuro. Solo lectura y sesiones; no crea notas, mensajes ni cuentas. Usar cuentas sintéticas ALUMNO y ADMIN.'),item=items,event=[dict(listen='prerequest',script=dict(type='text/javascript',exec=["const url=pm.variables.replaceIn(pm.request.url.toString()); if (!url.startsWith('https://')) throw new Error('Esta colección requiere HTTPS');"])),dict(listen='test',script=dict(type='text/javascript',exec=["pm.test('HTTPS y cabeceras del proxy',()=>{pm.expect(pm.variables.replaceIn(pm.request.url.toString())).to.match(/^https:\\/\\//); pm.expect(pm.response.headers.get('Strict-Transport-Security')).to.include('max-age='); pm.expect(pm.response.headers.get('X-Content-Type-Options')).to.equal('nosniff');});"]))])
(out/'MAIN-HTTPS.postman_collection.json').write_text(json.dumps(collection,ensure_ascii=False,indent=2),encoding='utf-8')
for name,base in [('local-HTTPS','https://localhost:8443'),('servidor-HTTPS','https://main.example.org')]:
    values=[dict(key=k,value=v,type='secret' if 'assword' in k else 'default',enabled=True) for k,v in dict(userBase=base,adminBase=base,username='',password='',adminUsername='',adminPassword='').items()]
    (out/f'MAIN-{name}.postman_environment.json').write_text(json.dumps(dict(name='MAIN '+name,values=values,_postman_variable_scope='environment'),indent=2),encoding='utf-8')
print('Colección Postman con 25 solicitudes y dos entornos sin credenciales generados.')
