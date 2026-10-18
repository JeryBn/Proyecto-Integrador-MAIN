'use strict';
let offset=0;
const queue=document.getElementById('queue'),notice=document.getElementById('notice');
async function request(path,options={}){
 const response=await fetch(path,{...options,credentials:'same-origin',headers:{Accept:'application/json',...options.headers}});
 if(response.status===401||response.status===403)throw Error('Inicia sesión con una cuenta administradora desde Administración.');
 const value=await response.json();
 if(!response.ok)throw Error(value.error?.message||'No se pudo completar la operación.');
 return value;
}
function element(tag,text){const item=document.createElement(tag);if(text!==undefined)item.textContent=text;return item;}
async function review(message,approved,reason,button){
 button.disabled=true;notice.textContent='';
 try{
  const csrf=await request('/api/admin/csrf');
  await request('/api/admin/v1/moderation/messages/'+encodeURIComponent(message.id)+'/review',{
   method:'POST',headers:{'Content-Type':'application/json',[csrf.headerName]:csrf.token},
   body:JSON.stringify({revision:message.revision,contentHash:message.contentHash,approved,reason})
  });
  await load();notice.textContent=approved?'Versión aprobada.':'Versión rechazada; el emisor puede corregirla y solicitar otra revisión.';
 }catch(error){notice.textContent=error.message;button.disabled=false;}
}
async function load(){
 notice.textContent='Cargando…';
 try{
  await loadStatuses();
  const messages=await request('/api/admin/v1/moderation/messages?offset='+offset+'&limit=20');
  queue.replaceChildren();
  for(const message of messages){
   const card=element('article');card.append(element('h2',message.senderName+' · versión '+message.revision),element('p',message.createdAt),element('pre',message.text));
   const label=element('label','Norma o categoría de rechazo (según reglas aprobadas del equipo)');
   const reason=element('input');reason.maxLength=300;label.append(reason);
   const approve=element('button','Aprobar esta versión'),reject=element('button','Rechazar esta versión');
   approve.addEventListener('click',()=>review(message,true,'',approve));
   reject.addEventListener('click',()=>{if(!reason.value.trim()){reason.focus();notice.textContent='Indica la norma que explica el rechazo.';return;}review(message,false,reason.value.trim(),reject);});
   card.append(label,approve,reject);queue.append(card);
  }
  notice.textContent=messages.length?'Revisa el contenido antes de decidir.':'No hay mensajes pendientes en esta página.';
  document.getElementById('previous').disabled=offset===0;document.getElementById('next').disabled=messages.length<20;
 }catch(error){notice.textContent=error.message;}
}
document.getElementById('refresh').addEventListener('click',load);
document.getElementById('previous').addEventListener('click',()=>{offset=Math.max(0,offset-20);load();});
document.getElementById('next').addEventListener('click',()=>{offset+=20;load();});
load();

async function loadStatuses(){
 const target=document.getElementById('statusQueue');target.replaceChildren();
 const statuses=await request('/api/admin/v1/moderation/statuses?offset='+offset+'&limit=20');
 if(!statuses.length)target.append(element('p','No hay estados pendientes en esta página.'));
 for(const status of statuses){
  const card=element('article');card.append(element('h3',status.name+' · '+status.kind),element('p','Caduca: '+status.expiresAt+' · Audiencia: '+status.audience),element('pre',status.text));
  if(status.kind!=='TEXT'){
   const media=element(status.kind==='IMAGE'?'img':status.kind==='VIDEO'?'video':'audio');media.src='/api/admin/v1/moderation/statuses/'+encodeURIComponent(status.id)+'/media';
   if(status.kind==='IMAGE'){media.alt='Imagen pendiente de revisión';media.style.maxWidth='100%';}else{media.controls=true;media.preload='metadata';media.style.maxWidth='100%';}
   card.append(media);
  }
  const reason=element('input');reason.placeholder='Motivo del rechazo';reason.maxLength=300;
  const approve=element('button','Aprobar estado'),reject=element('button','Rechazar estado');
  async function decide(approved){
   if(!approved&&!reason.value.trim()){notice.textContent='Indica el motivo del rechazo.';return;}
   approve.disabled=reject.disabled=true;
   try{const csrf=await request('/api/admin/csrf');await request('/api/admin/v1/moderation/statuses/'+encodeURIComponent(status.id)+'/review',{method:'POST',headers:{'Content-Type':'application/json',[csrf.headerName]:csrf.token},body:JSON.stringify({contentHash:status.contentHash,approved,reason:reason.value.trim()})});await load();}
   catch(error){notice.textContent=error.message;approve.disabled=reject.disabled=false;}
  }
  approve.addEventListener('click',()=>decide(true));reject.addEventListener('click',()=>decide(false));card.append(reason,approve,reject);target.append(card);
 }
}
