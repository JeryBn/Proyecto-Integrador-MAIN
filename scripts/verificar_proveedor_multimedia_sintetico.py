"""Explicit real-provider probe for a locally generated synthetic clip only.

The caller creates the clip with OS speech synthesis and a plain color frame.
No queue, database, microphone or user uploads are accessed.
"""
import json,os,sys
from pathlib import Path
from unittest.mock import patch
repo=Path(__file__).resolve().parents[1]
source=Path(os.environ['MAIN_SYNTHETIC_MEDIA']).resolve()
if not source.is_relative_to((repo/'var').resolve()) or source.name!='synthetic-probe.mp4':
    raise SystemExit('Use only a freshly generated synthetic-probe.mp4 inside var.')
sys.path.insert(0,str(repo/'admin-django'));os.environ.setdefault('DJANGO_SETTINGS_MODULE','main_admin.test_settings')
import django
django.setup()
from administration.automatic_moderation import OpenAiModerator,ProviderUnavailable
from administration import media_assessment as media
provider=OpenAiModerator(os.environ.get('OPENAI_API_KEY',''))
calls={'transcriptionSucceeded':0,'moderationSucceeded':0}
original_transcribe=media.transcribe;original_assess=provider.assess
def transcribe(*args):
    value=original_transcribe(*args);calls['transcriptionSucceeded']+=1;return value
def assess(*args):
    value=original_assess(*args);calls['moderationSucceeded']+=1;return value
provider.assess=assess
result=dict(synthetic=True,realProvider=True,databaseChanged=False,personalRecording=False)
try:
    with patch.object(media,'transcribe',side_effect=transcribe):
        outcome=provider.assess_media('VIDEO',source.read_bytes(),'Ejercicio de clase sintético')
    result.update(outcome=outcome.outcome,model=outcome.model)
except ProviderUnavailable as error:result.update(outcome='BLOCKED',error=str(error))
result.update(calls)
target=Path(os.environ['MAIN_SYNTHETIC_MEDIA_EVIDENCE']);target.parent.mkdir(parents=True,exist_ok=True)
target.write_text(json.dumps(result,indent=2),encoding='utf-8')
print(json.dumps(result))
if result['outcome']=='BLOCKED' or not calls['transcriptionSucceeded'] or calls['moderationSucceeded']<2:raise SystemExit(2)
