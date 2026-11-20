import {useEffect,useRef,useState} from 'react';
import {userApi,submissionKey} from '../datos/user-api.mjs';
import {assistantRecovery} from './assistant-model.mjs';
import {Icon} from '../../ui-icons.jsx';
export const emptyAssistant=()=>({question:'',attempt:null,answer:'',answeredQuestion:'',model:'',error:'',code:''});
export function Assistant({draft,setDraft}){
 const [busy,setBusy]=useState(false);const control=useRef(null),lock=useRef(false),alive=useRef(true);
 useEffect(()=>()=>{alive.current=false;control.current?.abort();},[]);
 const same=draft.attempt?.payload===draft.question.trim();
 const knownFailure=!!draft.code&&assistantRecovery(draft.code)==='new'&&same;
 async function send(e){e.preventDefault();if(lock.current||knownFailure||!draft.question.trim())return;
  const question=draft.question.trim(),attempt=submissionKey(draft.attempt,question);setDraft(old=>({...old,attempt,error:'',code:'',answer:'',model:'',answeredQuestion:''}));
  lock.current=true;setBusy(true);control.current=new AbortController();
  try{const response=await userApi.call('/api/v1/assistant',{method:'POST',json:{clientKey:attempt.key,question},signal:control.current.signal});setDraft(old=>({...old,answer:response.answer,answeredQuestion:question,model:response.model,error:'',code:''}));}
  catch(e){setDraft(old=>({...old,error:e.message,code:e.code||'REQUEST_FAILED'}));}
  finally{lock.current=false;if(alive.current)setBusy(false);}
 }
 const prompts=['Explícame mis próximas actividades','Ayúdame a organizar mi semana','¿Cómo puedo estudiar mejor este tema?'];
 return <section className="assistant-experience" aria-label="Asistente académico">
  <header className="assistant-hero"><span className="assistant-brand"><Icon name="spark" size={28}/></span><div><h1>Asistente académico</h1></div></header>
  <div>
   <main className="workspace-card assistant-workspace">
    <div className="assistant-suggestions">{prompts.map(prompt=><button key={prompt} type="button" disabled={busy} onClick={()=>setDraft(old=>({...old,question:prompt}))}><Icon name="spark" size={16}/>{prompt}</button>)}</div>
    <form onSubmit={send}><label className="assistant-input"><span className="sr-only">Consulta académica</span><textarea required maxLength={2000} rows={6} disabled={busy} value={draft.question} onChange={e=>setDraft(old=>({...old,question:e.target.value}))} placeholder="Escribe tu pregunta o cuéntame qué necesitas preparar…"/><span className="assistant-counter">{draft.question.length}/2000</span></label>
     <div className="assistant-actions">{busy?<><p role="status">MAIN está preparando una respuesta…</p><button type="button" onClick={()=>control.current?.abort()}>Cancelar espera</button></>:<button className="primary-action" disabled={!draft.question.trim()||knownFailure}><Icon name="send" size={17}/>{same&&draft.attempt?'Recuperar respuesta':'Consultar a MAIN'}</button>}<button type="button" disabled={busy} onClick={()=>setDraft(old=>({...emptyAssistant(),question:old.question}))}>Nueva solicitud</button></div>
    </form>
    {draft.error&&<div className="assistant-error" role="alert" data-error-code={draft.code}><span><Icon name="alert"/></span><div><strong>No pudimos completar la consulta</strong><p>{draft.error}</p>{knownFailure?<p>Prepara una nueva solicitud cuando el servicio esté disponible.</p>:<p>Recuperar la misma solicitud evita crear un envío duplicado.</p>}</div></div>}
    {draft.answer&&<article className="assistant-answer modern-answer"><header><span><Icon name="spark"/></span><div><span className="section-kicker">RESPUESTA DE MAIN</span><h2>{draft.answeredQuestion}</h2></div></header><p className="message-text">{draft.answer}</p><footer><Icon name="shield" size={15}/>Verifica la información importante · {draft.model}</footer></article>}
   </main>
  </div>
 </section>;
}
