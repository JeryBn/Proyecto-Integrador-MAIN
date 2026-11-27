import {useEffect,useState} from 'react';
import {userApi} from '../datos/user-api.mjs';
import './school-portal.css';

export function StudentSchoolReport({revision}) {
  const [catalog,setCatalog]=useState(null),[year,setYear]=useState(''),[period,setPeriod]=useState(''),[report,setReport]=useState(null),[error,setError]=useState('');
  useEffect(()=>{let alive=true;setCatalog(null);setReport(null);setError('');userApi.call('/api/v1/school/catalog').then(c=>{if(alive){setCatalog(c);setYear(v=>c.years.some(y=>String(y.id)===v)?v:String(c.years[0]?.id||''));}}).catch(e=>alive&&setError(e.message));return()=>{alive=false;};},[revision]);
  const current=catalog?.years.find(y=>String(y.id)===year),periods=catalog?.periods.filter(p=>String(p.yearId)===year)||[];
  useEffect(()=>setPeriod(current?.activePeriodId||periods[0]?.id||''),[catalog,year]);
  useEffect(()=>{let alive=true;setReport(null);setError('');if(year&&period)userApi.call(`/api/v1/school/report?${new URLSearchParams({yearId:year,periodId:period})}`).then(r=>alive&&setReport(r)).catch(e=>alive&&setError(e.message));return()=>{alive=false;};},[year,period,revision]);
  if(error)return <p role="alert">{error}</p>;
  if(!catalog)return <p role="status">Cargando evaluaciones escolares…</p>;
  if(!catalog.years.length)return <p>Aún no tienes evaluaciones organizadas por año y aula. Administración debe registrar tu matrícula escolar.</p>;
  return <section className="school-portal user-panel"><h2>Mis evaluaciones escolares</h2><div className="school-form-grid"><label>Año escolar<select value={year} onChange={e=>setYear(e.target.value)}>{catalog.years.map(y=><option key={y.id} value={y.id}>{y.name}</option>)}</select></label><label>Periodo<select value={period} onChange={e=>setPeriod(e.target.value)}>{periods.map(p=><option key={p.id} value={p.id}>{p.name}{p.id===current?.activePeriodId?' · actual':''}</option>)}</select></label></div>
    {report?<><div className="school-data-table"><table><thead><tr><th>Curso</th><th>Competencia</th><th>Evaluación</th><th>Nota / logro</th></tr></thead><tbody>{report.marks.map(m=><tr key={m.id}><td>{m.course}</td><td>{m.competency}</td><td>{m.assessment}</td><td>{m.achievement||`${m.score} / ${m.maximum}`}</td></tr>)}</tbody></table></div>{!report.marks.length&&<p>Aún no hay calificaciones para este periodo.</p>}</>:<p role="status">Cargando calificaciones del periodo…</p>}
  </section>;
}
