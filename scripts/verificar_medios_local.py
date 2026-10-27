"""Real FFmpeg extraction with generated media and a controlled provider, no API."""
import json,os,sys,tempfile,subprocess,wave,math,struct
from pathlib import Path
from unittest.mock import patch
from types import SimpleNamespace
repo=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(repo/'admin-django'))
os.environ.setdefault('DJANGO_SETTINGS_MODULE','main_admin.test_settings')
import django
django.setup()
from administration.media_assessment import extract,assess
from administration.automatic_moderation import Assessment

with tempfile.TemporaryDirectory(prefix='main-synthetic-') as folder:
    root=Path(folder);audio=root/'synthetic.wav';video=root/'synthetic.mp4'
    with wave.open(str(audio),'wb') as output:
        output.setparams((1,2,16000,0,'NONE','not compressed'))
        output.writeframes(b''.join(struct.pack('<h',int(1000*math.sin(i*2*math.pi*440/16000))) for i in range(32000)))
    subprocess.run([os.environ['MAIN_FFMPEG_EXECUTABLE'],'-v','error','-nostdin','-f','lavfi',
        '-i','color=c=blue:s=64x64:r=10','-i',str(audio),'-t','2','-c:v','libx264','-pix_fmt','yuv420p','-c:a','aac',str(video)],
        check=True,timeout=20,stdin=subprocess.DEVNULL,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,
        creationflags=subprocess.CREATE_NO_WINDOW if os.name=='nt' else 0)
    results=[]
    for kind,path in [('AUDIO',audio),('VIDEO',video)]:
        raw=path.read_bytes();extracted=extract(kind,raw)
        provider=SimpleNamespace(key='controlled-only',assess=lambda *args:Assessment('CLEAR','controlled'))
        with patch('administration.media_assessment.transcribe',return_value='Consulta educativa sintética'):
            result=assess(provider,kind,raw,'Contenido de prueba')
        assert result.outcome=='HUMAN_REQUIRED'
        assert extracted.audio and (kind=='AUDIO' or extracted.frames)
        results.append(dict(kind=kind,duration=extracted.duration,normalizedAudioBytes=len(extracted.audio),
            sampledFrames=len(extracted.frames),outcome=result.outcome))
    evidence=dict(synthetic=True,realDecoder=True,realProvider=False,databaseChanged=False,checks=results)
    target=Path(os.environ['MAIN_MEDIA_EVIDENCE']);target.parent.mkdir(parents=True,exist_ok=True)
    target.write_text(json.dumps(evidence,indent=2),encoding='utf-8')
    print('Audio y video sintéticos: extracción FFmpeg real y derivación humana comprobadas; sin API ni cambios de BD.')
