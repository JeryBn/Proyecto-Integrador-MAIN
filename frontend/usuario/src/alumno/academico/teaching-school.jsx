import {useEffect,useState} from 'react';
import {userApi} from '../datos/user-api.mjs';
import './school-portal.css';

const fresh=()=>({enrollmentId:'',assessment:'',competency:'',score:'',achievement:'A',expectedVersion:-1});
export function TeachingSchool() {
  const [scopes,setScopes]=useState(null),[assignment,setAssignment]=useState(''),[period,setPeriod]=useState(''),[sheet,setSheet]=useState(null);
  const [form,setForm]=useState(fresh),[error,setError]=useState(''),[notice,setNotice]=useState(''),[busy,setBusy]=useState(false),[revision,setRevision]=useState(0);
  useEffect(()=>{let alive=true;userApi.call('/api/v1/teaching/school/scopes').then(s=>{if(alive){setScopes(s);setAssignment(v=>s.some(a=>a.id===v)?v:s[0]?.id||'');}}).catch(e=>alive&&setError(e.message));return()=>{alive=false;};},[revision]);
  useEffect(()=>{setPeriod('');setForm(fresh());setNotice('');},[assignment]);
  useEffect(()=>{let alive=true;setSheet(null);setError('');
    if(assignment)userApi.call(`/api/v1/teaching/school/${assignment}/marks${period?`?periodId=${period}`:''}`).then(s=>{if(alive){setSheet(s);if(!period)setPeriod(s.scope.activePeriodId||s.periods[0]?.id||'');}}).catch(e=>alive&&setError(e.message));
    return()=>{alive=false;};
  },[assignment,period,revision]);
  const scope=scopes?.find(s=>s.id===assignment);
  const change=(key,value)=>setForm({...form,[key]:value});
  async function save(e){e.preventDefault();setBusy(true);setError('');setNotice('');try{
    await userApi.call(`/api/v1/teaching/school/${assignment}/marks`,{method:'POST',json:{...form,periodId:period,score:scope.mode==='NUMERIC'?Number(form.score):null,achievement:scope.mode==='LITERAL'?form.achievement:null}});
    setNotice('Evaluación guardada. El apoderado vinculado ya puede consultarla en su libreta.');setForm(fresh());setRevision(v=>v+1);
  }catch(e){setError(e.message);}finally{setBusy(false);}}
  return <section className="school-portal"><header className="school-page-heading"><span className="eyebrow">COLEGIA MAIN · DOCENTES</span><h1>Evaluación escolar</h1><p>Registra evaluaciones por competencia y periodo para las aulas que tienes asignadas.</p><button onClick={()=>setRevision(v=>v+1)}>Actualizar evaluaciones</button></header>
    {error&&<p role="alert">{error}</p>}{notice&&<p role="status" className="school-notice">{notice}</p>}
    {scopes?.length===0&&<div className="user-panel school-empty-state"><h2>Tu aula está por asignar</h2><p>Administración debe asignarte como docente de un curso en Seguimiento escolar → Aulas y matrículas. Después podrás registrar las evaluaciones del periodo.</p><a href="#cursos">Consultar mis cursos y notas anteriores →</a></div>}
    {!!scopes?.length&&<div className="user-panel school-form-grid"><label>Aula y curso<select value={assignment} onChange={e=>setAssignment(e.target.value)}>{scopes.map(s=><option key={s.id} value={s.id}>{s.yearId} · {s.level==='PRIMARIA'?'Primaria':'Secundaria'} {s.grade}° {s.section} · {s.course}</option>)}</select></label><label>Periodo<select value={period} onChange={e=>{setPeriod(e.target.value);setForm(fresh());setNotice('');}}>{sheet?.periods.map(p=><option key={p.id} value={p.id}>{p.name}{p.id===scope?.activePeriodId?' · actual':''}</option>)}</select></label></div>}
    {assignment&&!sheet&&!error&&<p role="status">Cargando evaluaciones del aula…</p>}
    {sheet&&period&&<>
      <form className="user-panel" onSubmit={save}><h2>{form.expectedVersion===-1?'Registrar evaluación':'Editar evaluación'}</h2><fieldset className="school-form-grid" disabled={busy||!sheet.students.length}>
        <label>Estudiante<select required value={form.enrollmentId} onChange={e=>change('enrollmentId',e.target.value)} disabled={form.expectedVersion!==-1}><option value="">Selecciona…</option>{sheet.students.map(s=><option key={s.enrollmentId} value={s.enrollmentId}>{s.name}</option>)}</select></label>
        <label>Competencia<input required maxLength="120" placeholder="Nombre de la competencia evaluada" value={form.competency} onChange={e=>change('competency',e.target.value)}/></label>
        <label>Evaluación<input required maxLength="80" readOnly={form.expectedVersion!==-1} placeholder="Ej.: Práctica 1 · Resolución de problemas" value={form.assessment} onChange={e=>change('assessment',e.target.value)}/></label>
        {scope.mode==='NUMERIC'?<label>Nota (0 a {scope.maximum})<input type="number" required min="0" max={scope.maximum} step="0.01" value={form.score} onChange={e=>change('score',e.target.value)}/></label>:<label>Logro<select value={form.achievement} onChange={e=>change('achievement',e.target.value)}>{['AD','A','B','C'].map(v=><option key={v}>{v}</option>)}</select></label>}
        <button type="submit" disabled={busy||!sheet.students.length}>{busy?'Guardando…':'Guardar evaluación'}</button>{form.expectedVersion!==-1&&<button type="button" onClick={()=>setForm(fresh())}>Cancelar edición</button>}
      </fieldset>{!sheet.students.length&&<p>No hay estudiantes matriculados en esta aula.</p>}</form>
      <div className="user-panel"><h2>Evaluaciones registradas</h2><div className="school-data-table"><table><thead><tr>{['Estudiante','Competencia','Evaluación','Nota / logro','Acción'].map(h=><th key={h}>{h}</th>)}</tr></thead><tbody>{sheet.marks.map(m=><tr key={m.id}><td>{m.student}</td><td>{m.competency}</td><td>{m.assessment}</td><td>{m.achievement||`${m.score} / ${scope.maximum}`}</td><td><button onClick={()=>setForm({enrollmentId:m.enrollmentId,assessment:m.assessment,competency:m.competency,score:m.score??'',achievement:m.achievement||'A',expectedVersion:m.version})}>Editar</button></td></tr>)}</tbody></table></div>{!sheet.marks.length&&<p>Aún no hay evaluaciones para este periodo.</p>}</div>
    </>}
  </section>;
}
