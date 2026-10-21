"""Prueba HTTP real Django -> MySQL compartido -> Spring Boot.

Solo datos sintéticos. Requiere MAIN_TEST_ADMIN_PASSWORD y MAIN_TEST_STUDENT_PASSWORD.
--mode create guarda una actividad nueva; --mode verify comprueba la misma después del reinicio.
No registra contraseñas, cookies, tokens ni contenidos privados.
"""
import argparse
import http.cookiejar
import json
import os
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

class Session:
    def __init__(self, base):
        self.base=base.rstrip('/')
        self.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

    def call(self, path, method='GET', body=None, token=None, form=False):
        headers={'Accept':'*/*' if form else 'application/json'}
        if body is not None:
            headers['Content-Type']='application/x-www-form-urlencoded' if form else 'application/json'
            body=(urllib.parse.urlencode(body) if form else json.dumps(body)).encode()
        if token:
            headers[token['headerName']]=token['token']
        request=urllib.request.Request(self.base+path, data=body, headers=headers, method=method)
        try:
            response=self.opener.open(request,timeout=20)
        except urllib.error.HTTPError as error:
            response=error
        text=response.read().decode('utf-8')
        try: data=json.loads(text)
        except ValueError: data=None
        return response.status,data

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--admin-base',default='http://127.0.0.1:8090')
    parser.add_argument('--user-base',default='http://127.0.0.1:8089')
    parser.add_argument('--mode',choices=['create','verify'],default='create')
    parser.add_argument('--evidence-file',type=Path,required=True)
    args=parser.parse_args()
    admin_password=os.environ['MAIN_TEST_ADMIN_PASSWORD']
    student_password=os.environ['MAIN_TEST_STUDENT_PASSWORD']
    checks=[]
    def expect(name,actual,wanted):
        ok=actual==wanted
        checks.append(dict(name=name,passed=ok,actual=actual,expected=wanted))
        if not ok:
            raise AssertionError(f'{name}: {actual!r} != {wanted!r}')
    def django_login(session,name,password):
        _,token=session.call('/api/admin/csrf')
        return session.call('/api/admin/login','POST',dict(username=name,password=password),token=token,form=True)[0]
    def spring_login(session,name):
        _,token=session.call('/login-token')
        return session.call('/login','POST',dict(username=name,password=student_password,**{token['parameterName']:token['token']}),form=True)[0]
    admin=Session(args.admin_base)
    expect('django_anonymous',admin.call('/api/admin/actividades')[0],401)
    expect('spring_anonymous',Session(args.user_base).call('/api/actividades/pendientes')[0],401)
    student_admin=Session(args.admin_base)
    expect('student_cannot_administer_django',django_login(student_admin,'alumno',student_password),403)
    expect('django_login',django_login(admin,'android_test_admin',admin_password),200)
    expect('django_csrf_required',admin.call('/api/admin/actividades','POST',{})[0],403)
    if args.mode=='create':
        item_id=int(time.time()*1000)
        body=dict(id=item_id,cursoId=1,titulo='Sprint 12: Django y Spring compartidos — dato sintético',
                  descripcion='Verificación asistida para Jery. Actividad de prueba, no una tarea académica real.',
                  fechaLimite='2026-11-20T15:30:00')
        _,token=admin.call('/api/admin/csrf')
        status,data=admin.call('/api/admin/actividades','POST',body,token=token)
        expect('django_create',status,201)
        expect('lima_date_preserved',data['fechaLimite'],body['fechaLimite'])
        expect('duplicate_does_not_overwrite',admin.call('/api/admin/actividades','POST',body,token=token)[0],409)
    else:
        item_id=json.loads(args.evidence_file.read_text(encoding='utf-8'))['activityId']
    expect('django_reads_activity',admin.call(f'/api/admin/actividades/{item_id}')[0],200)
    enrolled=Session(args.user_base)
    expect('enrolled_login',spring_login(enrolled,'alumno'),200)
    status,data=enrolled.call('/api/actividades/pendientes')
    expect('spring_list',status,200)
    expect('enrolled_list_contains_django_record',item_id in [item['id'] for item in data],True)
    status,data=enrolled.call(f'/api/actividades/{item_id}')
    expect('enrolled_detail',status,200)
    expect('spring_lima_date',data['fechaLimite'],'2026-11-20T15:30:00')
    stranger=Session(args.user_base)
    expect('other_login',spring_login(stranger,'otro'),200)
    expect('other_detail_hidden',stranger.call(f'/api/actividades/{item_id}')[0],404)
    expect('other_list_excludes_record',item_id not in [item['id'] for item in stranger.call('/api/actividades/pendientes')[1]],True)
    empty=Session(args.user_base)
    expect('empty_login',spring_login(empty,'vacio'),200)
    expect('empty_list',empty.call('/api/actividades/pendientes')[1],[])
    result=dict(timestampUTC=datetime.now(timezone.utc).isoformat(),mode=args.mode,activityId=item_id,
                adminBase=args.admin_base,userBase=args.user_base,passed=len(checks),checks=checks,
                limitation='Identidades sintéticas; aceptación académica y despliegue HTTPS no incluidos.')
    destination=args.evidence_file if args.mode=='create' else args.evidence_file.with_name(args.evidence_file.stem+'-after-restart.json')
    destination.write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
    print(f'{len(checks)} comprobaciones HTTP aprobadas; evidencia segura: {destination.name}')

if __name__=='__main__':
    main()
