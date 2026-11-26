"""Verify school activation locally with one temporary, synthetic family account.

--run is explicit: create the fixture, check the real React proxies/Django/Spring,
then remove only this test's account/contact/tokens. Existing accounts are untouched.
No email is sent; the test aborts if automatic email has been configured.
"""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import sys
import uuid

LOCAL = Path.home() / 'Documents/Codex/2026-10-22/ho/work'
sys.path.insert(0, str(LOCAL / 'main-python-libs'))
import MySQLdb


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', action='store_true')
    args = parser.parse_args()
    if not args.run:
        print('Vista previa: se usará una cuenta sintética temporal; añade --run para comprobar el recorrido y limpiar esa cuenta.')
        return
    spec = importlib.util.spec_from_file_location('verify_stitch', Path(__file__).with_name('Verify-StitchDemo.py'))
    helper = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(helper)
    def session(port):
        value = helper.Session(port)
        value.base = value.base.replace('localhost', '127.0.0.1')
        return value
    config = json.loads((LOCAL / 'main-review-private.json').read_text(encoding='utf-8-sig'))
    db = MySQLdb.connect(host='127.0.0.1', port=int(config['port']), user='main_django', passwd=config['django_password'],
                         db=config['database'], charset='utf8mb4')
    db.autocommit(True)
    username = 'qa_access_' + uuid.uuid4().hex[:12]
    name, email, secret = 'Solicitud sintética de verificación', username + '@example.test', 'SyntheticRequest2026!'
    created = False
    results = {}
    try:
        with db.cursor() as cursor:
            cursor.execute('SELECT COUNT(*) FROM main_user WHERE username=%s', [username])
            assert cursor.fetchone()[0] == 0
            cursor.execute('SELECT COUNT(*) FROM main_user')
            before_count = cursor.fetchone()[0]
        public = session(5173)
        status, token = public.call('/login-token')
        assert status == 200
        headers = {'X-CSRF-TOKEN': token['token']}
        status, error = public.call('/api/access/reset-request', {'identifier': username}, headers=headers)
        if status != 503 or 'MAIL_NOT_CONFIGURED' not in str(error):
            raise RuntimeError('Prueba detenida: el estado del correo cambió. No se registró ninguna cuenta ni se envió un mensaje.')
        status, result = public.call('/api/access/register-family', dict(username=username, name=name, email=email, password=secret), headers=headers)
        assert status == 200, status
        created = True
        assert result['emailSent'] is False
        with db.cursor() as cursor:
            cursor.execute('SELECT role,enabled FROM main_user WHERE username=%s', [username])
            assert cursor.fetchone() == ('FAMILIAR', 0)
        public.call('/login', dict(username=username, password=secret, **{token['parameterName']: token['token']}), form=True)
        assert public.call('/api/perfil')[0] == 401
        results['registro'] = 'guardado, sin correo enviado y sin acceso antes de activar'
        print('Registro y bloqueo inicial: OK', flush=True)

        admin = session(5174)
        admin.login('admin.monica')
        status, requests = admin.call('/api/admin/access/requests?offset=0&limit=20')
        assert status == 200 and any(row['username'] == username for row in requests)
        status, invitation = admin.admin_post('/v1/users/' + username + '/invitation', {'delivery': 'SCHOOL'})
        assert status == 201, status
        code = invitation['code']
        with db.cursor() as cursor:
            cursor.execute('SELECT token_hash FROM account_access_token WHERE username=%s AND used_at IS NULL', [username])
            assert cursor.fetchone()[0] == hashlib.sha256(code.encode()).hexdigest()
        status, requests = admin.call('/api/admin/access/requests?offset=0&limit=20')
        assert status == 200 and not any(row['username'] == username for row in requests)
        results['revision'] = 'visible como pendiente; revisada al emitir invitación en Django'
        print('Revisión e invitación de Django: OK', flush=True)

        public = session(5173)
        status, token = public.call('/login-token')
        assert status == 200
        headers = {'X-CSRF-TOKEN': token['token']}
        confirmation = dict(username=username, code=code, intent='activate', password=secret)
        status, _ = public.call('/api/access/confirm', confirmation, headers=headers)
        assert status == 200, status
        assert public.call('/api/access/confirm', confirmation, headers=headers)[0] == 400
        account = session(5173)
        profile = account.login(username, secret)
        assert profile['rol'] == 'Familiar'
        assert account.call('/api/v1/family/students') == (200, [])
        assert account.call('/api/v1/family/students/880001/catalog')[0] == 403
        assert account.call('/api/v1/family/students/880002/catalog')[0] == 403
        results['activacion'] = 'consumida en Spring; login válido; segundo uso rechazado'
        results['privacidad'] = 'no puede consultar notas de hijos sin vínculo verificado'
        print('Activación, uso único y protección familiar: OK', flush=True)
    finally:
        if created:
            db.autocommit(False)
            try:
                with db.cursor() as cursor:
                    cursor.execute('SELECT display_name,role FROM main_user WHERE username=%s FOR UPDATE', [username])
                    assert cursor.fetchone() == (name, 'FAMILIAR'), 'La identidad de la cuenta temporal cambió; no se elimina.'
                    cursor.execute('SELECT COUNT(*) FROM guardian_link WHERE guardian=%s', [username])
                    assert cursor.fetchone()[0] == 0, 'La cuenta temporal tiene un vínculo nuevo; no se elimina.'
                    cursor.execute('DELETE FROM account_access_token WHERE username=%s', [username])
                    cursor.execute('DELETE FROM account_contact WHERE username=%s AND email=%s', [username, email])
                    cursor.execute('DELETE FROM main_user WHERE username=%s', [username])
                db.commit()
                with db.cursor() as cursor:
                    cursor.execute('SELECT COUNT(*) FROM main_user')
                    assert cursor.fetchone()[0] == before_count
                results['limpieza'] = 'solo cuenta sintética y sus códigos; cuentas anteriores conservadas'
            except Exception:
                db.rollback()
                raise
        db.close()
    print(json.dumps(results, ensure_ascii=False), flush=True)


if __name__ == '__main__':
    main()
