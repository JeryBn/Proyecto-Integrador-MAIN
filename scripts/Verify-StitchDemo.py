"""Read the local seeded portals. Only the optional test invitation changes test data."""
import argparse
import http.cookiejar
import json
import urllib.error
import urllib.parse
import urllib.request


class Session:
    def __init__(self, port):
        self.base = 'http://localhost:' + str(port)
        self.http = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

    def call(self, path, value=None, form=False, headers=None):
        head = dict(headers or {})
        body = None
        if value is not None:
            body = (urllib.parse.urlencode(value) if form else json.dumps(value)).encode()
            head['Content-Type'] = 'application/x-www-form-urlencoded' if form else 'application/json'
        try:
            with self.http.open(urllib.request.Request(self.base + path, data=body, headers=head), timeout=20) as r:
                data = r.read()
                return r.status, json.loads(data) if 'json' in r.headers.get('Content-Type', '') else data
        except urllib.error.HTTPError as exc:
            return exc.code, exc.read().decode()[:1000]

    def login(self, user, password='ColegiaDemo2026!'):
        if self.base.endswith('5174'):
            status, token = self.call('/api/admin/csrf')
            assert status == 200
            status, result = self.call('/api/admin/login', dict(username=user, password=password), True,
                                      {token['headerName']: token['token'], 'Origin': self.base})
        else:
            status, token = self.call('/login-token')
            assert status == 200
            self.call('/login', dict(username=user, password=password, **{token['parameterName']: token['token']}), True)
            status, result = self.call('/api/perfil')
        assert status == 200, (user, status)
        return result

    def admin_post(self, path, value):
        status, token = self.call('/api/admin/csrf')
        assert status == 200
        return self.call('/api/admin' + path, value, headers={token['headerName']: token['token'], 'Origin': self.base})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--test-invitation', action='store_true', help='Redeem one school invitation for synthetic ana.torres; retain its demonstration password.')
    args = parser.parse_args()
    results = {}
    for user in ['mateo.morales', 'valentina.morales', 'ana.morales', 'ana.torres', 'carlos.mendoza', 'monica.silva', 'elena.ramos', 'diego.salazar', 'laura.vega']:
        session = Session(5173)
        profile = session.login(user)
        results[user] = profile['rol']
        if user == 'mateo.morales':
            status, tasks = session.call('/api/actividades/pendientes')
            assert status == 200 and len(tasks) == 5 and all(not task['vencida'] for task in tasks)
            status, agenda = session.call('/api/v1/agenda')
            assert status == 200 and len(agenda['timetable']) == 15
            results['mateo_tasks'] = len(tasks)
        if user == 'ana.morales':
            status, students = session.call('/api/v1/family/students')
            assert status == 200 and {student['id'] for student in students} == {880001, 880002}
            status, catalog = session.call('/api/v1/family/students/880002/catalog')
            assert status == 200
            year = catalog['years'][0]
            query = urllib.parse.urlencode(dict(yearId=year['id'], periodId=year['activePeriodId']))
            status, report = session.call('/api/v1/family/students/880002/report?' + query)
            assert status == 200 and len(report['marks']) == 10
            assert all(mark['achievement'] in ['AD', 'A', 'B', 'C'] for mark in report['marks'])
            status, pdf = session.call('/api/v1/family/students/880002/report.pdf?' + query)
            assert status == 200 and pdf.startswith(b'%PDF')
            results['libreta_pdf'] = 'OK'
            status, _ = session.call('/api/v1/family/students/1/catalog')
            assert status == 403
            results['unlinked_student'] = '403'
        if user == 'ana.torres':
            status, students = session.call('/api/v1/family/students')
            assert status == 200 and students == []
            for student in [880001, 880002]:
                status, _ = session.call('/api/v1/family/students/' + str(student) + '/catalog')
                assert status == 403
            results['ana_torres_access'] = 'sin hijos; consultas ajenas rechazadas'
        if user == 'carlos.mendoza':
            status, scopes = session.call('/api/v1/teaching/school/scopes')
            assert status == 200 and len(scopes) == 2
            status, posts = session.call('/api/v1/teaching/publications')
            assert status == 200 and posts and any(post['readers'] for post in posts)
            results['lecturas'] = 'OK'
        print('Comprobado: ' + user, flush=True)
    admin = Session(5174)
    admin.login('admin.monica')
    status, users = admin.call('/api/admin/v1/users?offset=0&limit=20')
    assert status == 200 and len(users) == 15
    status, contact = admin.call('/api/admin/v1/users/mateo.morales/contact')
    assert status == 200 and contact['email'] == ''
    if args.test_invitation:
        status, invitation = admin.admin_post('/v1/users/ana.torres/invitation', {'delivery': 'SCHOOL'})
        assert status == 201
        session = Session(5173)
        status, token = session.call('/login-token')
        value = dict(username='ana.torres', code=invitation['code'], intent='activate', password='ColegiaDemo2026!')
        status, _ = session.call('/api/access/confirm', value, headers={'X-CSRF-TOKEN': token['token']})
        assert status == 200, status
        status, _ = session.call('/api/access/confirm', value, headers={'X-CSRF-TOKEN': token['token']})
        assert status == 400
        results['django_invitation_spring_activation'] = 'OK; replay rechazado; contraseña de prueba conservada'
    admin = Session(5174)
    admin.login('admin', 'admin123')
    results['admin_original'] = 'OK'
    print(json.dumps(results, ensure_ascii=False), flush=True)


if __name__ == '__main__':
    main()
