import {useState} from 'react';
import {localDate,userApi} from '../datos/user-api.mjs';
import {Feedback,useConfirmation,useResource} from '../datos/user-hooks.jsx';

const endpoint='/api/v1/campus/feed';
export function CampusFeed({profile,campusName,campusColor}){
  const [view,setView]=useState('for-you'),[offset,setOffset]=useState(0),[draft,setDraft]=useState('');
  const [busy,setBusy]=useState(false),[message,setMessage]=useState(null);
  const [course,setCourse]=useState(''),[pinned,setPinned]=useState(false),[question,setQuestion]=useState(false);
  const [thread,setThread]=useState(null);
  const agenda=useResource('/api/v1/agenda');
  const posts=useResource(`${endpoint}?view=${view}&offset=${offset}${course?'&courseId='+encodeURIComponent(course):''}`);
  const {confirm,confirmation}=useConfirmation();
  const canPublish=profile.rol==='Alumno'||!!course;
  async function run(action,text){
    setBusy(true);setMessage(null);
    try{await action();posts.reload();setMessage({text});}
    catch(error){setMessage({error:true,text:error.message});}
    finally{setBusy(false);}
  }
  return <section className="campus-feed" aria-label="Campus" style={{'--campus-color':campusColor}}>
    {confirmation}<header className="campus-banner"><small>{campusName}</small><h1>Campus</h1></header>
    {agenda.error&&<Feedback resource={agenda}/>}
    {!!agenda.data?.courses.length&&<label>Espacio <select value={course} disabled={busy}
      onChange={event=>{setCourse(event.target.value);setPinned(false);setQuestion(false);setThread(null);setOffset(0);setView('for-you');setMessage(null);}}>
      <option value="">Todo el colegio</option>
      {agenda.data.courses.map(item=><option key={item.id} value={item.id}>{item.name}{item.placement?' · '+item.placement.section:''}</option>)}
    </select></label>}
    <div className="campus-switch" role="group" aria-label="Vista del campus">{[['for-you','Para ti'],['recent','Recientes'],...(canPublish?[['mine','Mis publicaciones']]:[])].map(([id,label])=>
      <button type="button" key={id} aria-pressed={view===id} disabled={busy} onClick={()=>{setOffset(0);setView(id);setThread(null);}}>{label}</button>)}</div>
    {canPublish&&<form className="campus-composer" onSubmit={event=>{event.preventDefault();run(async()=>{
      await userApi.call(endpoint,{method:'POST',json:{text:draft,courseId:course?Number(course):null,pinned,question}});setDraft('');setPinned(false);setQuestion(false);setOffset(0);setView('mine');
    },'Publicación enviada a revisión.');}}>
      <label><span className="sr-only">Publicación</span><textarea placeholder="Escribe una publicación" rows={2} maxLength={1000} disabled={busy} value={draft} onChange={event=>setDraft(event.target.value)}/></label>
      {!!course&&<label><input type="checkbox" checked={question} onChange={event=>{setQuestion(event.target.checked);setPinned(false);}} disabled={busy}/> Pregunta</label>}
      {profile.rol==='Docente'&&<label><input type="checkbox" checked={pinned} onChange={event=>{setPinned(event.target.checked);setQuestion(false);}} disabled={busy}/> Fijar anuncio</label>}
      <button disabled={busy||!draft.trim()}>Enviar a revisión</button>
    </form>}
    {message&&<p role={message.error?'alert':'status'}>{message.text}</p>}
    <Feedback resource={posts}/>
    {posts.data?.length===0&&<p className="campus-empty">No hay publicaciones en esta vista.</p>}
    <ul className="campus-posts">{posts.data?.map(post=><li key={post.id}><article>
      <header><span className="campus-avatar" aria-hidden="true">{Array.from(post.author)[0]?.toUpperCase()}</span><div><strong>{post.author}</strong><small>{post.course&&post.course+' · '}{localDate(post.createdAt)}</small></div></header>
      {post.pinned&&<small>Anuncio fijado</small>}<p className="message-text">{post.text}</p>
      {post.kind==='QUESTION'&&post.state==='PUBLISHED'&&<button type="button" aria-expanded={thread===post.id}
        onClick={()=>setThread(current=>current===post.id?null:post.id)}>{thread===post.id?'Cerrar respuestas':'Ver respuestas'}</button>}
      {thread===post.id&&<CampusReplies key={post.id} postId={post.id}/>}
      {view==='mine'&&<><small>{{PENDING:'En revisión',PUBLISHED:'Publicada',REJECTED:'Rechazada'}[post.state]}</small>{' '}
        <button disabled={busy} onClick={()=>confirm('¿Retirar esta publicación?',()=>run(
          ()=>userApi.call(endpoint+'/'+encodeURIComponent(post.id),{method:'DELETE'}),'Publicación retirada.'))}>Retirar</button></>}
    </article></li>)}</ul>
    {(offset>0||posts.data?.length===20)&&<nav className="campus-pages" aria-label="Páginas del campus">
      <button disabled={busy||offset===0} onClick={()=>setOffset(value=>value-20)}>Anteriores</button>
      <button disabled={busy||posts.data?.length!==20} onClick={()=>setOffset(value=>value+20)}>Más publicaciones</button>
    </nav>}
  </section>;
}


function CampusReplies({postId}) {
  const [draft,setDraft]=useState(''),[offset,setOffset]=useState(0);
  const [busy,setBusy]=useState(false),[feedback,setFeedback]=useState(null);
  const path=endpoint+'/'+encodeURIComponent(postId)+'/replies';
  const replies=useResource(path+'?offset='+offset,{interval:5000});
  async function send(event){
    event.preventDefault();
    if(busy||!draft.trim())return;
    setBusy(true);setFeedback(null);
    try {
      await userApi.call(path,{method:'POST',json:{text:draft.trim()}});
      setDraft('');
      setFeedback({text:'Respuesta enviada a revisión.'});
      replies.reload();
    } catch(error){setFeedback({error:true,text:error.message});}
    finally{setBusy(false);}
  }
  return <section className="campus-thread" aria-label="Respuestas a la pregunta">
    <Feedback resource={replies}/>
    {replies.data?.length===0&&<p>Todavía no hay respuestas.</p>}
    <ul>{replies.data?.map(item=><li key={item.id}><strong>{item.author}</strong>
      {item.state!=='PUBLISHED'&&<small> · {item.state==='PENDING'?'En revisión':'Rechazada'}</small>}
      <p className="message-text">{item.text}</p></li>)}</ul>
    {(offset>0||replies.data?.length===20)&&<div className="campus-pages">
      <button type="button" disabled={offset===0} onClick={()=>setOffset(value=>value-20)}>Anteriores</button>
      <button type="button" disabled={replies.data?.length!==20} onClick={()=>setOffset(value=>value+20)}>Más respuestas</button>
    </div>}
    <form className="campus-composer" onSubmit={send}>
      <label><span className="sr-only">Respuesta</span><textarea placeholder="Escribe una respuesta" maxLength={1000}
        rows={2} value={draft} disabled={busy} onChange={event=>setDraft(event.target.value)}/></label>
      <button disabled={busy||!draft.trim()}>Responder</button>
    </form>
    {feedback&&<p role={feedback.error?'alert':'status'}>{feedback.text}</p>}
  </section>;
}
