import {Icon} from './ui-icons.jsx';
import {useResource} from './user-hooks.jsx';
import {localDate} from './user-api.mjs';
import {nextClass,progressPercent,recentNotices} from './dashboard-model.mjs';
import {noticeLabel,noticeTarget} from './notice-model.mjs';

const days=['','Lunes','Martes','Miércoles','Jueves','Viernes','Sábado','Domingo'];

function Greeting({profile}){
 const hour=new Date().getHours();
 const greeting=hour<12?'Buenos días':hour<19?'Buenas tardes':'Buenas noches';
 const firstName=profile?.nombre?.split(' ')[0]||'Hola';
 const copy=profile?.rol==='Docente'?'Revisa tus cursos y acompaña el avance de tus estudiantes.':profile?.rol==='Familiar'?'Acompaña el progreso académico desde un solo lugar.':'Tu jornada, tus entregas y tu comunidad están organizadas aquí.';
 return <div><span className="eyebrow">CENTRO DE ACTIVIDAD</span><h1>{greeting}, {firstName}</h1><p>{copy}</p></div>;
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

export function Dashboard({profile,items,loading,error,onReload,notices=[]}){
 const agenda=useResource('/api/v1/agenda',{enabled:!!profile,interval:0});
 const grades=useResource('/api/v1/grades',{enabled:profile?.rol==='Alumno',interval:0});
 const ordered=[...items].sort((a,b)=>Date.parse(a.fechaLimite)-Date.parse(b.fechaLimite));
 const next=nextClass(agenda.data?.timetable||[]);
 const gradeProgress=progressPercent(grades.data||[]);
 const latest=recentNotices(notices);
 const courses=agenda.data?.courses||[];
 return <section className="institutional-dashboard" aria-label="Inicio académico">
  <header className="dashboard-hero">
   <Greeting profile={profile}/>
   <div className="hero-actions"><a className="primary-action" href={profile?.rol==='Alumno'?'#actividades':'#cursos'}><Icon name="clipboard"/>Ver mi jornada</a><a className="secondary-action" href="#ia"><Icon name="spark"/>Preguntar a MAIN IA</a></div>
  </header>

  <section className="metrics-grid" aria-label="Resumen de tu cuenta">
   <StatCard icon="clipboard" label="Actividades pendientes" value={loading?'—':items.length} detail={items.some(item=>item.vencida)?`${items.filter(item=>item.vencida).length} requieren atención`:'Todo dentro del plazo'} tone={items.some(item=>item.vencida)?'attention':''}/>
   <StatCard icon="calendar" label="Próxima clase" value={next?String(next.startsAt).slice(0,5):'—'} detail={next?`${days[next.dayOfWeek]} · ${next.course}`:'Sin horario próximo'}/>
   <StatCard icon="chart" label="Rendimiento" value={gradeProgress===null?'—':gradeProgress+'%'} detail={gradeProgress===null?'Aún no hay evaluaciones':'Promedio normalizado registrado'}/>
   <StatCard icon="bell" label="Avisos nuevos" value={notices.filter(item=>!item.read).length} detail="Información de tu comunidad"/>
  </section>

  <div className="dashboard-layout">
   <section className="dashboard-card dashboard-priority">
    <header><div><span className="section-kicker">PRIORIDAD</span><h2>Lo siguiente en tu agenda</h2></div><a href="#actividades">Ver todas <Icon name="arrowRight" size={16}/></a></header>
    {loading&&<div className="dashboard-loading" role="status">Organizando tus actividades…</div>}
    {error&&<div className="inline-error" role="alert"><p>{error}</p><button onClick={onReload}>Reintentar</button></div>}
    {!loading&&!error&&!ordered.length&&<div className="dashboard-empty"><span><Icon name="checkCircle" size={28}/></span><div><strong>Estás al día</strong><p>No tienes actividades pendientes en este momento.</p></div></div>}
    {!loading&&!error&&ordered.slice(0,4).map(item=><TaskRow key={item.id} item={item}/>)}
   </section>

   <aside className="dashboard-side-stack">
    <section className="dashboard-card next-class-card">
     <header><div><span className="section-kicker">HORARIO</span><h2>Tu próxima clase</h2></div><span className="round-icon"><Icon name="clock"/></span></header>
     {agenda.loading?<p role="status">Consultando horario…</p>:next?<><strong className="class-time">{String(next.startsAt).slice(0,5)}</strong><h3>{next.course}</h3><p>{days[next.dayOfWeek]} · {next.classroom||'Aula por confirmar'}</p><a href="#cursos">Abrir horario completo <Icon name="arrowRight" size={16}/></a></>:<div className="compact-empty"><p>No hay clases próximas registradas.</p></div>}
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
    {!latest.length?<div className="compact-empty"><p>No tienes avisos recientes.</p></div>:<ul>{latest.map(item=><li key={item.id}><a href={noticeTarget(item)||'#avisos'}><span className="notice-dot" data-read={item.read}/><span><strong>{noticeLabel(item)}</strong><small>{item.createdAt?localDate(item.createdAt):'Aviso de MAIN'}</small></span><Icon name="arrowRight" size={16}/></a></li>)}</ul>}
   </section>
  </div>
 </section>;
}
