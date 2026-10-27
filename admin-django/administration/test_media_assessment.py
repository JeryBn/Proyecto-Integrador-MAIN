import io,json,subprocess,urllib.error
from unittest.mock import Mock,patch
from django.test import SimpleTestCase
from . import media_assessment as media
from .automatic_moderation import Assessment,ProviderUnavailable,OpenAiModerator

class MediaAssessmentTests(SimpleTestCase):
    def test_quota_is_distinct_from_rate_limit_without_logging_response(self):
        from .automatic_moderation import provider_http_error
        for code,expected in [('insufficient_quota','QUOTA_EXHAUSTED'),('rate_limit_exceeded','RATE_LIMITED'),('unknown','RATE_OR_QUOTA')]:
            error=urllib.error.HTTPError('fixed',429,'private',{},io.BytesIO(json.dumps({'error':{'code':code,'message':'not retained'}}).encode()))
            self.assertEqual(provider_http_error(error),expected)
    def test_size_rejected_before_decoder(self):
        with patch.object(media,'_run') as run:
            for kind,raw in [('TEXT',b'x'),('AUDIO',b''),('VIDEO',b'x'*(12*1024*1024+1))]:
                with self.assertRaises(media.MediaUnavailable):media.extract(kind,raw)
            run.assert_not_called()

    def test_untrusted_duration_extra_tracks_and_dimensions_stay_human(self):
        fixtures=[{'format':{'duration':duration},'streams':[{'codec_type':'video','width':16,'height':16}]} for duration in ['NaN','Infinity','-1','61']]
        fixtures.extend([{'format':{'duration':'2'},'streams':[{'codec_type':'audio'}]*2},
            {'format':{'duration':'2'},'streams':[{'codec_type':'video','width':99999,'height':1}]},
            {'format':{'duration':'2'},'streams':[{'codec_type':'subtitle'}]}])
        with patch.dict('os.environ',{'MAIN_FFMPEG_EXECUTABLE':'ffmpeg','MAIN_FFPROBE_EXECUTABLE':'ffprobe'}):
            for fixture in fixtures:
                with patch.object(media,'_run',return_value=json.dumps(fixture).encode()):
                    with self.assertRaises(media.MediaUnavailable):media.extract('VIDEO',b'input')

    def test_timeout_discards_decoder_details(self):
        with patch.object(media.subprocess,'run',side_effect=subprocess.TimeoutExpired('private-path',1)):
            with self.assertRaisesMessage(media.MediaUnavailable,'MEDIA_DECODER_UNAVAILABLE'):media._run(['ffprobe'],1)

    def test_transcription_is_fixed_endpoint_multipart_without_identity(self):
        response=Mock();response.__enter__=Mock(return_value=response);response.__exit__=Mock(return_value=False)
        response.read.return_value=b'{"text":"Consulta educativa"}'
        with patch.object(media.urllib.request,'build_opener') as opener:
            opener.return_value.open.return_value=response
            self.assertEqual(media.transcribe('synthetic-key',b'RIFF-synthetic'),'Consulta educativa')
            request=opener.return_value.open.call_args.args[0]
            self.assertEqual(request.full_url,'https://api.openai.com/v1/audio/transcriptions')
            self.assertIn(b'whisper-1',request.data);self.assertIn(b'filename="audio.wav"',request.data)
            self.assertNotIn(b'synthetic-key',request.data)

    def test_transcription_quota_and_invalid_response_remain_private(self):
        with patch.object(media.urllib.request,'build_opener') as opener:
            opener.return_value.open.side_effect=urllib.error.HTTPError('fixed',429,'private-error',{},None)
            with self.assertRaisesMessage(ProviderUnavailable,'RATE_OR_QUOTA'):media.transcribe('test',b'audio')

    def test_audio_and_video_even_clear_require_human_and_transcript_not_retained(self):
        provider=Mock(key='test');provider.assess.return_value=Assessment('CLEAR','controlled')
        with patch.object(media,'extract',return_value=media.ExtractedMedia(5,b'normalized',(b'frame',))),patch.object(media,'transcribe',return_value='Voz sintética privada'):
            result=media.assess(provider,'VIDEO',b'input','Pie sintético')
        self.assertEqual(result.outcome,'HUMAN_REQUIRED');self.assertEqual(provider.assess.call_count,2)
        self.assertNotIn('Voz',repr(result))

    def test_categories_combined_but_never_irreversible_rejection(self):
        provider=Mock(key='test');provider.assess.side_effect=[Assessment('HUMAN_REQUIRED','mock',('harassment',)),Assessment('HUMAN_REQUIRED','mock',('violence',))]
        with patch.object(media,'extract',return_value=media.ExtractedMedia(2,b'wave',(b'frame',))),patch.object(media,'transcribe',return_value='Texto sintético'):
            result=media.assess(provider,'VIDEO',b'input','')
        self.assertEqual(result.categories,('harassment','violence'));self.assertEqual(result.outcome,'HUMAN_REQUIRED')

    def test_missing_decoder_routes_to_human(self):
        with patch.object(media,'extract',side_effect=media.MediaUnavailable('MEDIA_DECODER_UNAVAILABLE')):
            with self.assertRaisesMessage(ProviderUnavailable,'HUMAN_REQUIRED'):OpenAiModerator('test').assess_media('AUDIO',b'input','')
