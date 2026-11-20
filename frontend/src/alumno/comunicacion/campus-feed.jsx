import {useState} from 'react';
import {localDate,userApi} from '../datos/user-api.mjs';
import {Feedback,useConfirmation,useResource} from '../datos/user-hooks.jsx';

const endpoint='/api/v1/campus/feed';
export function CampusFeed({profile}){
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
  return <section className="workspace-card contacts-card" aria-label="Campus">
    {confirmation}<h1>Campus</h1>
    {student&&<form onSubmit={event=>{event.preventDefault();run(async()=>{
      await userApi.call(endpoint,{method:'POST',json:{text:draft}});setDraft('');setOffset(0);setView('mine');
    },'Publicación enviada a revisión.');}}>
      <label>Publicación<textarea rows={3} maxLength={1000} disabled={busy} value={draft} onChange={event=>setDraft(event.target.value)}/></label>
      <button disabled={busy||!draft.trim()}>Enviar a revisión</button>
    </form>}
    <div role="group" aria-label="Vista del campus">{[['for-you','Para ti'],['recent','Recientes'],...(student?[['mine','Mis publicaciones']]:[])].map(([id,label])=>
      <button type="button" key={id} aria-pressed={view===id} disabled={busy} onClick={()=>{setOffset(0);setView(id);}}>{label}</button>)}</div>
    {message&&<p role={message.error?'alert':'status'}>{message.text}</p>}
    <Feedback resource={posts}/>
    {posts.data?.length===0&&<p>No hay publicaciones en esta vista.</p>}
    <ul className="contact-list">{posts.data?.map(post=><li key={post.id}><article>
      <header><strong>{post.author}</strong> <small>{localDate(post.createdAt)}</small></header>
      <p className="message-text">{post.text}</p>
      {view==='mine'&&<><small>{{PENDING:'En revisión',PUBLISHED:'Publicada',REJECTED:'Rechazada'}[post.state]}</small>{' '}
        <button disabled={busy} onClick={()=>confirm('¿Retirar esta publicación?',()=>run(
          ()=>userApi.call(endpoint+'/'+encodeURIComponent(post.id),{method:'DELETE'}),'Publicación retirada.'))}>Retirar</button></>}
    </article></li>)}</ul>
    <button disabled={busy||offset===0} onClick={()=>setOffset(value=>value-20)}>Anteriores</button>
    <button disabled={busy||posts.data?.length!==20} onClick={()=>setOffset(value=>value+20)}>Más publicaciones</button>
  </section>;
}
