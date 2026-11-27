import {useEffect,useState} from 'react';
import {userApi} from '@main/alumno/datos/user-api.mjs';

const days=['','Lunes','Martes','Miércoles','Jueves','Viernes','Sábado','Domingo'];
const date=value=>new Intl.DateTimeFormat('es-PE',{dateStyle:'medium',timeStyle:'short',timeZone:'America/Lima'}).format(new Date(value+'-05:00'));
export function StudentAgenda({items}){
 const [agenda,setAgenda]=useState(null),[error,setError]=useState('');
 useEffect(()=>{let active=true;userApi.call('/api/v1/agenda').then(a=>active&&setAgenda(a)).catch(e=>active&&setError(e.message));return()=>{active=false;};},[]);
 return <section className="model-page"><header><h1>Horarios y Agenda</h1><p>Organiza tu semana y consulta las fechas de tus actividades.</p></header>{error&&<p role="alert">{error}</p>}
 <section className="model-card"><h2>Mi horario semanal</h2>{!agenda&&!error?<p role="status">Cargando horario…</p>:agenda?.timetable.length?<div className="model-week">{days.slice(1).map((day,i)=><section key={day}><h3>{day}</h3>{agenda.timetable.filter(s=>s.dayOfWeek===i+1).map(s=><article key={s.id}><strong>{s.startsAt} – {s.endsAt}</strong><p>{s.course}</p><small>{s.classroom}</small></article>)}</section>)}</div>:<p>No hay horarios registrados. El colegio debe asignar las clases de tus cursos.</p>}</section>
 <section className="model-card"><h2>Actividades por fecha</h2>{items.length?<div className="model-agenda">{[...items].sort((a,b)=>a.fechaLimite.localeCompare(b.fechaLimite)).map(a=><a key={a.id} href={'#actividad/'+a.id}><time>{date(a.fechaLimite)}</time><div><strong>{a.titulo}</strong><p>{a.curso}</p></div><span>{a.vencida?'Vencida':'Pendiente'} →</span></a>)}</div>:<p>No tienes actividades pendientes.</p>}</section></section>;
}
export function StudentReports(){
 const [courses,setCourses]=useState(null),[error,setError]=useState('');
 useEffect(()=>{let active=true;userApi.call('/api/v1/grades').then(g=>active&&setCourses(g)).catch(e=>active&&setError(e.message));return()=>{active=false;};},[]);
 const count=courses?.reduce((n,c)=>n+c.grades.length,0)||0;
 return <section className="model-page"><header><h1>Reportes &amp; BI</h1><p>Una vista de las evaluaciones numéricas que puedes consultar en tu cuenta.</p></header>{error&&<p role="alert">{error}</p>}
 <div className="model-stat-row"><article className="model-card"><span>Cursos</span><strong>{courses?.length??'—'}</strong></article><article className="model-card"><span>Evaluaciones registradas</span><strong>{courses?count:'—'}</strong></article></div>
 <section className="model-card"><h2>Rendimiento por curso</h2><p>Cada evaluación se convierte a porcentaje y tiene el mismo peso. Esta referencia no reemplaza la libreta ni determina la promoción escolar.</p>{courses?courses.length?courses.map(c=>{const grades=c.grades.filter(g=>g.maximum>0),average=grades.length?grades.reduce((n,g)=>n+100*g.score/g.maximum,0)/grades.length:null;return <div className="model-grade-row" key={c.courseId}><div><strong>{c.course}</strong><span>{average===null?'Sin notas':average.toFixed(1)+'%'}</span></div>{average!==null?<meter min="0" max="100" value={average} aria-label={'Promedio orientativo de '+c.course}>{average.toFixed(1)}%</meter>:<p>Aún no hay evaluaciones numéricas.</p>}</div>}):<p>No tienes cursos autorizados.</p>:!error&&<p role="status">Cargando evaluaciones…</p>}</section><a className="model-text-link" href="#cursos">Consultar calificaciones por competencia →</a></section>;
}
