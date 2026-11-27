import {useEffect,useRef,useState} from 'react';
import {userApi,submissionKey} from '../datos/user-api.mjs';
import {assistantRecovery} from './assistant-model.mjs';
export const emptyAssistant=()=>({question:'',attempt:null,answer:'',answeredQuestion:'',model:'',error:'',code:''});
export function Assistant({draft,setDraft,compact=false}){
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
 if(compact)return <section className="assistant-compact" aria-label="Consulta al asistente académico">
  <form className="assistant-compose" onSubmit={send}><label className="sr-only" htmlFor="assistant-question">Consulta académica</label><textarea id="assistant-question" required maxLength={2000} rows={1} placeholder="Escribe tu pregunta…" disabled={busy} value={draft.question} onChange={e=>setDraft(old=>({...old,question:e.target.value}))}/><button type="submit" aria-label={busy?'Consultando':'Enviar consulta'} disabled={busy||!draft.question.trim()||knownFailure}><span className="material-symbols-outlined" aria-hidden="true">{busy?'hourglass_top':'send'}</span></button></form>
  {busy&&<div className="assistant-feedback"><span role="status">Consultando…</span><button type="button" onClick={()=>control.current?.abort()}>Cancelar espera</button></div>}
  {draft.error&&<div className="assistant-feedback" role="alert"><p>{draft.error}</p>{knownFailure&&<button type="button" onClick={()=>setDraft(old=>({...emptyAssistant(),question:old.question}))}>Preparar nueva solicitud</button>}</div>}
  {draft.answer&&<article className="assistant-feedback assistant-answer"><strong>Respuesta del asistente</strong><p className="message-text">{draft.answer}</p><button type="button" onClick={()=>setDraft(emptyAssistant())}>Nueva consulta</button></article>}
  <details className="assistant-privacy"><summary>Sobre tus consultas</summary><p>Tu consulta y tus cursos y horarios autorizados se envían al proveedor de IA configurado por el colegio. Evita incluir datos personales. Comprueba la información importante: las respuestas pueden contener errores.</p><small>{draft.question.length}/2000 caracteres · Hasta 20 consultas por cuenta cada 24 horas.</small></details>
 </section>;
 return <section className="user-panel assistant-panel" aria-label="Asistente académico"><h1>Asistente académico</h1>
  <p>Tu consulta y los nombres de tus cursos y horarios autorizados se envían al proveedor de IA configurado por el colegio. No se envían tus chats ni contactos. Evita incluir datos personales. Comprueba la información importante: las respuestas pueden contener errores.</p>
  <form onSubmit={send}><label>Consulta académica<textarea required maxLength={2000} rows={5} disabled={busy} value={draft.question} onChange={e=>setDraft(old=>({...old,question:e.target.value}))}/></label><small>{draft.question.length}/2000 caracteres · Hasta 20 consultas por cuenta cada 24 horas</small>
   <div className="toolbar">{busy?<><p role="status">Consultando al servicio…</p><button type="button" onClick={()=>control.current?.abort()}>Cancelar espera de IA</button></>:<button disabled={!draft.question.trim()||knownFailure}>{same&&draft.attempt?'Recuperar respuesta de la misma solicitud':'Consultar'}</button>}
    <button type="button" disabled={busy} onClick={()=>setDraft(old=>({...emptyAssistant(),question:old.question}))}>Preparar nueva solicitud</button>
   </div>
  </form>
  {draft.error&&<div role="alert" data-error-code={draft.code}><p>{draft.error}</p>{knownFailure?<p>Prepara una nueva solicitud cuando el servicio esté disponible. No se enviará automáticamente otra consulta.</p>:<p>Si el servidor ya recibió la consulta, recuperar la misma solicitud evita crear un envío duplicado.</p>}</div>}
  {draft.answer&&<article className="assistant-answer"><h2>Respuesta de IA</h2><p className="question-reference">Consulta: {draft.answeredQuestion}</p><p className="message-text">{draft.answer}</p><small>Modelo informado por el servidor: {draft.model}</small></article>}
 </section>;
}
