"""Revisión humana explícita sobre las mismas revisiones y archivos de Spring."""
import hashlib
import re
import uuid
from datetime import datetime
from zoneinfo import ZoneInfo
from django.conf import settings
from django.db import connection, transaction
from django.http import JsonResponse, HttpResponse
from django.views.decorators.http import require_GET, require_POST
from .views import admin_only, error, pagination, STATIC
from .academic import body, record
from .identity import mutation, text
from .grades import timestamp


def now():
    return datetime.now(ZoneInfo(settings.TIME_ZONE)).replace(tzinfo=None)


def rows(sql, args=()):
    with connection.cursor() as cursor:
        cursor.execute(sql,args)
        keys=[c[0] for c in cursor.description]
        return [dict(zip(keys,r)) for r in cursor.fetchall()]


def execute(sql,args=()):
    with connection.cursor() as cursor:
        cursor.execute(sql,args)
        return cursor.rowcount


def message(mid):
    values=rows('SELECT m.*,r.revision,r.body,r.state,r.reason,r.content_hash,u.display_name FROM message m JOIN message_revision r ON r.message_id=m.id JOIN main_user u ON u.username=m.sender WHERE m.id=%s AND r.revision=(SELECT MAX(revision) FROM message_revision WHERE message_id=m.id)',[mid])
    return values[0] if values else None


def attachment(mid):
    values=rows('SELECT * FROM message_attachment WHERE message_id=%s',[mid])
    return values[0] if values else None


def message_data(m):
    media=attachment(m['id'])
    return dict(id=m['id'],conversationId=m['conversation_id'],sender=m['sender'],senderName=m['display_name'],
                revision=m['revision'],text=m['body'],state=m['state'],reason=m['reason'] or '',
                correctedBeforeDelivery=bool(m['corrected_before']),editedAfterDelivery=bool(m['edited_after']),
                createdAt=timestamp(m['created_at']),contentHash=m['content_hash'],
                attachmentKind=media['kind'] if media else 'NONE',attachmentMime=media['mime'] if media else '',
                attachmentName=media['file_name'] if media else '',attachmentSize=media['byte_size'] if media else 0)


def status_data(s):
    def instant(value):
        return value.replace(tzinfo=ZoneInfo(settings.TIME_ZONE)).isoformat()
    return dict(id=s['id'],owner=s['owner'],name=s['display_name'],text=s['body'],background=s['background'],
                audience=s['audience'],kind=s['kind'],state=s['state'],contentHash=s['content_hash'],
                createdAt=instant(s['created_at']),expiresAt=instant(s['expires_at']),reason=s['reason'] or '',seen=False,views=0)


@require_GET
@admin_only
def queue(request, kind):
    if kind not in ('messages','statuses'):
        return error(404,'NOT_FOUND')
    page=pagination(request)
    if page is None:
        return error(400,'VALIDATION_ERROR','Paginación inválida.')
    offset,limit=page
    if kind=='messages':
        values=rows("SELECT m.id FROM message m JOIN message_revision r ON r.message_id=m.id WHERE r.state='PENDING' AND r.revision=(SELECT MAX(revision) FROM message_revision WHERE message_id=m.id) ORDER BY m.created_at,m.id LIMIT %s OFFSET %s",[limit,offset])
        return JsonResponse([message_data(m) for v in values if (m:=message(v['id'])) and m['state']=='PENDING'],safe=False)
    values=rows("SELECT s.*,u.display_name FROM status_update s JOIN main_user u ON u.username=s.owner WHERE s.state='PENDING' AND s.expires_at>%s ORDER BY s.created_at,s.id LIMIT %s OFFSET %s",[now(),limit,offset])
    return JsonResponse([status_data(s) for s in values],safe=False)


def blob(table, column, item):
    # Los identificadores proceden de constantes internas; el ID usa parámetro SQL.
    return b''.join(bytes(r['payload']) for r in rows(f'SELECT payload FROM {table} WHERE {column}=%s ORDER BY sequence_no',[item]))


def message_bytes(mid, media):
    data=blob('message_attachment_chunk','message_id',mid)
    if len(data)!=media['byte_size'] or hashlib.sha256(data).hexdigest()!=media['content_hash']:
        raise ValueError('La integridad del adjunto no es válida; no se permite publicarlo.')
    return data


def status_bytes(s):
    data=blob('status_media_chunk','status_id',s['id'])
    calculated=hashlib.sha256((s['kind']+'\n'+s['body']+'\n'+s['background']+'\n'+s['audience']+'\n').encode()+data).hexdigest()
    if calculated!=s['content_hash']:
        raise ValueError('La integridad del estado no es válida; no se permite publicarlo.')
    return data


@require_GET
@admin_only
def download(request, kind, item_id):
    try:
        with transaction.atomic():
            if kind=='messages':
                m=message(item_id)
                raw=request.GET.get('revision','')
                if not m or not re.fullmatch(r'[0-9]+',raw) or m['revision']!=int(raw) or m['state']!='PENDING':
                    return error(404,'NOT_FOUND')
                media=attachment(item_id)
                if not media:
                    return error(404,'NOT_FOUND')
                data=message_bytes(item_id,media)
                mime=media['mime']; name=media['file_name']; disposition='attachment' if media['kind']=='FILE' else 'inline'
            else:
                values=rows("SELECT * FROM status_update WHERE id=%s AND state='PENDING' AND expires_at>%s",[item_id,now()])
                if not values:
                    return error(404,'NOT_FOUND')
                s=values[0];data=status_bytes(s)
                mime={'IMAGE':'image/jpeg','VIDEO':'video/mp4','AUDIO':'audio/mp4'}.get(s['kind'],'application/octet-stream')
                name='estado';disposition='inline'
        response=HttpResponse(data,content_type=mime)
        response['Cache-Control']='no-store'
        response['X-Content-Type-Options']='nosniff'
        response['Content-Disposition']=f'{disposition}; filename="{re.sub(r"[^a-zA-Z0-9._-]", "_", name)}"'
        return response
    except ValueError as exc:
        return error(409,'CONTENT_INTEGRITY',str(exc))


def can_publish(m, conversation):
    active=rows("SELECT u.username FROM main_user u JOIN conversation_member cm ON cm.username=u.username WHERE u.username=%s AND u.enabled=TRUE AND cm.conversation_id=%s AND cm.state='ACTIVE'",[m['sender'],m['conversation_id']])
    if not active:
        return False
    if conversation['kind']!='DIRECT':
        return True
    others=rows('SELECT u.username,u.enabled FROM conversation_member cm JOIN main_user u ON u.username=cm.username WHERE cm.conversation_id=%s AND cm.username<>%s',[m['conversation_id'],m['sender']])
    if len(others)!=1 or not others[0]['enabled']:
        return False
    other=others[0]['username'];first,second=sorted([other,m['sender']])
    return bool(rows('SELECT user_a FROM contact_link WHERE user_a=%s AND user_b=%s',[first,second])) and not blocked(other,m['sender'])


def blocked(first,second):
    return bool(rows('SELECT owner FROM contact_preference WHERE blocked=TRUE AND ((owner=%s AND subject=%s) OR (owner=%s AND subject=%s))',[first,second,second,first]))


def notify(m):
    recipients=rows("SELECT u.* FROM conversation_member cm JOIN main_user u ON u.username=cm.username WHERE cm.conversation_id=%s AND cm.state='ACTIVE' AND cm.joined_at<=%s AND u.enabled=TRUE AND u.notifications=TRUE AND u.username<>%s",[m['conversation_id'],m['created_at'],m['sender']])
    for recipient in recipients:
        who=recipient['username'];event='message:'+m['id']
        if blocked(who,m['sender']) or rows('SELECT owner FROM contact_preference WHERE owner=%s AND subject=%s AND muted=TRUE',[who,m['sender']]):
            continue
        if not rows('SELECT id FROM notification WHERE recipient=%s AND event_key=%s',[who,event]):
            execute('INSERT INTO notification(id,recipient,sender,conversation_id,message_id,kind,event_key,created_at) VALUES(%s,%s,%s,%s,%s,%s,%s,%s)',[str(uuid.uuid4()),who,m['sender'],m['conversation_id'],m['id'],'MESSAGE',event,now()])


@require_POST
@admin_only
@mutation
def review(request, kind, item_id):
    value=body(request, {'revision','contentHash','approved','reason','attachmentReviewed'})
    if type(value.get('approved')) is not bool or not isinstance(value.get('contentHash'),str):
        raise ValueError('Decisión explícita y huella del contenido requeridas.')
    approved=value['approved'];state='APPROVED' if approved else 'REJECTED'
    reason='' if approved else text(value.get('reason'),300)
    if kind=='statuses':
        suffix=' FOR UPDATE' if connection.vendor!='sqlite' else ''
        values=rows('SELECT * FROM status_update WHERE id=%s'+suffix,[item_id])
        if not values or values[0]['state']!='PENDING' or values[0]['expires_at']<=now() or values[0]['content_hash']!=value['contentHash']:
            return error(409,'CONFLICT','El estado cambió o caducó. Actualiza la cola.')
        s=values[0]
        if s['owner']==request.admin_actor:
            return error(403,'FORBIDDEN')
        if approved:
            if s['kind']!='TEXT' and value.get('attachmentReviewed') is not True:
                raise ValueError('Confirma la revisión completa de la imagen, video o audio.')
            status_bytes(s)
            if not rows('SELECT username FROM main_user WHERE username=%s AND enabled=TRUE',[s['owner']]):
                return error(403,'FORBIDDEN')
        execute('UPDATE status_update SET state=%s,reason=%s,reviewed_by=%s,reviewed_at=%s WHERE id=%s',[state,reason,request.admin_actor,now(),item_id])
        record(request,'STATUS_APPROVED' if approved else 'STATUS_REJECTED',item_id)
    else:
        m=message(item_id)
        if not m:
            return error(404,'NOT_FOUND')
        suffix=' FOR UPDATE' if connection.vendor!='sqlite' else ''
        conversation=rows('SELECT * FROM conversation WHERE id=%s'+suffix,[m['conversation_id']])[0]
        m=message(item_id) # Releer después de adquirir el mismo bloqueo que usa Spring.
        if m['sender']==request.admin_actor:
            return error(403,'FORBIDDEN')
        if type(value.get('revision')) is not int or value['revision']!=m['revision'] or value['contentHash']!=m['content_hash'] or m['state']!='PENDING':
            return error(409,'REVISION_CONFLICT','La versión cambió. Actualiza la cola.')
        media=attachment(item_id)
        if approved:
            if media and value.get('attachmentReviewed') is not True:
                raise ValueError('Confirma la revisión del adjunto completo.')
            if media:
                message_bytes(item_id,media)
            digest=hashlib.sha256((m['body'] if not media else 'MAIN-ATTACHMENT\n'+m['body']+'\x00'+media['content_hash']).encode()).hexdigest()
            if digest!=m['content_hash']:
                raise ValueError('La integridad de la revisión no es válida.')
            if not can_publish(m,conversation):
                return error(403,'FORBIDDEN','El emisor ya no puede publicar en esta conversación.')
        execute('UPDATE message_revision SET state=%s,reason=%s,reviewed_by=%s,reviewed_at=%s WHERE message_id=%s AND revision=%s',[state,reason,request.admin_actor,now(),item_id,m['revision']])
        record(request,'MESSAGE_APPROVED' if approved else 'MESSAGE_REJECTED',f"{item_id}:{m['revision']}")
        if approved:
            published=m['published_revision'] is not None
            corrected=bool(m['corrected_before']) if published else bool(rows("SELECT revision FROM message_revision WHERE message_id=%s AND state='REJECTED'",[item_id]))
            execute('UPDATE message SET published_revision=%s,corrected_before=%s,edited_after=%s WHERE id=%s',[m['revision'],corrected,published,item_id])
            if not published:
                notify(m)
    return JsonResponse({'reviewed':True})


@require_GET
@admin_only
def page(request, name='moderation.html'):
    if name not in ('moderation.html','moderation.js','moderation.css'):
        return error(404,'NOT_FOUND')
    return HttpResponse((STATIC/name).read_bytes(),content_type={'.html':'text/html','.js':'text/javascript','.css':'text/css'}[__import__('pathlib').Path(name).suffix])
