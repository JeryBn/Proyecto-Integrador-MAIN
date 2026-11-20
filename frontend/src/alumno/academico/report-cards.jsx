import {useState} from 'react';
import {userApi} from '../datos/user-api.mjs';
import {Feedback,useConfirmation,useResource} from '../datos/user-hooks.jsx';

export function PeriodClosure({courseId,roster}) {
  const path=`/api/v1/teaching/courses/${courseId}/periods`;
  const periods=useResource(path);
  const [number,setNumber]=useState(''),[maximum,setMaximum]=useState('20'),[scores,setScores]=useState({});
  const [busy,setBusy]=useState(false),[message,setMessage]=useState(null);
  const {confirm,confirmation}=useConfirmation();
  const period=periods.data?.find(item=>String(item.number)===number);
  const valid=Number(maximum)>0&&Number(maximum)<=1000&&roster.length>0&&roster.length<=300&&
    roster.every(item=>scores[item.studentId]?.trim()&&Number(scores[item.studentId])>=0&&Number(scores[item.studentId])<=Number(maximum));

  async function close() {
    setBusy(true);setMessage(null);
    try {
      await userApi.call(path+'/'+period.number+'/close',{method:'PUT',json:{maximum:Number(maximum),
        scores:roster.map(item=>({studentId:item.studentId,score:Number(scores[item.studentId])}))}});
      setScores({});
      setMessage({text:'Período cerrado. Los resultados ya están disponibles en Boletines.'});
      periods.reload();
    }catch(error){setMessage({text:error.message,error:true});}
    finally{setBusy(false);}
  }

  return <details className="workspace-card grades-section">
    <summary>Cierre de notas por período</summary>
    {confirmation}<Feedback resource={periods}/>
    {message&&<p role={message.error?'alert':'status'}>{message.text}</p>}
    {periods.data?.length===0&&<p>Vincula este curso a una sección con períodos configurados.</p>}
    {!!periods.data?.length&&<label>Período <select value={number} disabled={busy}
      onChange={event=>{setNumber(event.target.value);setScores({});setMessage(null);}}>
      <option value="">Selecciona un período</option>
      {periods.data.map(item=><option key={item.number} value={item.number}>{item.name}{item.closed?' · Cerrado':''}</option>)}
    </select></label>}
    {period?.closed&&<p>El período ya está cerrado.</p>}
    {period&&!period.closed&&!period.canClose&&<p>Disponible para cierre el {period.endsOn}.</p>}
    {period?.canClose&&<form onSubmit={event=>{event.preventDefault();if(valid&&!busy)
      confirm('¿Cerrar este período y publicar las notas finales? Las notas del boletín quedarán cerradas.',close);}}>
      <fieldset disabled={busy}>
        <label>Escala máxima <input type="number" min="0.01" max="1000" step="0.01" required
          value={maximum} onChange={event=>setMaximum(event.target.value)}/></label>
        {roster.map(item=><label key={item.studentId}>{item.name} <input type="number" min="0" max={maximum} step="0.01" required
          value={scores[item.studentId]??''} onChange={event=>setScores(value=>({...value,[item.studentId]:event.target.value}))}/></label>)}
        {roster.length===0&&<p>La lista del curso está vacía.</p>}
        <button disabled={!valid||busy}>{busy?'Guardando…':'Cerrar y publicar boletín'}</button>
      </fieldset>
    </form>}
  </details>;
}

export function ReportCards() {
  const [offset,setOffset]=useState(0);
  const resource=useResource('/api/v1/report-cards?offset='+offset);
  return <details className="workspace-card grades-section"><summary>Boletines</summary>
    <Feedback resource={resource}/>
    {resource.data?.length===0&&<p>Todavía no hay notas finales publicadas.</p>}
    {!!resource.data?.length&&<div className="student-roster">{resource.data.map(report=><article
      key={[report.courseId,report.studentId,report.year,report.period].join(':')}>
      <strong>{report.course} · {report.score}/{report.maximum}</strong>
      <p>{report.student} · {report.periodName} · {report.year}</p>
    </article>)}</div>}
    {(offset>0||resource.data?.length===30)&&<div className="form-actions">
      <button type="button" disabled={offset===0} onClick={()=>setOffset(offset-30)}>Anteriores</button>
      <button type="button" disabled={resource.data?.length!==30} onClick={()=>setOffset(offset+30)}>Más notas</button>
    </div>}
  </details>;
}
