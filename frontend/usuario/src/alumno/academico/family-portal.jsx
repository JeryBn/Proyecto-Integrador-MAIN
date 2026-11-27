import {StitchFamily} from '../../diseno/stitch-portals.jsx';
import {useEffect,useState} from 'react';
import {userApi,localDate} from '../datos/user-api.mjs';
import {PublicationList} from './school-publications.jsx';
import './school-portal.css';

const attendanceLabels={PRESENT:'Presente',LATE:'Tardanza',ABSENT:'Falta',JUSTIFIED:'Falta justificada'};
const value=mark=>mark.achievement||`${mark.score} / ${mark.maximum}`;

export function FamilyPortal({profile,section}) {
  const [students,setStudents]=useState(null),[studentId,setStudent]=useState(''),[catalog,setCatalog]=useState(null);
  const [yearId,setYear]=useState(''),[periodId,setPeriod]=useState(''),[courseId,setCourse]=useState('');
  const [report,setReport]=useState(null),[attendance,setAttendance]=useState(null),[publications,setPublications]=useState(null);
  const [from,setFrom]=useState(''),[to,setTo]=useState(''),[error,setError]=useState(''),[downloading,setDownloading]=useState(false),[revision,setRevision]=useState(0);
  const tab=section==='#asistencia'?'attendance':section==='#publicaciones'?'publications':section==='#notas'?'grades':'summary';
  useEffect(()=>{let alive=true;setError('');
    userApi.call('/api/v1/family/students').then(data=>{if(alive){setStudents(data);setStudent(v=>data.some(s=>String(s.id)===v)?v:String(data[0]?.id||''));}}).catch(e=>alive&&setError(e.message));
    return()=>{alive=false;};
  },[revision]);
  useEffect(()=>{let alive=true;setCatalog(null);setReport(null);setPublications(null);setAttendance(null);setYear('');setPeriod('');setCourse('');setError('');
    if(studentId)Promise.all([userApi.call(`/api/v1/family/students/${studentId}/catalog`),userApi.call(`/api/v1/family/students/${studentId}/publications`)]).then(([c,p])=>{if(alive){setCatalog(c);setPublications(p);setYear(String(c.years[0]?.id||''));}}).catch(e=>alive&&setError(e.message));
    return()=>{alive=false;};
  },[studentId,revision]);
  const year=catalog?.years.find(y=>String(y.id)===yearId),periods=catalog?.periods.filter(p=>String(p.yearId)===yearId)||[];
  useEffect(()=>{setPeriod(year?.activePeriodId||periods[0]?.id||'');setCourse('');setFrom(yearId?`${yearId}-01-01`:'');setTo(yearId?`${yearId}-12-31`:'');},[yearId,catalog]);
  useEffect(()=>{let alive=true;setReport(null);setError('');
    if(studentId&&yearId&&periodId){const q=new URLSearchParams({yearId,periodId});if(courseId)q.set('courseId',courseId);
      userApi.call(`/api/v1/family/students/${studentId}/report?${q}`).then(r=>alive&&setReport(r)).catch(e=>alive&&setError(e.message));}
    return()=>{alive=false;};
  },[studentId,yearId,periodId,courseId,revision]);
  useEffect(()=>{let alive=true;setAttendance(null);
    if(studentId&&yearId&&from&&to&&(tab==='attendance'||tab==='summary'))userApi.call(`/api/v1/family/students/${studentId}/attendance?${new URLSearchParams({yearId,from,to})}`).then(r=>{if(alive){setAttendance(r);setError('');}}).catch(e=>alive&&setError(e.message));
    return()=>{alive=false;};
  },[studentId,yearId,from,to,tab,revision]);
  async function download(){setDownloading(true);setError('');try{
    const q=new URLSearchParams({yearId,periodId});if(courseId)q.set('courseId',courseId);
    const blob=await userApi.download(`/api/v1/family/students/${studentId}/report.pdf?${q}`);
    const url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download=`libreta-${studentId}-${yearId}.pdf`;a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);
  }catch(e){setError(e.message||'No se pudo descargar la libreta. Intenta nuevamente.');}finally{setDownloading(false);}}
  async function read(item){await userApi.call(`/api/v1/family/students/${studentId}/publications/${item.id}/read`,{method:'POST'});setPublications(items=>items.map(p=>p.id===item.id?{...p,read:true}:p));}
  const heading={summary:'Acompaña su aprendizaje',grades:'Notas y libreta',attendance:'Asistencia escolar',publications:'Comunicados del aula'}[tab];
  if(tab==='summary')return <StitchFamily profile={profile} students={students} studentId={studentId} setStudent={setStudent} catalog={catalog} year={year} report={report} attendance={attendance} publications={publications} error={error} download={download} downloading={downloading}/>;
  return <section className="school-portal family-portal">
    <header className="school-page-heading"><span className="eyebrow">COLEGIA · ESPACIO DEL APODERADO</span><h1>{heading}</h1><p>Hola, {profile.nombre}. Consulta la información de los estudiantes vinculados a tu cuenta.</p><button onClick={()=>setRevision(v=>v+1)}>Actualizar información</button></header>
    {error&&<p className="school-error" role="alert">{error}</p>}
    {students===null&&!error&&<p role="status">Cargando tu familia…</p>}
    {students?.length===0&&<div className="user-panel school-empty-state"><h2>Aún no tienes estudiantes vinculados</h2><p>Solicita a administración que verifique tu identidad y registre el parentesco. Después podrás consultar la información de tu hijo.</p></div>}
    {!!students?.length&&<>
      <div className="user-panel school-form-grid"><label>Estudiante vinculado<select aria-label="Estudiante vinculado" value={studentId} onChange={e=>setStudent(e.target.value)}>{students.map(s=><option key={s.id} value={s.id}>{s.name}</option>)}</select></label>
        {catalog?.years.length>0&&<label>Año escolar<select value={yearId} onChange={e=>setYear(e.target.value)}>{catalog.years.map(y=><option key={y.id} value={y.id}>{y.name}</option>)}</select></label>}
        {year&&<div className="school-context"><strong>{year.level==='PRIMARIA'?'Primaria':'Secundaria'} · {year.grade}° {year.section}</strong><span>{catalog.student.name}</span></div>}
      </div>
      {!catalog&&!error&&<p role="status">Cargando información del estudiante…</p>}
      {catalog&&tab==='summary'&&<>
        <div className="family-welcome user-panel"><div><span className="eyebrow">INFORMACIÓN QUE TE ACERCA</span><h2>{catalog.student.name}</h2><p>Revisa sus evaluaciones, acompaña su asistencia y mantente al día con su aula.</p></div><div className="school-orbit" aria-hidden="true"><span>C</span></div></div>
        <div className="school-overview-cards"><a className="user-panel" href="#notas"><span className="school-card-icon">01</span><h3>Notas y libreta</h3><p>Evaluaciones por curso, competencias y trimestre. Descarga su libreta en PDF.</p><strong>Consultar notas →</strong></a><a className="user-panel" href="#asistencia"><span className="school-card-icon">02</span><h3>Asistencia</h3><p>Presencias, tardanzas y faltas registradas por el colegio.</p><strong>Ver asistencia →</strong></a><a className="user-panel" href="#publicaciones"><span className="school-card-icon">03</span><h3>Su comunidad escolar</h3><p>{publications?`${publications.filter(p=>!p.read).length} publicaciones por leer.`:'Comunicados, tareas y próximos exámenes.'}</p><strong>Ver publicaciones →</strong></a></div>
      </>}
      {catalog&&!catalog.years.length&&tab!=='publications'&&<p className="user-panel">Administración todavía debe registrar la matrícula escolar de {catalog.student.name} en un año y aula. Las notas y la asistencia aparecerán después de ese registro.</p>}
      {tab==='grades'&&year&&<>
        <div className="user-panel school-form-grid"><label>Periodo<select aria-label="Periodo" value={periodId} onChange={e=>setPeriod(e.target.value)}>{periods.map(p=><option key={p.id} value={p.id}>{p.name}{p.id===year.activePeriodId?' · actual':''}</option>)}</select></label><label>Curso<select aria-label="Curso" value={courseId} onChange={e=>setCourse(e.target.value)}><option value="">Todos los cursos</option>{report?.courses.map(c=><option key={c.id} value={c.id}>{c.name}</option>)}</select></label><button onClick={download} disabled={downloading||!report}>{downloading?'Preparando libreta…':'Descargar libreta PDF'}</button></div>
        {!year.activePeriodId&&<p>El colegio aún no ha establecido el periodo actual. Puedes consultar cada periodo con el filtro.</p>}
        {!report&&!error&&<p role="status">Cargando calificaciones…</p>}
        {report&&<div className="user-panel"><h2>{report.period.name} · Calificaciones</h2><p>Las valoraciones AD/A/B/C se muestran tal como fueron registradas. Las notas numéricas incluyen su escala.</p><div className="school-data-table"><table><thead><tr>{['Curso','Competencia','Evaluación','Nota / logro','Registro'].map(h=><th key={h}>{h}</th>)}</tr></thead><tbody>{report.marks.map(m=><tr key={m.id}><td>{m.course}</td><td>{m.competency}</td><td>{m.assessment}</td><td><strong className="school-grade">{value(m)}</strong></td><td>{localDate(m.recordedAt)}</td></tr>)}</tbody></table></div>{!report.marks.length&&<p className="school-empty-state">Aún no hay calificaciones para este curso y periodo.</p>}</div>}
      </>}
      {tab==='attendance'&&year&&<>
        <div className="user-panel school-form-grid"><label>Desde<input type="date" min={`${yearId}-01-01`} max={`${yearId}-12-31`} value={from} onChange={e=>setFrom(e.target.value)}/></label><label>Hasta<input type="date" min={from} max={`${yearId}-12-31`} value={to} onChange={e=>setTo(e.target.value)}/></label></div>
        {attendance&&<><div className="school-stat-grid">{Object.entries(attendanceLabels).map(([k,label])=><article className="user-panel" key={k}><span>{label}</span><strong>{attendance.filter(a=>a.status===k).length}</strong></article>)}</div><div className="user-panel"><h2>Registro diario</h2><p>Solo se incluyen días registrados. Un día sin registro no se cuenta como falta.</p><div className="school-data-table"><table><thead><tr><th>Fecha</th><th>Asistencia</th><th>Observación</th></tr></thead><tbody>{attendance.map(a=><tr key={a.date}><td>{a.date}</td><td><span className={`school-pill attendance-${a.status}`}>{attendanceLabels[a.status]}</span></td><td>{a.note||'—'}</td></tr>)}</tbody></table></div>{!attendance.length&&<p>No hay asistencias registradas para estas fechas.</p>}</div></>}
      </>}
      {tab==='publications'&&publications&&<PublicationList items={publications} onRead={read}/>}
    </>}
  </section>;
}
