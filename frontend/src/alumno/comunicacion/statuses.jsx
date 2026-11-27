import {useEffect,useRef,useState} from 'react';
import {userApi,enc,fileError,submissionKey,localDate} from '../datos/user-api.mjs';
import {useResource,Feedback,useConfirmation} from '../datos/user-hooks.jsx';
import {PrivateMedia} from './medios/private-media.jsx';
import {canPublish,ownerPlaylist,statusTextColor} from './status-model.mjs';
import {MediaCapture} from './medios/media-capture.jsx';
import {AppealAction} from '../apelaciones/appeals.jsx';
import {Icon} from '../../ui-icons.jsx';

export const emptyStatusDraft=()=>({text:'',background:'#087f68',audience:'CONTACTS',file:null,attempt:null});
const statusColors=['#087f68','#176b87','#5b4db7','#a8436c','#c06028','#27364a'];
export function Statuses({profile,preferences,draft,setDraft,onChat}){
 const {confirm,confirmation}=useConfirmation();
 const feed=useResource('/api/v1/statuses',{interval:5000});
 const [compose,setCompose]=useState(false),[selected,setSelected]=useState(null),[error,setError]=useState(''),[notice,setNotice]=useState(''),[busy,setBusy]=useState(false);
 const publish=canPublish(profile.rol);
 const own=(feed.data||[]).filter(s=>s.owner===profile.usuario);
 const owners=[...new Set((feed.data||[]).filter(s=>s.owner!==profile.usuario).map(s=>s.owner))];
 const current=feed.data?.find(s=>s.id===selected);
 useEffect(()=>{if(feed.data&&selected&&!feed.data.some(s=>s.id===selected))setSelected(null);},[feed.data,selected]);
 function open(status){setError('');setSelected(status.id);if(status.owner!==profile.usuario)userApi.call('/api/v1/statuses/'+enc(status.id)+'/seen',{method:'POST',json:{}}).then(feed.reload).catch(e=>{setError(e.message);setSelected(null);feed.reload();});}
 async function remove(status){if(busy)return;setBusy(true);setError('');try{await userApi.call('/api/v1/statuses/'+enc(status.id)+'/delete',{method:'POST',json:{}});setSelected(null);setNotice('Estado eliminado.');feed.reload();}catch(e){setError(e.message);}finally{setBusy(false);}}
 async function reply(status){if(busy)return;setBusy(true);setError('');try{const chat=await userApi.call('/api/v1/conversations/direct',{method:'POST',json:{username:status.owner}});onChat(chat.id);}catch(e){setError(e.message);feed.reload();}finally{setBusy(false);}}
 return <section className={'status-hub '+((current||compose)?'status-open':'')} aria-label="Estados de Colegia Main">{confirmation}
  <aside className="user-panel status-sidebar"><div className="status-sidebar-header"><div><span className="section-kicker">24 HORAS</span><h1>Estados</h1></div><div className="chat-header-actions"><button className="icon-button" aria-label="Actualizar estados" title="Actualizar estados" onClick={feed.reload}><Icon name="refresh"/></button>{publish&&<button className="icon-button primary-icon" aria-label={compose?'Cerrar creación':'Crear estado'} title="Crear estado" onClick={()=>{setCompose(v=>!v);setSelected(null);setNotice('');}}><Icon name={compose?'arrowLeft':'plus'}/></button>}</div></div>
   {error&&<p role="alert">{error}</p>}{notice&&<p role="status">{notice}</p>}<Feedback resource={feed}/>
   <section className="status-section"><h2>Mi estado</h2>{own.length?<ul className="status-list">{own.map(s=><li key={s.id}><button className={s.id===selected?'selected-status':''} onClick={()=>{setCompose(false);open(s);}}><span className={'status-ring '+(s.state==='APPROVED'?'':'pending')}><span>{profile.nombre?.slice(0,1)||'M'}</span></span><span className="grow"><strong>{s.text||'Estado multimedia'}</strong><small>{s.state==='PENDING'?'Pendiente de revisión':s.state==='REJECTED'?'Rechazado · solo visible para ti':'Aprobado'} · {localDate(s.createdAt)}</small></span></button>{s.state==='REJECTED'&&<AppealAction kind="statuses" item={s}/>}</li>)}</ul>:<button className="my-status-empty" disabled={!publish} onClick={()=>setCompose(true)}><span className="status-ring my-empty"><span>{profile.nombre?.slice(0,1)||'M'}</span><b><Icon name="plus" size={13}/></b></span><span className="grow"><strong>Mi estado</strong><small>{publish?'Comparte una actualización':'No tienes estados activos'}</small></span></button>}</section>
   <section className="status-section"><h2>Recientes</h2>{!feed.loading&&owners.length===0&&<div className="empty-state compact"><Icon name="status"/><p>No hay estados nuevos de tus contactos.</p></div>}
   <ul className="status-list">{owners.map(owner=>{const updates=ownerPlaylist(feed.data,owner),first=updates.find(s=>!s.seen)||updates[0];return <li key={owner}><button className={first.id===selected?'selected-status':''} onClick={()=>{setCompose(false);open(first);}}><span className={'status-ring '+(updates.every(s=>s.seen)?'seen':'')}><span>{first.name.slice(0,1)}</span></span><span className="grow"><strong>{first.name}</strong><small>{updates.length} {updates.length===1?'actualización':'actualizaciones'} · {updates.every(s=>s.seen)?'Vistos':'Sin ver'}</small></span></button></li>;})}</ul></section>
   {!publish&&<p className="status-policy"><Icon name="shield" size={16}/> Las cuentas familiares pueden ver estados autorizados; estudiantes y docentes pueden publicar.</p>}
  </aside>
  <article className="user-panel status-stage">
   {publish&&compose&&<StatusComposer draft={draft} setDraft={setDraft} preferences={preferences} onSaved={()=>{setCompose(false);setNotice('Estado guardado, pendiente de revisión. Aún no está publicado para tus contactos.');feed.reload();}}/>}
   {current&&<StatusViewer key={current.id} status={current} playlist={ownerPlaylist(feed.data,current.owner)} mine={current.owner===profile.usuario} onOpen={open} onClose={()=>setSelected(null)} onDelete={()=>confirm('¿Eliminar este estado? Dejará de estar disponible para tus contactos.',()=>remove(current))} onReply={onChat?()=>reply(current):undefined} busy={busy}/>}
   {!current&&!compose&&<div className="status-welcome"><div className="welcome-orbit status-orbit"><span><Icon name="status" size={34}/></span></div><span className="section-kicker">MOMENTOS DE Colegia Main</span><h2>Comparte tu día académico</h2><p>Publica texto, fotos, video o audio. Tus estados desaparecen después de 24 horas y respetan tu audiencia.</p>{publish&&<div className="status-create-actions"><button onClick={()=>setCompose(true)}><Icon name="pencil"/> Estado de texto</button><button onClick={()=>setCompose(true)}><Icon name="camera"/> Foto o video</button></div>}<small><Icon name="shield" size={15}/> Cada publicación pasa por la revisión segura de Colegia Main.</small></div>}
  </article>
 </section>;
}

function StatusComposer({draft,setDraft,preferences,onSaved}){
 const [busy,setBusy]=useState(false),[error,setError]=useState(''),[progress,setProgress]=useState(0),[preview,setPreview]=useState(null);
 const lock=useRef(false),controller=useRef(null),alive=useRef(true),input=useRef(null);
 useEffect(()=>()=>{alive.current=false;controller.current?.abort();},[]);
 useEffect(()=>{if(!draft.file){setPreview(null);return;}const url=URL.createObjectURL(draft.file);setPreview(url);return()=>URL.revokeObjectURL(url);},[draft.file]);
 const update=patch=>setDraft(old=>({...old,...patch}));
 async function send(e){e.preventDefault();if(lock.current||(!draft.text.trim()&&!draft.file))return;
  const invalid=fileError(draft.file,{status:true});if(invalid){setError(invalid);return;}
  const payload=JSON.stringify({text:draft.text,background:draft.background,audience:draft.audience,file:draft.file?[draft.file.name,draft.file.size,draft.file.type,draft.file.lastModified]:null});
  const attempt=submissionKey(draft.attempt,payload);update({attempt});lock.current=true;setBusy(true);setError('');setProgress(0);controller.current=new AbortController();
  try{const form=new FormData();form.set('clientKey',attempt.key);form.set('text',draft.text);form.set('background',draft.background);form.set('audience',draft.audience);if(draft.file)form.set('file',draft.file);
   await userApi.upload('/api/v1/statuses',form,{signal:controller.current.signal,onProgress:setProgress});setDraft(emptyStatusDraft());if(alive.current)onSaved();
  }catch(e){if(alive.current)setError(e.message);}finally{lock.current=false;if(alive.current)setBusy(false);}
 }
 return <form className="card-form status-composer modern-status-composer" aria-label="Crear estado" onSubmit={send}>
  <div className="composer-heading"><span className="composer-icon"><Icon name="spark"/></span><div><span className="section-kicker">NUEVA ACTUALIZACIÓN</span><h2>Crear estado</h2><p>Comparte un momento con tu comunidad académica.</p></div></div>
  {error&&<div className="composer-error" role="alert"><Icon name="alert" size={18}/><span>{error}</span></div>}
  <fieldset className="status-composer-fields" disabled={busy}><legend className="sr-only">Contenido y audiencia</legend>
   <div className="status-compose-grid">
    <section className="status-live-preview" style={{backgroundColor:draft.background,color:statusTextColor(draft.background)}} aria-label="Vista previa del estado">
     <header><span><Icon name="status" size={15}/>Vista previa</span><span>{draft.audience==='PRIVATE'?'Solo yo':'Contactos'}</span></header>
     <div className="status-preview-body">
      {preview&&(draft.file.type.startsWith('image/')?<img src={preview} alt="Vista previa del archivo seleccionado"/>:draft.file.type.startsWith('video/')?<video src={preview} controls preload="metadata"/>:<audio src={preview} controls preload="metadata"/>)}
      <p className={draft.text?'':'preview-placeholder'}>{draft.text||'Escribe algo para comenzar…'}</p>
     </div>
     <footer><span>Tu estado</span><span>Ahora · 24 h</span></footer>
    </section>
    <div className="status-control-panel">
     <label className="status-text-control"><span>Mensaje</span><textarea maxLength={700} placeholder="¿Qué quieres compartir hoy?" value={draft.text} onChange={e=>update({text:e.target.value,attempt:null})}/><small className={draft.text.length>630?'near-limit':''}>{draft.text.length}/700</small></label>
     <div className="composer-option" role="group" aria-label="Color del fondo"><div><strong>Estilo</strong><small>Elige un color para tu estado</small></div><div className="status-color-palette">{statusColors.map(color=><button type="button" key={color} className={draft.background.toLowerCase()===color?'selected':''} style={{backgroundColor:color}} aria-label={'Usar color '+color} aria-pressed={draft.background.toLowerCase()===color} onClick={()=>update({background:color,attempt:null})}/>)}<label className="custom-status-color" title="Elegir otro color"><Icon name="pencil" size={15}/><span className="sr-only">Elegir otro color</span><input type="color" value={draft.background} onChange={e=>update({background:e.target.value,attempt:null})}/></label></div></div>
     <div className="composer-option audience-option"><div><strong>¿Quién puede verlo?</strong><small>Controla la audiencia antes de publicar</small></div><div className="audience-buttons"><button type="button" className={draft.audience==='CONTACTS'?'selected':''} aria-pressed={draft.audience==='CONTACTS'} onClick={()=>update({audience:'CONTACTS',attempt:null})}><Icon name="users" size={17}/>Contactos</button><button type="button" className={draft.audience==='PRIVATE'?'selected':''} aria-pressed={draft.audience==='PRIVATE'} onClick={()=>update({audience:'PRIVATE',attempt:null})}><Icon name="lock" size={17}/>Solo yo</button></div></div>
    </div>
   </div>
   {preferences?.statusAudience==='PRIVATE'&&<div className="privacy-warning"><Icon name="lock" size={17}/><span>Tu privacidad global está en <strong>Solo yo</strong>. Tus contactos no verán este estado hasta que cambies ese ajuste.</span></div>}
   <section className="status-media-card"><header><span className="media-card-icon"><Icon name="paperclip"/></span><div><strong>Añade una foto, video o audio</strong><small>Opcional · máximo 12 MB y 60 segundos</small></div></header>
    <label className="status-upload-zone"><input ref={input} className="sr-only" type="file" accept="image/jpeg,image/png,video/*,audio/*" onChange={e=>{const file=e.target.files?.[0]||null,invalid=fileError(file,{status:true});if(invalid){setError(invalid);e.target.value='';return;}setError('');update({file,attempt:null});}}/><span><Icon name="file"/></span><strong>{draft.file?'Cambiar archivo':'Seleccionar desde el equipo'}</strong><small>JPG, PNG, video o audio</small></label>
    <MediaCapture disabled={busy} onFile={file=>{setError('');update({file,attempt:null});}}/>
    {draft.file&&<div className="status-selected-file"><span><Icon name="checkCircle" size={17}/></span><div><strong>{draft.file.name}</strong><small>{Math.ceil(draft.file.size/1024)} KB · listo para revisar</small></div><button type="button" onClick={()=>{update({file:null,attempt:null});if(input.current)input.current.value='';}}>Quitar</button></div>}
    {draft.file&&<div className="selected-status-file"><span><Icon name={draft.file.type.startsWith('image/')?'camera':'file'} size={18}/></span><div><strong>{draft.file.name}</strong><small>{Math.ceil(draft.file.size/1024)} KB · listo para revisar</small></div><button type="button" onClick={()=>{update({file:null,attempt:null});if(input.current)input.current.value='';}} aria-label="Quitar archivo"><Icon name="plus" size={16}/></button></div>}
   </section>
   <footer className="status-composer-footer"><p><Icon name="shield" size={16}/> Colegia Main revisará el contenido antes de mostrarlo a tu audiencia.</p><button className="publish-status-button" disabled={!draft.text.trim()&&!draft.file}><Icon name="send" size={18}/>Enviar a revisión</button></footer>
  </fieldset>
  {busy&&<div className="status-upload-progress" role="status"><div><span style={{width:Math.max(4,progress)+'%'}}/></div><p>{progress<100?`Subiendo ${progress}%`:'Confirmando con Colegia Main…'}</p><button type="button" onClick={()=>controller.current?.abort()}>Cancelar</button></div>}
 </form>;
}

function StatusViewer({status,playlist,mine,onOpen,onClose,onDelete,onReply,busy}){
 const [paused,setPaused]=useState(false),[hidden,setHidden]=useState(document.hidden);const next=useRef(null);
 const index=playlist.findIndex(s=>s.id===status.id);
 const advance=()=>{if(index+1<playlist.length)onOpen(playlist[index+1]);else onClose();};next.current=advance;
 useEffect(()=>{const changed=()=>setHidden(document.hidden);document.addEventListener('visibilitychange',changed);return()=>document.removeEventListener('visibilitychange',changed);},[]);
 useEffect(()=>{if(paused||hidden||!['TEXT','IMAGE'].includes(status.kind))return;const timer=setTimeout(()=>next.current(),5000);return()=>clearTimeout(timer);},[status.id,status.kind,paused,hidden]);
 const background=/^#[a-f0-9]{6}$/i.test(status.background)?status.background:'#087f68';
 return <section className="status-viewer" aria-label="Visor de estado"><div className="status-progress" aria-hidden="true">{playlist.map((item,itemIndex)=><span key={item.id} className={itemIndex<index?'complete':itemIndex===index?'current':''}/>)}</div><header className="status-viewer-header"><span className="avatar small chat-avatar">{status.name?.slice(0,1)||'M'}</span><div className="grow"><h2>{status.name}</h2><p>{index+1} de {playlist.length} · {localDate(status.createdAt)}</p></div><button className="icon-button" aria-label="Cerrar estado" title="Cerrar estado" onClick={onClose}><Icon name="arrowLeft"/></button></header>
  <div className="status-content" style={{backgroundColor:background,color:statusTextColor(background)}}>{status.text&&<p className="status-text">{status.text}</p>}{status.kind!=='TEXT'&&<PrivateMedia key={status.id} kind={status.kind} path={'/api/v1/statuses/'+enc(status.id)+'/media'} name="Contenido del estado" onEnded={()=>{if(!paused&&!hidden)advance();}}/>}</div>
  <div className="toolbar"><button disabled={index===0} onClick={()=>onOpen(playlist[index-1])}>Estado anterior</button><button onClick={()=>setPaused(v=>!v)}>{paused?'Reanudar avance':'Pausar avance'}</button><button onClick={advance}>Estado siguiente</button></div>
  <p>Caduca {localDate(status.expiresAt)} · {status.audience==='PRIVATE'?'Solo yo':'Contactos autorizados'}</p>
  {!mine&&onReply&&<button disabled={busy} onClick={onReply}>Responder por mensaje</button>}
  {mine&&<><p>{status.state==='APPROVED'?`Aprobado · ${status.views} vistas`:status.state==='PENDING'?'Pendiente de revisión · solo visible para ti':'Rechazado · solo visible para ti'}</p>{status.reason&&<p role="alert">{status.reason}</p>}<button disabled={busy} onClick={onDelete}>Eliminar mi estado</button></>}
 </section>;
}
