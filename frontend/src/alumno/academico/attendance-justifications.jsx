import {useState} from 'react';
import {userApi} from '../datos/user-api.mjs';
import {Feedback,useResource} from '../datos/user-hooks.jsx';

const endpoint='/api/v1/attendance/justifications';
const labels={PENDING:'En revisión',APPROVED:'Justificada',REJECTED:'No justificada'};

export function AttendanceJustifications({courseId}) {
  const teacher=courseId!=null;
  const [offset,setOffset]=useState(0);
  const rows=useResource(endpoint+'?offset='+offset+(teacher?'&courseId='+courseId:''));
  const [selected,setSelected]=useState(null),[reason,setReason]=useState('');
  const [busy,setBusy]=useState(false),[feedback,setFeedback]=useState(null);

  async function save(item,approved){
    setBusy(true);setFeedback(null);
    try {
      if(teacher)await userApi.call(endpoint+'/'+encodeURIComponent(item.id),{method:'PUT',json:{approved,note:reason.trim()}});
      else await userApi.call(endpoint,{method:'POST',json:{courseId:item.courseId,studentId:item.studentId,
        date:item.date,session:item.session,reason:reason.trim()}});
      setSelected(null);setReason('');rows.reload();
      setFeedback({message:teacher?'Decisión guardada.':'Justificación enviada.'});
    } catch(error){
      setFeedback({error:true,message:error.message});
    } finally {setBusy(false);}
  }

  return <section className="workspace-card grades-section" aria-label="Justificaciones de asistencia">
    <h2>Justificaciones</h2>
    <Feedback resource={rows}/>
    {feedback&&<p role={feedback.error?'alert':'status'}>{feedback.message}</p>}
    {rows.data?.length===0&&<p>{teacher?'No hay solicitudes para este curso.':'No hay faltas ni tardanzas recientes.'}</p>}
    <div className="student-roster">{rows.data?.map(item=>{
      const key=item.id||[item.courseId,item.studentId,item.date,item.session].join(':');
      const actionable=teacher?item.state==='PENDING':!item.id;
      return <article key={key}>
        <strong>{item.student} · {item.course}</strong>
        <p>{item.date} · Sesión {item.session} · {item.attendance==='LATE'?'Tardanza':item.attendance==='ABSENT'?'Ausencia':'Presente'}</p>
        {item.state&&<p>{labels[item.state]}</p>}
        {item.reason&&<p>{item.reason}</p>}
        {item.decisionNote&&<p>{item.decisionNote}</p>}
        {actionable&&selected!==key&&<button type="button" disabled={busy} onClick={()=>{setSelected(key);setReason('');}}>
          {teacher?'Revisar solicitud':'Justificar'}
        </button>}
        {actionable&&selected===key&&<form onSubmit={event=>{event.preventDefault();if(!teacher&&reason.trim())save(item);}}>
          <label>{teacher?'Nota de revisión (opcional)':'Motivo'} <textarea value={reason} disabled={busy}
            maxLength={teacher?300:500} required={!teacher} onChange={event=>setReason(event.target.value)}/></label>
          <div className="form-actions">
            <button type="button" disabled={busy} onClick={()=>setSelected(null)}>Cancelar</button>
            {teacher?<><button type="button" disabled={busy} onClick={()=>save(item,false)}>Rechazar</button>
              <button type="button" disabled={busy} onClick={()=>save(item,true)}>Aprobar</button></>:
              <button disabled={busy||!reason.trim()}>Enviar</button>}
          </div>
        </form>}
      </article>;
    })}</div>
    {(offset>0||rows.data?.length===30)&&<div className="form-actions">
      <button type="button" disabled={busy||offset===0} onClick={()=>{setSelected(null);setOffset(offset-30);}}>Anteriores</button>
      <button type="button" disabled={busy||rows.data?.length!==30} onClick={()=>{setSelected(null);setOffset(offset+30);}}>Más registros</button>
    </div>}
  </section>;
}
