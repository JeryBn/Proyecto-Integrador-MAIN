import {useState} from 'react';
import {userApi,enc} from './user-api.mjs';
import {useResource,Feedback,useConfirmation} from './user-hooks.jsx';
import {ReminderSettings} from './reminder-settings.jsx';
import {AppealHistory} from './appeals.jsx';
import {noticeLabel,noticeTarget} from './notice-model.mjs';

export function Profile({profile,preferences,onPreferences,active,onChat}){
  const {confirm,confirmation}=useConfirmation();
  const contacts=useResource('/api/v1/contacts',{enabled:active,interval:5000});
  const [error,setError]=useState(''),[notice,setNotice]=useState(''),[busy,setBusy]=useState(false),[search,setSearch]=useState(''),[blockedOnly,setBlockedOnly]=useState(false),[report,setReport]=useState(null),[reason,setReason]=useState('');
  async function save(action,message){setBusy(true);setError('');setNotice('');try{await action();setNotice(message);contacts.reload();}catch(e){setError(e.message);}finally{setBusy(false);}}
  return <section className="user-panel" aria-label="Perfil y privacidad">{confirmation}<h1>Perfil y ajustes</h1>
    <div className="profile-heading"><span className="avatar">{profile.nombre?.slice(0,1)}</span><div><h2>{profile.nombre}</h2><p>{profile.rol} · {profile.usuario}</p></div></div>
    {error&&<p role="alert">{error}</p>}{notice&&<p role="status">{notice}</p>}
    <ReminderSettings active={active&&profile.rol==='Alumno'}/>
    <AppealHistory active={active}/>
    <h2>Ajustes</h2>{preferences?<fieldset disabled={busy} className="settings-list"><legend>Preferencias de tu cuenta</legend>
      <label><input type="checkbox" checked={preferences.darkTheme} onChange={e=>save(()=>onPreferences({...preferences,darkTheme:e.target.checked}),'Tema guardado.')}/> Tema oscuro</label>
      <label><input type="checkbox" checked={preferences.notifications} onChange={e=>save(()=>onPreferences({...preferences,notifications:e.target.checked}),'Preferencia de avisos guardada.')}/> Avisos dentro de MAIN</label>
      <label>Privacidad de estados<select value={preferences.statusAudience} onChange={e=>save(()=>onPreferences({...preferences,statusAudience:e.target.value}),'Privacidad guardada.')}><option value="CONTACTS">Mis contactos autorizados</option><option value="PRIVATE">Solo yo</option></select></label>
      <p>Esta preferencia limita quién puede ver tus estados. Los bloqueos y la audiencia se comprueban también en el servidor.</p>
    </fieldset>:<p role="status">No se cargaron tus preferencias. Usa Actualizar cuenta para reintentar.</p>}
    <h2>Contactos y privacidad</h2><div className="toolbar"><label>Buscar contacto<input type="search" value={search} onChange={e=>setSearch(e.target.value)}/></label><label><input type="checkbox" checked={blockedOnly} onChange={e=>setBlockedOnly(e.target.checked)}/> Solo bloqueados</label><button onClick={contacts.reload}>Actualizar contactos</button></div>
    <Feedback resource={contacts}/>{contacts.data&&<><p>Solo aparecen relaciones autorizadas por tu institución. No puedes otorgarte contactos ni parentescos.</p>
      <ul className="contact-list">{contacts.data.filter(c=>(!blockedOnly||c.blocked)&&(c.name+' '+c.username).toLocaleLowerCase('es').includes(search.toLocaleLowerCase('es'))).map(c=><li key={c.username}><span className="avatar small">{c.name.slice(0,1)}</span><div className="grow"><strong>{c.name}</strong><small>{c.role} · {c.username}{c.blocked?' · Bloqueado':''}</small></div><div className="row-actions">
        {onChat&&<button disabled={busy||c.blocked} onClick={()=>save(async()=>{const chat=await userApi.call('/api/v1/conversations/direct',{method:'POST',json:{username:c.username}});onChat(chat.id);},'')}>Mensaje a {c.name}</button>}
        <button disabled={busy} onClick={()=>save(()=>userApi.call('/api/v1/contacts/'+enc(c.username)+'/preferences',{method:'PUT',json:{muted:!c.muted,blocked:c.blocked}}),c.muted?'Contacto con avisos.':'Contacto silenciado.')}>{c.muted?'Activar avisos':'Silenciar'} {c.name}</button>
        <button disabled={busy} onClick={()=>{const action=()=>save(()=>userApi.call('/api/v1/contacts/'+enc(c.username)+'/preferences',{method:'PUT',json:{muted:c.muted,blocked:!c.blocked}}),c.blocked?'Contacto desbloqueado.':'Contacto bloqueado.');if(c.blocked)action();else confirm('¿Bloquear a '+c.name+'? Se impedirá el intercambio privado.',action);}}>{c.blocked?'Desbloquear':'Bloquear'} {c.name}</button>
        <button disabled={busy} onClick={()=>{setReport(c);setReason('');}}>Reportar a {c.name}</button>
      </div></li>)}</ul>{!contacts.data.length&&<p>No tienes contactos autorizados.</p>}</>}
    {report&&<form className="card-form" onSubmit={e=>{e.preventDefault();save(async()=>{await userApi.call('/api/v1/contacts/'+enc(report.username)+'/reports',{method:'POST',json:{reason}});setReport(null);setReason('');},'Reporte enviado a administración.');}}><h3>Reportar a {report.name}</h3><label>Motivo del reporte<textarea required maxLength={1000} value={reason} onChange={e=>setReason(e.target.value)}/></label><p>El motivo se comparte únicamente con administración.</p><button disabled={busy||!reason.trim()}>Enviar reporte</button><button type="button" disabled={busy} onClick={()=>setReport(null)}>Cancelar reporte</button></form>}
  </section>;
}

export function Notifications({active,onChat,onUpdated}){
  const [offset,setOffset]=useState(0),[busy,setBusy]=useState(false),[error,setError]=useState('');
  const notices=useResource(`/api/v1/notifications?offset=${offset}&limit=30`,{enabled:active,interval:5000});
  async function mark(n){setBusy(true);setError('');try{await userApi.call('/api/v1/notifications/'+enc(n.id)+'/read',{method:'POST',json:{}});notices.reload();onUpdated();}catch(e){setError(e.message);}finally{setBusy(false);}}
  return <section className="user-panel" aria-label="Avisos"><h1>Avisos</h1><button onClick={notices.reload}>Actualizar avisos</button><Feedback resource={notices}/>{error&&<p role="alert">{error}</p>}
    {notices.data?.length===0&&<p>No hay avisos visibles. Los mensajes pendientes de revisión no generan avisos de entrega.</p>}
    <ul className="contact-list">{notices.data?.map(n=><li key={n.id}><div className="grow"><strong>{noticeLabel(n)}</strong><small>{n.read?'Leído':'Sin leer'}</small></div>{noticeTarget(n)&&<a href={noticeTarget(n)}>{n.kind==='REMINDER'?'Ver actividad':'Abrir conversación'}</a>}{!n.read&&<button disabled={busy} onClick={()=>mark(n)}>Marcar leído</button>}</li>)}</ul>
    <div className="pager"><button disabled={offset===0} onClick={()=>setOffset(Math.max(0,offset-30))}>Avisos anteriores</button><span>Página {offset/30+1}</span><button disabled={notices.data?.length!==30} onClick={()=>setOffset(offset+30)}>Más avisos</button></div>
  </section>;
}
