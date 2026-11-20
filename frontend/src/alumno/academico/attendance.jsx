import {useState} from 'react';
import {userApi} from '../datos/user-api.mjs';
import {useResource} from '../datos/user-hooks.jsx';

const states = [['PRESENT','Presente'],['ABSENT','Ausente'],['LATE','Tardanza']];
const todayInLima = () => new Intl.DateTimeFormat('sv-SE', {
  timeZone:'America/Lima',year:'numeric',month:'2-digit',day:'2-digit'
}).format(new Date());

export function Attendance({courseId}) {
  const [date,setDate] = useState(todayInLima);
  const [session,setSession] = useState(1);
  const [draft,setDraft] = useState({});
  const [saving,setSaving] = useState(false);
  const [feedback,setFeedback] = useState(null);
  const path = '/api/v1/teaching/courses/'+encodeURIComponent(courseId)+'/attendance/'+date;
  const query = "?session="+session;
  const roster = useResource(path+query,{enabled:!!date});

  const changes = Object.values(draft);
  function select(student,status) {
    setFeedback(null);
    setDraft(current=>{
      const next={...current};
      if(status===student.status)delete next[student.studentId];
      else next[student.studentId]={studentId:student.studentId,status,expectedStatus:student.status};
      return next;
    });
  }

  async function save() {
    if(saving||!changes.length)return;
    setFeedback(null);
    setSaving(true);
    try {
      await userApi.call(path+'/batch'+query,{method:'PUT',json:{changes}});
      setDraft({});
      roster.reload();
      setFeedback({text:'Asistencia guardada.'});
    } catch (error) {
      roster.reload();
      setFeedback({error:true,text:error.message});
    } finally {
      setSaving(false);
    }
  }

  return <section className="workspace-card grades-section" aria-label="Asistencia">
    <header><h2>Asistencia</h2><label>Fecha{' '}
      <input type="date" value={date} min="1970-01-01" max={todayInLima()} disabled={saving||changes.length>0}
        onChange={event=>{setFeedback(null);setDate(event.target.value);}}/>
    </label><label>Sesión <select value={session} disabled={saving||changes.length>0}
      onChange={event=>{setFeedback(null);setSession(Number(event.target.value));}}>
      {Array.from({length:8},(_,index)=><option key={index+1} value={index+1}>Sesión {index+1}</option>)}
    </select></label></header>
    {roster.error&&<p role="alert">{roster.error} <button type="button" onClick={roster.reload}>Reintentar</button></p>}
    {feedback&&<p role={feedback.error?'alert':'status'}>{feedback.text}</p>}
    {date&&roster.loading&&<p role="status">Cargando lista…</p>}
    {date&&roster.data?.length===0&&<p>No hay estudiantes matriculados.</p>}
    {date&&!roster.loading&&<div className="student-roster">{roster.data?.map(student=>
      <article key={student.studentId}>
        <strong>{student.name}</strong>
        <div className="grade-edit-actions" role="group" aria-label={'Asistencia de '+student.name}>
          {states.map(([status,label])=><button type="button" key={status}
            aria-pressed={(draft[student.studentId]?.status||student.status)===status} disabled={saving}
            onClick={()=>select(student,status)}>{label}{(draft[student.studentId]?.status||student.status)===status?' ✓':''}</button>)}
        </div>
      </article>
    )}</div>}
  </section>;
}
