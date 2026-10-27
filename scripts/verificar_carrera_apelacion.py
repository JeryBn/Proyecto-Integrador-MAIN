"""Synthetic MySQL lock race. Requires an explicit local test operator, never production."""
import argparse,json,os,sys,time,threading,uuid
from datetime import timedelta
from pathlib import Path
from verificar_arquitectura_compartida import Session
parser=argparse.ArgumentParser();parser.add_argument('--configuration',type=Path,required=True);args=parser.parse_args()
config=json.loads(args.configuration.read_text(encoding='utf-8-sig'))
assert config['database']=='main_architecture_review' and config['port']==3307,'Solo la base local aislada de pruebas'
password=os.environ['MAIN_TEST_OPERATOR_PASSWORD']
os.environ.update(MAIN_DB_NAME=config['database'],MAIN_DB_HOST='127.0.0.1',MAIN_DB_PORT=str(config['port']),MAIN_DB_USER=os.getenv('MAIN_TEST_OPERATOR_USER','root'),MAIN_DB_PASSWORD=password,MAIN_DJANGO_SECRET_KEY=config['django_secret'],DJANGO_SETTINGS_MODULE='main_admin.settings')
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'admin-django'))
import django
django.setup()
from django.db import transaction
from administration import moderation as human,retention
owner=json.loads(Path('var/appeal-web-fixture.json').read_text())['owner']
client=Session('http://127.0.0.1:8088');_,token=client.call('/login-token')
assert client.call('/login','POST',dict(username=owner,password='SyntheticAppealOwner2026!',**{token['parameterName']:token['token']}),form=True)[0]==200
_,token=client.call('/api/csrf')
admin=Session('http://127.0.0.1:8090');_,csrf=admin.call('/api/admin/csrf')
assert admin.call('/api/admin/login','POST',dict(username='android_test_admin',password='SyntheticAndroidTest2026!'),csrf,True)[0]==200
_,csrf=admin.call('/api/admin/csrf')
code,group=client.call('/api/v1/conversations/groups','POST',dict(title='Carrera SINTETICA retención/apelación'),token);assert code==201
conversation=group['id'];message=None;result={};worker=None
try:
    code,message=client.call('/api/v1/conversations/'+conversation+'/messages','POST',dict(clientKey=str(uuid.uuid4()),text='Dato SINTETICO desechable para carrera de bloqueo'),token);assert code==202
    mid=message['id'];digest=message['contentHash']
    assert admin.call('/api/admin/v1/moderation/messages/'+mid+'/review','POST',dict(revision=1,contentHash=digest,approved=False,reason='Prueba sintética'),csrf)[0]==200
    instant=human.now()
    with transaction.atomic():
        assert retention.parent('messages',mid)
        human.execute('UPDATE message_revision SET submitted_at=%s,reviewed_at=%s WHERE message_id=%s',[instant-timedelta(days=31),instant-timedelta(days=31),mid])
        selected=human.rows('SELECT * FROM message_revision WHERE message_id=%s',[mid])[0]
        def appeal():
            try:result['httpStatus']=client.call('/api/v1/appeals','POST',dict(kind='messages',subjectId=mid,revision=1,contentHash=digest,reason='Apelación sintética concurrente'),token)[0]
            except Exception:result['threadFailed']=True
        worker=threading.Thread(target=appeal,daemon=True);worker.start()
        deadline=time.monotonic()+12;waiting=False
        while time.monotonic()<deadline:
            waiting=human.rows("SELECT COUNT(*) AS total FROM performance_schema.data_lock_waits w JOIN performance_schema.data_locks b ON b.ENGINE_LOCK_ID=w.BLOCKING_ENGINE_LOCK_ID WHERE b.OBJECT_SCHEMA=%s AND b.OBJECT_NAME='conversation' AND b.LOCK_DATA LIKE %s",[config['database'],'%'+conversation+'%'])[0]['total']>0
            if waiting:break
            time.sleep(0.05)
        assert waiting,'No se observó la espera real de Spring por el bloqueo'
        result['observedLockWait']=True
        assert retention.minimize('messages',selected,instant,True)=='purged'
        result['retentionPurgedSyntheticVersion']=True
    worker.join(timeout=15)
    assert not worker.is_alive() and result.get('httpStatus')==409,'La apelación debe rechazar la versión ya eliminada'
    assert not human.rows('SELECT id FROM moderation_appeal WHERE subject_id=%s',[mid])
    result.update(noOrphanAppeal=True,synthetic=True,passed=4)
    Path('var/appeal-concurrency.json').write_text(json.dumps(result,indent=2))
    print('Cuatro condiciones de la carrera real MySQL aprobadas: espera, limpieza, HTTP 409 y ninguna apelación huérfana.')
finally:
    if worker and worker.is_alive():worker.join(timeout=15)
    with transaction.atomic():
        if message:
            mid=message['id']
            human.execute('DELETE FROM moderation_appeal WHERE subject_id=%s',[mid])
            for table in ('message_attachment_chunk','message_attachment','message_revision'):
                human.execute(f'DELETE FROM {table} WHERE message_id=%s',[mid])
            human.execute('DELETE FROM message WHERE id=%s',[mid])
            human.execute('DELETE FROM administrative_audit WHERE subject IN (%s,%s)',[mid,mid+':1'])
        human.execute('DELETE FROM conversation_member WHERE conversation_id=%s',[conversation])
        human.execute('DELETE FROM conversation WHERE id=%s',[conversation])
        human.execute('DELETE FROM administrative_audit WHERE subject=%s',[conversation])
