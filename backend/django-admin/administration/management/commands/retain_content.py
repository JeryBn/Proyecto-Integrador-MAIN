import json, os, time
from django.core.management.base import BaseCommand, CommandError
from django.db import close_old_connections
from administration.retention import batch

class Command(BaseCommand):
    help = 'Retención 7/30/90 días. Vista previa por defecto; --watch repite lotes acotados.'
    def add_arguments(self, parser):
        parser.add_argument('--limit', type=int, default=100)
        parser.add_argument('--apply', action='store_true')
        parser.add_argument('--watch', action='store_true')
        parser.add_argument('--interval', type=int, default=3600)
    def handle(self, *args, **options):
        if not 1 <= options['limit'] <= 500 or not 60 <= options['interval'] <= 86400:
            raise CommandError('Lote 1–500; intervalo 60–86400 segundos.')
        if options['apply'] and os.getenv('MAIN_RETENTION_APPLY') != 'jery-2026-10-27-v1':
            raise CommandError('Requiere MAIN_RETENTION_APPLY=jery-2026-10-27-v1 y --apply.')
        try:
            while True:
                close_old_connections()
                self.stdout.write(json.dumps(batch(options['limit'], options['apply'])))
                self.stdout.flush()
                if not options['watch']: return
                time.sleep(options['interval'])
        except KeyboardInterrupt:
            return
