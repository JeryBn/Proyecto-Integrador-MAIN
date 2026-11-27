import {ReportCards} from './alumno/academico/report-cards.jsx';
import {AttendanceJustifications} from './alumno/academico/attendance-justifications.jsx';
import {Icon} from './ui-icons.jsx';
import {useEffect,useState} from 'react';
import {useResource} from './alumno/datos/user-hooks.jsx';
import {localDate,userApi} from './alumno/datos/user-api.mjs';
import {nextClass,progressPercent,recentNotices} from './dashboard-model.mjs';
import {noticeLabel,noticeTarget} from './alumno/comunicacion/notice-model.mjs';

const days=['','Lunes','Martes','Miércoles','Jueves','Viernes','Sábado','Domingo'];

function Greeting({profile}){
 const hour=new Date().getHours();
 const greeting=hour<12?'Buenos días':hour<19?'Buenas tardes':'Buenas noches';
 const firstName=profile?.nombre?.split(' ')[0]||'Hola';
 return <div><span className="eyebrow">{greeting}, {firstName}</span><h1>Hoy en mi colegio</h1></div>;
}

function StatCard({icon,label,value,detail,tone=''}){
 return <article className={'metric-card '+tone}><span className="metric-icon"><Icon name={icon}/></span><div><span>{label}</span><strong>{value}</strong><small>{detail}</small></div></article>;
}

function TaskRow({item}){
 return <a className="dashboard-task" href={'#actividad/'+item.id}>
  <span className={'task-state '+(item.vencida?'late':'')}><Icon name={item.vencida?'alert':'clipboard'} size={18}/></span>
  <span className="grow"><small>{item.curso}</small><strong>{item.titulo}</strong><span>{localDate(item.fechaLimite)}</span></span>
  <span className={'badge '+(item.vencida?'late':'')}>{item.vencida?'Vencida':'Próxima'}</span><Icon name="arrowRight" size={17}/>
 </a>;
}

function CourseRow({course,index}){
 return <a href="#cursos" className="course-mini-card" style={{'--course-index':index%4}}>
  <span className="course-symbol"><Icon name="book" size={20}/></span>
  <span><strong>{course.name}</strong><small>{course.teacher}</small></span><Icon name="arrowRight" size={17}/>
 </a>;
}

function useTeacherOverview(courses,enabled){
 const [state,setState]=useState({loading:false,groups:[],error:''});
 const courseKey=courses.map(course=>course.id).join(',');
 useEffect(()=>{let active=true;if(!enabled||!courseKey){setState({loading:false,groups:[],error:''});return()=>{active=false};}setState(current=>({...current,loading:true,error:''}));Promise.all(courses.map(course=>userApi.call(`/api/v1/teaching/courses/${course.id}/grades`).then(students=>({course,students})))).then(groups=>{if(active)setState({loading:false,groups,error:''});}).catch(reason=>{if(active)setState({loading:false,groups:[],error:reason.message});});return()=>{active=false};},[enabled,courseKey]);
 return state;
}

function TeacherCourseRow({group}){
 const evaluations=group.students.reduce((total,student)=>total+student.grades.length,0);
 const withoutGrades=group.students.filter(student=>!student.grades.length).length;
 return <a className="dashboard-task" href="#cursos"><span className={'task-state '+(withoutGrades?'late':'')}><Icon name={withoutGrades?'alert':'graduation'} size={18}/></span><span className="grow"><small>CURSO DOCENTE</small><strong>{group.course.name}</strong><span>{group.students.length} estudiantes · {evaluations} evaluaciones</span></span><span className={'badge '+(withoutGrades?'late':'')}>{withoutGrades?`${withoutGrades} sin nota`:'Al día'}</span><Icon name="arrowRight" size={17}/></a>;
}

function FamilyDashboard({profile}){
 const summary=useResource('/api/v1/family/weekly');
 return <section className="institutional-dashboard" aria-label="Inicio familiar">
  <header className="dashboard-hero"><Greeting profile={profile}/><div className="hero-actions">
   <a className="primary-action" href="#cursos"><Icon name="book"/>Ver horarios</a>
   <a className="secondary-action" href="#avisos"><Icon name="bell"/>Avisos</a>
  </div></header>
  {summary.loading&&<p role="status">Preparando el resumen familiar…</p>}
  {summary.error&&<div className="inline-error" role="alert"><p>{summary.error}</p><button onClick={summary.reload}>Reintentar</button></div>}
  {summary.data?.students.length===0&&<section className="dashboard-card"><h2>Estudiantes vinculados</h2><p>Solicita a tu colegio que verifique el vínculo familiar.</p></section>}
  {summary.data?.students.map(student=><article className="dashboard-card" key={student.id}>
   <header><div><h2>{student.name}</h2><p>Asistencia de los últimos 7 días</p></div></header>
   <div className="metrics-grid">
    <StatCard icon="checkCircle" label="Presentes" value={student.attendance.present}/>
    <StatCard icon="alert" label="Ausencias" value={student.attendance.absent}/>
    <StatCard icon="clock" label="Tardanzas" value={student.attendance.late}/>
   </div>
   <h3>Próximas entregas y pendientes</h3>
   {student.tasks.length?<ul>{student.tasks.map((task,index)=><li key={task.course+':'+task.title+':'+index}>
    <strong>{task.title}</strong> · {task.course} · {localDate(task.dueAt)}{task.overdue?' · Vencida':''}
   </li>)}</ul>:<p>Sin entregas pendientes para los próximos días.</p>}
  </article>)}
  <AttendanceJustifications/>
  <ReportCards/>
 </section>;
}

export function Dashboard(props){
 return props.profile?.rol==='Familiar'?<FamilyDashboard profile={props.profile}/>:<AcademicDashboard {...props}/>;
}

function AcademicDashboard({profile,items,loading,error,onReload,notices=[]}){
 const agenda=useResource('/api/v1/agenda',{enabled:!!profile,interval:0});
 const grades=useResource('/api/v1/grades',{enabled:profile?.rol==='Alumno',interval:0});
 const pending=items.filter(item=>!item.completada);
 const ordered=[...pending].sort((a,b)=>Date.parse(a.fechaLimite)-Date.parse(b.fechaLimite));
 const next=nextClass(agenda.data?.timetable||[]);
 const gradeProgress=progressPercent(grades.data||[]);
 const latest=recentNotices(notices);
 const courses=agenda.data?.courses||[];
 const teacher=profile?.rol==='Docente';
 const teacherOverview=useTeacherOverview(courses,teacher);
 const teacherStudents=new Set(teacherOverview.groups.flatMap(group=>group.students.map(student=>student.studentId))).size;
 const teacherGrades=teacherOverview.groups.reduce((total,group)=>total+group.students.reduce((sum,student)=>sum+student.grades.length,0),0);
 const teacherWithoutGrades=teacherOverview.groups.reduce((total,group)=>total+group.students.filter(student=>!student.grades.length).length,0);
 return <section className="institutional-dashboard" aria-label="Inicio académico">
  <header className="dashboard-hero">
   <Greeting profile={profile}/>
   <div className="hero-actions"><a className="primary-action" href={profile?.rol==='Alumno'?'#actividades':'#cursos'}><Icon name={teacher?'graduation':'clipboard'}/>{teacher?'Gestionar cursos':'Ver mi jornada'}</a>{profile?.rol!=='Familiar'&&<a className="secondary-action" href="#campus"><Icon name="graduation"/>Abrir campus</a>}</div>
  </header>

  <section className="metrics-grid" aria-label="Resumen de tu cuenta">
   {teacher?<><StatCard icon="book" label="Cursos activos" value={agenda.loading||agenda.error?'—':courses.length} detail="Asignados a tu cuenta docente"/><StatCard icon="users" label="Estudiantes" value={teacherOverview.loading||teacherOverview.error?'—':teacherStudents} detail="Matrículas autorizadas"/><StatCard icon="chart" label="Evaluaciones" value={teacherOverview.loading||teacherOverview.error?'—':teacherGrades} detail={teacherWithoutGrades?`${teacherWithoutGrades} matrículas aún sin nota`:'Seguimiento al día'} tone={teacherWithoutGrades?'attention':''}/></>:<><StatCard icon="clipboard" label="Actividades pendientes" value={loading?'—':pending.length} detail={pending.some(item=>item.vencida)?`${pending.filter(item=>item.vencida).length} requieren atención`:'Todo dentro del plazo'} tone={pending.some(item=>item.vencida)?'attention':''}/><StatCard icon="calendar" label="Próxima clase" value={next?String(next.startsAt).slice(0,5):'—'} detail={next?`${days[next.dayOfWeek]} · ${next.course}`:'Sin horario próximo'}/><StatCard icon="chart" label="Rendimiento" value={gradeProgress===null?'—':gradeProgress+'%'} detail={gradeProgress===null?'Aún no hay evaluaciones':'Promedio normalizado registrado'}/></>}
   <StatCard icon="bell" label="Avisos nuevos" value={notices.filter(item=>!item.read).length} detail="Información de tu comunidad"/>
  </section>

  {profile?.rol==='Alumno'&&<details className="dashboard-card"><summary>Justificar una falta o tardanza</summary><AttendanceJustifications/></details>}
  {profile?.rol==='Alumno'&&<ReportCards/>}
  <div className="dashboard-layout">
   <section className="dashboard-card dashboard-priority">
    <header><div><span className="section-kicker">PRIORIDAD</span><h2>{teacher?'Seguimiento de tus cursos':'Lo siguiente en tu agenda'}</h2></div><a href={teacher?'#cursos':'#actividades'}>Ver todo <Icon name="arrowRight" size={16}/></a></header>
    {teacher?teacherOverview.loading?<div className="dashboard-loading" role="status">Preparando el seguimiento docente…</div>:teacherOverview.error?<div className="inline-error" role="alert"><p>{teacherOverview.error}</p><a href="#cursos">Abrir cursos</a></div>:teacherOverview.groups.length?teacherOverview.groups.slice(0,4).map(group=><TeacherCourseRow key={group.course.id} group={group}/>):<div className="dashboard-empty"><span><Icon name="book" size={28}/></span><div><strong>Sin cursos asignados</strong><p>Administración todavía no vinculó cursos a tu cuenta.</p></div></div>:<>{loading&&<div className="dashboard-loading" role="status">Organizando tus actividades…</div>}{error&&<div className="inline-error" role="alert"><p>{error}</p><button onClick={onReload}>Reintentar</button></div>}{!loading&&!error&&!ordered.length&&<div className="dashboard-empty"><span><Icon name="checkCircle" size={28}/></span><div><strong>Estás al día</strong><p>No tienes actividades pendientes en este momento.</p></div></div>}{!loading&&!error&&ordered.slice(0,4).map(item=><TaskRow key={item.id} item={item}/>)}</>}
   </section>

   <aside className="dashboard-side-stack">
    <section className="dashboard-card next-class-card">
     <header><div><span className="section-kicker">HORARIO</span><h2>Tu próxima clase</h2></div><span className="round-icon"><Icon name="clock"/></span></header>
     {agenda.loading?<p role="status">Consultando horario…</p>:next?<><strong className="class-time">{String(next.startsAt).slice(0,5)}</strong><h3>{next.course}</h3><p>{days[next.dayOfWeek]} · {next.classroom||'Aula por confirmar'}</p><a href={teacher&&next.courseId?'#cursos/'+next.courseId+'/asistencia':'#cursos'}>{teacher&&next.courseId?'Pasar lista':'Abrir horario'} <Icon name="arrowRight" size={16}/></a></>:<div className="compact-empty"><p>No hay clases próximas registradas.</p></div>}
    </section>
    <section className="dashboard-card quick-card">
     <span className="section-kicker">ACCESOS RÁPIDOS</span><h2>¿Qué necesitas hacer?</h2>
     <div className="quick-grid"><a href="#cursos"><Icon name="book"/><span>Cursos</span></a><a href="#chats"><Icon name="message"/><span>Mensajes</span></a><a href="#estados"><Icon name="status"/><span>Novedades</span></a><a href="#perfil"><Icon name="user"/><span>Mi cuenta</span></a></div>
    </section>
   </aside>

   <section className="dashboard-card dashboard-courses">
    <header><div><span className="section-kicker">APRENDIZAJE</span><h2>Mis cursos</h2></div><a href="#cursos">Explorar cursos <Icon name="arrowRight" size={16}/></a></header>
    {agenda.error&&<p role="alert">{agenda.error}</p>}
    {agenda.loading?<p role="status">Cargando tus cursos…</p>:<div className="course-mini-grid">{courses.slice(0,4).map((course,index)=><CourseRow key={course.id} course={course} index={index}/>)}</div>}
   </section>

   <section className="dashboard-card dashboard-notices">
    <header><div><span className="section-kicker">COMUNIDAD</span><h2>Avisos recientes</h2></div><a href="#avisos">Ver bandeja <Icon name="arrowRight" size={16}/></a></header>
    {!latest.length?<div className="compact-empty"><p>No tienes avisos recientes.</p></div>:<ul>{latest.map(item=><li key={item.id}><a href={noticeTarget(item)||'#avisos'}><span className="notice-dot" data-read={item.read}/><span><strong>{noticeLabel(item)}</strong><small>{item.createdAt?localDate(item.createdAt):'Aviso de COLEGIA MAIN'}</small></span><Icon name="arrowRight" size={16}/></a></li>)}</ul>}
   </section>
  </div>
 </section>;
}
