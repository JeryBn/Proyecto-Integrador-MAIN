"""Appeal decisions return a version to human review, never publish it."""
from django.db import transaction
from django.http import JsonResponse, HttpResponse
from django.views.decorators.http import require_GET, require_POST
from . import moderation as human, retention
from .academic import body
from .identity import mutation, text
from .views import admin_only, error, pagination

def find(appeal_id):
    values=human.rows('SELECT * FROM moderation_appeal WHERE id=%s',[appeal_id])
    return values[0] if values else None

def content(appeal):
    if appeal['subject_kind']=='messages':
        values=human.rows('SELECT r.*,m.sender AS owner FROM message_revision r JOIN message m ON m.id=r.message_id WHERE r.message_id=%s AND r.revision=%s',[appeal['subject_id'],appeal['revision']])
    else:values=human.rows('SELECT * FROM status_update WHERE id=%s',[appeal['subject_id']])
    return values[0] if values else None

def current(appeal, value):
    if not value or value['purged_at'] is not None or value['content_hash']!=appeal['content_hash'] or value['state']!='REJECTED':return False
    if appeal['subject_kind']=='messages':
        latest=human.rows('SELECT MAX(revision) AS latest FROM message_revision WHERE message_id=%s',[appeal['subject_id']])[0]['latest']
        return latest==appeal['revision']
    return True

def data(appeal):
    return dict(id=appeal['id'],owner=appeal['owner'],kind=appeal['subject_kind'],subjectId=appeal['subject_id'],revision=appeal['revision'],contentHash=appeal['content_hash'],reason=appeal['request_reason'],state=appeal['state'],createdAt=appeal['created_at'].isoformat(),decisionReason=appeal['decision_reason'] or '')

@require_GET
@admin_only
def queue(request):
    page=pagination(request)
    if page is None:return error(400,'VALIDATION_ERROR')
    offset,limit=page
    return JsonResponse([data(a) for a in human.rows("SELECT * FROM moderation_appeal WHERE state='OPEN' ORDER BY created_at,id LIMIT %s OFFSET %s",[limit,offset])],safe=False)

@require_GET
@admin_only
def detail(request, appeal_id):
    appeal=find(appeal_id)
    if not appeal:return error(404,'NOT_FOUND')
    value=content(appeal); result=data(appeal)
    media=human.attachment(appeal['subject_id']) if appeal['subject_kind']=='messages' else None
    result.update(text=value['body'] if value and value['purged_at'] is None else '',current=current(appeal,value),
        mediaKind=media['kind'] if media else value['kind'] if value and appeal['subject_kind']=='statuses' else 'NONE',
        expired=bool(value and appeal['subject_kind']=='statuses' and value['expires_at']<=human.now()))
    return JsonResponse(result)

@require_GET
@admin_only
def download(request, appeal_id):
    appeal=find(appeal_id)
    if not appeal or appeal['state']!='OPEN':return error(404,'NOT_FOUND')
    value=content(appeal)
    if not value or value['purged_at'] is not None or value['content_hash']!=appeal['content_hash']:return error(409,'CONTENT_CHANGED')
    try:
        if appeal['subject_kind']=='messages':
            media=human.attachment(appeal['subject_id'])
            if not media:return error(404,'NOT_FOUND')
            payload=human.message_bytes(appeal['subject_id'],media);mime=media['mime']
        else:
            if value['kind']=='TEXT':return error(404,'NOT_FOUND')
            payload=human.status_bytes(value);mime={'IMAGE':'image/jpeg','VIDEO':'video/mp4','AUDIO':'audio/mp4'}[value['kind']]
        response=HttpResponse(payload,content_type=mime)
        response['Cache-Control']='no-store';response['X-Content-Type-Options']='nosniff'
        response['Content-Disposition']='inline; filename="contenido-apelado"'
        return response
    except ValueError:return error(409,'CONTENT_INTEGRITY')

@require_POST
@admin_only
@mutation
def resolve(request, appeal_id):
    value=body(request,{'outcome','reason','contentHash'})
    if value.get('outcome') not in ('UPHELD','RETURNED','SUPERSEDED'):raise ValueError('Decisión de apelación inválida.')
    reason=text(value.get('reason'),1000); appeal=find(appeal_id)
    if not appeal or not retention.parent(appeal['subject_kind'],appeal['subject_id']):return error(404,'NOT_FOUND')
    appeal=find(appeal_id)
    if appeal['state']!='OPEN' or value.get('contentHash')!=appeal['content_hash']:return error(409,'APPEAL_CONFLICT','La apelación ya cambió.')
    item=content(appeal); is_current=current(appeal,item)
    if appeal['owner']==request.admin_actor or (item and item['reviewed_by']==request.admin_actor):return error(403,'INDEPENDENT_REVIEW_REQUIRED','Debe resolver una persona distinta de quien rechazó el contenido.')
    outcome=value['outcome']; instant=human.now()
    if outcome=='RETURNED' and (not is_current or (appeal['subject_kind']=='statuses' and item['expires_at']<=instant)):
        return error(409,'CONTENT_CHANGED','La versión cambió o el estado caducó. Puedes cerrar con una explicación, sin republicar.')
    expired=bool(item and appeal['subject_kind']=='statuses' and item['expires_at']<=instant)
    if outcome=='SUPERSEDED' and is_current and not expired:return error(409,'CONTENT_CURRENT','La versión sigue vigente; confirma o devuelve a revisión.')
    if outcome=='RETURNED':
        if appeal['subject_kind']=='messages':
            human.execute("UPDATE message_revision SET state='PENDING',reason=NULL,reviewed_by=NULL,reviewed_at=NULL,submitted_at=%s WHERE message_id=%s AND revision=%s",[instant,appeal['subject_id'],appeal['revision']])
        else:human.execute("UPDATE status_update SET state='PENDING',reason=NULL,reviewed_by=NULL,reviewed_at=NULL,retention_started_at=%s WHERE id=%s",[instant,appeal['subject_id']])
    else:
        table,col,start=('message_revision','message_id','submitted_at') if appeal['subject_kind']=='messages' else ('status_update','id','retention_started_at')
        # Preserve reviewer attribution; retention uses the new conservative floor.
        human.execute(f'UPDATE {table} SET {start}=%s WHERE {col}=%s AND purged_at IS NULL',[instant,appeal['subject_id']])
    human.execute('UPDATE moderation_appeal SET state=%s,decision_reason=%s,decided_by=%s,decided_at=%s WHERE id=%s',[outcome,reason,request.admin_actor,instant,appeal_id])
    retention.event('APPEAL_RESOLVED',appeal['subject_id'],instant,request.admin_actor)
    return JsonResponse({'resolved':True,'outcome':outcome,'published':False})
