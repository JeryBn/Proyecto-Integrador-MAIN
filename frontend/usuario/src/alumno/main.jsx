import React, { useState, useEffect, useRef } from 'react';
import { createRoot } from 'react-dom/client';
import './estilos/app.css';
import {Brand,Icon,RoleStrip,NavGlyph} from '../diseno/ui.jsx';
import {StitchStudent as StudentDashboard} from '../diseno/stitch-portals.jsx';
import {StudentAgenda,StudentReports} from './academico/student-overview.jsx';
import {Academic} from '@main/alumno/academico/academic.jsx';
import {Profile,Notifications} from '@main/alumno/perfil/profile.jsx';
import {userApi} from '@main/alumno/datos/user-api.mjs';
import {useResource} from '@main/alumno/datos/user-hooks.jsx';
import './estilos/workspace.css';
import '@main/alumno/estilos/workspace.css';
import '@main/alumno/estilos/campus.css';
import {CallPanel} from '@main/alumno/comunicacion/calls.jsx';
import {CampusFeed} from '@main/alumno/comunicacion/campus-feed.jsx';
import '../estilos/academic-theme.css';
import {Messages} from '@main/alumno/comunicacion/messages.jsx';
import {Statuses,emptyStatusDraft} from '@main/alumno/comunicacion/statuses.jsx';
import {Assistant,emptyAssistant} from '@main/alumno/asistente/assistant.jsx';
import {AppErrorBoundary,ConnectionStatus} from '../resilience.jsx';

const date = s => new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'America/Lima' }).format(new Date(s + '-05:00'));

async function request(url) {
  return userApi.call(url);
}

function ActivityCard({ activity: a }) {
  return (
    <article className="activity-card">
      <span className="activity-icon">↗</span>
      <div className="activity-body">
        <span className="course-label">{a.curso}</span>
        <h3>{a.titulo}</h3>
        <p className="deadline">Fecha límite: {date(a.fechaLimite)}</p>
      </div>
      <div className="activity-side">
        <span className={'badge' + (a.vencida ? ' late' : '')}>{a.vencida ? 'Vencida' : 'Pendiente'}</span>
        <button
          className="detail-button"
          aria-label={'Ver detalle de ' + a.titulo}
          onClick={() => { location.hash = 'actividad/' + a.id }}
        >
          Ver detalle →
        </button>
      </div>
    </article>
  );
}

function ActivityDetail({ id }) {
  const [activity, setActivity] = useState(null), [error, setError] = useState('');
  useEffect(() => {
    let current = true;
    setActivity(null);
    setError('');
    request('/api/actividades/' + id).then(a => { if (current) setActivity(a) }).catch(e => { if (current) setError(e.message) });
    return () => { current = false };
  }, [id]);

  const back = () => { location.hash = '' };
  if (error) return <section className="detail-panel" aria-label="Detalle de actividad"><h2>No pudimos abrir esta actividad</h2><p>{error}</p><button className="back-button" onClick={back}>Volver a mis actividades</button></section>;
  if (!activity) return <section className="detail-panel"><p role="status">Cargando detalle…</p></section>;

  const a = activity;
  return (
    <section className="detail-panel" aria-label="Detalle de actividad">
      <button className="back-button" onClick={back}>← Volver a mis actividades</button>
      <span className="eyebrow">{a.curso}</span>
      <h2>{a.titulo}</h2>
      <span className={'badge' + (a.vencida ? ' late' : '')}>{a.completada ? 'Completada' : a.vencida ? 'Vencida' : 'Pendiente'}</span>
      <div className="metadata">
        <p>Docente: {a.docente}</p>
        <p>Fecha límite: {date(a.fechaLimite)}</p>
      </div>
      <h3>Indicaciones</h3>
      <p className="instructions">{a.descripcion}</p>
      <p className="detail-note">Consulta las instrucciones antes de preparar tu entrega.</p>
    </section>
  );
}

function App() {
  const previousRoute=useRef(null);
  const [items, setItems] = useState([]), [profile, setProfile] = useState(null), [signingOut, setSigningOut] = useState(false), [sessionError, setSessionError] = useState(''), [loading, setLoading] = useState(true), [error, setError] = useState(''), [search, setSearch] = useState(''), [filter, setFilter] = useState('all'), [hash, setHash] = useState(location.hash);
  const [preferences,setPreferences]=useState(null);
  const [chatDrafts,setChatDrafts]=useState({});
  const [statusDraft,setStatusDraft]=useState(emptyStatusDraft);
  const [assistantDraft,setAssistantDraft]=useState(emptyAssistant);
  const openChat=id=>{location.hash=id?'chats/'+id:'chats';};
  const notices=useResource('/api/v1/notifications?limit=30',{enabled:!!profile,interval:5000});
  useResource('/api/perfil',{enabled:!!profile,interval:10000});
  useEffect(()=>{document.documentElement.dataset.theme=preferences?.darkTheme?'dark':'light';},[preferences?.darkTheme]);
  async function savePreferences(value){const saved=await userApi.call('/api/v1/preferences',{method:'PUT',json:value});setPreferences(saved);notices.reload();}

  const logout = async event => {
    event.preventDefault();
    if (signingOut) return;
    setSigningOut(true); setSessionError('');
    try {
      for (let attempt = 0; attempt < 2; attempt++) {
        const token = await userApi.call('/api/csrf');
        const response = await fetch('/logout', {method: 'POST', headers:{'X-MAIN-Expected-User':profile.usuario}, body: new URLSearchParams({[token.parameterName]: token.token})});
        if (response.status === 403 && attempt === 0) continue;
        if (!response.ok || !response.redirected || !new URL(response.url).pathname.endsWith('/login.html'))
          throw Error('No pudimos confirmar el cierre. Intenta nuevamente.');
        location.href = '/login.html?salida';
        return;
      }
    } catch (error) {
      setSessionError(error?.message === 'No pudimos confirmar el cierre. Intenta nuevamente.'
        ? error.message : 'No hay conexión. Intenta nuevamente.');
    } finally { setSigningOut(false); }
  };

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const currentProfile = await request('/api/perfil');
      userApi.setIdentity(currentProfile.usuario);
      setProfile(currentProfile);
      const saved=await request('/api/v1/preferences');
      setPreferences(saved);
      if (currentProfile.rol === 'Alumno') setItems(await request('/api/actividades/pendientes'));
      else setItems([]);
    } catch (error) { setError(error.message); }
    finally { setLoading(false); }
  };

  useEffect(() => {
    load();
    const change = () => setHash(location.hash);
    window.addEventListener('hashchange', change);
    return () => window.removeEventListener('hashchange', change);
  }, []);

  const courseRoute=hash.match(/^#cursos(?:\/(\d+)\/asistencia)?$/);
  const match = hash.match(/^#actividad\/(\d+)$/);
  const chatRoute=/^#chats(?:\/[A-Za-z0-9-]+)?$/.test(hash);
  const conversationId=hash.match(/^#chats\/([A-Za-z0-9-]+)$/)?.[1]||null;
  const pageTitle=match?'Detalle de actividad':chatRoute?'Mensajes':hash==='#estados'?'Comunidad':hash==='#ia'?'Asistente académico':hash==='#perfil'?'Perfil y ajustes':hash==='#avisos'?'Avisos':hash==='#cursos'?'Cursos y notas':hash==='#actividades'?'Mis actividades':hash==='#notas'?'Notas y libreta':hash==='#asistencia'?'Asistencia escolar':hash==='#publicaciones'?'Campus':hash==='#evaluaciones'?'Evaluación escolar':hash==='#agenda'?'Horarios y Agenda':hash==='#reportes'?'Reportes & BI':profile?.rol==='Familiar'?'Mi familia':profile?.rol==='Docente'?'Mi aula':'Inicio académico';
  const navigation=profile?.rol==='Familiar'
    ? [['#inicio','Mi familia'],['#notas','Notas y libreta'],['#asistencia','Asistencia'],['#chats','Mensajes'],['#perfil','Mi cuenta']]
    : profile?.rol==='Docente'
      ? [['#inicio','Mi aula'],['#evaluaciones','Evaluación escolar'],['#cursos','Rendimiento'],['#publicaciones','Campus'],['#chats','Mensajes'],['#perfil','Mi cuenta']]
      : [['#inicio','Panel General'],['#cursos','Calificaciones'],['#actividades','Aula Virtual'],['#agenda','Horarios y Agenda'],['#chats','Mensajes'],['#estados','Estados'],['#publicaciones','Campus'],['#reportes','Reportes & BI']];
  useEffect(()=>{
    document.title=pageTitle+' · COLEGIA MAIN';
    const changed=previousRoute.current!==null&&previousRoute.current!==hash;
    previousRoute.current=hash;
    if(!changed)return;
    const frame=requestAnimationFrame(()=>{document.getElementById('contenido-principal')?.focus({preventScroll:true});window.scrollTo(0,0);});
    return ()=>cancelAnimationFrame(frame);
  },[hash,pageTitle]);
  const shown = items.filter(a => (filter === 'all' || (filter === 'late' ? a.vencida : !a.vencida)) && (a.titulo + ' ' + a.curso).toLocaleLowerCase('es').includes(search.toLocaleLowerCase('es').trim()));

  return (
    <div className="app-shell modernism" data-role={profile?.rol}>
      <a className="skip-link" href="#contenido-principal" onClick={event=>{event.preventDefault();document.getElementById('contenido-principal')?.focus();}}>Saltar al contenido principal</a>
      <nav className="sidebar" aria-label="Navegación principal">
        <Brand href="#inicio"/>
        <span className="workspace-label">MENÚ PRINCIPAL</span>
        <div className="primary-menu">{navigation.map(([route,label])=>{
          const active=hash===route||(route==='#inicio'&&!hash)||(route==='#actividades'&&!!match)||(route==='#chats'&&chatRoute);
          return <a key={route} className={'nav-item '+(active?'active':'')} aria-current={active?'page':undefined} href={route}><NavGlyph route={route}/><span>{label}</span></a>;
        })}</div>
        {profile&&<details className="profile-menu"><summary className="sidebar-profile" aria-label={'Opciones de '+profile.nombre}><span className="modern-avatar">{profile.nombre.slice(0,1)}</span><span className="profile-name"><strong>{profile.nombre}</strong><small>{profile.rol==='Familiar'?'Apoderado':profile.rol}</small></span><span className="material-symbols-outlined" aria-hidden="true">more_vert</span></summary><div className="profile-options"><a href="#perfil">Mi cuenta</a><a href="#chats">Mensajes</a><a href="#estados">Comunidad</a><a href="#ia">Asistente académico</a><a href="/bienvenida.html">Conoce COLEGIA MAIN</a><form onSubmit={logout}><button disabled={signingOut}>{signingOut?'Cerrando…':'Cerrar sesión'}</button></form>{sessionError&&<p role="alert">{sessionError}</p>}</div></details>}
      </nav>
      <div className="main-area">
        <header className="topbar">
          <RoleStrip role={profile?.rol||'Alumno'}/>
          <nav aria-label="Secciones académicas"><a href="#inicio">Inicio</a> · <a href={profile?.rol==='Familiar'?'#notas':'#cursos'}>{profile?.rol==='Familiar'?'Notas y libreta':'Cursos y notas'}</a> · {profile?.rol!=='Familiar'&&<><a href="#publicaciones">Campus</a> · </>}<a href="/bienvenida.html">Conoce COLEGIA MAIN</a></nav>
          <div className="user-area"><a className="assistant-trigger" href="#ia"><Icon name="spark"/><span>Asistente COLEGIA MAIN</span><i className="stitch-spark"/></a>
            <a className="notice-button" href="#avisos" aria-label="Ver avisos"><Icon name="bell"/>{notices.data?.some(n=>!n.read)&&<i className="notice-dot"/>}</a>
            <a className="header-avatar modern-avatar" href="#perfil" aria-label={'Mi cuenta: '+(profile?.nombre||'')} title={profile?.nombre}>{profile?.nombre.slice(0,1)||'C'}</a>
          </div>
        </header>
        {profile&&<CallPanel key={profile.usuario} username={profile.usuario}/>}
        <main className="content" id="contenido-principal" tabIndex={-1} aria-label={pageTitle}>
          {!profile ? <>{loading?<p role="status">Cargando tu cuenta…</p>:<div role="alert"><p>{error}</p><button onClick={load}>Reintentar acceso</button></div>}</> : chatRoute?<Messages profile={profile} conversationId={conversationId} onOpen={openChat} drafts={chatDrafts} setDrafts={setChatDrafts}/>:hash==='#estados'?<Statuses profile={profile} preferences={preferences} draft={statusDraft} setDraft={setStatusDraft} onChat={openChat}/>:hash==='#ia'?<Assistant draft={assistantDraft} setDraft={setAssistantDraft}/>:hash==='#perfil'?<><button onClick={load}>Actualizar cuenta</button><Profile profile={profile} preferences={preferences} onPreferences={savePreferences} active onChat={openChat}/></>:hash==='#avisos'?<Notifications active onUpdated={notices.reload} onChat={openChat}/>:profile.rol==='Alumno'&&hash==='#agenda'?<StudentAgenda items={items}/>:profile.rol==='Alumno'&&hash==='#reportes'?<StudentReports/>:hash==='#publicaciones'&&profile.rol!=='Familiar'?<CampusFeed profile={profile} campusName="COLEGIA MAIN" campusColor="#087f68"/>:courseRoute||profile.rol!=='Alumno' ? <Academic key={courseRoute?.[1]||'courses'} profile={profile} attendanceCourseId={courseRoute?.[1]} /> :(!hash||hash==='#inicio')&&!match?<StudentDashboard profile={profile} items={items} loading={loading} error={error} onRefresh={load} assistantDraft={assistantDraft} setAssistantDraft={setAssistantDraft}/>: <>
          <div className="page-heading">
            <div>
              <span className="eyebrow">TU AGENDA ACADÉMICA</span>
              <h1>{hash==='#actividades'||match?'Mis actividades':'Tu espacio académico'}</h1>
              <p>Encuentra lo pendiente y prepara tu siguiente paso.</p>
            </div>
            <span className="date-tag">{profile.nombre}</span>
          </div>
          <section className="summary-grid" aria-label="Resumen">
            <article><span>Pendientes</span><strong>{loading ? '—' : items.length}</strong><small>Actividades por completar</small></article>
            <article><span>Vencidas</span><strong>{loading ? '—' : items.filter(a => a.vencida).length}</strong><small>Revisa sus indicaciones</small></article>
            <article><span>Cursos con pendientes</span><strong>{loading ? '—' : new Set(items.map(a => a.curso)).size}</strong><small>Solo tus cursos</small></article>
          </section>
          {match ? <ActivityDetail id={match[1]} /> : (
            <section className="list-panel">
              <div className="list-heading">
                <div>
                  <h2>Lo que tienes por hacer</h2>
                  <p>Ordenado por fecha límite</p>
                  <button disabled={loading} onClick={load}>Actualizar actividades</button>
                </div>
                <label className="search-label">
                  <span className="sr-only">Buscar actividades</span>
                  <input type="search" value={search} onChange={e => setSearch(e.target.value)} placeholder="Buscar por título o curso" />
                </label>
              </div>
              <div className="filter-row">
                {[['all', 'Todas'], ['late', 'Vencidas'], ['next', 'Próximas']].map(([value, text]) => (
                  <button key={value} className={'chip' + (filter === value ? ' selected' : '')} aria-pressed={filter === value} onClick={() => setFilter(value)}>{text}</button>
                ))}
              </div>
              {loading ? <p role="status">Cargando tus actividades…</p> : error ? (
                <div role="alert">
                  <p>{error}</p>
                  <button className="back-button" onClick={load}>Reintentar</button>
                </div>
              ) : (
                <>
                  <p role="status">{shown.length ? '' : items.length ? 'No hay actividades que coincidan con tu búsqueda o filtro.' : 'No tienes actividades pendientes. ¡Estás al día!'}</p>
                  <div className="activity-list">{shown.map(a => <ActivityCard key={a.id} activity={a} />)}</div>
                </>
              )}
            </section>
          )}
          {!match&&hash!=='#actividades'&&<Academic profile={profile}/>}
          <footer>COLEGIA MAIN · Información de tu cuenta según tus permisos</footer>
          </>}
        </main>
      </div>
    </div>
  );
}

export default App;
