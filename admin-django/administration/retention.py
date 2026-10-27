"""Bounded, transactional content minimization; no provider or filesystem access."""
from datetime import timedelta
import uuid
from django.db import connection, transaction
from django.http import JsonResponse
from django.views.decorators.http import require_http_methods
from . import moderation as human
from .academic import body
from .identity import mutation, text
from .views import admin_only, error

POLICY = 'jery-2026-10-27-v1'
PENDING_DAYS, REJECTED_DAYS, AUDIT_DAYS = 7, 30, 90
REASON = 'Contenido eliminado por plazo de conservación.'

def locked(sql, args):
    return human.rows(sql + (' FOR UPDATE' if connection.vendor != 'sqlite' else ''), args)

def parent(kind, item):
    if kind == 'messages':
        values = human.rows('SELECT conversation_id FROM message WHERE id=%s', [item])
        return bool(values and locked('SELECT id FROM conversation WHERE id=%s', [values[0]['conversation_id']]))
    return bool(locked('SELECT id FROM status_update WHERE id=%s', [item]))

def held(kind, item):
    return bool(human.rows('SELECT subject_id FROM moderation_retention_hold WHERE subject_kind=%s AND subject_id=%s AND active=TRUE', [kind, item]))

def event(action, subject, instant, actor='retention-worker'):
    human.execute('INSERT INTO administrative_audit(id,actor,action,subject,occurred_at) VALUES(%s,%s,%s,%s,%s)',
                  [str(uuid.uuid4()), actor, action, subject, instant])

@require_http_methods(['GET', 'POST'])
@admin_only
@mutation
def hold(request, kind, item_id):
    if kind not in ('messages', 'statuses'):
        return error(404, 'NOT_FOUND')
    if not parent(kind, item_id):
        return error(404, 'NOT_FOUND')
    if request.method == 'GET':
        return JsonResponse({'active': held(kind, item_id)})
    value = body(request, {'active', 'reason'})
    if type(value.get('active')) is not bool:
        raise ValueError('Indica explícitamente activar o resolver la protección.')
    reason = text(value.get('reason'), 300)
    active = value['active']; instant = human.now()
    table, column = ('message_revision', 'message_id') if kind == 'messages' else ('status_update', 'id')
    if active and not human.rows(f'SELECT {column} FROM {table} WHERE {column}=%s AND purged_at IS NULL', [item_id]):
        return error(409, 'CONTENT_PURGED', 'El contenido ya fue eliminado; una protección no lo recupera.')
    # The same parent lock serializes hold changes, human review and purge.
    if held(kind, item_id) == active:
        return JsonResponse({'active': active, 'changed': False})
    existing = human.rows('SELECT subject_id FROM moderation_retention_hold WHERE subject_kind=%s AND subject_id=%s', [kind, item_id])
    if existing:
        if active:
            human.execute('UPDATE moderation_retention_hold SET active=TRUE,reason=%s,opened_by=%s,opened_at=%s,closed_by=NULL,closed_at=NULL WHERE subject_kind=%s AND subject_id=%s', [reason, request.admin_actor, instant, kind, item_id])
        else:
            human.execute('UPDATE moderation_retention_hold SET active=FALSE,reason=%s,closed_by=%s,closed_at=%s WHERE subject_kind=%s AND subject_id=%s', [reason, request.admin_actor, instant, kind, item_id])
            # Full new grace period after an appeal/hold is resolved.
            table, column, date = ('message_revision', 'message_id', 'submitted_at') if kind == 'messages' else ('status_update', 'id', 'retention_started_at')
            human.execute(f'UPDATE {table} SET {date}=%s,reviewed_at=CASE WHEN state=\'REJECTED\' THEN %s ELSE reviewed_at END WHERE {column}=%s AND purged_at IS NULL', [instant, instant, item_id])
    elif active:
        human.execute('INSERT INTO moderation_retention_hold(subject_kind,subject_id,active,reason,opened_by,opened_at) VALUES(%s,%s,TRUE,%s,%s,%s)', [kind, item_id, reason, request.admin_actor, instant])
    event('RETENTION_HOLD_OPENED' if active else 'RETENTION_HOLD_CLOSED', item_id, instant, request.admin_actor)
    return JsonResponse({'active': active, 'changed': True})

def eligible(value, start, instant):
    if value['purged_at'] is not None:
        return False
    if value['state'] == 'PENDING':
        return value[start] <= instant - timedelta(days=PENDING_DAYS)
    if value['state'] == 'REJECTED' and value['reviewed_at'] is not None:
        # Migration/start timestamp is a conservative floor for legacy decisions.
        return max(value[start], value['reviewed_at']) <= instant - timedelta(days=REJECTED_DAYS)
    return False

def selection(table, start, instant, limit):
    kind, key = ('messages', 'message_id') if table == 'message_revision' else ('statuses', 'id')
    published = ' AND NOT EXISTS(SELECT 1 FROM message m WHERE m.id=t.message_id AND m.published_revision=t.revision)' if kind == 'messages' else ''
    return human.rows(f"SELECT t.* FROM {table} t WHERE purged_at IS NULL AND NOT EXISTS(SELECT 1 FROM moderation_retention_hold h WHERE h.subject_kind='{kind}' AND h.subject_id=t.{key} AND h.active=TRUE){published} AND ((state='PENDING' AND {start}<=%s) OR (state='REJECTED' AND reviewed_at<=%s AND {start}<=%s)) ORDER BY {start},t.{key} LIMIT %s",
                      [instant-timedelta(days=PENDING_DAYS), instant-timedelta(days=REJECTED_DAYS), instant-timedelta(days=REJECTED_DAYS), limit])

def minimize(kind, selected, instant, apply):
    item = selected['message_id'] if kind == 'messages' else selected['id']
    with transaction.atomic():
        if not parent(kind, item) or held(kind, item):
            return 'protected'
        if kind == 'messages':
            values = human.rows('SELECT * FROM message_revision WHERE message_id=%s AND revision=%s', [item, selected['revision']])
            start = 'submitted_at'
        else:
            values = human.rows('SELECT * FROM status_update WHERE id=%s', [item]); start = 'retention_started_at'
        if not values or not eligible(values[0], start, instant) or values[0]['content_hash'] != selected['content_hash']:
            return 'changed'
        value = values[0]
        # Never modify a revision referenced as published, even if data is inconsistent.
        if kind == 'messages' and human.rows('SELECT id FROM message WHERE id=%s AND published_revision=%s', [item, value['revision']]):
            return 'protected'
        if not apply:
            return 'eligible'
        if kind == 'messages':
            human.execute("UPDATE message_revision SET body='',reason=%s,state='REJECTED',purged_at=%s WHERE message_id=%s AND revision=%s", [REASON, instant, item, value['revision']])
            # One attachment belongs to the message and can still serve approved/other revisions.
            if not human.rows('SELECT revision FROM message_revision WHERE message_id=%s AND purged_at IS NULL', [item]) and not human.rows('SELECT id FROM message WHERE id=%s AND published_revision IS NOT NULL', [item]):
                human.execute('DELETE FROM message_attachment_chunk WHERE message_id=%s', [item])
                human.execute('DELETE FROM message_attachment WHERE message_id=%s', [item])
            live = human.rows('SELECT revision FROM message_revision WHERE message_id=%s AND content_hash=%s AND purged_at IS NULL', [item, value['content_hash']])
            subject = f"{item}:{value['revision']}"
        else:
            human.execute("UPDATE status_update SET body='',reason=%s,state='DELETED',purged_at=%s WHERE id=%s", [REASON, instant, item])
            human.execute('DELETE FROM status_media_chunk WHERE status_id=%s', [item])
            human.execute('DELETE FROM status_view WHERE status_id=%s', [item])
            human.execute('DELETE FROM status_recipient WHERE status_id=%s', [item])
            live = False; subject = item
        if not live:
            human.execute('DELETE FROM automatic_assessment WHERE subject_kind=%s AND subject_id=%s AND content_hash=%s', [kind, item, value['content_hash']])
        event('RETENTION_CONTENT_PURGED', subject, instant)
        return 'purged'

def batch(limit=100, apply=False, instant=None):
    if type(limit) is not int or not 1 <= limit <= 500:
        raise ValueError('Lote entre 1 y 500.')
    instant = instant or human.now()
    counts = dict(eligible=0, purged=0, protected=0, changed=0, auditEligible=0, auditPurged=0, closedHoldsEligible=0, closedHoldsPurged=0, assessmentsEligible=0, assessmentsPurged=0)
    for kind, table, start in [('messages', 'message_revision', 'submitted_at'), ('statuses', 'status_update', 'retention_started_at')]:
        for value in selection(table, start, instant, limit):
            outcome = minimize(kind, value, instant, apply); counts[outcome] += 1
    # Only moderation/retention audit. Other administrative history is outside this policy.
    suffix = "(h.subject_id || ':')" if connection.vendor == 'sqlite' else "CONCAT(h.subject_id,':')"
    audits = human.rows(f"SELECT a.* FROM administrative_audit a WHERE occurred_at<=%s AND action IN ('MESSAGE_APPROVED','MESSAGE_REJECTED','STATUS_APPROVED','STATUS_REJECTED','RETENTION_CONTENT_PURGED','RETENTION_HOLD_OPENED','RETENTION_HOLD_CLOSED') AND NOT EXISTS(SELECT 1 FROM moderation_retention_hold h WHERE h.active=TRUE AND (a.subject=h.subject_id OR SUBSTR(a.subject,1,LENGTH(h.subject_id)+1)={suffix})) ORDER BY occurred_at,id LIMIT %s", [instant-timedelta(days=AUDIT_DAYS), limit])
    for audit in audits:
        item = audit['subject'].split(':', 1)[0]
        # Serialize with both possible parent kinds; UUIDs share the namespace.
        with transaction.atomic():
            parent('messages', item); parent('statuses', item)
            if held('messages', item) or held('statuses', item):
                continue
            counts['auditEligible'] += 1
            if apply:
                counts['auditPurged'] += human.execute('DELETE FROM administrative_audit WHERE id=%s AND occurred_at<=%s', [audit['id'], instant-timedelta(days=AUDIT_DAYS)])
    cutoff = instant-timedelta(days=AUDIT_DAYS)
    assessments = human.rows('SELECT a.* FROM automatic_assessment a WHERE updated_at<=%s AND NOT EXISTS(SELECT 1 FROM moderation_retention_hold h WHERE h.subject_kind=a.subject_kind AND h.subject_id=a.subject_id AND h.active=TRUE) ORDER BY updated_at,id LIMIT %s', [cutoff, limit])
    for value in assessments:
        with transaction.atomic():
            parent(value['subject_kind'], value['subject_id'])
            if held(value['subject_kind'], value['subject_id']): continue
            counts['assessmentsEligible'] += 1
            if apply:
                counts['assessmentsPurged'] += human.execute('DELETE FROM automatic_assessment WHERE id=%s AND updated_at<=%s', [value['id'], cutoff])
    closed = human.rows('SELECT * FROM moderation_retention_hold WHERE active=FALSE AND closed_at<=%s ORDER BY closed_at,subject_kind,subject_id LIMIT %s', [cutoff, limit])
    for value in closed:
        with transaction.atomic():
            parent(value['subject_kind'], value['subject_id'])
            if held(value['subject_kind'], value['subject_id']): continue
            counts['closedHoldsEligible'] += 1
            if apply:
                counts['closedHoldsPurged'] += human.execute('DELETE FROM moderation_retention_hold WHERE subject_kind=%s AND subject_id=%s AND active=FALSE AND closed_at<=%s', [value['subject_kind'], value['subject_id'], cutoff])
    return dict(policy=POLICY, mode='apply' if apply else 'preview', counts=counts)
