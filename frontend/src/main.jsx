import React, { useState, useEffect } from 'react';
import { createRoot } from 'react-dom/client';
import './app.css';
import {Academic} from './academic.jsx';
import {Profile,Notifications} from './profile.jsx';
import {userApi} from './user-api.mjs';
import {useResource} from './user-hooks.jsx';
import './workspace.css';
import {Messages} from './messages.jsx';
import {Statuses,emptyStatusDraft} from './statuses.jsx';
import {Assistant,emptyAssistant} from './assistant.jsx';

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
      setSessionError(error.message || 'No hay conexión. Intenta nuevamente.');
    } finally { setSigningOut(false); }
  };

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const currentProfile = await request('/api/perfil');
      userApi.setIdentity(currentProfile.usuario);
      setProfile(currentProfile);
      setPreferences(await request('/api/v1/preferences'));
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

  const match = hash.match(/^#actividad\/(\d+)$/);
  const chatRoute=/^#chats(?:\/[A-Za-z0-9-]+)?$/.test(hash);
  const conversationId=hash.match(/^#chats\/([A-Za-z0-9-]+)$/)?.[1]||null;
  const shown = items.filter(a => (filter === 'all' || (filter === 'late' ? a.vencida : !a.vencida)) && (a.titulo + ' ' + a.curso).toLocaleLowerCase('es').includes(search.toLocaleLowerCase('es').trim()));

  return (
    <div className="app-shell">
      <a className="skip-link" href="#contenido-principal" onClick={event=>{event.preventDefault();document.getElementById('contenido-principal')?.focus();}}>Saltar al contenido principal</a>
      <aside className="sidebar" aria-label="Navegación principal">
        <a className="brand" href="/"><span className="logo">M</span>MAIN</a>
        <span className="workspace-label">ESPACIO ACADÉMICO</span>
        {[['#inicio','Inicio'],['#chats','Chats'],['#estados','Estados'],['#ia','IA'],['#perfil','Perfil']].map(([route,label])=><a key={route} className={'nav-item '+((hash===route||(!hash&&route==='#inicio')||(route==='#chats'&&chatRoute))?'active':'')} aria-current={hash===route?'page':undefined} href={route}>{label}</a>)}
        <div className="sidebar-bottom">
          <span className="sprint-badge">Tu comunidad académica</span>
          <p>Cursos, tareas y conversaciones<br/>con los mismos datos de tu app.</p>
        </div>
      </aside>
      <div className="main-area">
        <header className="topbar">
          <span className="breadcrumb">MAIN <span>/</span> {profile?.rol||'Mi espacio'}</span>
          <nav aria-label="Secciones académicas"><a href="#inicio">Inicio</a> · <a href="#cursos">Cursos y notas</a>{profile?.rol==='Alumno'&&<> · <a href="#actividades">Tareas</a></>}</nav>
          <div className="user-area">
            <a className="notice-button" href="#avisos" aria-label="Ver avisos">Avisos {notices.data?.some(n=>!n.read)?`(${notices.data.filter(n=>!n.read).length}${notices.data.length===30?'+':''})`:''}</a>
            <span>{profile?.nombre || 'Alumno'}</span>
            <form onSubmit={logout}>
              <button className="text-button" type="submit" disabled={signingOut}>{signingOut ? 'Cerrando…' : 'Salir ↗'}</button>
              {sessionError && <p role="alert">{sessionError}</p>}
            </form>
          </div>
        </header>
        <main className="content" id="contenido-principal" tabIndex={-1}>
          {!profile ? <>{loading?<p role="status">Cargando tu cuenta…</p>:<div role="alert"><p>{error}</p><button onClick={load}>Reintentar acceso</button></div>}</> : chatRoute?<Messages profile={profile} conversationId={conversationId} onOpen={openChat} drafts={chatDrafts} setDrafts={setChatDrafts}/>:hash==='#estados'?<Statuses profile={profile} preferences={preferences} draft={statusDraft} setDraft={setStatusDraft} onChat={openChat}/>:hash==='#ia'?<Assistant draft={assistantDraft} setDraft={setAssistantDraft}/>:hash==='#perfil'?<><button onClick={load}>Actualizar cuenta</button><Profile profile={profile} preferences={preferences} onPreferences={savePreferences} active onChat={openChat}/></>:hash==='#avisos'?<Notifications active onUpdated={notices.reload} onChat={openChat}/>:hash === '#cursos'||profile.rol!=='Alumno' ? <Academic profile={profile} /> : <>
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
          <footer>MAIN · Información de tu cuenta según tus permisos</footer>
          </>}
        </main>
      </div>
    </div>
  );
}

createRoot(document.getElementById('root')).render(<App />);
