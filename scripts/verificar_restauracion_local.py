"""Ensayo de respaldo/restauración SOLO de main_architecture_review, MySQL sintético local.

Requiere usuario local con permiso para crear/eliminar una base temporal. El dump
puede contener datos privados: se guarda en var, nunca se publica como evidencia.
No sustituye el respaldo cifrado, custodia o recuperación de un servidor real.
"""
import argparse, datetime, hashlib, json, os, re, subprocess, uuid
from pathlib import Path
import MySQLdb

def fingerprint(connection):
    result={}
    with connection.cursor() as cursor:
        cursor.execute('SHOW TABLES')
        for (table,) in cursor.fetchall():
            if not re.fullmatch(r'[A-Za-z0-9_]+',table):raise ValueError('Tabla no admitida')
            cursor.execute(f'SELECT * FROM `{table}`')
            encoded=[]
            for row in cursor.fetchall():
                values=[{'bytes':value.hex()} if isinstance(value,bytes) else str(value) if value is not None else None for value in row]
                encoded.append(json.dumps(values,ensure_ascii=False,separators=(',',':')).encode())
            digest=hashlib.sha256()
            for row in sorted(encoded):digest.update(len(row).to_bytes(8,'big'));digest.update(row)
            result[table]={'rows':len(encoded),'sha256':digest.hexdigest()}
    return result

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--mysql-bin',type=Path,required=True)
    parser.add_argument('--result',type=Path,required=True)
    args=parser.parse_args()
    source='main_architecture_review';target='main_restore_probe_'+uuid.uuid4().hex
    options=dict(host='127.0.0.1',port=3307,user=os.environ['MAIN_RESTORE_PROBE_USER'],passwd=os.environ.get('MAIN_RESTORE_PROBE_PASSWORD',''),charset='utf8mb4')
    private=Path(__file__).resolve().parents[1]/'var'/'restore-probe'/target
    private.mkdir(parents=True,exist_ok=False);dump=private/'database.sql'
    connection=MySQLdb.connect(**options,db=source,autocommit=True)
    created=False
    try:
        before=fingerprint(connection)
        env=os.environ.copy();env['MYSQL_PWD']=options['passwd']
        common=['--host=127.0.0.1','--port=3307','--protocol=TCP','--user='+options['user'],'--default-character-set=utf8mb4']
        with dump.open('wb') as output:
            process=subprocess.run([str(args.mysql_bin/'mysqldump.exe'),'--no-defaults',*common,'--single-transaction','--skip-lock-tables','--no-tablespaces','--hex-blob','--set-gtid-purged=OFF',source],stdout=output,stderr=subprocess.PIPE,env=env)
        if process.returncode:raise RuntimeError('Falló el respaldo local; no se imprimen detalles privados del dump.')
        with connection.cursor() as cursor:cursor.execute(f'CREATE DATABASE `{target}` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci')
        created=True
        with dump.open('rb') as input_file:
            process=subprocess.run([str(args.mysql_bin/'mysql.exe'),'--no-defaults',*common,target],stdin=input_file,stdout=subprocess.DEVNULL,stderr=subprocess.PIPE,env=env)
        if process.returncode:raise RuntimeError('Falló la restauración en la base temporal aislada.')
        restored=MySQLdb.connect(**options,db=target)
        try:after=fingerprint(restored)
        finally:restored.close()
        if before!=after:raise AssertionError('Las filas restauradas difieren del origen. Repetir sin escrituras concurrentes.')
        result=dict(measuredAtUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),source=source,
            tables=len(before),rows=sum(value['rows'] for value in before.values()),contentVerified=True,
            dumpSha256=hashlib.sha256(dump.read_bytes()).hexdigest(),
            limitation='Ensayo local de base sintética; no acredita hosting, cifrado, custodia ni restauración de producción.')
        args.result.parent.mkdir(parents=True,exist_ok=True);args.result.write_text(json.dumps(result,indent=2),encoding='utf-8')
        print(json.dumps(result))
    finally:
        if created and re.fullmatch(r'main_restore_probe_[0-9a-f]{32}',target):
            with connection.cursor() as cursor:cursor.execute(f'DROP DATABASE `{target}`')
        connection.close()

if __name__=='__main__':main()
