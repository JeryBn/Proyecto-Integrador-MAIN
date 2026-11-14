import React, { useState, useEffect, useRef } from 'react';
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
import {AppErrorBoundary,ConnectionStatus} from './resilience.jsx';
import {Dashboard} from './dashboard.jsx';
import {Activities,ActivityDetail} from './activities.jsx';
import {Icon} from './ui-icons.jsx';

async function request(url) {
  return userApi.call(url);
}

function App() {
  const previousRoute=useRef(null);
  const [items, setItems] = useState([]), [profile, setProfile] = useState(null), [signingOut, setSigningOut] = useState(false), [sessionError, setSessionError] = useState(''), [loading, setLoading] = useState(true), [error, setError] = useState(''), [hash, setHash] = useState(location.hash);
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
      setPreferences(await request('/api/v1/preferences'));
      if (currentProfile.rol === 'Alumno') setItems(await request('/api/actividades'));
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
  const pageTitle=match?'Detalle de actividad':chatRoute?'Chats':hash==='#estados'?'Estados':hash==='#ia'?'Asistente académico':hash==='#perfil'?'Perfil y ajustes':hash==='#avisos'?'Avisos':hash==='#cursos'?'Cursos y notas':hash==='#actividades'?'Mis actividades':'Inicio académico';
  useEffect(()=>{
    document.title=pageTitle+' · MAIN';
    const changed=previousRoute.current!==null&&previousRoute.current!==hash;
    previousRoute.current=hash;
    if(!changed)return;
    window.scrollTo(0,0);
    const frame=requestAnimationFrame(()=>document.getElementById('contenido-principal')?.focus());
    return ()=>cancelAnimationFrame(frame);
  },[hash,pageTitle]);
  const navigation=[
    {route:'#inicio',label:'Inicio',icon:'home'},
    {route:'#cursos',label:'Cursos',icon:'book'},
    ...(profile?.rol==='Alumno'?[{route:'#actividades',label:'Actividades',icon:'clipboard'}]:[]),
    {route:'#chats',label:'Chats',icon:'message',group:'Comunidad'},
    {route:'#estados',label:'Novedades',icon:'status'},
    {route:'#ia',label:'MAIN IA',icon:'spark',group:'Herramientas'},
    {route:'#perfil',label:'Mi cuenta',icon:'user'}
  ];

  return (
    <div className="app-shell">
      <a className="skip-link" href="#contenido-principal" onClick={event=>{event.preventDefault();document.getElementById('contenido-principal')?.focus();}}>Saltar al contenido principal</a>
      <nav className="sidebar institutional-sidebar" aria-label="Navegación principal">
        <a className="brand" href="#inicio"><span className="logo">M</span><span><strong>MAIN</strong><small>Campus digital</small></span></a>
        <div className="sidebar-navigation">
        {navigation.map((item,index)=>{
          const active=hash===item.route||(item.route==='#inicio'&&(!hash||hash==='#inicio'))||(item.route==='#actividades'&&!!match)||(item.route==='#chats'&&chatRoute);
          return <React.Fragment key={item.route}>{item.group&&<span className="nav-group-label">{item.group}</span>}<a className={'nav-item '+(active?'active':'')} aria-current={active?'page':undefined} href={item.route}><Icon name={item.icon}/><span>{item.label}</span>{item.route==='#avisos'&&notices.data?.some(n=>!n.read)&&<b>{notices.data.filter(n=>!n.read).length}</b>}</a></React.Fragment>;
        })}
        </div>
        <div className="sidebar-bottom"><div className="campus-card"><span><Icon name="graduation"/></span><div><strong>Comunidad MAIN</strong><p>Aprende, participa y avanza junto a tu institución.</p></div></div></div>
      </nav>
      <div className="main-area">
        <header className="topbar institutional-topbar">
          <div className="topbar-context"><span className="mobile-brand"><span className="logo">M</span>MAIN</span><span className="breadcrumb"><Icon name={hash==='#cursos'?'book':hash==='#actividades'||match?'clipboard':chatRoute?'message':hash==='#estados'?'status':hash==='#ia'?'spark':hash==='#perfil'?'user':'home'} size={17}/>{pageTitle}</span></div>
          <div className="topbar-actions">
            <a className="topbar-search" href="#actividades"><Icon name="search" size={18}/><span>Buscar en MAIN</span></a>
            <a className="notice-button icon-notice" href="#avisos" aria-label="Ver avisos"><Icon name="bell" size={19}/>{notices.data?.some(n=>!n.read)&&<b>{notices.data.filter(n=>!n.read).length}{notices.data.length===30?'+':''}</b>}</a>
            <a className="user-identity" href="#perfil"><span className="avatar small">{profile?.nombre?.slice(0,1)||'M'}</span><span><strong>{profile?.nombre||'Mi cuenta'}</strong><small>{profile?.rol||'MAIN'}</small></span></a>
          <div className="user-area">
            <form onSubmit={logout}>
              <button className="text-button logout-button" type="submit" disabled={signingOut} aria-label="Cerrar sesión"><Icon name="logout" size={18}/><span>{signingOut?'Cerrando…':'Salir'}</span></button>
              {sessionError && <p role="alert">{sessionError}</p>}
            </form>
          </div>
          </div>
        </header>
        <main className="content" id="contenido-principal" tabIndex={-1} aria-label={pageTitle}>
          {!profile ? <>{loading?<p role="status">Cargando tu cuenta…</p>:<div role="alert"><p>{error}</p><button onClick={load}>Reintentar acceso</button></div>}</> : chatRoute?<Messages profile={profile} conversationId={conversationId} onOpen={openChat} drafts={chatDrafts} setDrafts={setChatDrafts}/>:hash==='#estados'?<Statuses profile={profile} preferences={preferences} draft={statusDraft} setDraft={setStatusDraft} onChat={openChat}/>:hash==='#ia'?<Assistant draft={assistantDraft} setDraft={setAssistantDraft}/>:hash==='#perfil'?<Profile profile={profile} preferences={preferences} onPreferences={savePreferences} active onChat={openChat}/>:hash==='#avisos'?<Notifications active onUpdated={notices.reload} onChat={openChat}/>:hash==='#cursos'?<Academic profile={profile}/>:match?<ActivityDetail id={match[1]}/>:hash==='#actividades'&&profile.rol==='Alumno'?<Activities items={items} loading={loading} error={error} onReload={load}/>:<Dashboard profile={profile} items={items} loading={loading} error={error} onReload={load} notices={notices.data||[]}/>}
        </main>
      </div>
    </div>
  );
}

createRoot(document.getElementById('root')).render(<AppErrorBoundary><ConnectionStatus/><App /></AppErrorBoundary>);
