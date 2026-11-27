import json
import os
from urllib.request import Request, build_opener

from django.core.cache import cache
from django.http import JsonResponse
from django.views.decorators.http import require_POST

from .automatic_moderation import NoRedirect
from .views import admin_only, error


@require_POST
@admin_only
def ask(request):
    try:
        payload = json.loads(request.body) if request.content_type == 'application/json' and len(request.body) <= 12000 else None
        question = payload.get('question') if isinstance(payload, dict) else None
        question = question.strip() if isinstance(question, str) else ''
    except (ValueError, UnicodeError):
        question = ''
    if not 1 <= len(question) <= 2000:
        return error(400, 'VALIDATION_ERROR', 'Escribe una consulta de hasta 2000 caracteres.')

    key = f'school-assistant:{request.admin_actor}'
    if not cache.add(key, True, timeout=50):
        return error(429, 'AI_BUSY', 'Espera a que termine la consulta anterior.')
    try:
        model = os.environ.get('MAIN_OLLAMA_MODEL', 'gemma3:4b')
        endpoint = os.environ.get('MAIN_OLLAMA_CHAT_URL', 'http://127.0.0.1:11434/api/chat')
        body = {
            'model': model,
            'stream': False,
            'options': {'num_predict': 400, 'num_ctx': 4096},
            'messages': [
                {'role': 'system', 'content': 'Eres el asistente de gestión escolar de COLEGIA. Responde brevemente en español. Ofrece orientación general y borradores. No solicites ni inventes datos personales. No tienes acceso a las cuentas ni puedes realizar cambios.'},
                {'role': 'user', 'content': question},
            ],
        }
        provider = Request(endpoint, data=json.dumps(body).encode(), headers={'Content-Type': 'application/json'})
        with build_opener(NoRedirect()).open(provider, timeout=40) as response:
            raw = response.read(65537)
        data = json.loads(raw) if len(raw) <= 65536 else {}
        answer = data['message']['content'].strip()
        if data.get('done') is not True or not 1 <= len(answer) <= 16000:
            raise ValueError('Invalid response')
        return JsonResponse({'answer': answer, 'model': model})
    except (OSError, ValueError, TypeError, KeyError, AttributeError):
        return error(503, 'AI_UNAVAILABLE', 'El asistente no está disponible. Intenta nuevamente.')
    finally:
        cache.delete(key)
