"""HTTPS-only deployment behind a same-host, explicitly trusted Waitress proxy.

Waitress validates the peer and translates X-Forwarded-Proto to wsgi.url_scheme.
Django must not independently trust arbitrary client headers.
"""
from urllib.parse import urlsplit
from django.core.exceptions import ImproperlyConfigured
from .settings import *

origin = os.environ.get('MAIN_PUBLIC_ORIGIN', '')
parsed = urlsplit(origin)
if (parsed.scheme != 'https' or not parsed.hostname or parsed.username or parsed.password
        or parsed.path not in ('', '/') or parsed.query or parsed.fragment
        or '*' in parsed.netloc):
    raise ImproperlyConfigured('MAIN_PUBLIC_ORIGIN debe ser un origen HTTPS exacto, sin rutas ni credenciales.')
ALLOWED_HOSTS = [parsed.hostname]
CSRF_TRUSTED_ORIGINS = [origin.rstrip('/')]
SESSION_COOKIE_SECURE = True
CSRF_COOKIE_SECURE = True
SECURE_SSL_REDIRECT = True
SECURE_PROXY_SSL_HEADER = None
