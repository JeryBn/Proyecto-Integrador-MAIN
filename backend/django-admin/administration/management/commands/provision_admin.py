import json
import os
import re

from django.core.management.base import BaseCommand, CommandError
from django.db import transaction

from administration.academic import ValidationError
from administration.identity import password
from administration.models import SharedUser
from administration.moderation import now
from administration.retention import event


class Command(BaseCommand):
    help = 'Crea el primer ADMIN con una clave inicial privada; nunca sobrescribe una cuenta existente.'

    def add_arguments(self, parser):
        parser.add_argument('--username', required=True)
        parser.add_argument('--name', required=True)

    def handle(self, *args, **options):
        username = options['username'].strip()
        display_name = options['name'].strip()
        if not re.fullmatch(r'[a-z][a-z0-9_.-]{2,59}', username):
            raise CommandError('Identidad administrativa inválida.')
        if not 1 <= len(display_name) <= 100:
            raise CommandError('Nombre administrativo inválido.')

        with transaction.atomic():
            existing = SharedUser.objects.select_for_update().filter(username=username).first()
            if existing:
                if existing.role != 'ADMIN' or existing.display_name != display_name:
                    raise CommandError('La identidad ya existe con otro nombre o rol; no se modifica.')
                self.stdout.write(json.dumps({
                    'username': username,
                    'role': existing.role,
                    'created': False,
                    'mustChangePassword': existing.password_change_required,
                }))
                return

            try:
                hashed = password(os.environ.get('MAIN_ADMIN_INITIAL_PASSWORD'))
            except ValidationError as exc:
                raise CommandError(
                    'Configura una clave inicial válida en MAIN_ADMIN_INITIAL_PASSWORD; no se imprime.'
                ) from exc

            SharedUser.objects.create(
                username=username,
                display_name=display_name,
                role='ADMIN',
                password_hash=hashed,
                enabled=True,
                password_change_required=True,
                notifications=False,
                status_audience='PRIVATE',
            )
            event('ADMIN_PROVISIONED', username, now(), actor='admin-provisioner')

        self.stdout.write(json.dumps({
            'username': username,
            'role': 'ADMIN',
            'created': True,
            'mustChangePassword': True,
        }))
