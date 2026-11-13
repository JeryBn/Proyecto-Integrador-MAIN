from unittest.mock import patch

from django.core.cache import cache
from django.db import DatabaseError
from django.test import SimpleTestCase, TestCase


class HealthTests(TestCase):
    def test_database_health_is_anonymous_and_minimal(self):
        response = self.client.get('/api/admin/health')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {'status': 'UP'})


class UnavailableHealthTests(SimpleTestCase):
    def test_database_failure_is_safe(self):
        cache.clear()
        with patch('administration.views.connection') as unavailable_database, patch('administration.views.logger') as logger:
            unavailable_database.cursor.side_effect = DatabaseError('private database detail')
            response = self.client.get('/api/admin/health')
            repeated = self.client.get('/api/admin/health')
        self.assertEqual(response.status_code, 503)
        self.assertEqual(repeated.status_code, 503)
        self.assertEqual(response.json(), {'status': 'DOWN'})
        self.assertNotIn('private database detail', response.content.decode())
        logger.exception.assert_called_once_with('Django database health check failed')
