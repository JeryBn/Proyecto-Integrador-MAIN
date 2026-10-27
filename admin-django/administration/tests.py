import json
import hashlib
from datetime import datetime,timedelta
from pathlib import Path
from unittest.mock import patch
import bcrypt
from django.core.cache import cache
from django.db import connection, IntegrityError
from django.test import Client, TransactionTestCase
from .models import Activity, Audit, Course, SharedUser, Teacher

PASSWORD = 'SyntheticDjangoTest2026!'
HASH = bcrypt.hashpw(PASSWORD.encode(), bcrypt.gensalt(rounds=4)).decode()

class AdministrationTests(TransactionTestCase):
    moderation_tables=('moderation_appeal','moderation_retention_hold','automatic_assessment','notification','message_attachment_chunk','message_attachment','message_revision','message',
                       'conversation_member','conversation','status_view','status_recipient','status_media_chunk','status_update','contact_preference')
    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        # Únicamente la BD desechable de pruebas. En ejecución real crea Flyway.
        with connection.schema_editor() as editor:
            for model in (Teacher, Course, Activity, SharedUser, Audit):
                editor.create_model(model)
        with connection.cursor() as cursor:
            cursor.execute('CREATE TABLE alumno(id BIGINT PRIMARY KEY, usuario VARCHAR(60) UNIQUE NOT NULL, nombre VARCHAR(100) NOT NULL)')
            cursor.execute('CREATE TABLE matricula(alumno_id BIGINT NOT NULL REFERENCES alumno(id), curso_id BIGINT NOT NULL REFERENCES curso(id), PRIMARY KEY(alumno_id,curso_id))')
            cursor.execute('CREATE TABLE timetable(id VARCHAR(36) PRIMARY KEY, curso_id BIGINT NOT NULL REFERENCES curso(id),day_of_week INTEGER,starts_at TIME,ends_at TIME,classroom VARCHAR(100))')
            cursor.execute('CREATE TABLE course_grade(id VARCHAR(36) PRIMARY KEY,course_id BIGINT,student_id BIGINT,assessment VARCHAR(120),score DECIMAL(8,2),maximum DECIMAL(8,2),recorded_by VARCHAR(100),recorded_at TIMESTAMP,UNIQUE(course_id,student_id,assessment))')
            cursor.execute('CREATE TABLE guardian_link(guardian VARCHAR(60), alumno_id BIGINT, verified_by VARCHAR(60), verified_at TIMESTAMP, PRIMARY KEY(guardian,alumno_id))')
            cursor.execute('CREATE TABLE contact_link(user_a VARCHAR(60), user_b VARCHAR(60), approved_by VARCHAR(60), PRIMARY KEY(user_a,user_b))')
            cursor.execute('CREATE TABLE contact_report(id VARCHAR(36), reporter VARCHAR(60), subject VARCHAR(60), reason VARCHAR(1000), created_at TIMESTAMP)')
            cursor.execute('CREATE TABLE contact_preference(owner VARCHAR(60),subject VARCHAR(60),muted BOOLEAN,blocked BOOLEAN,PRIMARY KEY(owner,subject))')
            migrations=Path(__file__).resolve().parents[2]/'src/main/resources/db/migration'
            for prefix in ('V3__','V5__','V7__','V9__','V11__','V14__','V15__','V16__'):
                source=next(migrations.glob(prefix+'*.sql')).read_text()
                for sql in source.split(';'):
                    if sql.strip():
                        cursor.execute(sql)

    @classmethod
    def tearDownClass(cls):
        with connection.cursor() as cursor:
            for table in cls.moderation_tables:
                cursor.execute('DROP TABLE '+table)
            for table in ('course_grade','guardian_link','contact_link','contact_report','timetable', 'matricula', 'alumno'):
                cursor.execute('DROP TABLE ' + table)
        with connection.schema_editor() as editor:
            for model in (Audit, SharedUser, Activity, Course, Teacher):
                editor.delete_model(model)
        super().tearDownClass()

    def setUp(self):
        cache.clear()
        with connection.cursor() as cursor:
            for table in self.moderation_tables:
                cursor.execute('DELETE FROM '+table)
            for table in ('course_grade','guardian_link','contact_link','contact_report','timetable', 'matricula', 'alumno'):
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
        self.assertContains(self.client.get('/admin.html'), '<html lang="es">')
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
            cursor.execute("INSERT INTO timetable(id,curso_id) VALUES('slot',1)")
        self.assertEqual(self.academic_call('courses/1', value, 'put').status_code, 400)
        for field, invalid in [('id',True), ('name',' '), ('teacherUsername','alumno'), ('expectedVersion',True)]:
            candidate = dict(value); candidate[field] = invalid
            self.assertEqual(self.academic_call('courses/1', candidate, 'put').status_code, 400)
        self.assertEqual(Course.objects.get(pk=1).admin_version, 0)
        self.assertEqual(Audit.objects.count(), 0)

    def test_enrollment_idempotent_and_revoke_disabled_student(self):
        self.prepare_academic()
        with connection.cursor() as cursor:
            cursor.execute("INSERT INTO alumno VALUES(1,'alumno','Alumno sintético')")
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

    def test_identity_creation_hashes_and_atomic_conflicts(self):
        self.login()
        value = dict(username='new_student', name='Sintético', role='ALUMNO', password=PASSWORD, alumnoId=77)
        response = self.academic_call('users', value)
        self.assertEqual(response.status_code, 201)
        self.assertNotIn('password', str(response.json()))
        self.assertTrue(bcrypt.checkpw(PASSWORD.encode(), SharedUser.objects.get(pk='new_student').password_hash.encode()))
        self.assertEqual(self.academic_call('users', value).status_code, 409)
        value.update(username='other_student')
        self.assertEqual(self.academic_call('users', value).status_code, 400)
        value.update(username='admin_injected', role='ADMIN')
        self.assertEqual(self.academic_call('users', value).status_code, 400)
        self.assertEqual(Audit.objects.count(), 1)

    def test_identity_reset_disable_and_admin_protection(self):
        self.login()
        self.assertEqual(self.academic_call('users/admin_test/disable', {}).status_code, 403)
        self.assertEqual(self.academic_call('users/admin_test/reset-password', {'password':PASSWORD}).status_code, 403)
        self.assertEqual(self.academic_call('users/alumno/reset-password', {'password':'short'}).status_code, 400)
        self.assertEqual(self.academic_call('users/alumno/reset-password', {'password':'ReplacementSynthetic2026!'}).status_code, 200)
        self.assertEqual(self.academic_call('users/alumno/disable', {}).status_code, 200)
        self.assertFalse(SharedUser.objects.get(pk='alumno').enabled)

    def test_guardian_contact_idempotency_and_revocation(self):
        self.login()
        self.academic_call('users', dict(username='family_test',name='Familia',role='FAMILIAR',password=PASSWORD))
        with connection.cursor() as cursor:
            cursor.execute("INSERT INTO alumno VALUES(1,'alumno','Alumno')")
        for _ in range(2):
            self.assertEqual(self.academic_call('guardians', dict(guardian='family_test',studentId=1)).status_code, 200)
            self.assertEqual(self.academic_call('contacts', dict(first='alumno',second='family_test')).status_code, 200)
        self.assertEqual(Audit.objects.count(), 3)
        self.assertEqual(self.academic_call('guardians/family_test/1', {}, 'delete').status_code, 200)
        self.assertEqual(self.academic_call('contacts', dict(first='admin_test',second='family_test')).status_code, 400)
        self.assertEqual(self.academic_call('contacts', dict(first='alumno',second='alumno')).status_code, 400)

    def test_identity_creation_rolls_back_academic_row_when_audit_fails(self):
        self.login()
        with patch('administration.identity.record', side_effect=IntegrityError('synthetic')):
            response = self.academic_call('users', dict(username='rollback_student',name='Prueba',role='ALUMNO',password=PASSWORD,alumnoId=99))
        self.assertEqual(response.status_code, 409)
        self.assertFalse(SharedUser.objects.filter(pk='rollback_student').exists())
        with connection.cursor() as cursor:
            cursor.execute('SELECT COUNT(*) FROM alumno WHERE id=99')
            self.assertEqual(cursor.fetchone()[0], 0)

    def test_timetable_overlap_and_removal(self):
        self.login()
        value = dict(courseId=1,dayOfWeek=1,startsAt='08:00',endsAt='09:00',classroom='A1')
        response = self.academic_call('timetable',value)
        self.assertEqual(response.status_code,201)
        self.assertEqual(self.academic_call('timetable',value).status_code,400)
        value.update(startsAt='09:00',endsAt='10:00')
        self.assertEqual(self.academic_call('timetable',value).status_code,201)
        self.assertEqual(self.academic_call('timetable/'+response.json()['id'],{},'delete').status_code,200)
        self.assertEqual(self.academic_call('timetable/'+response.json()['id'],{},'delete').status_code,404)
        for field,invalid in [('dayOfWeek',True),('startsAt','08:00:00.1'),('endsAt','07:00')]:
            candidate=dict(value);candidate[field]=invalid
            self.assertEqual(self.academic_call('timetable',candidate).status_code,400)

    def test_grade_contract_conflict_and_revoked_enrollment(self):
        self.login()
        with connection.cursor() as cursor:
            cursor.execute("INSERT INTO alumno VALUES(1,'alumno','Alumno')")
            cursor.execute('INSERT INTO matricula VALUES(1,1)')
        value=dict(studentId=1,assessment='Evaluación',score=18.5,maximum=20,expectedRecordedAt='NEW')
        response=self.academic_call('courses/1/grades',value,'put')
        self.assertEqual(response.status_code,200)
        self.assertEqual(response.json()['score'],18.5)
        self.assertEqual(self.academic_call('courses/1/grades',value,'put').status_code,409)
        value.update(score=19,expectedRecordedAt=response.json()['recordedAt'])
        self.assertEqual(self.academic_call('courses/1/grades',value,'put').status_code,200)
        with connection.cursor() as cursor:
            cursor.execute('DELETE FROM matricula')
        self.assertEqual(self.academic_call('courses/1/grades',value,'put').status_code,404)

    def test_grade_rejects_invalid_numbers_and_rolls_back_audit_failure(self):
        self.login()
        with connection.cursor() as cursor:
            cursor.execute("INSERT INTO alumno VALUES(1,'alumno','Alumno')")
            cursor.execute('INSERT INTO matricula VALUES(1,1)')
        value=dict(studentId=1,assessment='Evaluación',score=18,maximum=20)
        for field,invalid in [('score',True),('score',20.001),('score',-1),('maximum',0),('maximum',1001),('score','18')]:
            candidate=dict(value);candidate[field]=invalid
            self.assertEqual(self.academic_call('courses/1/grades',candidate,'put').status_code,400)
        with patch('administration.grades.record',side_effect=IntegrityError('synthetic')):
            self.assertEqual(self.academic_call('courses/1/grades',value,'put').status_code,409)
        with connection.cursor() as cursor:
            cursor.execute('SELECT COUNT(*) FROM course_grade')
            self.assertEqual(cursor.fetchone()[0],0)

    def moderation_fixture(self, media=False):
        from .moderation import execute,now
        self.login()
        SharedUser.objects.create(username='recipient',display_name='Destinatario',role='ALUMNO',password_hash=HASH,enabled=True)
        execute("INSERT INTO conversation VALUES('conv','DIRECT','Sintética','alumno',%s,'alumno:recipient')",[now()])
        for name in ('alumno','recipient'):
            execute("INSERT INTO conversation_member VALUES('conv',%s,'ACTIVE',FALSE,%s)",[name,now()-timedelta(minutes=1)])
        execute("INSERT INTO contact_link VALUES('alumno','recipient','admin_test')")
        execute("INSERT INTO message(id,conversation_id,sender,client_key,created_at) VALUES('msg','conv','alumno','key',%s)",[now()])
        digest=hashlib.sha256(b'hola').hexdigest()
        if media:
            content=hashlib.sha256(b'archivo').hexdigest()
            execute("INSERT INTO message_attachment VALUES('msg','FILE','text/plain','documento.txt',7,%s,%s)",[content,content])
            execute("INSERT INTO message_attachment_chunk VALUES('msg',0,%s)",[b'archivo'])
            digest=hashlib.sha256(('MAIN-ATTACHMENT\nhola\x00'+content).encode()).hexdigest()
        execute("INSERT INTO message_revision(message_id,revision,body,state,content_hash) VALUES('msg',1,'hola','PENDING',%s)",[digest])
        return dict(revision=1,contentHash=digest,approved=True,attachmentReviewed=False)

    def test_moderation_message_contract_publish_and_notification_once(self):
        from .moderation import rows
        value=self.moderation_fixture()
        self.assertEqual(self.client.get('/api/admin/v1/moderation/messages').json()[0]['senderName'],'alumno')
        self.assertEqual(self.academic_call('moderation/messages/msg/review',value).status_code,200)
        self.assertEqual(self.academic_call('moderation/messages/msg/review',value).status_code,409)
        self.assertEqual(rows("SELECT published_revision FROM message WHERE id='msg'")[0]['published_revision'],1)
        self.assertEqual(len(rows('SELECT id FROM notification')),1)
        self.assertEqual(self.client.get('/api/admin/v1/moderation/messages').json(),[])

    def test_moderation_attachment_requires_review_and_integrity(self):
        from .moderation import execute
        value=self.moderation_fixture(media=True)
        path='/api/admin/v1/moderation/messages/msg/attachment?revision=1'
        response=self.client.get(path)
        self.assertEqual(response.content,b'archivo')
        self.assertIn('no-store',response['Cache-Control'])
        self.assertEqual(response['X-Content-Type-Options'],'nosniff')
        self.assertEqual(self.academic_call('moderation/messages/msg/review',value).status_code,400)
        value['attachmentReviewed']=True
        execute("UPDATE message_attachment_chunk SET payload=%s WHERE message_id='msg'",[b'corrupto'])
        self.assertEqual(self.client.get(path).status_code,409)
        self.assertEqual(self.academic_call('moderation/messages/msg/review',value).status_code,400)

    def test_moderation_blocked_sender_stale_revision_and_audit_rollback(self):
        from .moderation import execute,rows
        value=self.moderation_fixture()
        stale=dict(value,revision=2)
        self.assertEqual(self.academic_call('moderation/messages/msg/review',stale).status_code,409)
        execute("INSERT INTO contact_preference VALUES('recipient','alumno',FALSE,TRUE)")
        self.assertEqual(self.academic_call('moderation/messages/msg/review',value).status_code,403)
        value.update(approved=False,reason='Categoría sintética')
        with patch('administration.moderation.record',side_effect=IntegrityError('synthetic')):
            self.assertEqual(self.academic_call('moderation/messages/msg/review',value).status_code,409)
        self.assertEqual(rows('SELECT state FROM message_revision')[0]['state'],'PENDING')

    def status_fixture(self, kind='TEXT'):
        from .moderation import execute,now
        self.login()
        raw=b'' if kind=='TEXT' else b'imagen'
        digest=hashlib.sha256((kind+'\nhola\n#008800\nCONTACTS\n').encode()+raw).hexdigest()
        execute("INSERT INTO status_update(id,owner,client_key,body,background,audience,kind,state,content_hash,created_at,expires_at) VALUES('status','alumno','key','hola','#008800','CONTACTS',%s,'PENDING',%s,%s,%s)",[kind,digest,now(),now()+timedelta(hours=1)])
        if raw:
            execute("INSERT INTO status_media_chunk VALUES('status',0,%s)",[raw])
        return dict(contentHash=digest,approved=True)

    def test_status_review_expiry_hash_and_explicit_attachment(self):
        from .moderation import execute,now
        value=self.status_fixture('IMAGE')
        self.assertEqual(self.client.get('/api/admin/v1/moderation/statuses').json()[0]['kind'],'IMAGE')
        self.assertEqual(self.client.get('/api/admin/v1/moderation/statuses/status/media').content,b'imagen')
        self.assertEqual(self.academic_call('moderation/statuses/status/review',value).status_code,400)
        value['attachmentReviewed']=True
        self.assertEqual(self.academic_call('moderation/statuses/status/review',dict(value,contentHash='stale')).status_code,409)
        self.assertEqual(self.academic_call('moderation/statuses/status/review',value).status_code,200)
        self.assertEqual(self.client.get('/api/admin/v1/moderation/statuses/status/media').status_code,404)
        execute("UPDATE status_update SET state='PENDING',expires_at=%s WHERE id='status'",[now()-timedelta(seconds=1)])
        self.assertEqual(self.academic_call('moderation/statuses/status/review',value).status_code,409)

    def test_moderation_authentication_csrf_and_page_assets(self):
        self.assertEqual(self.client.get('/moderation.html').status_code,401)
        self.assertEqual(self.client.get('/api/admin/v1/moderation/messages/msg/attachment?revision=1').status_code,401)
        self.login()
        for path in ('/moderation.html','/moderation.js','/moderation.css'):
            self.assertEqual(self.client.get(path).status_code,200)
        self.assertEqual(self.client.post('/api/admin/v1/moderation/messages/msg/review','{}',content_type='application/json').status_code,403)

    def test_automatic_clear_shadow_never_publishes_and_is_cached(self):
        from .automatic_moderation import process,Assessment
        from .moderation import rows
        from unittest.mock import Mock
        self.moderation_fixture();provider=Mock();provider.assess.return_value=Assessment('CLEAR','synthetic')
        self.assertEqual(process('messages','msg',provider),'CLEAR_SHADOW')
        self.assertEqual(process('messages','msg',provider),'CACHED_SHADOW')
        self.assertIsNone(rows("SELECT published_revision FROM message WHERE id='msg'")[0]['published_revision'])
        self.assertEqual(len(rows('SELECT id FROM notification')),0);self.assertEqual(provider.assess.call_count,1)

    def test_automatic_apply_uses_shared_review_and_notifies_once(self):
        from .automatic_moderation import process,Assessment
        from .moderation import rows
        from unittest.mock import Mock
        self.moderation_fixture();provider=Mock();provider.assess.return_value=Assessment('CLEAR','synthetic')
        self.assertEqual(process('messages','msg',provider,True),'APPROVED')
        self.assertEqual(process('messages','msg',provider,True),'STALE')
        self.assertEqual(rows("SELECT published_revision FROM message WHERE id='msg'")[0]['published_revision'],1)
        self.assertEqual(len(rows('SELECT id FROM notification')),1)
        self.assertEqual(rows("SELECT reviewed_by FROM message_revision WHERE message_id='msg'")[0]['reviewed_by'],'MAIN_AUTOMATION')

    def test_automatic_flagged_content_remains_private_for_human(self):
        from .automatic_moderation import process,Assessment
        from .moderation import rows
        from unittest.mock import Mock
        self.moderation_fixture();provider=Mock();provider.assess.return_value=Assessment('HUMAN_REQUIRED','synthetic',('harassment',))
        self.assertEqual(process('messages','msg',provider,True),'HUMAN_REQUIRED')
        self.assertEqual(rows("SELECT state FROM message_revision WHERE message_id='msg'")[0]['state'],'PENDING')
        self.assertEqual(len(rows('SELECT id FROM notification')),0)

    def test_automatic_provider_outage_is_bounded_and_fail_closed(self):
        from .automatic_moderation import process,ProviderUnavailable
        from unittest.mock import Mock
        self.moderation_fixture();provider=Mock();provider.assess.side_effect=ProviderUnavailable('QUOTA')
        self.assertEqual(process('messages','msg',provider,True),'PROVIDER_UNAVAILABLE')
        self.assertEqual(process('messages','msg',provider,True),'WAITING_OR_LIMITED')
        self.assertEqual(provider.assess.call_count,1)

    def test_automatic_video_audio_are_never_partially_approved(self):
        from .automatic_moderation import process
        from unittest.mock import Mock
        self.status_fixture('VIDEO');provider=Mock()
        self.assertEqual(process('statuses','status',provider,True),'HUMAN_REQUIRED');provider.assess.assert_not_called()

    def test_enabled_multimedia_even_clear_stays_private_and_cached(self):
        from .automatic_moderation import process,Assessment
        from .moderation import rows
        from unittest.mock import Mock
        self.status_fixture('VIDEO');provider=Mock();provider.assess_media.return_value=Assessment('CLEAR','controlled')
        with patch.dict('os.environ',{'MAIN_MODERATION_MEDIA_ENABLED':'true'}):
            self.assertEqual(process('statuses','status',provider,True),'HUMAN_REQUIRED')
            self.assertEqual(process('statuses','status',provider,True),'HUMAN_REQUIRED')
        self.assertEqual(provider.assess_media.call_count,1)
        self.assertEqual(rows("SELECT state FROM status_update WHERE id='status'")[0]['state'],'PENDING')
        self.assertEqual(len(rows('SELECT id FROM notification')),0)

    def test_automatic_changed_revision_cannot_be_published(self):
        from .automatic_moderation import process,Assessment
        from .moderation import execute,rows
        from unittest.mock import Mock
        self.moderation_fixture();provider=Mock()
        def changed(*args):
            execute("UPDATE message_revision SET content_hash='changed' WHERE message_id='msg'")
            return Assessment('CLEAR','synthetic')
        provider.assess.side_effect=changed
        self.assertEqual(process('messages','msg',provider,True),'STALE_OR_FORBIDDEN')
        self.assertIsNone(rows("SELECT published_revision FROM message WHERE id='msg'")[0]['published_revision'])

    def test_automatic_requires_approved_policy_and_authorized_reviewer(self):
        from .automatic_moderation import run_batch
        from unittest.mock import Mock
        provider=Mock()
        with self.assertRaisesMessage(ValueError,'POLICY_NOT_APPROVED'):run_batch(provider,env={})
        env=dict(MAIN_MODERATION_POLICY_APPROVED='true',MAIN_MODERATION_POLICY_VERSION='test',MAIN_MODERATION_REVIEWER='alumno',MAIN_MODERATION_RETENTION_DAYS='30')
        with self.assertRaisesMessage(ValueError,'REVIEWER_NOT_AUTHORIZED'):run_batch(provider,env=env)
        provider.assess.assert_not_called()

    def test_automatic_parser_rejects_incomplete_provider_response(self):
        from .automatic_moderation import parse_assessment,ProviderUnavailable
        with self.assertRaises(ProviderUnavailable):parse_assessment({'results':[{'flagged':False,'categories':{}}]},'TEXT')

    def test_automatic_shadow_can_be_applied_without_second_provider_call(self):
        from .automatic_moderation import process,Assessment
        from unittest.mock import Mock
        self.moderation_fixture();provider=Mock();provider.assess.return_value=Assessment('CLEAR','synthetic')
        self.assertEqual(process('messages','msg',provider),'CLEAR_SHADOW')
        self.assertEqual(process('messages','msg',provider,True),'APPROVED')
        self.assertEqual(provider.assess.call_count,1)

    def test_automatic_completed_assessment_does_not_starve_other_queue_items(self):
        from .automatic_moderation import process,pending,Assessment
        from unittest.mock import Mock
        self.moderation_fixture();provider=Mock();provider.assess.return_value=Assessment('HUMAN_REQUIRED','synthetic')
        process('messages','msg',provider)
        self.status_fixture()
        self.assertEqual(pending(1,'test-only',False),[('statuses','status')])

    def test_automatic_image_never_releases_with_partial_modality_coverage(self):
        from .automatic_moderation import process,Assessment
        from .moderation import rows
        from unittest.mock import Mock
        self.status_fixture('IMAGE');provider=Mock();provider.assess.return_value=Assessment('CLEAR','synthetic')
        self.assertEqual(process('statuses','status',provider,True),'HUMAN_REQUIRED')
        self.assertEqual(rows("SELECT state FROM status_update WHERE id='status'")[0]['state'],'PENDING')

    def test_admin_summary_is_current_hash_only_and_contains_no_transcript(self):
        from .automatic_moderation import process,Assessment
        from .moderation import execute
        from unittest.mock import Mock
        self.moderation_fixture();provider=Mock();provider.assess.return_value=Assessment('HUMAN_REQUIRED','controlled',('harassment','unknown-provider-field'))
        process('messages','msg',provider)
        summary=self.client.get('/api/admin/v1/moderation/messages').json()[0]['automaticReview']
        self.assertEqual(summary['categories'],['harassment']);self.assertEqual(summary['coverage'],'TEXT')
        self.assertTrue(summary['humanRequired']);self.assertNotIn('transcript',summary);self.assertNotIn('model',summary)
        execute("UPDATE message_revision SET content_hash='new-version' WHERE message_id='msg'")
        self.assertIsNone(self.client.get('/api/admin/v1/moderation/messages').json()[0]['automaticReview'])
        self.assertEqual(Client().get('/api/admin/v1/moderation/messages').status_code,401)

    def test_video_summary_has_partial_coverage_without_automatic_release(self):
        from .automatic_moderation import process,Assessment
        from .moderation import rows
        from unittest.mock import Mock
        self.status_fixture('VIDEO');provider=Mock()
        provider.assess_media.return_value=Assessment('CLEAR','controlled',('violence',),'VIDEO_SAMPLED_PARTIAL',2,7.25)
        with patch.dict('os.environ',{'MAIN_MODERATION_MEDIA_ENABLED':'true'}):process('statuses','status',provider,True)
        summary=self.client.get('/api/admin/v1/moderation/statuses').json()[0]['automaticReview']
        self.assertEqual(summary['outcome'],'HUMAN_REQUIRED');self.assertEqual(summary['coverage'],'VIDEO_SAMPLED_PARTIAL')
        self.assertEqual(summary['sampledFrames'],2);self.assertEqual(summary['durationSeconds'],7.25)
        self.assertEqual(rows("SELECT state FROM status_update WHERE id='status'")[0]['state'],'PENDING')
        self.assertEqual(len(rows('SELECT id FROM notification')),0)

    def test_retention_preview_boundary_apply_and_idempotency(self):
        from .retention import batch
        from .moderation import execute, rows, now
        self.moderation_fixture(media=True); instant=now()
        execute('UPDATE message_revision SET submitted_at=%s',[instant-timedelta(days=7)+timedelta(seconds=1)])
        self.assertEqual(batch(instant=instant)['counts']['eligible'],0)
        execute('UPDATE message_revision SET submitted_at=%s',[instant-timedelta(days=7)])
        self.assertEqual(batch(instant=instant)['counts']['eligible'],1)
        self.assertEqual(rows('SELECT body FROM message_revision')[0]['body'],'hola')
        self.assertEqual(batch(apply=True,instant=instant)['counts']['purged'],1)
        self.assertEqual(rows('SELECT body,state FROM message_revision')[0],{'body':'','state':'REJECTED'})
        self.assertFalse(rows('SELECT * FROM message_attachment_chunk'))
        self.assertFalse(rows('SELECT * FROM message_attachment'))
        self.assertFalse(rows('SELECT * FROM notification'))
        self.assertEqual(batch(apply=True,instant=instant)['counts']['purged'],0)
        self.assertEqual(len(rows("SELECT id FROM administrative_audit WHERE action='RETENTION_CONTENT_PURGED'")),1)

    def test_retention_keeps_published_attachment_and_fresh_edit(self):
        from .retention import batch
        from .moderation import execute, rows, now
        self.moderation_fixture(media=True); instant=now()
        execute("UPDATE message_revision SET state='APPROVED',submitted_at=%s",[instant-timedelta(days=100)])
        execute('UPDATE message SET published_revision=1,created_at=%s',[instant-timedelta(days=100)])
        execute("INSERT INTO message_revision(message_id,revision,body,state,content_hash,submitted_at) VALUES('msg',2,'edición','PENDING','new',%s)",[instant])
        self.assertEqual(batch(apply=True,instant=instant)['counts']['purged'],0)
        execute('UPDATE message_revision SET submitted_at=%s WHERE revision=2',[instant-timedelta(days=8)])
        self.assertEqual(batch(apply=True,instant=instant)['counts']['purged'],1)
        self.assertEqual(rows('SELECT body FROM message_revision WHERE revision=1')[0]['body'],'hola')
        self.assertEqual(len(rows('SELECT * FROM message_attachment_chunk')),1)
        self.assertEqual(rows('SELECT published_revision FROM message')[0]['published_revision'],1)

    def test_retention_rejected_uses_decision_and_migration_floor(self):
        from .retention import batch
        from .moderation import execute, now
        self.moderation_fixture(); instant=now()
        execute("UPDATE message_revision SET state='REJECTED',submitted_at=%s,reviewed_at=%s",[instant,instant-timedelta(days=100)])
        self.assertEqual(batch(apply=True,instant=instant)['counts']['purged'],0)
        execute('UPDATE message_revision SET submitted_at=%s,reviewed_at=%s',[instant-timedelta(days=100),instant-timedelta(days=29)])
        self.assertEqual(batch(apply=True,instant=instant)['counts']['purged'],0)
        execute('UPDATE message_revision SET reviewed_at=%s',[instant-timedelta(days=30)])
        self.assertEqual(batch(apply=True,instant=instant)['counts']['purged'],1)

    def test_retention_hold_auth_csrf_audit_and_release_grace(self):
        from .retention import batch
        from .moderation import execute, rows, now
        self.moderation_fixture(); instant=now()
        path='/api/admin/v1/moderation/messages/msg/retention-hold'
        value={'active':True,'reason':'Apelación sintética abierta'}
        self.assertEqual(Client().get(path).status_code,401)
        self.assertEqual(self.client.post(path,json.dumps(value),content_type='application/json').status_code,403)
        self.assertEqual(self.academic_call('moderation/messages/msg/retention-hold',value).status_code,200)
        execute('UPDATE message_revision SET submitted_at=%s',[instant-timedelta(days=100)])
        execute('UPDATE administrative_audit SET occurred_at=%s',[instant-timedelta(days=100)])
        self.assertEqual(batch(apply=True,instant=instant)['counts']['purged'],0)
        self.assertTrue(rows('SELECT id FROM administrative_audit'))
        self.assertEqual(self.academic_call('moderation/messages/msg/retention-hold',dict(value,active=False)).status_code,200)
        self.assertEqual(batch(apply=True,instant=instant)['counts']['purged'],0)
        self.login('alumno')
        self.assertEqual(self.academic_call('moderation/messages/msg/retention-hold',value).status_code,401)

    def test_retention_status_media_audit_scope_and_rollback(self):
        from .retention import batch, event
        from .moderation import execute, rows, now
        self.status_fixture('VIDEO'); instant=now()
        execute('UPDATE status_update SET retention_started_at=%s',[instant-timedelta(days=8)])
        with patch('administration.retention.event',side_effect=IntegrityError('synthetic')):
            with self.assertRaises(IntegrityError): batch(apply=True,instant=instant)
        self.assertEqual(rows('SELECT body FROM status_update')[0]['body'],'hola')
        self.assertTrue(rows('SELECT * FROM status_media_chunk'))
        event('STATUS_REJECTED','status',instant-timedelta(days=90))
        event('COURSE_CREATED','1',instant-timedelta(days=100))
        result=batch(apply=True,instant=instant)['counts']
        self.assertEqual(result['purged'],1); self.assertEqual(result['auditPurged'],1)
        self.assertFalse(rows('SELECT * FROM status_media_chunk'))
        self.assertEqual(rows('SELECT body,state FROM status_update')[0],{'body':'','state':'DELETED'})
        self.assertTrue(rows("SELECT id FROM administrative_audit WHERE action='COURSE_CREATED'"))

    def test_retention_rechecks_stale_hash_and_command_gate(self):
        from .retention import minimize
        from .moderation import execute, rows, now
        from django.core.management import call_command
        from django.core.management.base import CommandError
        from io import StringIO
        self.moderation_fixture(); instant=now()
        execute('UPDATE message_revision SET submitted_at=%s',[instant-timedelta(days=8)])
        selected=rows('SELECT * FROM message_revision')[0]
        execute("UPDATE message_revision SET content_hash='changed'")
        self.assertEqual(minimize('messages',selected,instant,True),'changed')
        with patch.dict('os.environ',{'MAIN_RETENTION_APPLY':''}):
            with self.assertRaises(CommandError): call_command('retain_content',apply=True,stdout=StringIO())
        with self.assertRaises(CommandError): call_command('retain_content',limit=501,stdout=StringIO())

    def test_retention_hold_listing_is_admin_only_and_paginated(self):
        self.moderation_fixture()
        self.assertEqual(Client().get('/api/admin/v1/retention-holds').status_code,401)
        self.assertEqual(self.client.get('/api/admin/v1/retention-holds?limit=1000').status_code,400)
        self.assertEqual(self.academic_call('moderation/messages/msg/retention-hold',{'active':True,'reason':'Apelación de prueba'}).status_code,200)
        self.assertEqual(self.client.get('/api/admin/v1/retention-holds').json()[0]['id'],'msg')
        self.assertEqual(self.client.get('/api/admin/v1/retention-holds?offset=1').json(),[])

    def test_retention_old_assessments_and_closed_holds_expire(self):
        from .retention import batch
        from .moderation import execute, rows, now
        self.moderation_fixture(); instant=now(); old=instant-timedelta(days=90)
        execute("UPDATE message_revision SET state='APPROVED'")
        execute("INSERT INTO automatic_assessment(id,policy_version,subject_kind,subject_id,content_hash,outcome,attempts,lease_until,updated_at) VALUES('assessment','synthetic','messages','msg','hash','HUMAN_REQUIRED',1,%s,%s)",[old,old])
        execute("INSERT INTO moderation_retention_hold VALUES('messages','msg',TRUE,'Apelación','admin_test',%s,NULL,NULL)",[old])
        self.assertEqual(batch(apply=True,instant=instant)['counts']['assessmentsPurged'],0)
        execute("UPDATE moderation_retention_hold SET active=FALSE,closed_at=%s",[old])
        preview=batch(instant=instant)['counts']
        self.assertEqual(preview['assessmentsEligible'],1);self.assertEqual(preview['closedHoldsEligible'],1)
        self.assertTrue(rows('SELECT * FROM automatic_assessment'))
        result=batch(apply=True,instant=instant)['counts']
        self.assertEqual(result['assessmentsPurged'],1);self.assertEqual(result['closedHoldsPurged'],1)
        self.assertFalse(rows('SELECT * FROM moderation_retention_hold'))

    def appeal_fixture(self, status=False):
        from .moderation import execute,now
        value=self.status_fixture('IMAGE') if status else self.moderation_fixture(media=True)
        kind,item,revision=('statuses','status',0) if status else ('messages','msg',1)
        table,col=('status_update','id') if status else ('message_revision','message_id')
        execute(f"UPDATE {table} SET state='REJECTED',reviewed_by='admin_test',reviewed_at=%s WHERE {col}=%s",[now(),item])
        execute("INSERT INTO moderation_appeal(id,owner,subject_kind,subject_id,revision,content_hash,request_reason,state,created_at) VALUES('appeal','alumno',%s,%s,%s,%s,'Contexto educativo','OPEN',%s)",[kind,item,revision,value['contentHash'],now()])
        SharedUser.objects.create(username='reviewer_test',display_name='Revisor sintético',role='ADMIN',password_hash=HASH,enabled=True)
        return dict(outcome='RETURNED',reason='Requiere una segunda revisión del contexto.',contentHash=value['contentHash'])

    def test_appeal_independent_resolution_returns_to_review_without_publication(self):
        from .moderation import rows
        value=self.appeal_fixture()
        self.assertEqual(self.academic_call('appeals/appeal/resolve',value).status_code,403)
        self.login('reviewer_test')
        response=self.academic_call('appeals/appeal/resolve',value)
        self.assertEqual(response.status_code,200);self.assertFalse(response.json()['published'])
        self.assertEqual(rows('SELECT state FROM message_revision')[0]['state'],'PENDING')
        self.assertIsNone(rows('SELECT published_revision FROM message')[0]['published_revision'])
        self.assertFalse(rows('SELECT * FROM notification'))
        self.assertEqual(self.academic_call('appeals/appeal/resolve',value).status_code,409)

    def test_open_appeal_protects_content_audit_and_cannot_be_removed_as_manual_hold(self):
        from .moderation import execute,rows,now
        from .retention import batch
        self.appeal_fixture();instant=now()
        execute('UPDATE message_revision SET submitted_at=%s,reviewed_at=%s',[instant-timedelta(days=100)]*2)
        execute("INSERT INTO administrative_audit VALUES('audit','admin_test','APPEAL_OPENED','msg',%s)",[instant-timedelta(days=100)])
        self.assertEqual(batch(apply=True,instant=instant)['counts']['purged'],0)
        self.assertTrue(rows("SELECT id FROM administrative_audit WHERE id='audit'"))
        self.assertEqual(self.academic_call('moderation/messages/msg/retention-hold',{'active':False,'reason':'No puede cerrar apelación'}).status_code,409)

    def test_appeal_detail_media_auth_csrf_and_hash_integrity(self):
        from .moderation import execute
        value=self.appeal_fixture()
        self.assertEqual(Client().get('/api/admin/v1/appeals').status_code,401)
        self.assertEqual(self.client.get('/api/admin/v1/appeals').json()[0]['reason'],'Contexto educativo')
        self.assertEqual(self.client.get('/api/admin/v1/appeals/appeal').json()['text'],'hola')
        self.assertEqual(self.client.get('/api/admin/v1/appeals/appeal/media').content,b'archivo')
        self.assertEqual(self.client.post('/api/admin/v1/appeals/appeal/resolve',json.dumps(value),content_type='application/json').status_code,403)
        execute("UPDATE message_attachment_chunk SET payload=%s",[b'bad'])
        self.assertEqual(self.client.get('/api/admin/v1/appeals/appeal/media').status_code,409)

    def test_appeal_stale_revision_cannot_return_and_closes_as_superseded(self):
        from .moderation import execute,rows,now
        value=self.appeal_fixture();self.login('reviewer_test')
        execute("INSERT INTO message_revision(message_id,revision,body,state,content_hash) VALUES('msg',2,'nuevo','PENDING','new')")
        self.assertEqual(self.academic_call('appeals/appeal/resolve',value).status_code,409)
        self.assertEqual(self.academic_call('appeals/appeal/resolve',dict(value,outcome='SUPERSEDED')).status_code,200)
        self.assertEqual(rows('SELECT state FROM message_revision WHERE revision=2')[0]['state'],'PENDING')

    def test_expired_status_appeal_can_close_without_extending_lifetime(self):
        from .moderation import execute,rows,now
        value=self.appeal_fixture(status=True);self.login('reviewer_test')
        expired=now()-timedelta(days=1);execute('UPDATE status_update SET expires_at=%s',[expired])
        self.assertEqual(self.academic_call('appeals/appeal/resolve',value).status_code,409)
        self.assertEqual(self.academic_call('appeals/appeal/resolve',dict(value,outcome='SUPERSEDED')).status_code,200)
        self.assertEqual(rows('SELECT expires_at,state FROM status_update')[0],{'expires_at':expired,'state':'REJECTED'})

    def test_appeal_resolution_rolls_back_and_resolved_history_expires(self):
        from .moderation import rows,execute,now
        from .retention import batch
        value=self.appeal_fixture();self.login('reviewer_test')
        with patch('administration.retention.event',side_effect=IntegrityError('synthetic')):
            self.assertEqual(self.academic_call('appeals/appeal/resolve',value).status_code,409)
        self.assertEqual(rows('SELECT state FROM moderation_appeal')[0]['state'],'OPEN')
        self.assertEqual(rows('SELECT state FROM message_revision')[0]['state'],'REJECTED')
        self.assertEqual(self.academic_call('appeals/appeal/resolve',dict(value,outcome='UPHELD')).status_code,200)
        old=now()-timedelta(days=90);execute('UPDATE moderation_appeal SET decided_at=%s',[old])
        self.assertEqual(batch(apply=True)['counts']['appealsPurged'],1)

    def reviewer_fixture(self,initial=False):
        SharedUser.objects.create(username='limited_reviewer',display_name='Revisor sintético',role='REVIEWER',password_hash=HASH,enabled=True,password_change_required=initial)
        return self.login('limited_reviewer')

    def test_reviewer_can_access_review_but_never_academic_identity_or_audit_modules(self):
        self.assertEqual(self.reviewer_fixture().status_code,200)
        for path in ('moderation/messages','moderation/statuses','appeals','retention-holds'):
            self.assertEqual(self.client.get('/api/admin/v1/'+path).status_code,200,path)
        for path in ('users','courses','audit'):
            self.assertEqual(self.client.get('/api/admin/v1/'+path).status_code,403,path)
        self.assertEqual(self.client.get('/api/admin/actividades').status_code,403)
        self.assertEqual(self.academic_call('timetable',{}).status_code,403)
        self.assertEqual(self.academic_call('courses/1/grades',{},'put').status_code,403)
        self.assertEqual(self.client.get('/api/admin/perfil').json()['rol'],'REVIEWER')
        self.assertEqual(self.academic_call('users',{'username':'escalation','role':'ADMIN'}).status_code,403)

    def test_initial_reviewer_must_set_password_before_reading_any_content(self):
        self.assertTrue(self.reviewer_fixture(initial=True).json()['mustChangePassword'])
        response=self.client.get('/api/admin/v1/moderation/messages')
        self.assertEqual(response.status_code,403);self.assertEqual(response.json()['error']['code'],'PASSWORD_CHANGE_REQUIRED')
        self.assertEqual(self.client.get('/moderation.html').status_code,403)
        self.assertEqual(self.client.get('/api/admin/perfil').status_code,200)
        self.assertEqual(self.client.post('/api/admin/logout',HTTP_X_CSRFTOKEN=self.token()).status_code,200)

    def change_own_password(self,current=PASSWORD,new='SyntheticPersonalReview2026!'):
        return self.client.post('/api/admin/reviewer/password',json.dumps({'currentPassword':current,'newPassword':new}),content_type='application/json',HTTP_X_CSRFTOKEN=self.token())

    def test_reviewer_password_change_revokes_other_sessions_and_old_credentials(self):
        self.reviewer_fixture(initial=True)
        other=Client(enforce_csrf_checks=True);csrf=other.get('/api/admin/csrf').json()['token']
        self.assertEqual(other.post('/api/admin/login',{'username':'limited_reviewer','password':PASSWORD},HTTP_X_CSRFTOKEN=csrf).status_code,200)
        self.assertEqual(self.change_own_password().status_code,200)
        self.assertFalse(self.client.get('/api/admin/perfil').json()['mustChangePassword'])
        self.assertEqual(self.client.get('/api/admin/v1/moderation/messages').status_code,200)
        self.assertEqual(other.get('/api/admin/perfil').status_code,401)
        self.assertEqual(self.login('limited_reviewer').status_code,401)
        self.assertEqual(self.login('limited_reviewer','SyntheticPersonalReview2026!').status_code,200)

    def test_reviewer_password_requires_current_secret_csrf_and_atomic_audit(self):
        self.reviewer_fixture(initial=True)
        self.assertEqual(self.change_own_password(current='incorrect').status_code,400)
        self.assertEqual(self.change_own_password(new=PASSWORD).status_code,400)
        self.assertEqual(self.change_own_password(new='short').status_code,400)
        self.assertEqual(self.client.post('/api/admin/reviewer/password','{}',content_type='application/json').status_code,403)
        with patch('administration.review_accounts.record',side_effect=IntegrityError('synthetic')):
            self.assertEqual(self.change_own_password().status_code,409)
        self.assertTrue(SharedUser.objects.get(username='limited_reviewer').password_change_required)
        self.assertEqual(self.login('limited_reviewer').status_code,200)

    def test_reviewer_provision_is_cli_only_and_never_overwrites_existing_identity(self):
        from django.core.management import call_command
        from django.core.management.base import CommandError
        from io import StringIO
        with patch.dict('os.environ',{'MAIN_REVIEWER_INITIAL_PASSWORD':'SyntheticInitialReview2026!'}):
            call_command('provision_reviewer',username='new.reviewer',name='Revisor sintético',stdout=StringIO())
        user=SharedUser.objects.get(username='new.reviewer');digest=user.password_hash
        self.assertEqual(user.role,'REVIEWER');self.assertTrue(user.password_change_required)
        with self.assertRaises(CommandError):call_command('provision_reviewer',username='new.reviewer',name='Otro',stdout=StringIO())
        self.assertEqual(SharedUser.objects.get(username='new.reviewer').password_hash,digest)
        self.login()
        self.assertEqual(self.academic_call('users',dict(username='escalation',name='No autorizado',role='REVIEWER',password=PASSWORD)).status_code,400)
        self.assertEqual(self.academic_call('users/new.reviewer/disable',{}).status_code,403)

    def test_disabled_reviewer_is_immediately_revoked_and_cannot_be_a_contact(self):
        self.reviewer_fixture();SharedUser.objects.filter(username='limited_reviewer').update(enabled=False)
        self.assertEqual(self.client.get('/api/admin/v1/moderation/messages').status_code,401)
        SharedUser.objects.filter(username='limited_reviewer').update(enabled=True)
        self.login();self.assertEqual(self.academic_call('contacts',dict(first='alumno',second='limited_reviewer')).status_code,400)

    def test_limited_reviewer_can_resolve_appeal_independently(self):
        value=self.appeal_fixture();self.reviewer_fixture()
        self.assertEqual(self.academic_call('appeals/appeal/resolve',value).status_code,200)
        self.assertEqual(self.client.get('/api/admin/v1/users').status_code,403)

    def test_reviewer_password_limits_repeated_wrong_current_secret(self):
        self.reviewer_fixture(initial=True)
        for attempt in range(8):self.assertEqual(self.change_own_password(current='incorrect').status_code,400)
        self.assertEqual(self.change_own_password().status_code,429)

    def test_unactivated_reviewer_cannot_enable_automatic_processing(self):
        from .automatic_moderation import run_batch
        from unittest.mock import Mock
        self.reviewer_fixture(initial=True);provider=Mock()
        env=dict(MAIN_MODERATION_POLICY_APPROVED='true',MAIN_MODERATION_POLICY_VERSION='synthetic-reviewer-test',MAIN_MODERATION_REVIEWER='limited_reviewer',MAIN_MODERATION_RETENTION_DAYS='30')
        with self.assertRaisesRegex(ValueError,'REVIEWER_NOT_AUTHORIZED'):run_batch(provider,env=env)
        SharedUser.objects.filter(username='limited_reviewer').update(password_change_required=False)
        self.assertEqual(run_batch(provider,env=env),{})
        provider.assess.assert_not_called()
