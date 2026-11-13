"""HTTPS-only deployment behind a same-host, explicitly trusted Waitress proxy.

Waitress validates the peer and translates X-Forwarded-Proto to wsgi.url_scheme.
Django must not independently trust arbitrary client headers.
"""
from urllib.parse import urlsplit
from pathlib import Path
from copy import deepcopy
from django.core.exceptions import ImproperlyConfigured
from .settings import *

DATABASES = deepcopy(DATABASES)

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

database_host = os.environ.get('MAIN_DB_HOST', DATABASES['default']['HOST']).strip()
DATABASES['default']['HOST'] = database_host
normalized_database_host = database_host.lower()
local_database_hosts = {'', '127.0.0.1', 'localhost', '::1'}
if normalized_database_host not in local_database_hosts and not database_host.startswith('/'):
    database_ca = os.environ.get('MAIN_DB_SSL_CA', '').strip()
    if not database_ca or not Path(database_ca).is_file():
        raise ImproperlyConfigured(
            'MAIN_DB_SSL_CA debe apuntar a la CA de MySQL cuando la base no está en este servidor.'
        )
    DATABASES['default']['OPTIONS'] = {
        **DATABASES['default']['OPTIONS'],
        'ssl_mode': 'VERIFY_IDENTITY',
        'ssl': {'ca': database_ca},
    }

DATABASES['default']['CONN_MAX_AGE'] = 60
DATABASES['default']['CONN_HEALTH_CHECKS'] = True
