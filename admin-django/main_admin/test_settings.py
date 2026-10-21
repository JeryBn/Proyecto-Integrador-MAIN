import os
os.environ.setdefault('MAIN_DJANGO_SECRET_KEY', 'synthetic-test-key-only-not-for-deployment')
for name in ('MAIN_DB_NAME', 'MAIN_DB_USER', 'MAIN_DB_PASSWORD'):
    os.environ.setdefault(name, 'unused-test')
from .settings import *
DATABASES = {'default': {'ENGINE': 'django.db.backends.sqlite3', 'NAME': ':memory:'}}
ALLOWED_HOSTS = ['testserver', 'localhost', '127.0.0.1']
