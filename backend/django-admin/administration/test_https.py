import os
import runpy
import tempfile
from unittest.mock import patch
from django.core.exceptions import ImproperlyConfigured
from django.test import RequestFactory, SimpleTestCase, override_settings

class HttpsDeploymentTests(SimpleTestCase):
    def configuration(self, origin):
        with patch.dict(os.environ, {'MAIN_PUBLIC_ORIGIN': origin}):
            return runpy.run_module('main_admin.production_settings')

    def test_exact_origin_and_secure_session(self):
        config = self.configuration('https://main.example.org:8443')
        self.assertEqual(config['ALLOWED_HOSTS'], ['main.example.org'])
        self.assertEqual(config['CSRF_TRUSTED_ORIGINS'], ['https://main.example.org:8443'])
        for flag in ('SESSION_COOKIE_SECURE', 'CSRF_COOKIE_SECURE', 'SECURE_SSL_REDIRECT'):
            self.assertTrue(config[flag])

    def test_invalid_deployment_origins_fail_closed(self):
        for origin in ('', 'http://main.example.org', 'https://user:pass@main.example.org',
                       'https://*.example.org', 'https://main.example.org/api',
                       'https://main.example.org?token=x', 'https://main.example.org#x'):
            with self.subTest(origin=origin), self.assertRaises(ImproperlyConfigured):
                self.configuration(origin)

    def test_remote_database_requires_a_real_ca(self):
        with patch.dict(os.environ, {
            'MAIN_PUBLIC_ORIGIN': 'https://main.example.org',
            'MAIN_DB_HOST': 'db.example.org',
            'MAIN_DB_SSL_CA': '',
        }):
            with self.assertRaises(ImproperlyConfigured):
                runpy.run_module('main_admin.production_settings')

    def test_remote_database_verifies_certificate_and_hostname(self):
        with tempfile.NamedTemporaryFile() as certificate:
            with patch.dict(os.environ, {
                'MAIN_PUBLIC_ORIGIN': 'https://main.example.org',
                'MAIN_DB_HOST': 'db.example.org',
                'MAIN_DB_SSL_CA': certificate.name,
            }):
                config = runpy.run_module('main_admin.production_settings')
        database = config['DATABASES']['default']
        self.assertEqual(database['OPTIONS']['ssl_mode'], 'VERIFY_IDENTITY')
        self.assertEqual(database['OPTIONS']['ssl']['ca'], certificate.name)
        self.assertEqual(database['CONN_MAX_AGE'], 60)
        self.assertTrue(database['CONN_HEALTH_CHECKS'])

    def test_same_host_database_also_verifies_identity_when_ca_is_supplied(self):
        with tempfile.NamedTemporaryFile() as certificate:
            with patch.dict(os.environ, {
                'MAIN_PUBLIC_ORIGIN': 'https://main.example.org',
                'MAIN_DB_HOST': '127.0.0.1',
                'MAIN_DB_SSL_CA': certificate.name,
            }):
                config = runpy.run_module('main_admin.production_settings')
        self.assertEqual(config['DATABASES']['default']['OPTIONS']['ssl_mode'], 'VERIFY_IDENTITY')

        with patch.dict(os.environ, {
            'MAIN_PUBLIC_ORIGIN': 'https://main.example.org',
            'MAIN_DB_HOST': '127.0.0.1',
            'MAIN_DB_SSL_CA': 'missing-private-ca.pem',
        }):
            with self.assertRaises(ImproperlyConfigured):
                runpy.run_module('main_admin.production_settings')

    @override_settings(SECURE_PROXY_SSL_HEADER=None)
    def test_only_wsgi_scheme_not_untrusted_raw_header_marks_https(self):
        request = RequestFactory().get('/', HTTP_X_FORWARDED_PROTO='https')
        self.assertFalse(request.is_secure())
        self.assertTrue(RequestFactory().get('/', secure=True).is_secure())
