import React,{useEffect,useState} from 'react';
import {createRoot} from 'react-dom/client';
import App from './alumno/main.jsx';
import {AppErrorBoundary,ConnectionStatus} from './resilience.jsx';
import './public-pages.css';
import landing from './portada.html?raw';
import {CommunityLogin} from './diseno/access.jsx';
import './diseno/modernism.css';
import './diseno/stitch-exact.css';
import './diseno/model-shell.css';
function Entry(){
 const [state,setState]=useState(location.pathname==='/login.html'?'login':location.pathname==='/bienvenida.html'?'landing':'loading');
 useEffect(()=>{if(state!=='loading')return;let current=true;fetch('/api/perfil',{credentials:'same-origin',cache:'no-store'}).then(r=>{if(!current)return;if(r.ok)setState('portal');else if(r.status===401||r.status===403)setState('login');else setState('error');}).catch(()=>current&&setState('error'));return()=>{current=false;};},[]);
 useEffect(()=>{document.body.classList.toggle('school-login',state==='login');document.body.classList.toggle('login-body',state==='login');if(state==='landing'){const a=document.getElementById('administration-link');if(a)a.href=import.meta.env.VITE_ADMIN_URL||new URL('http://'+location.hostname+':5174/').href;}},[state]);
 if(state==='loading')return <div className="modernism portal-loading"><p role="status">Cargando COLEGIA…</p></div>;
 if(state==='error')return <main className="modernism portal-loading"><h1>No pudimos conectar con COLEGIA</h1><p>Comprueba que el servidor esté disponible.</p><button onClick={()=>location.reload()}>Reintentar</button></main>;
 if(state==='portal')return <AppErrorBoundary><ConnectionStatus/><App/></AppErrorBoundary>;
 if(state==='login')return <CommunityLogin/>;
 return <div className="modernism public-modernism" dangerouslySetInnerHTML={{__html:landing}}/>;
}
createRoot(document.getElementById('root')).render(<Entry/>);
