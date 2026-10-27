import json,os,re
from django.core.management.base import BaseCommand,CommandError
from django.db import transaction
from administration.models import SharedUser
from administration.identity import password
from administration.retention import event
from administration.moderation import now

class Command(BaseCommand):
    help='Provisiona un REVIEWER limitado con clave temporal por entorno. Nunca sobrescribe cuentas existentes.'
    def add_arguments(self,parser):
        parser.add_argument('--username',required=True)
        parser.add_argument('--name',required=True)
        parser.add_argument('--inspect',action='store_true')
    def handle(self,*args,**options):
        username=options['username'];name=options['name']
        if not re.fullmatch(r'[a-z][a-z0-9_.-]{2,59}',username) or not 1<=len(name.strip())<=100:raise CommandError('Identidad de revisor inválida.')
        with transaction.atomic():
            existing=SharedUser.objects.select_for_update().filter(username=username).first()
            if options['inspect']:
                if existing and (existing.role!='REVIEWER' or existing.display_name!=name):raise CommandError('La cuenta existente no corresponde al revisor indicado; no se modifica.')
                self.stdout.write(json.dumps(dict(exists=bool(existing),enabled=existing.enabled if existing else False,mustChangePassword=existing.password_change_required if existing else False)));return
            if existing:raise CommandError('La cuenta ya existe; no se cambia su contraseña ni sus permisos.')
            try:hashed=password(os.environ.get('MAIN_REVIEWER_INITIAL_PASSWORD'))
            except ValueError:raise CommandError('Configura una clave inicial válida en MAIN_REVIEWER_INITIAL_PASSWORD; no se imprime.') from None
            SharedUser.objects.create(username=username,display_name=name,role='REVIEWER',password_hash=hashed,enabled=True,password_change_required=True,notifications=False,status_audience='PRIVATE')
            event('REVIEWER_PROVISIONED',username,now(),actor='reviewer-provisioner')
        self.stdout.write(json.dumps(dict(username=username,role='REVIEWER',created=True,mustChangePassword=True)))
