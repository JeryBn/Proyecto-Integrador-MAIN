"""Read-only HTTP checks against a disposable, explicitly seeded Compose installation."""
import os,json,time,urllib.request,urllib.parse,http.cookiejar
from pathlib import Path

# Compose reads .env itself; the host-side verifier reads the same local file.
envfile=Path(__file__).resolve().parents[1]/'.env'
if envfile.exists():
    for line in envfile.read_text(encoding='utf-8-sig').splitlines():
        if '=' in line and not line.lstrip().startswith('#'):
            key,value=line.split('=',1)
            os.environ.setdefault(key.strip(),value.strip().strip('"').strip("'"))
def request(client,origin,path,form=None,headers=None):
    data=urllib.parse.urlencode(form).encode() if form is not None else None
    with client.open(urllib.request.Request(origin+path,data=data,headers=headers or {}),timeout=20) as response:
        raw=response.read()
        return json.loads(raw) if 'application/json' in response.headers.get('Content-Type','') else raw
def client(): return urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
def verify():
    user=os.getenv('COLEGIA_USER_ORIGIN','http://localhost:5173')
    admin=os.getenv('COLEGIA_ADMIN_ORIGIN','http://localhost:5174')
    u=client();a=client()
    assert b'COLEGIA' in request(u,user,'/')
    assert b'COLEGIA' in request(a,admin,'/')
    token=request(u,user,'/login-token')
    request(u,user,'/login',{'username':'alumno','password':'ColegiaDemo2026!',token['parameterName']:token['token']})
    assert request(u,user,'/api/perfil')['usuario']=='alumno'
    pending=request(u,user,'/api/actividades/pendientes');assert pending
    detail=request(u,user,'/api/actividades/'+str(pending[0]['id']))
    assert all(detail.get(k) for k in ['titulo','descripcion','curso','docente','fechaLimite'])
    token=request(a,admin,'/api/admin/csrf')
    request(a,admin,'/api/admin/login',{'username':os.getenv('ADMIN_USERNAME','admin'),'password':os.environ['ADMIN_PASSWORD']}, {token['headerName']:token['token']})
    profile=request(a,admin,'/api/admin/perfil');assert profile
    assert request(a,admin,'/api/admin/health')['status']=='UP'
    print('VERIFIED: frontends HTTP, sesiones/CSRF de usuario y admin, HU06/HU07 con actividad anterior.')
    print('Pendiente funcional: school_publication TASK/EXAM no alimenta actividad ni agenda unificada.')
if __name__=='__main__':
    for attempt in range(30):
        try:verify();break
        except Exception:
            if attempt==29:raise
            time.sleep(2)
