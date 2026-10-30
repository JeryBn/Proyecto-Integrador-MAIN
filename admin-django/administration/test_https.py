import os
import runpy
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

    @override_settings(SECURE_PROXY_SSL_HEADER=None)
    def test_only_wsgi_scheme_not_untrusted_raw_header_marks_https(self):
        request = RequestFactory().get('/', HTTP_X_FORWARDED_PROTO='https')
        self.assertFalse(request.is_secure())
        self.assertTrue(RequestFactory().get('/', secure=True).is_secure())
