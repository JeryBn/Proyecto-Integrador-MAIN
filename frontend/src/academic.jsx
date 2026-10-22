import {useEffect, useState} from 'react';
import {userApi} from './user-api.mjs';

async function api(path, body) {
  return userApi.call('/api/v1/'+path,{method:body?'PUT':'GET',json:body});
}
export function Academic({profile}) {
  const [agenda,setAgenda]=useState(null),[grades,setGrades]=useState([]),[error,setError]=useState('');
  const [course,setCourse]=useState(''),[roster,setRoster]=useState([]),[revision,setRevision]=useState(0);
  const [busy,setBusy]=useState(false),[notice,setNotice]=useState('');
  const [form,setForm]=useState({studentId:'',assessment:'',score:'',maximum:'20',expectedRecordedAt:'NEW'});
  const teacher=profile?.rol==='Docente';
  useEffect(()=>{
    if(!profile)return;
    let active=true;setError('');setAgenda(null);
    Promise.all([api('agenda'),profile.rol==='Alumno'?api('grades'):Promise.resolve([])])
      .then(([a,g])=>{if(active){setAgenda(a);setGrades(g);}}).catch(e=>{if(active)setError(e.message);});
    return ()=>{active=false;};
  },[profile,revision]);
  useEffect(()=>{
    let active=true;setRoster([]);setForm({studentId:'',assessment:'',score:'',maximum:'20',expectedRecordedAt:'NEW'});
    if(teacher && course)api(`teaching/courses/${course}/grades`).then(r=>{if(active)setRoster(r);}).catch(e=>{if(active)setError(e.message);});
    return ()=>{active=false;};
  },[course,teacher,revision]);
  const list=gs=>gs.length?<ul>{gs.map(g=><li key={g.id}><strong>{g.assessment}</strong>: {g.score} / {g.maximum}</li>)}</ul>:<p>Aún no se registraron notas.</p>;
  return <section className="list-panel academic" aria-label="Cursos y notas"><h1>Cursos y notas</h1>
    {error && <div role="alert"><p>{error}</p><button onClick={()=>setRevision(v=>v+1)}>Actualizar cursos</button></div>}
    {notice && <p role="status">{notice}</p>}
    {!agenda && !error && <p role="status">Cargando cursos…</p>}
    {agenda && <>
      {!agenda.courses.length && <p>No tienes cursos autorizados.</p>}
      {agenda.courses.map(c=><article className="activity-card" key={c.id}><div><h2>{c.name}</h2><p>Docente: {c.teacher}</p>
        {profile?.rol==='Alumno' && list(grades.find(g=>g.courseId===c.id)?.grades || [])}
        {teacher && <button onClick={()=>{setCourse(String(c.id));setError('');setNotice('');}}>Registrar y editar notas de {c.name}</button>}
      </div></article>)}
      <h2>Horario</h2>{!agenda.timetable.length?<p>No hay horarios registrados.</p>:<ul>{agenda.timetable.map(t=><li key={t.id}>{['','Lunes','Martes','Miércoles','Jueves','Viernes','Sábado','Domingo'][t.dayOfWeek]} {t.startsAt}–{t.endsAt} · {t.course} · {t.classroom}</li>)}</ul>}
      {teacher && course && <section><h2>Notas del curso seleccionado</h2>
        {roster.map(s=><article key={s.studentId} className="activity-card"><div><h3>{s.name}</h3>{list(s.grades)}
          <button onClick={()=>setForm({studentId:String(s.studentId),assessment:'',score:'',maximum:'20',expectedRecordedAt:'NEW'})}>Nueva nota para {s.name}</button>
          {s.grades.map(g=><button key={g.id} onClick={()=>setForm({studentId:String(s.studentId),assessment:g.assessment,score:String(g.score),maximum:String(g.maximum),expectedRecordedAt:g.recordedAt})}>Editar {g.assessment} de {s.name}</button>)}
        </div></article>)}
        {!roster.length && <p>No hay estudiantes cargados. Actualiza para volver a consultar.</p>}
        {form.studentId && <form onSubmit={async e=>{e.preventDefault();setBusy(true);setError('');setNotice('');
          try {await api(`teaching/courses/${course}/grades`,{...form,studentId:Number(form.studentId),score:Number(form.score),maximum:Number(form.maximum)});setNotice('Nota guardada. El estudiante puede verla en la app y en la web.');setRevision(v=>v+1);}
          catch(e){setError(e.message);}finally{setBusy(false);}
        }}><fieldset disabled={busy}><legend>Evaluación de {roster.find(s=>String(s.studentId)===form.studentId)?.name}</legend>
          <label>Evaluación<input required maxLength="120" readOnly={form.expectedRecordedAt!=='NEW'} value={form.assessment} onChange={e=>setForm({...form,assessment:e.target.value})}/></label>
          <label>Nota<input required type="number" min="0" max={form.maximum} step="0.01" value={form.score} onChange={e=>setForm({...form,score:e.target.value})}/></label>
          <label>Escala máxima<input required type="number" min="0.01" max="1000" step="0.01" value={form.maximum} onChange={e=>setForm({...form,maximum:e.target.value})}/></label>
          <p>No se calcula un promedio oficial. Si otra persona cambia la nota, actualiza el curso antes de reintentar.</p><button type="submit">{busy?'Guardando…':'Guardar nota'}</button>
        </fieldset></form>}
      </section>}
    </>}
  </section>;
}
