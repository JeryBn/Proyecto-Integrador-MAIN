import json, os
from django.core.management.base import BaseCommand,CommandError
from administration.automatic_moderation import OpenAiModerator,ProviderUnavailable,run_batch

class Command(BaseCommand):
    help='Evaluar pendientes mediante OpenAI; shadow por defecto, publicación solo con --apply y política aprobada.'
    def add_arguments(self,parser):
        parser.add_argument('--limit',type=int,default=10)
        parser.add_argument('--apply',action='store_true')
        parser.add_argument('--synthetic-probe',action='store_true')
    def handle(self,*args,**options):
        provider=OpenAiModerator(os.environ.get('OPENAI_API_KEY',''))
        if options['synthetic_probe']:
            if options['apply']:raise CommandError('La prueba sintética no admite --apply.')
            try:result=provider.assess('En clase repasaremos multiplicación y división.')
            except ProviderUnavailable as error:raise CommandError(str(error)) from None
            self.stdout.write(json.dumps({'synthetic':True,'outcome':result.outcome,'model':result.model,'databaseChanged':False}));return
        try:summary=run_batch(provider,options['limit'],options['apply'])
        except ValueError as error:raise CommandError(str(error)) from None
        self.stdout.write(json.dumps({'mode':'apply' if options['apply'] else 'shadow','counts':summary}))
