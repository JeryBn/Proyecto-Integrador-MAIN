from pathlib import Path
import os
from django.core.management.base import BaseCommand, CommandError
from django.db import connection, transaction

class Command(BaseCommand):
    help = 'Carga demostración explícita; rechaza una base con alumnos existentes.'
    def handle(self, *args, **options):
        import bcrypt
        source = Path(os.getenv('COLEGIA_SEED_FILE', '/app/database/seed.sql'))
        if not source.exists():
            source = Path(__file__).resolve().parents[5] / 'database/seed.sql'
        with transaction.atomic(), connection.cursor() as cursor:
            cursor.execute('SELECT COUNT(*) FROM alumno')
            if cursor.fetchone()[0]:
                raise CommandError('La base contiene alumnos; no se modifica. Usa una base de QA vacía.')
            sql = '\n'.join(line for line in source.read_text(encoding='utf-8').splitlines() if not line.startswith('--'))
            for statement in sql.split(';'):
                if statement.strip(): cursor.execute(statement)
            hashed = bcrypt.hashpw(b'ColegiaDemo2026!', bcrypt.gensalt(rounds=10)).decode()
            for username, name, role, student, teacher in [('alumno','Alex Demo','ALUMNO',1,None),('otro','Sam Demo','ALUMNO',2,None),('vacio','Sol Demo','ALUMNO',3,None),('docente','Docente Demo','DOCENTE',None,1),('apoderado','Apoderado Demo','FAMILIAR',None,None)]:
                cursor.execute('INSERT INTO main_user(username,display_name,role,password_hash,enabled,alumno_id,docente_id) VALUES(%s,%s,%s,%s,TRUE,%s,%s)',[username,name,role,hashed,student,teacher])
            cursor.execute("INSERT INTO guardian_link VALUES('apoderado',1,%s,CURRENT_TIMESTAMP)",[os.getenv('MAIN_ADMIN_USERNAME','admin')])
        self.stdout.write(self.style.SUCCESS('Demostración cargada. Clave de cuentas ficticias: ColegiaDemo2026!'))
