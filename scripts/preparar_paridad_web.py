"""Soporte local de pruebas sintéticas. No opera cuentas ni contenidos reales."""
import sys,os,json,time,uuid,struct,zlib,argparse
from pathlib import Path
REPO=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(REPO/'scripts'))
from verificar_arquitectura_compartida import Session
OUT=Path(os.environ.get('MAIN_WEB_PARITY_FIXTURES',str(REPO/'var'/'web-parity')));OUT.mkdir(parents=True,exist_ok=True)
PASSWORD=os.environ['MAIN_TEST_WEB_PASSWORD']
def administrator():
    session=Session('http://127.0.0.1:8090');_,token=session.call('/api/admin/csrf')
    assert session.call('/api/admin/login','POST',dict(username='android_test_admin',password=os.environ['MAIN_TEST_ADMIN_PASSWORD']),token,True)[0]==200
    _,token=session.call('/api/admin/csrf');return session,token
def student(name):
    session=Session('http://127.0.0.1:8088');_,token=session.call('/login-token')
    assert session.call('/login','POST',dict(username=name,password=PASSWORD,**{token['parameterName']:token['token']}),form=True)[0]==200
    _,token=session.call('/api/csrf');return session,token
def png():
    def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',64,64,8,2,0,0,0))+chunk(b'IDAT',zlib.compress((b'\x00'+b'\x08\x7f\x68'*64)*64))+chunk(b'IEND',b'')
def main():
    parser=argparse.ArgumentParser();parser.add_argument('action',choices=['seed','approve-message','reject-message','approve-status','inspect','peer-send']);parser.add_argument('id',nargs='?');args=parser.parse_args()
    admin,token=administrator()
    def mutate(path,value,method='POST',expected=200):
        status,data=admin.call('/api/admin/v1/'+path,method,value,token);assert status==expected,(path,status,data);return data
    if args.action=='seed':
        seed=int(time.time()*1000);accounts={key:f'web_{key}_{seed}' for key in ('student','peer','teacher','family','outsider')}
        ids=dict(student=seed+1,teacher=seed+2,peer=seed+3,outsider=seed+4)
        for key,role,name in [('student','ALUMNO','Estudiante web de prueba'),('peer','ALUMNO','Compañero web de prueba'),('teacher','DOCENTE','Docente web de prueba'),('family','FAMILIAR','Familiar web de prueba'),('outsider','ALUMNO','Ajeno web de prueba')]:
            value=dict(username=accounts[key],name=name,role=role,password=PASSWORD)
            if role=='ALUMNO':value['alumnoId']=ids[key]
            if role=='DOCENTE':value['docenteId']=ids[key]
            mutate('users',value,expected=201)
        course=seed+10;activity=seed+11
        mutate('courses',dict(id=course,name='Paridad web curso sintético',teacherUsername=accounts['teacher']),expected=201)
        for key in ('student','peer'):mutate(f'courses/{course}/students',dict(username=accounts[key],enrolled=True),'PUT')
        mutate('guardians',dict(guardian=accounts['family'],studentId=ids['student']))
        for key in ('peer','teacher','family'):mutate('contacts',dict(first=accounts['student'],second=accounts[key]))
        mutate('contacts',dict(first=accounts['teacher'],second=accounts['peer']))
        mutate(f'courses/{course}/grades',dict(studentId=ids['student'],assessment='Evaluación web de prueba',score=18,maximum=20,expectedRecordedAt='NEW'),'PUT')
        slot=mutate('timetable',dict(courseId=course,dayOfWeek=1,startsAt='08:00',endsAt='09:00',classroom='WEB-'+str(seed)),expected=201)
        status,_=admin.call('/api/admin/actividades','POST',dict(id=activity,cursoId=course,titulo='WEB-PARITY actividad de prueba',descripcion='Dato sintético para comparar web y Android.',fechaLimite='2026-11-26T15:30:00'),token);assert status==201
        manifest=dict(accounts=accounts,academicIds=ids,courseId=course,activityId=activity,timetableId=slot['id'])
        (OUT/'fixtures.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
        (OUT/'imagen-web.png').write_bytes(png());(OUT/'documento-web.txt').write_text('WEB-PARITY documento sintético para pruebas de adjuntos.\n',encoding='utf-8')
        print(json.dumps(manifest,ensure_ascii=False));return
    fixtures=json.loads((OUT/'fixtures.json').read_text(encoding='utf-8'));allowed=set(fixtures['accounts'].values())
    if args.action in ('approve-message','reject-message','approve-status'):
        kind='statuses' if args.action=='approve-status' else 'messages'
        status,queue=admin.call('/api/admin/v1/moderation/'+kind+'?limit=100');assert status==200
        selected=next(q for q in queue if q['id']==args.id)
        assert selected.get('sender',selected.get('owner')) in allowed,'Contenido fuera de los fixtures; no se revisa'
        approved=args.action!='reject-message'
        body=dict(contentHash=selected['contentHash'],approved=approved,reason='' if approved else 'Categoría sintética para comprobar corrección',attachmentReviewed=True)
        if kind=='messages':body['revision']=selected['revision']
        mutate(f'moderation/{kind}/{args.id}/review',body)
        print(json.dumps(dict(id=args.id,reviewed=True,approved=approved)));return
    if args.action=='peer-send':
        client,csrf=student(fixtures['accounts']['peer'])
        code,value=client.call('/api/v1/conversations/direct','POST',dict(username=fixtures['accounts']['student']),csrf);assert code==201
        code,message=client.call('/api/v1/conversations/'+value['id']+'/messages','POST',dict(clientKey=str(uuid.uuid4()),text='WEB-PARITY respuesta del compañero desde API'),csrf);assert code==202
        (OUT/'peer-message.json').write_text(json.dumps(message,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps(dict(conversationId=value['id'],messageId=message['id'])));return
    observations={}
    for key,name in fixtures['accounts'].items():
        client,_=student(name)
        observations[key]={path:client.call('/api/v1/'+path)[1] for path in ('conversations?limit=100','statuses','notifications?limit=100','preferences','agenda')}
        observations[key]['messages']={c['id']:client.call('/api/v1/conversations/'+c['id']+'/messages?limit=100')[1] for c in observations[key]['conversations?limit=100'] if c['state']=='ACTIVE'}
    (OUT/'observations.json').write_text(json.dumps(observations,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps({key:dict(preferences=value['preferences'],conversations=value['conversations?limit=100'],
                              messages=[dict(id=m['id'],revision=m['revision'],text=m['text'],state=m['state']) for messages in value['messages'].values() for m in messages],
                              statuses=[dict(id=s['id'],state=s['state'],kind=s['kind'],text=s['text']) for s in value['statuses']],
                              notifications=len(value['notifications?limit=100'])) for key,value in observations.items()},ensure_ascii=False))
if __name__=='__main__':main()
