import {useState} from 'react';
import {localDate,userApi} from '../datos/user-api.mjs';
import {Feedback,useConfirmation,useResource} from '../datos/user-hooks.jsx';

const endpoint='/api/v1/campus/feed';
export function CampusFeed({profile,campusName,campusColor}){
  const [view,setView]=useState('for-you'),[offset,setOffset]=useState(0),[draft,setDraft]=useState('');
  const [busy,setBusy]=useState(false),[message,setMessage]=useState(null);
  const posts=useResource(`${endpoint}?view=${view}&offset=${offset}`);
  const {confirm,confirmation}=useConfirmation();
  const student=profile.rol==='Alumno';
  async function run(action,text){
    setBusy(true);setMessage(null);
    try{await action();posts.reload();setMessage({text});}
    catch(error){setMessage({error:true,text:error.message});}
    finally{setBusy(false);}
  }
  return <section className="campus-feed" aria-label="Campus" style={{'--campus-color':campusColor}}>
    {confirmation}<header className="campus-banner"><small>{campusName}</small><h1>Campus</h1></header>
    <div className="campus-switch" role="group" aria-label="Vista del campus">{[['for-you','Para ti'],['recent','Recientes'],...(student?[['mine','Mis publicaciones']]:[])].map(([id,label])=>
      <button type="button" key={id} aria-pressed={view===id} disabled={busy} onClick={()=>{setOffset(0);setView(id);}}>{label}</button>)}</div>
    {student&&<form className="campus-composer" onSubmit={event=>{event.preventDefault();run(async()=>{
      await userApi.call(endpoint,{method:'POST',json:{text:draft}});setDraft('');setOffset(0);setView('mine');
    },'Publicación enviada a revisión.');}}>
      <label><span className="sr-only">Publicación</span><textarea placeholder="Escribe una publicación" rows={2} maxLength={1000} disabled={busy} value={draft} onChange={event=>setDraft(event.target.value)}/></label>
      <button disabled={busy||!draft.trim()}>Enviar a revisión</button>
    </form>}
    {message&&<p role={message.error?'alert':'status'}>{message.text}</p>}
    <Feedback resource={posts}/>
    {posts.data?.length===0&&<p className="campus-empty">No hay publicaciones en esta vista.</p>}
    <ul className="campus-posts">{posts.data?.map(post=><li key={post.id}><article>
      <header><span className="campus-avatar" aria-hidden="true">{Array.from(post.author)[0]?.toUpperCase()}</span><div><strong>{post.author}</strong><small>{localDate(post.createdAt)}</small></div></header>
      <p className="message-text">{post.text}</p>
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
