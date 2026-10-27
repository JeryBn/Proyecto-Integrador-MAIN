"""Bounded local extraction and advisory assessment; never automatic publication."""
import json, math, os, shutil, subprocess, tempfile, uuid, urllib.request, urllib.error
from pathlib import Path
from dataclasses import dataclass

class MediaUnavailable(Exception):
    pass

@dataclass(frozen=True)
class ExtractedMedia:
    duration: float
    audio: bytes | None
    frames: tuple

def _run(args,timeout):
    try:
        value=subprocess.run(args,stdin=subprocess.DEVNULL,stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL,timeout=timeout,check=True,
            creationflags=subprocess.CREATE_NO_WINDOW if os.name=='nt' else 0)
        if len(value.stdout)>65536:raise MediaUnavailable('MEDIA_INVALID')
        return value.stdout
    except (OSError,subprocess.SubprocessError):raise MediaUnavailable('MEDIA_DECODER_UNAVAILABLE') from None

def extract(kind,content):
    if kind not in ('AUDIO','VIDEO') or not content or len(content)>12*1024*1024:
        raise MediaUnavailable('MEDIA_LIMIT')
    ffmpeg=os.environ.get('MAIN_FFMPEG_EXECUTABLE') or shutil.which('ffmpeg')
    ffprobe=os.environ.get('MAIN_FFPROBE_EXECUTABLE') or shutil.which('ffprobe')
    if not ffmpeg or not ffprobe:raise MediaUnavailable('MEDIA_DECODER_UNAVAILABLE')
    with tempfile.TemporaryDirectory(prefix='main-media-') as folder:
        root=Path(folder);source=root/'input.bin';source.write_bytes(content)
        # Fixed local input; manifests and network protocols are not accepted.
        formats='mov,matroska,webm,wav,mp3,ogg,flac,aac'
        common=['-max_alloc','67108864','-protocol_whitelist','file,pipe','-format_whitelist',formats]
        try:
            probe=json.loads(_run([ffprobe,'-v','error',*common,'-show_entries',
                'format=duration:stream=codec_type,width,height,sample_rate,channels','-of','json',str(source)],10))
            duration=float(probe['format']['duration']);streams=probe['streams']
            audio=[s for s in streams if s['codec_type']=='audio']
            video=[s for s in streams if s['codec_type']=='video']
            if not math.isfinite(duration) or not 0<duration<=60 or len(audio)>1 or len(video)>1:
                raise MediaUnavailable('MEDIA_LIMIT')
            if kind=='AUDIO' and (not audio or video):raise MediaUnavailable('MEDIA_INVALID')
            if kind=='VIDEO' and (not video or any(not 0<int(s['width'])<=4096 or not 0<int(s['height'])<=4096 for s in video)):
                raise MediaUnavailable('MEDIA_LIMIT')
            if any(s['codec_type'] not in ('audio','video') for s in streams):raise MediaUnavailable('MEDIA_INVALID')
            if any(not 1<=int(s['channels'])<=8 or not 1<=int(s['sample_rate'])<=192000 for s in audio):raise MediaUnavailable('MEDIA_LIMIT')
        except (KeyError,TypeError,ValueError):raise MediaUnavailable('MEDIA_INVALID') from None
        base=[ffmpeg,'-v','error','-nostdin','-threads','1',*common,'-i',str(source),'-t','60']
        normalized=None;frames=()
        if audio:
            target=root/'audio.wav'
            _run([*base,'-map','0:a:0','-vn','-ac','1','-ar','16000','-c:a','pcm_s16le','-fs','2000000',str(target)],25)
            normalized=target.read_bytes()
            if not normalized or len(normalized)>2000000:raise MediaUnavailable('MEDIA_LIMIT')
        if video:
            _run([*base,'-map','0:v:0','-an','-vf','fps=1/5:start_time=0:round=up,scale=640:640:force_original_aspect_ratio=decrease:force_divisible_by=2',
                '-frames:v','12','-threads','1','-pix_fmt','rgb24',str(root/'frame-%02d.png')],25)
            files=sorted(root.glob('frame-*.png'))
            if not files or len(files)>12 or any(p.stat().st_size>1024*1024 for p in files):raise MediaUnavailable('MEDIA_LIMIT')
            frames=tuple(p.read_bytes() for p in files)
        return ExtractedMedia(duration,normalized,frames)

def transcribe(key,audio):
    from .automatic_moderation import NoRedirect,ProviderUnavailable,provider_http_error
    if not key:raise ProviderUnavailable('NOT_CONFIGURED')
    if not audio or len(audio)>2000000:raise MediaUnavailable('MEDIA_LIMIT')
    boundary='MAIN'+uuid.uuid4().hex
    body=(f'--{boundary}\r\nContent-Disposition: form-data; name="model"\r\n\r\nwhisper-1\r\n'
          f'--{boundary}\r\nContent-Disposition: form-data; name="response_format"\r\n\r\njson\r\n'
          f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="audio.wav"\r\nContent-Type: audio/wav\r\n\r\n').encode()+audio+f'\r\n--{boundary}--\r\n'.encode()
    request=urllib.request.Request('https://api.openai.com/v1/audio/transcriptions',data=body,
        headers={'Authorization':'Bearer '+key,'Content-Type':'multipart/form-data; boundary='+boundary})
    try:
        with urllib.request.build_opener(NoRedirect()).open(request,timeout=45) as response:
            payload=response.read(100001)
        if len(payload)>100000:raise ProviderUnavailable('INVALID_RESPONSE')
        value=json.loads(payload);text=value.get('text')
        if not isinstance(text,str) or len(text)>20000:raise ProviderUnavailable('INVALID_RESPONSE')
        return text
    except urllib.error.HTTPError as error:
        raise ProviderUnavailable(provider_http_error(error)) from None
    except ProviderUnavailable:raise
    except Exception:raise ProviderUnavailable('CONNECTION_OR_INVALID_RESPONSE') from None

def assess(provider,kind,content,caption):
    from .automatic_moderation import Assessment
    extracted=extract(kind,content)
    transcript=transcribe(provider.key,extracted.audio) if extracted.audio else ''
    text='\n'.join(part for part in (caption,transcript) if part)
    if len(text)>20000:raise MediaUnavailable('MEDIA_LIMIT')
    categories=set()
    if text:categories.update(provider.assess(text).categories)
    for frame in extracted.frames:categories.update(provider.assess('',frame).categories)
    # Speech recognition can miss words/nonverbal abuse, and 5-second sampling
    # misses brief frames/OCR. Even an apparently clear result needs full review.
    return Assessment('HUMAN_REQUIRED','whisper-1 + omni-moderation-latest',tuple(sorted(categories)))


