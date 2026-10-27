"""Synthetic MySQL checks inside a rollback-only transaction. No real cleanup."""
import os, sys, json, uuid
from datetime import timedelta
from pathlib import Path
repo=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(repo/'admin-django'))
os.environ.setdefault('DJANGO_SETTINGS_MODULE','main_admin.settings')
import django
django.setup()
from django.db import transaction
from administration import retention as service, moderation as human

checks=[]
def check(name, value):
    assert value, name
    checks.append(dict(name=name,passed=True))

with transaction.atomic():
    instant=human.now(); old=instant-timedelta(days=100)
    owner='retention_'+uuid.uuid4().hex[:20]; conv=str(uuid.uuid4()); mid=str(uuid.uuid4()); status=str(uuid.uuid4())
    human.execute('INSERT INTO main_user(username,display_name,role,password_hash,enabled) VALUES(%s,%s,%s,%s,TRUE)',[owner,'Prueba sintética retención','ALUMNO','disabled-synthetic-no-login'])
    human.execute('INSERT INTO conversation(id,kind,title,created_by,created_at) VALUES(%s,%s,%s,%s,%s)',[conv,'GROUP','Prueba sintética',owner,instant])
    human.execute('INSERT INTO message(id,conversation_id,sender,client_key,created_at) VALUES(%s,%s,%s,%s,%s)',[mid,conv,owner,str(uuid.uuid4()),old])
    human.execute("INSERT INTO message_revision(message_id,revision,body,state,content_hash,submitted_at) VALUES(%s,1,'contenido sintético','APPROVED','approved',%s)",[mid,old])
    human.execute('UPDATE message SET published_revision=1 WHERE id=%s',[mid])
    human.execute("INSERT INTO message_attachment VALUES(%s,'FILE','text/plain','synthetic.txt',4,'synthetic','synthetic')",[mid])
    human.execute('INSERT INTO message_attachment_chunk VALUES(%s,0,%s)',[mid,b'test'])
    human.execute("INSERT INTO message_revision(message_id,revision,body,state,content_hash) VALUES(%s,2,'edición reciente','PENDING','fresh')",[mid])
    selected=human.rows('SELECT * FROM message_revision WHERE message_id=%s AND revision=2',[mid])[0]
    check('fresh_edit_of_old_message_kept',service.minimize('messages',selected,instant,True)=='changed')
    human.execute('UPDATE message_revision SET submitted_at=%s WHERE message_id=%s AND revision=2',[instant-timedelta(days=8),mid])
    selected=human.rows('SELECT * FROM message_revision WHERE message_id=%s AND revision=2',[mid])[0]
    check('preview_no_write',service.minimize('messages',selected,instant,False)=='eligible' and human.rows('SELECT body FROM message_revision WHERE message_id=%s AND revision=2',[mid])[0]['body']=='edición reciente')
    human.execute('INSERT INTO moderation_retention_hold(subject_kind,subject_id,active,reason,opened_by,opened_at) VALUES(%s,%s,TRUE,%s,%s,%s)',['messages',mid,'Apelación sintética',owner,instant])
    check('open_appeal_blocks_cleanup',service.minimize('messages',selected,instant,True)=='protected')
    human.execute('UPDATE moderation_retention_hold SET active=FALSE,closed_at=%s WHERE subject_id=%s',[instant,mid])
    check('stale_hash_refused',service.minimize('messages',dict(selected,content_hash='wrong'),instant,True)=='changed')
    check('old_pending_edit_minimized',service.minimize('messages',selected,instant,True)=='purged')
    check('published_text_unchanged',human.rows('SELECT body FROM message_revision WHERE message_id=%s AND revision=1',[mid])[0]['body']=='contenido sintético')
    check('published_attachment_unchanged',human.rows('SELECT payload FROM message_attachment_chunk WHERE message_id=%s',[mid])[0]['payload']==b'test')
    check('idempotent',service.minimize('messages',selected,instant,True)=='changed')
    human.execute("INSERT INTO status_update(id,owner,client_key,body,background,audience,kind,state,content_hash,created_at,expires_at,retention_started_at) VALUES(%s,%s,%s,'sintético','#008800','PRIVATE','VIDEO','PENDING','status',%s,%s,%s)",[status,owner,str(uuid.uuid4()),old,instant-timedelta(days=99),old])
    human.execute('INSERT INTO status_media_chunk VALUES(%s,0,%s)',[status,b'synthetic'])
    selected_status=human.rows('SELECT * FROM status_update WHERE id=%s',[status])[0]
    check('expired_pending_status_minimized',service.minimize('statuses',selected_status,instant,True)=='purged')
    check('status_bytes_removed',not human.rows('SELECT payload FROM status_media_chunk WHERE status_id=%s',[status]))
    # Exercise MySQL-specific audit/hold selection using read-only preview.
    preview=service.batch(limit=1,instant=instant)
    check('mysql_batch_sql_supported',preview['mode']=='preview')
    transaction.set_rollback(True)
check('fixtures_rolled_back',not human.rows('SELECT username FROM main_user WHERE username=%s',[owner]))
result=dict(synthetic=True,rollbackOnly=True,realProviderUsed=False,passed=len(checks),checks=checks)
target=Path(os.environ.get('MAIN_RETENTION_EVIDENCE','var/retention-mysql.json'))
target.parent.mkdir(parents=True,exist_ok=True); target.write_text(json.dumps(result,indent=2),encoding='utf-8')
print(str(len(checks))+' comprobaciones MySQL aprobadas; datos sintéticos revertidos.')
