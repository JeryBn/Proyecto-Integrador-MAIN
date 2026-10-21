import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent
SECRET_KEY = os.environ['MAIN_DJANGO_SECRET_KEY']
DEBUG = False
ALLOWED_HOSTS = os.getenv('MAIN_DJANGO_HOSTS', '127.0.0.1,localhost').split(',')
INSTALLED_APPS = ['django.contrib.sessions', 'administration']
MIDDLEWARE = ['django.middleware.security.SecurityMiddleware',
              'django.contrib.sessions.middleware.SessionMiddleware',
              'django.middleware.common.CommonMiddleware', 'django.middleware.csrf.CsrfViewMiddleware',
              'django.middleware.clickjacking.XFrameOptionsMiddleware']
ROOT_URLCONF = 'main_admin.urls'
WSGI_APPLICATION = 'main_admin.wsgi.application'
DATABASES = {'default': {
    'ENGINE': 'django.db.backends.mysql', 'NAME': os.environ['MAIN_DB_NAME'],
    'USER': os.environ['MAIN_DB_USER'], 'PASSWORD': os.environ['MAIN_DB_PASSWORD'],
    'HOST': os.getenv('MAIN_DB_HOST', '127.0.0.1'), 'PORT': os.getenv('MAIN_DB_PORT', '3306'),
    'OPTIONS': {'charset': 'utf8mb4', 'init_command': "SET sql_mode='STRICT_TRANS_TABLES', time_zone='-05:00'"},
}}
LANGUAGE_CODE = 'es-pe'
TIME_ZONE = 'America/Lima'
# Contrato existente: fechaLimite es hora de Lima sin offset; no convertir dos veces.
USE_TZ = False
SESSION_COOKIE_NAME = 'MAIN_ADMIN_SESSION'
SESSION_COOKIE_HTTPONLY = True
SESSION_COOKIE_SAMESITE = 'Lax'
SESSION_COOKIE_AGE = 1800
SESSION_COOKIE_SECURE = os.getenv('MAIN_HTTPS', 'false').lower() == 'true'
CSRF_COOKIE_SECURE = SESSION_COOKIE_SECURE
CSRF_USE_SESSIONS = True
CSRF_FAILURE_VIEW = 'administration.views.csrf_failure'
SECURE_CONTENT_TYPE_NOSNIFF = True
X_FRAME_OPTIONS = 'DENY'
DEFAULT_AUTO_FIELD = 'django.db.models.BigAutoField'
