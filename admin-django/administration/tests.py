import json
from unittest.mock import patch
import bcrypt
from django.core.cache import cache
from django.db import connection, IntegrityError
from django.test import Client, TransactionTestCase
from .models import Activity, Audit, Course, SharedUser, Teacher

PASSWORD = 'SyntheticDjangoTest2026!'
HASH = bcrypt.hashpw(PASSWORD.encode(), bcrypt.gensalt(rounds=4)).decode()

class AdministrationTests(TransactionTestCase):
    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        # Únicamente la BD desechable de pruebas. En ejecución real crea Flyway.
        with connection.schema_editor() as editor:
            for model in (Teacher, Course, Activity, SharedUser, Audit):
                editor.create_model(model)
        with connection.cursor() as cursor:
            cursor.execute('CREATE TABLE alumno(id BIGINT PRIMARY KEY, nombre VARCHAR(100) NOT NULL)')
            cursor.execute('CREATE TABLE matricula(alumno_id BIGINT NOT NULL REFERENCES alumno(id), curso_id BIGINT NOT NULL REFERENCES curso(id), PRIMARY KEY(alumno_id,curso_id))')
            cursor.execute('CREATE TABLE timetable(id VARCHAR(36) PRIMARY KEY, curso_id BIGINT NOT NULL REFERENCES curso(id))')

    @classmethod
    def tearDownClass(cls):
        with connection.cursor() as cursor:
            for table in ('timetable', 'matricula', 'alumno'):
                cursor.execute('DROP TABLE ' + table)
        with connection.schema_editor() as editor:
            for model in (Audit, SharedUser, Activity, Course, Teacher):
                editor.delete_model(model)
        super().tearDownClass()

    def setUp(self):
        cache.clear()
        with connection.cursor() as cursor:
            for table in ('timetable', 'matricula', 'alumno'):
                cursor.execute('DELETE FROM ' + table)
        for model in (Audit, Activity, SharedUser, Course, Teacher):
            model.objects.all().delete()
        Teacher.objects.create(id=1, nombre='Docente sintético')
        Course.objects.create(id=1, nombre='Curso sintético', docente_id=1)
        for name, role in [('admin_test', 'ADMIN'), ('alumno', 'ALUMNO')]:
            SharedUser.objects.create(username=name, display_name=name, role=role, password_hash=HASH, enabled=True)
        self.client = Client(enforce_csrf_checks=True)

    def token(self):
        return self.client.get('/api/admin/csrf').json()['token']

    def login(self, username='admin_test', password=PASSWORD):
        return self.client.post('/api/admin/login', {'username':username, 'password':password}, HTTP_X_CSRFTOKEN=self.token())

    def payload(self):
        return dict(id=100, cursoId=1, titulo='Actividad sintética', descripcion='Instrucciones', fechaLimite='2026-11-20T15:30:00')

    def create(self, body=None):
        return self.client.post('/api/admin/actividades', json.dumps(self.payload() if body is None else body),
                                content_type='application/json', HTTP_X_CSRFTOKEN=self.token())

    def test_anonymous_cannot_read_or_create(self):
        self.assertEqual(self.client.get('/api/admin/actividades').status_code, 401)
        self.assertEqual(self.create().status_code, 401)

    def test_csrf_required_even_for_login(self):
        self.assertEqual(self.client.post('/api/admin/login', {'username':'admin_test','password':PASSWORD}).status_code, 403)
        self.login()
        self.assertEqual(self.client.post('/api/admin/actividades', '{}', content_type='application/json').status_code, 403)

    def test_student_never_gets_admin_session(self):
        self.assertEqual(self.login('alumno').status_code, 403)
        self.assertEqual(self.client.get('/api/admin/perfil').status_code, 401)

    def test_wrong_disabled_and_unknown_accounts(self):
        self.assertEqual(self.login(password='incorrecta').status_code, 401)
        self.assertEqual(self.login(username='inexistente').status_code, 401)
        SharedUser.objects.filter(pk='admin_test').update(enabled=False)
        self.assertEqual(self.login().status_code, 401)

    def test_login_rotates_csrf_and_session_and_logout_invalidates_cookie(self):
        before = self.token()
        previous_cookie = self.client.cookies['MAIN_ADMIN_SESSION'].value
        self.assertEqual(self.login().status_code, 200)
        self.assertNotEqual(previous_cookie, self.client.cookies['MAIN_ADMIN_SESSION'].value)
        self.assertEqual(self.client.post('/api/admin/logout', HTTP_X_CSRFTOKEN=before).status_code, 403)
        saved_cookie = self.client.cookies['MAIN_ADMIN_SESSION'].value
        self.assertEqual(self.client.post('/api/admin/logout', HTTP_X_CSRFTOKEN=self.token()).status_code, 200)
        self.client.cookies['MAIN_ADMIN_SESSION'] = saved_cookie
        self.assertEqual(self.client.get('/api/admin/perfil').status_code, 401)

    def test_revoked_account_or_role_invalidates_session(self):
        self.login()
        SharedUser.objects.filter(pk='admin_test').update(role='ALUMNO')
        self.assertEqual(self.client.get('/api/admin/actividades').status_code, 401)

    def test_password_change_invalidates_session(self):
        self.login()
        SharedUser.objects.filter(pk='admin_test').update(password_hash=bcrypt.hashpw(b'AnotherSynthetic2026!',bcrypt.gensalt(rounds=4)).decode())
        self.assertEqual(self.client.get('/api/admin/perfil').status_code, 401)

    def test_create_contract_audit_and_duplicate_do_not_overwrite(self):
        self.login()
        response = self.create()
        self.assertEqual(response.status_code, 201)
        self.assertEqual(response['Location'], '/api/admin/actividades/100')
        self.assertEqual(response.json()['fechaLimite'], '2026-11-20T15:30:00')
        self.assertEqual(response.json()['docente'], 'Docente sintético')
        self.assertEqual(self.create().status_code, 409)
        self.assertEqual(Activity.objects.count(), 1)
        self.assertEqual(Audit.objects.count(), 1)
        self.assertEqual(Audit.objects.get().action, 'ACTIVITY_CREATED')

    def test_invalid_numbers_strings_unknown_fields_and_dates(self):
        self.login()
        for key, value in [('id',True),('id',1.5),('id','100'),('id',-1),('id',9007199254740992),
                           ('cursoId',0),('titulo',' '),('descripcion','x'*2001),('rol','ADMIN'),
                           ('fechaLimite','2026-02-30T15:30:00'),('fechaLimite','2026-11-20T15:30:00Z')]:
            body = self.payload(); body[key] = value
            with self.subTest(key=key, value=str(value)[:40]):
                self.assertEqual(self.create(body).status_code, 400)
        self.assertEqual(Activity.objects.count(), 0)

    def test_unknown_course_and_missing_fields(self):
        self.login()
        self.assertEqual(self.create({}).status_code, 400)
        body = self.payload(); body['cursoId']=999
        self.assertEqual(self.create(body).status_code, 400)

    def test_malformed_json_and_non_object(self):
        self.login()
        for body in ('{','[]','null'):
            self.assertEqual(self.client.post('/api/admin/actividades', body, content_type='application/json',
                                             HTTP_X_CSRFTOKEN=self.token()).status_code, 400)

    def test_pagination_order_and_detail(self):
        self.login(); self.create()
        body = self.payload(); body.update(id=101,fechaLimite='2026-11-19T15:30:00'); self.create(body)
        response = self.client.get('/api/admin/actividades?limit=1&offset=0').json()
        self.assertEqual(response['total'],2); self.assertEqual(response['items'][0]['id'],101)
        self.assertEqual(self.client.get('/api/admin/actividades/100').status_code,200)
        self.assertEqual(self.client.get('/api/admin/actividades/999').status_code,404)
        for query in ('limit=0','limit=101','offset=-1','offset=bad'):
            self.assertEqual(self.client.get('/api/admin/actividades?'+query).status_code,400)
        self.assertEqual(self.client.get('/api/admin/cursos').json()['items'][0]['id'],1)

    def test_audit_failure_rolls_back_creation(self):
        self.login()
        with patch('administration.views.Audit.objects.create', side_effect=IntegrityError('synthetic audit failure')):
            self.assertEqual(self.create().status_code,409)
        self.assertEqual(Activity.objects.count(),0)

    def test_login_throttle(self):
        for _ in range(10):
            self.assertEqual(self.login(password='incorrecta').status_code,401)
        self.assertEqual(self.login().status_code,429)

    def test_react_assets_and_unsupported_operations(self):
        self.assertContains(self.client.get('/admin.html'), 'data-admin-scope="academic"')
        self.assertEqual(self.client.get('/assets/admin.js').status_code,200)
        self.assertEqual(self.client.get('/assets/../settings.py').status_code,404)
        self.assertEqual(self.client.delete('/api/admin/actividades/100', HTTP_X_CSRFTOKEN=self.token()).status_code,405)

    def academic_call(self, path, value, method='post'):
        return getattr(self.client, method)('/api/admin/v1/' + path, json.dumps(value),
                     content_type='application/json', HTTP_X_CSRFTOKEN=self.token())

    def prepare_academic(self):
        self.login()
        SharedUser.objects.create(username='teacher', display_name='Teacher', role='DOCENTE', password_hash=HASH,
                                  enabled=True, docente_id=1)

    def test_course_create_edit_and_stale_version(self):
        self.prepare_academic()
        value = dict(id=2, name='Nuevo', teacherUsername='teacher', expectedVersion=None)
        self.assertEqual(self.academic_call('courses', value).status_code, 201)
        self.assertEqual(self.academic_call('courses', value).status_code, 409)
        value.update(name='Editado', expectedVersion=0)
        self.assertEqual(self.academic_call('courses/2', value, 'put').json()['version'], 1)
        self.assertEqual(self.academic_call('courses/2', value, 'put').status_code, 409)
        self.assertEqual(Course.objects.get(pk=2).nombre, 'Editado')
        self.assertEqual(Audit.objects.count(), 2)
        self.assertEqual(self.client.get('/api/admin/v1/courses').json()[1]['teacherId'], 1)

    def test_course_validation_teacher_and_timetable(self):
        self.prepare_academic()
        Teacher.objects.create(id=2, nombre='Otro docente')
        SharedUser.objects.create(username='teacher2', display_name='Teacher2', role='DOCENTE', password_hash=HASH,
                                  enabled=True, docente_id=2)
        value = dict(id=1, name='Cambio', teacherUsername='teacher2', expectedVersion=0)
        with connection.cursor() as cursor:
            cursor.execute("INSERT INTO timetable VALUES('slot',1)")
        self.assertEqual(self.academic_call('courses/1', value, 'put').status_code, 400)
        for field, invalid in [('id',True), ('name',' '), ('teacherUsername','alumno'), ('expectedVersion',True)]:
            candidate = dict(value); candidate[field] = invalid
            self.assertEqual(self.academic_call('courses/1', candidate, 'put').status_code, 400)
        self.assertEqual(Course.objects.get(pk=1).admin_version, 0)
        self.assertEqual(Audit.objects.count(), 0)

    def test_enrollment_idempotent_and_revoke_disabled_student(self):
        self.prepare_academic()
        with connection.cursor() as cursor:
            cursor.execute("INSERT INTO alumno VALUES(1,'Alumno sintético')")
        SharedUser.objects.filter(pk='alumno').update(alumno_id=1)
        value = dict(username='alumno', enrolled=True)
        for _ in range(2):
            self.assertEqual(self.academic_call('courses/1/students', value, 'put').status_code, 200)
        self.assertEqual(Audit.objects.count(), 1)
        self.assertEqual(self.client.get('/api/admin/v1/courses/1/students').json()[0]['studentId'], 1)
        SharedUser.objects.filter(pk='alumno').update(enabled=False)
        self.assertEqual(self.academic_call('courses/1/students', value, 'put').status_code, 400)
        value['enrolled'] = False
        for _ in range(2):
            self.assertEqual(self.academic_call('courses/1/students', value, 'put').status_code, 200)
        self.assertEqual(Audit.objects.count(), 2)
        self.assertEqual(self.client.get('/api/admin/v1/courses/1/students').json(), [])

    def test_academic_access_pagination_and_audit_read_only(self):
        for path in ('courses', 'courses/1/students', 'audit'):
            self.assertEqual(self.client.get('/api/admin/v1/'+path).status_code, 401)
        self.prepare_academic()
        for path in ('courses', 'courses/1/students', 'audit'):
            self.assertEqual(self.client.get('/api/admin/v1/'+path+'?limit=101').status_code, 400)
        self.assertEqual(self.academic_call('audit', {}).status_code, 405)
        self.assertEqual(self.academic_call('courses/1/students', {'username':'alumno','enrolled':'false'}, 'put').status_code, 400)

    def test_academic_audit_failure_rolls_back_course(self):
        self.prepare_academic()
        with patch('administration.academic.record', side_effect=IntegrityError('synthetic')):
            self.assertEqual(self.academic_call('courses', dict(id=2,name='Nuevo',teacherUsername='teacher')).status_code, 409)
        self.assertFalse(Course.objects.filter(pk=2).exists())
