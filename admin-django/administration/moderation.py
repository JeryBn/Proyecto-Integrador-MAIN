"""Revisión humana explícita sobre las mismas revisiones y archivos de Spring."""
import hashlib
import json
import re
from datetime import datetime
from zoneinfo import ZoneInfo
from django.conf import settings
from django.db import connection, transaction
from django.http import JsonResponse, HttpResponse
from django.views.decorators.http import require_GET, require_POST
from .views import review_only, error, pagination, STATIC
from .academic import ValidationError, body, record
from .identity import mutation, text


def now():
    return datetime.now(ZoneInfo(settings.TIME_ZONE)).replace(tzinfo=None)


@require_GET
@review_only
def campus_queue(request):
    page = pagination(request)
    if page is None:
        return error(400, 'INVALID_PAGINATION')
    offset, limit = page
    return JsonResponse(rows("""SELECT p.id,p.body,u.display_name FROM campus_post p
        JOIN main_user u ON u.username=p.author WHERE p.state='PENDING' AND p.withdrawn=FALSE
        ORDER BY p.created_at,p.id LIMIT %s OFFSET %s""", [limit,offset]), safe=False)


@require_POST
@review_only
@mutation
def campus_review(request, item_id):
    try:
        value = body(request, {'approved'})
    except ValidationError:
        return error(400, 'INVALID_REQUEST')
    if not isinstance(value, dict) or type(value.get('approved')) is not bool:
        return error(400, 'INVALID_REQUEST')
    state = 'PUBLISHED' if value['approved'] else 'REJECTED'
    with transaction.atomic():
        if execute("""UPDATE campus_post SET state=%s,reviewed_at=%s
                WHERE id=%s AND state='PENDING' AND withdrawn=FALSE""", [state,now(),item_id]) != 1:
            return error(409, 'CONFLICT', 'La publicación ya fue revisada o retirada.')
        record(request,'CAMPUS_POST_'+state,item_id)
    return JsonResponse({'state':state})


def rows(sql, args=()):
    with connection.cursor() as cursor:
        cursor.execute(sql,args)
        keys=[c[0] for c in cursor.description]
        return [dict(zip(keys,r)) for r in cursor.fetchall()]


def execute(sql,args=()):
    with connection.cursor() as cursor:
        cursor.execute(sql,args)
        return cursor.rowcount


def assessment_summary(kind,item,digest):
    values=rows('SELECT outcome,policy_version,coverage,categories_json,sampled_frames,duration_seconds FROM automatic_assessment WHERE subject_kind=%s AND subject_id=%s AND content_hash=%s ORDER BY updated_at DESC,id DESC LIMIT 1',[kind,item,digest])
    if not values:return None
    value=values[0]
    try:categories=json.loads(value['categories_json'] or '[]')
    except (TypeError,ValueError):categories=[]
    # Only metadata for the current exact hash reaches the authenticated queue.
    if not isinstance(categories,list):categories=[]
    categories=[category for category in categories if isinstance(category,str) and re.fullmatch(r'[a-z/-]{1,60}',category)][:20]
    return dict(outcome=value['outcome'],policyVersion=value['policy_version'],coverage=value['coverage'] or 'NOT_ANALYZED',
        categories=categories,sampledFrames=value['sampled_frames'] or 0,
        durationSeconds=float(value['duration_seconds']) if value['duration_seconds'] is not None else None,
        humanRequired=True)


def status_data(s):
    def instant(value):
        return value.replace(tzinfo=ZoneInfo(settings.TIME_ZONE)).isoformat()
    return dict(id=s['id'],owner=s['owner'],name=s['display_name'],text=s['body'],background=s['background'],
                audience=s['audience'],kind=s['kind'],state=s['state'],contentHash=s['content_hash'],
                createdAt=instant(s['created_at']),expiresAt=instant(s['expires_at']),reason=s['reason'] or '',seen=False,views=0,
                automaticReview=assessment_summary('statuses',s['id'],s['content_hash']))


@require_GET
@review_only
def queue(request, kind):
    if kind != 'statuses':
        return error(404,'NOT_FOUND')
    page=pagination(request)
    if page is None:
        return error(400,'VALIDATION_ERROR','Paginación inválida.')
    offset,limit=page
    values=rows("SELECT s.*,u.display_name FROM status_update s JOIN main_user u ON u.username=s.owner WHERE s.state='PENDING' AND s.expires_at>%s ORDER BY s.created_at,s.id LIMIT %s OFFSET %s",[now(),limit,offset])
    return JsonResponse([status_data(s) for s in values],safe=False)


def blob(table, column, item):
    # Los identificadores proceden de constantes internas; el ID usa parámetro SQL.
    return b''.join(bytes(r['payload']) for r in rows(f'SELECT payload FROM {table} WHERE {column}=%s ORDER BY sequence_no',[item]))


def status_bytes(s):
    data=blob('status_media_chunk','status_id',s['id'])
    calculated=hashlib.sha256((s['kind']+'\n'+s['body']+'\n'+s['background']+'\n'+s['audience']+'\n').encode()+data).hexdigest()
    if calculated!=s['content_hash']:
        raise ValidationError('La integridad del estado no es válida; no se permite publicarlo.')
    return data


@require_GET
@review_only
def download(request, kind, item_id):
    if kind != 'statuses':
        return error(404,'NOT_FOUND')
    try:
        with transaction.atomic():
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


@require_POST
@review_only
@mutation
def review(request, kind, item_id):
    if kind != 'statuses':
        return error(404,'NOT_FOUND')
    value=body(request, {'revision','contentHash','approved','reason','attachmentReviewed'})
    return review_value(request,kind,item_id,value)


def review_value(request,kind,item_id,value,*,automated=False):
    """Shared publication path; caller owns transaction and authorized identity."""
    if type(value.get('approved')) is not bool or not isinstance(value.get('contentHash'),str):
        raise ValidationError('Decisión explícita y huella del contenido requeridas.')
    approved=value['approved'];state='APPROVED' if approved else 'REJECTED'
    reason='' if approved else text(value.get('reason'),300)
    if kind != 'statuses':
        return error(404,'NOT_FOUND')
    suffix=' FOR UPDATE' if connection.vendor!='sqlite' else ''
    values=rows('SELECT * FROM status_update WHERE id=%s'+suffix,[item_id])
    if not values or values[0]['state']!='PENDING' or values[0]['expires_at']<=now() or values[0]['content_hash']!=value['contentHash']:
        return error(409,'CONFLICT','El estado cambió o caducó. Actualiza la cola.')
    s=values[0]
    if s['owner']==request.admin_actor:
        return error(403,'FORBIDDEN')
    if approved:
        if s['kind']!='TEXT' and value.get('attachmentReviewed') is not True:
            raise ValidationError('Confirma la revisión completa de la imagen, video o audio.')
        status_bytes(s)
        if not rows('SELECT username FROM main_user WHERE username=%s AND enabled=TRUE',[s['owner']]):
            return error(403,'FORBIDDEN')
    execute('UPDATE status_update SET state=%s,reason=%s,reviewed_by=%s,reviewed_at=%s WHERE id=%s',[state,reason,None if automated else request.admin_actor,now(),item_id])
    record(request,'STATUS_APPROVED' if approved else 'STATUS_REJECTED',item_id)
    return JsonResponse({'reviewed':True})


@require_GET
@review_only
def page(request, name='moderation.html'):
    if name not in ('moderation.html','moderation.js','moderation.css'):
        return error(404,'NOT_FOUND')
    response=HttpResponse((STATIC/name).read_bytes(),content_type={'.html':'text/html','.js':'text/javascript','.css':'text/css'}[__import__('pathlib').Path(name).suffix])
    response['Cache-Control']='no-store'
    return response
