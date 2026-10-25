import json, os
from django.core.management.base import BaseCommand,CommandError
from administration.automatic_moderation import OpenAiModerator,ProviderUnavailable,run_batch

class Command(BaseCommand):
    help='Evaluar pendientes mediante OpenAI; shadow por defecto, publicación solo con --apply y política aprobada.'
    def add_arguments(self,parser):
        parser.add_argument('--limit',type=int,default=10)
        parser.add_argument('--apply',action='store_true')
        parser.add_argument('--synthetic-probe',action='store_true')
        parser.add_argument('--synthetic-image',action='store_true')
    def handle(self,*args,**options):
        provider=OpenAiModerator(os.environ.get('OPENAI_API_KEY',''))
        if options['synthetic_probe']:
            if options['apply']:raise CommandError('La prueba sintética no admite --apply.')
            image=None
            if options['synthetic_image']:
                import struct,zlib
                def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
                image=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',32,32,8,2,0,0,0))+chunk(b'IDAT',zlib.compress((b'\x00'+b'\x00\x88\x99'*32)*32))+chunk(b'IEND',b'')
            try:result=provider.assess('En clase repasaremos multiplicación y división.',image)
            except ProviderUnavailable as error:raise CommandError(str(error)) from None
            self.stdout.write(json.dumps({'synthetic':True,'image':bool(image),'outcome':result.outcome,'model':result.model,'databaseChanged':False}));return
        if options['synthetic_image']:raise CommandError('--synthetic-image requiere --synthetic-probe.')
        try:summary=run_batch(provider,options['limit'],options['apply'])
        except ValueError as error:raise CommandError(str(error)) from None
        self.stdout.write(json.dumps({'mode':'apply' if options['apply'] else 'shadow','counts':summary}))
