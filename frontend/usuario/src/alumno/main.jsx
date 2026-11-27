import React, { useState, useEffect, useRef } from 'react';
import {Activities,ActivityDetail} from '@main/activities.jsx';
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

function App() {
  const previousRoute=useRef(null);
  const [items, setItems] = useState([]), [profile, setProfile] = useState(null), [signingOut, setSigningOut] = useState(false), [sessionError, setSessionError] = useState(''), [loading, setLoading] = useState(true), [error, setError] = useState(''), [hash, setHash] = useState(location.hash);
  const [pending,setPending]=useState([]);
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
      const currentProfile = await userApi.call('/api/perfil');
      userApi.setIdentity(currentProfile.usuario);
      setProfile(currentProfile);
      const saved=await userApi.call('/api/v1/preferences');
      setPreferences(saved);
      if (currentProfile.rol === 'Alumno') {
        const [all, upcoming] = await Promise.all([
          userApi.call('/api/actividades'),
          userApi.call('/api/actividades/pendientes'),
        ]);
        setItems(all);
        setPending(upcoming);
      } else {
        setItems([]);
        setPending([]);
      }
    } catch (error) { setError(error.message); }
    finally { setLoading(false); }
  };

  useEffect(() => {
    load();
    const change = () => setHash(location.hash);
    window.addEventListener('hashchange', change);
    return () => window.removeEventListener('hashchange', change);
  }, []);

  function updateActivity(activity) {
    setItems(current => current.map(item => item.id === activity.id ? activity : item));
    setPending(current => activity.completada ? current.filter(item => item.id !== activity.id) : current);
  }
  const courseRoute=hash.match(/^#cursos(?:\/(\d+)\/asistencia)?$/);
  const match = hash.match(/^#actividad\/(\d+)$/);
  const chatRoute=/^#chats(?:\/[A-Za-z0-9-]+)?$/.test(hash);
  const conversationId=hash.match(/^#chats\/([A-Za-z0-9-]+)$/)?.[1]||null;
  const pageTitle=match?'Detalle de actividad':chatRoute?'Mensajes':hash==='#estados'?'Comunidad':hash==='#ia'?'Asistente académico':hash==='#perfil'?'Perfil y ajustes':hash==='#avisos'?'Avisos':hash==='#cursos'?'Cursos y notas':hash==='#actividades'?'Mis actividades':hash==='#notas'?'Notas y libreta':hash==='#asistencia'?'Asistencia escolar':['#campus','#publicaciones'].includes(hash)?'Campus':hash==='#evaluaciones'?'Evaluación escolar':hash==='#agenda'?'Horarios y Agenda':hash==='#reportes'?'Reportes & BI':profile?.rol==='Familiar'?'Mi familia':profile?.rol==='Docente'?'Mi aula':'Inicio académico';
  const navigation=profile?.rol==='Familiar'
    ? [['#inicio','Mi familia'],['#chats','Mensajes'],['#estados','Estados'],['#perfil','Mi cuenta']]
    : profile?.rol==='Docente'
      ? [['#inicio','Mi aula'],['#publicaciones','Campus'],['#chats','Mensajes'],['#estados','Estados'],['#perfil','Mi cuenta']]
      : [['#inicio','Panel General'],['#cursos','Calificaciones'],['#actividades','Aula Virtual'],['#agenda','Horarios y Agenda'],['#chats','Mensajes'],['#estados','Estados'],['#publicaciones','Campus'],['#reportes','Reportes'],['#perfil','Mi cuenta']];
  useEffect(()=>{
    document.title=pageTitle+' · COLEGIA';
    const changed=previousRoute.current!==null&&previousRoute.current!==hash;
    previousRoute.current=hash;
    if(!changed)return;
    const frame=requestAnimationFrame(()=>{document.getElementById('contenido-principal')?.focus({preventScroll:true});window.scrollTo(0,0);});
    return ()=>cancelAnimationFrame(frame);
  },[hash,pageTitle]);

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
        {profile&&<details className="profile-menu"><summary className="sidebar-profile" aria-label={'Opciones de '+profile.nombre}><span className="modern-avatar">{profile.nombre.slice(0,1)}</span><span className="profile-name"><strong>{profile.nombre}</strong><small>{profile.rol==='Familiar'?'Apoderado':profile.rol}</small></span><span className="material-symbols-outlined" aria-hidden="true">more_vert</span></summary><div className="profile-options"><a href="#perfil">Mi cuenta</a><a href="#chats">Mensajes</a><a href="#estados">Comunidad</a><a href="#ia">Asistente académico</a><a href="/bienvenida.html">Conoce COLEGIA</a><form onSubmit={logout}><button disabled={signingOut}>{signingOut?'Cerrando…':'Cerrar sesión'}</button></form>{sessionError&&<p role="alert">{sessionError}</p>}</div></details>}
      </nav>
      <div className="main-area">
        <header className="topbar">
          <RoleStrip role={profile?.rol||'Alumno'}/>
          <nav aria-label="Secciones académicas"><a href="#inicio">Inicio</a> · <a href={profile?.rol==='Familiar'?'#notas':'#cursos'}>{profile?.rol==='Familiar'?'Notas y libreta':'Cursos y notas'}</a> · {profile?.rol!=='Familiar'&&<><a href="#publicaciones">Campus</a> · </>}<a href="/bienvenida.html">Conoce COLEGIA</a></nav>
          <div className="user-area"><a className="assistant-trigger" href="#ia"><Icon name="spark"/><span>Asistente COLEGIA</span><i className="stitch-spark"/></a>
            <a className="notice-button" href="#avisos" aria-label="Ver avisos"><Icon name="bell"/>{notices.data?.some(n=>!n.read)&&<i className="notice-dot"/>}</a>
            <a className="header-avatar modern-avatar" href="#perfil" aria-label={'Mi cuenta: '+(profile?.nombre||'')} title={profile?.nombre}>{profile?.nombre.slice(0,1)||'C'}</a>
          </div>
        </header>
        {profile&&<CallPanel key={profile.usuario} username={profile.usuario}/>}
        <main className="content" id="contenido-principal" tabIndex={-1} aria-label={pageTitle}>
          {!profile ? <>{loading?<p role="status">Cargando tu cuenta…</p>:<div role="alert"><p>{error}</p><button onClick={load}>Reintentar acceso</button></div>}</> : chatRoute?<Messages profile={profile} conversationId={conversationId} onOpen={openChat} drafts={chatDrafts} setDrafts={setChatDrafts}/>:hash==='#estados'?<Statuses profile={profile} preferences={preferences} draft={statusDraft} setDraft={setStatusDraft} onChat={openChat}/>:hash==='#ia'?<Assistant draft={assistantDraft} setDraft={setAssistantDraft}/>:hash==='#perfil'?<><button onClick={load}>Actualizar cuenta</button><Profile profile={profile} preferences={preferences} onPreferences={savePreferences} active onChat={openChat}/></>:hash==='#avisos'?<Notifications active onUpdated={notices.reload} onChat={openChat}/>:profile.rol==='Alumno'&&hash==='#agenda'?<StudentAgenda items={items}/>:profile.rol==='Alumno'&&hash==='#reportes'?<StudentReports/>:['#campus','#publicaciones'].includes(hash)&&profile.rol!=='Familiar'?<CampusFeed profile={profile} campusName="COLEGIA" campusColor="#087f68"/>:courseRoute||profile.rol!=='Alumno' ? <Academic key={courseRoute?.[1]||'courses'} profile={profile} attendanceCourseId={courseRoute?.[1]} /> :match?<ActivityDetail key={match[1]} id={match[1]} onSubmissionSaved={updateActivity}/>:hash==='#actividades'?<Activities items={items} loading={loading} error={error} onReload={load}/>:<StudentDashboard profile={profile} items={pending} loading={loading} error={error} onRefresh={load} assistantDraft={assistantDraft} setAssistantDraft={setAssistantDraft}/>}
        </main>
      </div>
    </div>
  );
}

export default App;
