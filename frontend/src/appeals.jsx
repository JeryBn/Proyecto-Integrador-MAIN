import {useRef,useState} from 'react';
import {userApi,localDate} from './user-api.mjs';
import {useResource,Feedback} from './user-hooks.jsx';
export const appealLabels={OPEN:'En revisión',UPHELD:'Se mantiene el rechazo',RETURNED:'Devuelto a revisión humana',SUPERSEDED:'Cerrada: contenido cambiado o caducado'};

export function AppealAction({kind,item}){
 const [open,setOpen]=useState(false),[reason,setReason]=useState(''),[busy,setBusy]=useState(false),[error,setError]=useState(''),[notice,setNotice]=useState('');const lock=useRef(false);
 async function submit(event){event.preventDefault();if(lock.current||!reason.trim())return;lock.current=true;setBusy(true);setError('');
  try{const value=await userApi.call('/api/v1/appeals',{method:'POST',json:{kind,subjectId:item.id,revision:kind==='messages'?item.revision:0,contentHash:item.contentHash,reason:reason.trim()}});setOpen(false);setReason('');setNotice((appealLabels[value.state]||'Solicitud registrada')+'. Consulta Mis apelaciones en Perfil.');}
  catch(e){setError(e.message);}finally{lock.current=false;setBusy(false);}
 }
 return <section aria-label="Apelar rechazo"><button disabled={busy} onClick={()=>setOpen(v=>!v)}>{open?'Cerrar solicitud':'Apelar rechazo'}</button>
  {open&&<form onSubmit={submit}><p>Explica por qué solicitas otra revisión. El contenido seguirá privado y protegido mientras se resuelve.</p><label>Motivo de apelación<textarea required maxLength={1000} value={reason} disabled={busy} onChange={e=>setReason(e.target.value)}/></label><button disabled={busy||!reason.trim()}>Enviar apelación</button></form>}
  {error&&<p role="alert">{error}</p>}{notice&&<p role="status">{notice}</p>}
 </section>;
}
export function AppealHistory({active=true}){
 const [offset,setOffset]=useState(0);const appeals=useResource(`/api/v1/appeals?offset=${offset}&limit=30`,{enabled:active,interval:10000});
 return <section aria-label="Mis apelaciones"><h2>Mis apelaciones</h2><p>Una apelación no publica el contenido. Si vuelve a revisión, espera la nueva decisión humana. El historial resuelto se conserva noventa días, salvo una protección abierta.</p><button onClick={appeals.reload}>Actualizar apelaciones</button><Feedback resource={appeals}/>
  {!appeals.loading&&appeals.data?.length===0&&<p>No tienes apelaciones en esta página.</p>}
  <ul>{appeals.data?.map(a=><li key={a.id}><h3>{a.kind==='messages'?'Mensaje · versión '+a.revision:'Estado'} · {appealLabels[a.state]||'Estado pendiente'}</h3><small>{localDate(a.createdAt)}</small><p>Tu motivo: {a.reason}</p>{a.decisionReason&&<p>Resolución: {a.decisionReason}</p>}{a.decidedAt&&<small>{localDate(a.decidedAt)}</small>}</li>)}</ul>
  <button disabled={offset===0} onClick={()=>setOffset(Math.max(0,offset-30))}>Apelaciones anteriores</button><button disabled={appeals.data?.length!==30} onClick={()=>setOffset(offset+30)}>Más apelaciones</button>
 </section>;
}
