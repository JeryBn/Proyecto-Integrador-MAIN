"""Automatic assessment with fail-closed publication and explicit policy gates.

No names, contacts, session IDs, course data or history go to the provider.
Video/audio and incomplete modality coverage always require a human reviewer.
"""
import base64, hashlib, json, os, urllib.request, urllib.error
from datetime import timedelta
from dataclasses import dataclass
from types import SimpleNamespace
from django.db import transaction, IntegrityError
from . import moderation as human
from .models import SharedUser

class ProviderUnavailable(Exception):
    pass

@dataclass(frozen=True)
class Assessment:
    outcome: str
    model: str
    categories: tuple = ()

def parse_assessment(value,kind):
    results=value.get('results')
    if not isinstance(results,list) or len(results)!=1:raise ProviderUnavailable('INVALID_RESPONSE')
    result=results[0];categories=result.get('categories');scores=result.get('category_scores')
    required={'sexual','sexual/minors','harassment','harassment/threatening','hate','hate/threatening','violence','violence/graphic'}
    if type(result.get('flagged')) is not bool or not isinstance(categories,dict) or not required.issubset(categories) or not all(type(v) is bool for v in categories.values()):raise ProviderUnavailable('INVALID_RESPONSE')
    if not isinstance(scores,dict) or not set(categories).issubset(scores) or not all(type(scores[k]) in (int,float) and 0<=scores[k]<=1 for k in categories):raise ProviderUnavailable('INVALID_RESPONSE')
    flagged=tuple(sorted(k for k,v in categories.items() if v))
    # Provider flag is a route to human review, never an irreversible rejection.
    # Image categories do not cover every text/OCR/audio risk: no automatic release.
    outcome='HUMAN_REQUIRED' if result['flagged'] or flagged or kind=='IMAGE' else 'CLEAR'
    return Assessment(outcome,str(value.get('model','omni-moderation-latest')),flagged)

class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self,*args,**kwargs):return None

class OpenAiModerator:
    def __init__(self,key):self.key=key
    def assess(self,text,image=None):
        if not self.key:raise ProviderUnavailable('NOT_CONFIGURED')
        inputs=[]
        if text:inputs.append({'type':'text','text':text})
        if image:
            mime='image/png' if image.startswith(b'\x89PNG\r\n\x1a\n') else 'image/jpeg' if image.startswith(b'\xff\xd8\xff') else None
            if not mime or len(image)>5*1024*1024:raise ProviderUnavailable('HUMAN_REQUIRED')
            inputs.append({'type':'image_url','image_url':{'url':'data:'+mime+';base64,'+base64.b64encode(image).decode()}})
        if not inputs or len(text)>20000:raise ProviderUnavailable('HUMAN_REQUIRED')
        request=urllib.request.Request('https://api.openai.com/v1/moderations',data=json.dumps({'model':'omni-moderation-latest','input':inputs}).encode(),headers={'Authorization':'Bearer '+self.key,'Content-Type':'application/json'})
        try:
            with urllib.request.build_opener(NoRedirect()).open(request,timeout=30) as response:
                data=response.read(1024*1024+1)
                if len(data)>1024*1024:raise ProviderUnavailable('INVALID_RESPONSE')
                return parse_assessment(json.loads(data),'IMAGE' if image else 'TEXT')
        except urllib.error.HTTPError as exc:
            code='RATE_OR_QUOTA' if exc.code==429 else 'ACCESS_DENIED' if exc.code in (401,403) else 'PROVIDER_UNAVAILABLE'
            raise ProviderUnavailable(code) from None
        except ProviderUnavailable:raise
        except Exception:raise ProviderUnavailable('CONNECTION_OR_INVALID_RESPONSE') from None

def approved_policy(env):
    return env.get('MAIN_MODERATION_POLICY_APPROVED')=='true' and 1<=len(env.get('MAIN_MODERATION_POLICY_VERSION','').strip())<=80 and env.get('MAIN_MODERATION_REVIEWER','').strip() and env.get('MAIN_MODERATION_RETENTION_DAYS','').isdigit() and 1<=int(env['MAIN_MODERATION_RETENTION_DAYS'])<=3650

def pending(limit,policy,apply):
    final="('HUMAN_REQUIRED','APPROVED')" if apply else "('HUMAN_REQUIRED','APPROVED','CLEAR_SHADOW')"
    def excluded(kind,item,content):
        return f"NOT EXISTS(SELECT 1 FROM automatic_assessment a WHERE a.subject_kind='{kind}' AND a.subject_id={item} AND a.content_hash={content} AND a.policy_version=%s AND (a.outcome IN {final} OR (a.outcome IN ('RUNNING','PROVIDER_UNAVAILABLE') AND (a.attempts>=3 OR a.lease_until>%s))))"
    messages=human.rows("SELECT m.id FROM message m JOIN message_revision r ON r.message_id=m.id WHERE r.state='PENDING' AND r.revision=(SELECT MAX(revision) FROM message_revision WHERE message_id=m.id) AND "+excluded('messages','m.id','r.content_hash')+" ORDER BY m.created_at LIMIT %s",[policy,human.now(),limit])
    statuses=human.rows("SELECT s.id FROM status_update s WHERE s.state='PENDING' AND s.expires_at>%s AND "+excluded('statuses','s.id','s.content_hash')+" ORDER BY s.created_at LIMIT %s",[human.now(),policy,human.now(),limit])
    return ([('messages',r['id']) for r in messages]+[('statuses',r['id']) for r in statuses])[:limit]

def snapshot(kind,item):
    if kind=='messages':
        value=human.message(item)
        if not value or value['state']!='PENDING':return None
        media=human.attachment(item);content=human.message_bytes(item,media) if media else None
        digest=hashlib.sha256((value['body'] if not media else 'MAIN-ATTACHMENT\n'+value['body']+'\x00'+media['content_hash']).encode()).hexdigest()
        if digest!=value['content_hash']:raise ValueError('CONTENT_INTEGRITY')
        return dict(kind=media['kind'] if media else 'TEXT',text=value['body'],media=content,revision=value['revision'],hash=digest)
    values=human.rows("SELECT * FROM status_update WHERE id=%s AND state='PENDING' AND expires_at>%s",[item,human.now()])
    if not values:return None
    value=values[0];content=human.status_bytes(value)
    return dict(kind=value['kind'],text=value['body'],media=content,revision=None,hash=value['content_hash'])

def process(kind,item,provider,apply=False,policy='test-only'):
    content=snapshot(kind,item)
    if not content:return 'STALE'
    key=hashlib.sha256((kind+'\n'+item+'\n'+content['hash']+'\n'+policy).encode()).hexdigest()
    def defer_human():
        try:
            with transaction.atomic():human.execute("INSERT INTO automatic_assessment(id,policy_version,subject_kind,subject_id,content_hash,outcome,attempts,lease_until,updated_at) VALUES(%s,%s,%s,%s,%s,'HUMAN_REQUIRED',0,%s,%s)",[key,policy,kind,item,content['hash'],human.now(),human.now()])
        except IntegrityError:pass
        return 'HUMAN_REQUIRED'
    media_kind=content['kind']
    if media_kind in ('VIDEO','AUDIO'):return defer_human()
    text=content['text'];image=None
    if media_kind=='FILE':
        try:text+='\n'+content['media'].decode('utf-8',errors='strict')
        except UnicodeError:return defer_human()
    elif media_kind=='IMAGE':image=content['media']
    elif media_kind not in ('TEXT','NONE'):return defer_human()
    if len(text)>20000:return defer_human()
    lease=human.now()+timedelta(minutes=5)
    try:
        with transaction.atomic():human.execute("INSERT INTO automatic_assessment(id,policy_version,subject_kind,subject_id,content_hash,outcome,attempts,lease_until,updated_at) VALUES(%s,%s,%s,%s,%s,'WAITING',0,%s,%s)",[key,policy,kind,item,content['hash'],human.now(),human.now()])
    except IntegrityError:pass
    previous=human.rows('SELECT outcome FROM automatic_assessment WHERE id=%s',[key])[0]['outcome']
    if previous=='HUMAN_REQUIRED':return previous
    if previous=='APPROVED':return 'STALE'
    if previous=='CLEAR_SHADOW':
        if not apply:return 'CACHED_SHADOW'
    else:
        claimed=human.execute("UPDATE automatic_assessment SET outcome='RUNNING',attempts=attempts+1,lease_until=%s,updated_at=%s WHERE id=%s AND attempts<3 AND lease_until<=%s AND outcome IN ('WAITING','RUNNING','PROVIDER_UNAVAILABLE')",[lease,human.now(),key,human.now()])
        if not claimed:return 'WAITING_OR_LIMITED'
        try:result=provider.assess(text,image)
        except ProviderUnavailable as error:
            state='HUMAN_REQUIRED' if str(error)=='HUMAN_REQUIRED' else 'PROVIDER_UNAVAILABLE'
            human.execute('UPDATE automatic_assessment SET outcome=%s,updated_at=%s WHERE id=%s',[state,human.now(),key]);return state
        state='HUMAN_REQUIRED' if result.outcome!='CLEAR' or media_kind=='IMAGE' else 'CLEAR_SHADOW'
        human.execute('UPDATE automatic_assessment SET outcome=%s,updated_at=%s WHERE id=%s',[state,human.now(),key])
        if state=='HUMAN_REQUIRED':return state
        if not apply:return state
    # Reuse exactly the human path: locks, latest hash/revision, permissions,
    # transactional audit and notification creation are still mandatory.
    with transaction.atomic():
        value=dict(contentHash=content['hash'],approved=True,attachmentReviewed=bool(content['media']))
        if content['revision'] is not None:value['revision']=content['revision']
        response=human.review_value(SimpleNamespace(admin_actor='MAIN_AUTOMATION'),kind,item,value)
        if response.status_code!=200:return 'STALE_OR_FORBIDDEN'
        human.execute("UPDATE automatic_assessment SET outcome='APPROVED',updated_at=%s WHERE id=%s",[human.now(),key])
    return 'APPROVED'

def run_batch(provider,limit=10,apply=False,env=None):
    env=os.environ if env is None else env
    if not approved_policy(env):raise ValueError('POLICY_NOT_APPROVED')
    if not SharedUser.objects.filter(username=env['MAIN_MODERATION_REVIEWER'],role='ADMIN',enabled=True).exists():raise ValueError('REVIEWER_NOT_AUTHORIZED')
    if not 1<=limit<=50:raise ValueError('LIMIT_INVALID')
    summary={}
    for kind,item in pending(limit,env['MAIN_MODERATION_POLICY_VERSION'],apply):
        try:result=process(kind,item,provider,apply,env['MAIN_MODERATION_POLICY_VERSION'])
        except (ValueError,UnicodeError):result='CONTENT_INTEGRITY'
        summary[result]=summary.get(result,0)+1
        if result=='PROVIDER_UNAVAILABLE':break
    return summary
