import {useEffect,useRef,useState} from 'react';
import {userApi,enc,fileError,submissionKey,labels,localDate} from './user-api.mjs';
import {useResource,Feedback,useConfirmation} from './user-hooks.jsx';
import {PrivateMedia} from './private-media.jsx';
import {AppealAction} from './appeals.jsx';
import {Icon} from './ui-icons.jsx';

const emptyDraft=()=>({text:'',file:null,edit:null,attempt:null});
export function Messages({profile,conversationId,onOpen,drafts,setDrafts}){
 const [offset,setOffset]=useState(0),[search,setSearch]=useState(''),[filter,setFilter]=useState('ALL'),[picker,setPicker]=useState(false),[groupName,setGroupName]=useState('');
 const [busy,setBusy]=useState(false),[error,setError]=useState('');const lock=useRef(false);
 const [contactSearch,setContactSearch]=useState(''),[contactRole,setContactRole]=useState('ALL');
 const chats=useResource(`/api/v1/conversations?offset=${offset}&limit=30`,{interval:5000});
 const contacts=useResource('/api/v1/contacts',{interval:5000});
 async function run(action){if(lock.current)return;lock.current=true;setBusy(true);setError('');try{await action();chats.reload();}catch(e){setError(e.message);}finally{lock.current=false;setBusy(false);}}
 const visible=(chats.data||[]).filter(c=>(filter==='ALL'||c.kind===filter)&&c.title.toLocaleLowerCase('es').includes(search.toLocaleLowerCase('es')));
 return <section className={'messaging-layout '+(conversationId?'chat-open':'')} aria-label="Chats de MAIN">
  <aside className="chat-list user-panel"><div className="chat-list-header"><div><span className="section-kicker">MENSAJERÍA</span><h1>Chats</h1></div><div className="chat-header-actions"><button className="icon-button" aria-label="Actualizar chats" title="Actualizar chats" onClick={()=>{chats.reload();contacts.reload();}}><Icon name="refresh"/></button><button className="icon-button primary-icon" aria-label={picker?'Cerrar nuevo chat':'Crear nuevo chat'} title="Nuevo chat" onClick={()=>setPicker(v=>!v)}><Icon name={picker?'arrowLeft':'plus'}/></button></div></div>
   <label className="chat-search"><span className="sr-only">Buscar chats</span><span className="search-control"><Icon name="search"/><input type="search" value={search} onChange={e=>setSearch(e.target.value)} placeholder="Buscar o iniciar un chat"/></span></label>
   <div className="chat-filters">{[['ALL','Todos'],['DIRECT','Privados'],['GROUP','Grupos']].map(([value,label])=><button className={'chip '+(filter===value?'selected':'')} key={value} aria-pressed={filter===value} onClick={()=>setFilter(value)}>{label}</button>)}</div>
   <Feedback resource={chats}/>{error&&<p role="alert">{error}</p>}
   {picker&&<section className="card-form" aria-label="Nuevo chat"><h2>Estudiantes y profesores</h2><Feedback resource={contacts}/><p>Solo tus contactos autorizados pueden iniciar un chat.</p>
    <label>Buscar estudiante o profesor<input type="search" value={contactSearch} onChange={e=>setContactSearch(e.target.value)}/></label><label>Tipo de contacto<select value={contactRole} onChange={e=>setContactRole(e.target.value)}><option value="ALL">Todos</option><option value="ALUMNO">Estudiantes</option><option value="DOCENTE">Profesores</option><option value="FAMILIAR">Familiares</option></select></label>
    {(contacts.data||[]).filter(c=>!c.blocked&&(contactRole==='ALL'||c.role===contactRole)&&(c.name+' '+c.username).toLocaleLowerCase('es').includes(contactSearch.toLocaleLowerCase('es'))).map(c=><button key={c.username} disabled={busy} onClick={()=>run(async()=>{const value=await userApi.call('/api/v1/conversations/direct',{method:'POST',json:{username:c.username}});setPicker(false);onOpen(value.id);})}>Conversar con {c.name} · {c.role}</button>)}
    {contacts.data?.length===0&&<p>No tienes contactos autorizados.</p>}
    <form onSubmit={e=>{e.preventDefault();run(async()=>{const value=await userApi.call('/api/v1/conversations/groups',{method:'POST',json:{title:groupName}});setGroupName('');setPicker(false);onOpen(value.id);});}}><label>Nombre del grupo<input required maxLength={100} value={groupName} onChange={e=>setGroupName(e.target.value)} disabled={busy}/></label><button disabled={busy||!groupName.trim()}>Crear grupo</button></form>
   </section>}
   {!chats.loading&&!visible.length&&<div className="empty-state chat-empty"><span className="empty-icon"><Icon name="message" size={25}/></span><strong>{search?'Sin coincidencias':'Tus conversaciones aparecerán aquí'}</strong><p>{search?'Prueba con otro nombre.':'Pulsa el botón + para conversar con tu comunidad.'}</p></div>}
   <ul className="conversation-list">{visible.map(c=><li key={c.id}><button className={c.id===conversationId?'selected-chat':''} onClick={()=>onOpen(c.id)}><span className={'avatar small chat-avatar '+(c.kind==='GROUP'?'group':'')}>{c.kind==='GROUP'?<Icon name="users" size={19}/>:c.title.slice(0,1)}</span><span className="conversation-summary"><strong>{c.title}</strong><small>{c.state==='INVITED'?'Invitación pendiente':c.kind==='GROUP'?'Grupo de MAIN':'Conversación privada'}</small></span><Icon name="more" className="conversation-more"/></button></li>)}</ul>
   <div className="pager"><button disabled={offset===0} onClick={()=>setOffset(Math.max(0,offset-30))}>Chats anteriores</button><span>{offset/30+1}</span><button disabled={chats.data?.length!==30} onClick={()=>setOffset(offset+30)}>Más chats</button></div>
  </aside>
  {conversationId?<Conversation key={conversationId} id={conversationId} profile={profile} contacts={contacts} onOpen={onOpen} onListChanged={chats.reload} draft={drafts[conversationId]||emptyDraft()} setDraft={value=>setDrafts(old=>({...old,[conversationId]:typeof value==='function'?value(old[conversationId]||emptyDraft()):value}))}/>:<div className="user-panel chat-welcome"><div className="welcome-orbit"><span><Icon name="message" size={34}/></span></div><span className="section-kicker">COMUNIDAD MAIN</span><h2>Conecta, aprende y colabora</h2><p>Conversa con compañeros y docentes en un espacio académico seguro.</p><div className="welcome-features"><span><Icon name="paperclip"/>Comparte archivos</span><span><Icon name="users"/>Crea grupos</span><span><Icon name="shield"/>Revisión segura</span></div><small><Icon name="lock" size={14}/> Tus conversaciones respetan los permisos de tu cuenta.</small></div>}
 </section>;
}

function Conversation({id,profile,contacts,onOpen,onListChanged,draft,setDraft}){
 const [offset,setOffset]=useState(0),[manage,setManage]=useState(false),[error,setError]=useState(''),[notice,setNotice]=useState(''),[busy,setBusy]=useState(false),[progress,setProgress]=useState(0);
 const {confirm,confirmation}=useConfirmation();
 const control=useRef(null),lock=useRef(false),fileInput=useRef(null),alive=useRef(true);
 const scrollRef=useRef(null),pinned=useRef(true);
 const metadata=useResource('/api/v1/conversations/'+enc(id),{interval:5000});
 const allowed=metadata.data?.state==='ACTIVE';
 const messages=useResource(`/api/v1/conversations/${enc(id)}/messages?offset=${offset}&limit=30`,{enabled:allowed,interval:5000});
 useEffect(()=>{if(offset===0&&pinned.current&&scrollRef.current)scrollRef.current.scrollTop=scrollRef.current.scrollHeight;},[messages.data,offset]);
 const members=useResource('/api/v1/conversations/'+enc(id)+'/members',{enabled:allowed,interval:5000});
 const other=members.data?.find(m=>m.username!==profile.usuario);
 const contact=metadata.data?.kind==='DIRECT'?(contacts.data||[]).find(c=>c.username===other?.username):null;
 useEffect(()=>()=>{alive.current=false;control.current?.abort();},[]);
 const update=value=>setDraft(old=>({...old,...value}));
 function refresh(){metadata.reload();messages.reload();members.reload();contacts.reload();onListChanged();}
 async function run(action,message=''){if(lock.current)return;lock.current=true;setBusy(true);setError('');setNotice('');try{await action();if(alive.current){refresh();setNotice(message);}}catch(e){if(alive.current)setError(e.message);}finally{lock.current=false;if(alive.current)setBusy(false);}}
 async function send(){
  if(lock.current||(!draft.text.trim()&&!draft.file&&(!draft.edit||draft.edit.attachmentKind==='NONE')))return;
  const invalid=fileError(draft.file);if(invalid){setError(invalid);return;}
  const payload=JSON.stringify({id,text:draft.text,edit:draft.edit?.id,revision:draft.edit?.revision,file:draft.file?[draft.file.name,draft.file.size,draft.file.lastModified,draft.file.type]:null});
  const attempt=submissionKey(draft.attempt,payload);update({attempt});
  await run(async()=>{
   control.current=new AbortController();setProgress(0);
   if(draft.edit)await userApi.call('/api/v1/messages/'+enc(draft.edit.id),{method:'PUT',json:{expectedRevision:draft.edit.revision,text:draft.text},signal:control.current.signal});
   else if(draft.file){const form=new FormData();form.set('clientKey',attempt.key);form.set('text',draft.text);form.set('file',draft.file);await userApi.upload('/api/v1/conversations/'+enc(id)+'/messages',form,{signal:control.current.signal,onProgress:setProgress});}
   else await userApi.call('/api/v1/conversations/'+enc(id)+'/messages',{method:'POST',json:{clientKey:attempt.key,text:draft.text},signal:control.current.signal});
   setDraft(emptyDraft());if(fileInput.current)fileInput.current.value='';if(alive.current)setOffset(0);
  },'Envío guardado; pendiente de revisión antes de entregarse.');
 }
 function edit(message){const apply=()=>{setDraft({text:message.text,file:null,edit:message,attempt:null});setError('');setNotice('La edición será revisada nuevamente.');};if(draft.text||draft.file)confirm('¿Reemplazar el borrador actual para editar este mensaje?',apply);else apply();}
 return <article className="conversation-panel user-panel" aria-label="Conversación seleccionada">{confirmation}<header className="conversation-header"><button className="icon-button mobile-back" aria-label="Volver a chats" title="Volver a chats" onClick={()=>onOpen(null)}><Icon name="arrowLeft"/></button><span className={'avatar small chat-avatar '+(metadata.data?.kind==='GROUP'?'group':'')}>{metadata.data?.kind==='GROUP'?<Icon name="users" size={19}/>:metadata.data?.title?.slice(0,1)||'M'}</span><div className="grow"><h2>{metadata.data?.title||'Conversación'}</h2><p><span className="online-dot"/>{metadata.data?.kind==='GROUP'?'Grupo de MAIN':'Chat privado'} · revisión protegida</p></div><button className="icon-button" aria-label="Actualizar conversación" title="Actualizar conversación" onClick={refresh}><Icon name="refresh"/></button>{metadata.data?.kind==='GROUP'&&allowed&&<button className={'icon-button '+(manage?'active-icon':'')} aria-label="Administrar miembros del grupo" title="Miembros del grupo" onClick={()=>setManage(v=>!v)}><Icon name="users"/></button>}</header>
  <Feedback resource={metadata}/>{error&&<p role="alert">{error}</p>}{notice&&<p role="status">{notice}</p>}
  {metadata.data?.state==='INVITED'&&<section className="card-form"><h3>Invitación a {metadata.data.title}</h3><p>Solo podrás leer los mensajes publicados después de aceptar.</p><button disabled={busy} onClick={()=>run(()=>userApi.call('/api/v1/conversations/'+enc(id)+'/invitation',{method:'POST',json:{accept:true}}),'Invitación aceptada.')}>Aceptar invitación</button><button disabled={busy} onClick={()=>run(async()=>{await userApi.call('/api/v1/conversations/'+enc(id)+'/invitation',{method:'POST',json:{accept:false}});onOpen(null);},'')}>Rechazar invitación</button></section>}
  {allowed&&<><Feedback resource={messages}/>{manage&&metadata.data.kind==='GROUP'&&<section className="card-form" aria-label="Administrar grupo"><h3>Miembros del grupo</h3><Feedback resource={members}/><ul className="contact-list">{members.data?.map(m=><li key={m.username}><span className="grow"><strong>{m.name}</strong><small>{m.state==='INVITED'?'Invitado':m.administrator?'Administrador':'Miembro'}</small></span>{metadata.data.administrator&&m.state==='ACTIVE'&&m.username!==profile.usuario&&<><button disabled={busy} onClick={()=>run(()=>userApi.call(`/api/v1/conversations/${enc(id)}/members/${enc(m.username)}`,{method:'PUT',json:{administrator:!m.administrator,remove:false}}),'Rol actualizado.')}>{m.administrator?'Quitar administración a':'Hacer administrador a'} {m.name}</button><button disabled={busy} onClick={()=>{confirm('¿Retirar a '+m.name+' del grupo?',()=>run(()=>userApi.call(`/api/v1/conversations/${enc(id)}/members/${enc(m.username)}`,{method:'PUT',json:{administrator:false,remove:true}}),'Miembro retirado.'));}}>Retirar a {m.name}</button></>}</li>)}</ul>
   {metadata.data.administrator&&<><h3>Invitar contacto</h3>{contacts.data?.filter(c=>!c.blocked&&!members.data?.some(m=>m.username===c.username&&['ACTIVE','INVITED'].includes(m.state))).map(c=><button key={c.username} disabled={busy} onClick={()=>run(()=>userApi.call('/api/v1/conversations/'+enc(id)+'/invitations',{method:'POST',json:{username:c.username}}),'Invitación enviada.')}>Invitar a {c.name}</button>)}</>}
   <p>El último administrador debe designar otro antes de salir.</p><button disabled={busy} onClick={()=>{confirm('¿Salir de este grupo?',()=>run(async()=>{await userApi.call(`/api/v1/conversations/${enc(id)}/members/${enc(profile.usuario)}`,{method:'PUT',json:{administrator:false,remove:true}});onOpen(null);},''));}}>Salir del grupo</button>
  </section>}
  <div className="pager"><button disabled={messages.data?.length!==30} onClick={()=>setOffset(offset+30)}>Mensajes anteriores</button><span>Página {offset/30+1}</span><button disabled={offset===0} onClick={()=>setOffset(Math.max(0,offset-30))}>Más recientes</button></div>
  {!messages.loading&&messages.data?.length===0&&<p className="empty-state">Todavía no hay mensajes visibles. Saluda para empezar.</p>}
  <ol ref={scrollRef} onScroll={e=>{pinned.current=e.currentTarget.scrollHeight-e.currentTarget.scrollTop-e.currentTarget.clientHeight<100;}} className="message-list" aria-label="Mensajes">{messages.data?.map(m=><li key={m.id+':'+m.revision} className={'message-bubble '+(m.sender===profile.usuario?'mine':'')}><strong>{m.senderName}</strong><p className="message-text">{m.text}</p>
   {m.attachmentKind!=='NONE'&&m.state!=='CANCELLED'&&<PrivateMedia key={m.id+':'+m.revision} kind={m.attachmentKind} name={m.attachmentName} path={`/api/v1/messages/${enc(m.id)}/attachment?revision=${m.revision}`}/>}
   <small>{localDate(m.createdAt)} · {labels[m.state]||m.state}</small>{m.reason&&<p className="rejection-reason">{m.reason}</p>}
   {m.correctedBeforeDelivery&&<small>Editado antes del envío para cumplir las normas de MAIN</small>}{m.editedAfterDelivery&&<small>Editado después del envío</small>}
   {m.sender===profile.usuario&&m.state!=='CANCELLED'&&<div className="row-actions"><button disabled={busy} onClick={()=>edit(m)}>{m.state==='REJECTED'?'Corregir mensaje':'Editar mensaje'}</button>{m.state!=='APPROVED'&&<button disabled={busy} onClick={()=>{confirm('¿Cancelar esta versión pendiente o rechazada?',()=>run(()=>userApi.call('/api/v1/messages/'+enc(m.id)+'/cancel',{method:'POST',json:{expectedRevision:m.revision}}),'Versión cancelada.'));}}>Cancelar envío</button>}</div>}
   {m.sender===profile.usuario&&m.state==='REJECTED'&&m.attachmentKind!=='NONE'&&<small>Para sustituir el archivo, cancela este envío y adjunta uno nuevo.</small>}
   {m.sender===profile.usuario&&m.state==='REJECTED'&&<AppealAction kind="messages" item={m}/>}
  </li>)}</ol>
  {contact?.blocked&&<p role="alert">Has bloqueado este contacto. Puedes desbloquearlo desde Perfil.</p>}
  <form className="message-composer" onSubmit={e=>{e.preventDefault();send();}}><fieldset disabled={busy||contact?.blocked}><legend>{draft.edit?'Editar mensaje':'Escribir mensaje'}</legend>
   <label>Mensaje<textarea maxLength={4000} value={draft.text} onChange={e=>update({text:e.target.value})} placeholder="Escribe un mensaje"/></label>
   {draft.edit?<><p>El archivo existente no cambia al editar. La nueva versión será revisada.</p><button type="button" onClick={()=>setDraft(emptyDraft())}>Descartar edición</button></>:<label>Adjuntar imagen, video, audio o TXT<input ref={fileInput} type="file" accept="image/jpeg,image/png,video/*,audio/*,text/plain" onChange={e=>{const file=e.target.files?.[0]||null;const invalid=fileError(file);if(invalid){setError(invalid);e.target.value='';return;}update({file,attempt:null});setError('');}}/></label>}
   {!draft.edit&&<MediaCapture disabled={busy} onFile={file=>{update({file,attempt:null});setError('');}}/>}
   {draft.file&&<p>{draft.file.name} · {Math.ceil(draft.file.size/1024)} KB <button type="button" onClick={()=>{update({file:null,attempt:null});if(fileInput.current)fileInput.current.value='';}}>Quitar adjunto</button></p>}
   <button disabled={!draft.text.trim()&&!draft.file&&(!draft.edit||draft.edit.attachmentKind==='NONE')}>{busy?'Esperando confirmación…':draft.edit?'Guardar edición':'Enviar mensaje'}</button>
  </fieldset>{busy&&<div role="status"><p>{draft.file?(progress<100?`Subiendo archivo: ${progress}%`:'Esperando confirmación del servidor…'):'Guardando…'}</p><button type="button" onClick={()=>control.current?.abort()}>Cancelar espera</button></div>}
  <p>La revisión previa aplica a todos los emisores. Solo las versiones aprobadas llegan a los demás.</p></form></>}
 </article>;
}
import {MediaCapture} from './media-capture.jsx';
