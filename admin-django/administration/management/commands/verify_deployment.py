import json
import re

from django.core.management.base import BaseCommand, CommandError
from django.db import connection

from administration.models import SharedUser


class Command(BaseCommand):
    help = 'Comprueba MySQL con TLS y la identidad ADMIN esperada sin mostrar datos privados.'

    def add_arguments(self, parser):
        parser.add_argument('--username', required=True)

    def handle(self, *args, **options):
        username = options['username'].strip()
        if not re.fullmatch(r'[a-z][a-z0-9_.-]{2,59}', username):
            raise CommandError('Identidad administrativa inválida.')
        if connection.vendor != 'mysql':
            raise CommandError('La verificación de despliegue exige MySQL.')

        try:
            with connection.cursor() as cursor:
                cursor.execute("SHOW SESSION STATUS LIKE 'Ssl_cipher'")
                row = cursor.fetchone()
        except Exception as exc:
            raise CommandError('No se pudo comprobar la conexión privada con MySQL.') from exc

        cipher = row[1].strip() if row and len(row) > 1 and row[1] else ''
        if not cipher:
            raise CommandError('MySQL respondió sin un canal TLS activo.')

        try:
            admin = SharedUser.objects.filter(username=username).only(
                'username', 'role', 'enabled', 'password_change_required'
            ).first()
        except Exception as exc:
            raise CommandError('No se pudo comprobar la identidad administrativa inicial.') from exc
        if not admin:
            raise CommandError('No existe la identidad administrativa esperada.')
        if admin.role != 'ADMIN' or not admin.enabled or not admin.password_change_required:
            raise CommandError('La identidad administrativa inicial no tiene el estado seguro esperado.')

        self.stdout.write(json.dumps({
            'database': 'mysql',
            'tls': True,
            'admin': username,
            'enabled': True,
            'mustChangePassword': True,
        }))
