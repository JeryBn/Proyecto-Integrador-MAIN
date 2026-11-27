'use strict';
let holdsOffset=0;
let appealsOffset=0;
const notice=document.getElementById('notice');
async function request(path,options={}){
 const response=await fetch(path,{...options,credentials:'same-origin',headers:{Accept:'application/json',...options.headers}});
 if(response.status===401)throw Error('Tu sesión terminó. Ingresa nuevamente desde el acceso de COLEGIA MAIN.');
 const value=await response.json();
 if(!response.ok)throw Error(value.error?.message||'No se pudo completar la operación.');
 return value;
}
function element(tag,text){const item=document.createElement(tag);if(text!==undefined)item.textContent=text;return item;}
function automaticSummary(value){
 const panel=element('aside');panel.className='automatic-summary';panel.append(element('h3','Evaluación automática orientativa'));
 if(!value){panel.append(element('p','No hay evaluación automática de esta versión. La revisión humana sigue disponible.'));return panel;}
 const states={HUMAN_REQUIRED:'Revisión humana necesaria',CLEAR_SHADOW:'Sin alertas en el análisis de texto',PROVIDER_UNAVAILABLE:'Proveedor no disponible; contenido privado',RUNNING:'Evaluación en curso',WAITING:'Evaluación pendiente'};
 const coverage={TEXT:'Texto',IMAGE_PARTIAL:'Imagen con cobertura parcial',AUDIO_TRANSCRIPT_PARTIAL:'Transcripción de audio con cobertura parcial',VIDEO_SAMPLED_PARTIAL:'Video muestreado y audio con cobertura parcial',NOT_ANALYZED:'Sin análisis completo'};
 panel.append(element('p',(states[value.outcome]||'Revisión humana pendiente')+' · '+(coverage[value.coverage]||'Cobertura no confirmada')));
 const categories={'sexual':'Contenido sexual','sexual/minors':'Contenido sexual relacionado con menores','harassment':'Acoso','harassment/threatening':'Acoso con amenazas','hate':'Discriminación','hate/threatening':'Discriminación con amenazas','violence':'Violencia','violence/graphic':'Violencia gráfica','self-harm':'Autolesión','self-harm/intent':'Riesgo de autolesión','self-harm/instructions':'Promoción de autolesión','illicit':'Actividad ilícita','illicit/violent':'Actividad ilícita violenta'};
 const found=Array.isArray(value.categories)?value.categories.filter(key=>Object.hasOwn(categories,key)):[];
 panel.append(element('p',found.length?'Alertas: '+found.map(key=>categories[key]).join(', '):'No hay categorías marcadas en este resumen.'));
 if(Number.isInteger(value.sampledFrames)&&value.sampledFrames>0&&value.sampledFrames<=12)panel.append(element('p','Fotogramas muestreados: '+value.sampledFrames));
 if(typeof value.durationSeconds==='number'&&Number.isFinite(value.durationSeconds))panel.append(element('p','Duración del medio: '+value.durationSeconds.toFixed(2)+' s'));
 panel.append(element('p','La ausencia de alertas no aprueba el contenido. Revisa el archivo completo, incluido audio y texto visible; las muestras y transcripciones pueden omitir contenido.'));
 return panel;
}
async function load(){
 notice.textContent='Cargando…';
 try{
  await loadAppeals();
  await loadHolds();
  await loadStatuses();
  await loadCampus();
 }catch(error){notice.textContent=error.message;}
}
document.getElementById('refresh').addEventListener('click',load);
load();

async function loadCampus(){
 const target=document.getElementById('campusQueue');
 const posts=await request('/api/admin/v1/moderation/campus?limit=20');
 target.replaceChildren();
 if(!posts.length)target.append(element('p','No hay publicaciones pendientes.'));
 for(const post of posts){
  const card=element('article');
  card.append(element('h3',post.display_name),element('pre',post.body));
  const approve=element('button','Publicar'),reject=element('button','Rechazar');
  async function decide(approved){
   approve.disabled=reject.disabled=true;
   try{
    const csrf=await request('/api/admin/csrf');
    await request('/api/admin/v1/moderation/campus/'+encodeURIComponent(post.id)+'/review',{
     method:'POST',headers:{'Content-Type':'application/json',[csrf.headerName]:csrf.token},
     body:JSON.stringify({approved})
    });
    await loadCampus();
   }catch(error){notice.textContent=error.message;approve.disabled=reject.disabled=false;}
  }
  approve.addEventListener('click',()=>decide(true));
  reject.addEventListener('click',()=>decide(false));
  card.append(approve,reject);target.append(card);
 }
}

async function loadStatuses(){
 const target=document.getElementById('statusQueue');target.replaceChildren();
 const statuses=await request('/api/admin/v1/moderation/statuses?limit=20');
 if(!statuses.length)target.append(element('p','No hay estados pendientes en esta página.'));
 for(const status of statuses){
  const card=element('article');card.append(element('h3',status.name+' · '+status.kind),element('p','Caduca: '+status.expiresAt+' · Audiencia: '+status.audience),element('pre',status.text),automaticSummary(status.automaticReview));
  let mediaReviewed=null;
  if(status.kind!=='TEXT'){
   mediaReviewed=element('input');mediaReviewed.type='checkbox';mediaReviewed.disabled=true;
   const confirmation=element('label','He revisado todo el contenido multimedia, incluido el audio.');confirmation.prepend(mediaReviewed);
   const media=element(status.kind==='IMAGE'?'img':status.kind==='VIDEO'?'video':'audio');media.src='/api/admin/v1/moderation/statuses/'+encodeURIComponent(status.id)+'/media';
   if(status.kind==='IMAGE'){media.alt='Imagen pendiente de revisión';media.style.maxWidth='100%';}else{media.controls=true;media.preload='metadata';media.style.maxWidth='100%';}
   media.addEventListener(status.kind==='IMAGE'?'load':'loadedmetadata',()=>{mediaReviewed.disabled=false;});
   media.addEventListener('error',()=>{mediaReviewed.checked=false;mediaReviewed.disabled=true;approve.disabled=true;});
   card.append(media,confirmation);
  }
  const reason=element('input');reason.placeholder='Motivo del rechazo';reason.maxLength=300;
  const approve=element('button','Aprobar estado'),reject=element('button','Rechazar estado');approve.className='approve-action';reject.className='reject-action';
  if(mediaReviewed){approve.disabled=!mediaReviewed.checked;mediaReviewed.addEventListener('change',()=>{approve.disabled=!mediaReviewed.checked;});}
  async function decide(approved){
   if(approved&&mediaReviewed&&!mediaReviewed.checked){notice.textContent='Revisa el archivo completo y confirma la revisión antes de aprobar.';return;}
   if(!approved&&!reason.value.trim()){notice.textContent='Indica el motivo del rechazo.';return;}
   approve.disabled=reject.disabled=true;
   try{const csrf=await request('/api/admin/csrf');await request('/api/admin/v1/moderation/statuses/'+encodeURIComponent(status.id)+'/review',{method:'POST',headers:{'Content-Type':'application/json',[csrf.headerName]:csrf.token},body:JSON.stringify({contentHash:status.contentHash,approved,reason:reason.value.trim(),attachmentReviewed:mediaReviewed?.checked||false})});await load();}
   catch(error){notice.textContent=error.message;approve.disabled=!!mediaReviewed&&!mediaReviewed.checked;reject.disabled=false;}
  }
  approve.addEventListener('click',()=>decide(true));reject.addEventListener('click',()=>decide(false));card.append(reason,approve,reject,protectButton('statuses',status.id));target.append(card);
 }
}

function selectProtection(kind,id){
 document.getElementById('retentionKind').value=kind;document.getElementById('retentionId').value=id;
 document.getElementById('retentionReason').value='';document.getElementById('retentionReason').focus();
 notice.textContent='Indica el motivo y confirma la protección o su resolución.';
}
function protectButton(kind,id){const button=element('button','Gestionar protección por apelación');button.addEventListener('click',()=>selectProtection(kind,id));return button;}
async function loadHolds(){
 const values=await request('/api/admin/v1/retention-holds?offset='+holdsOffset+'&limit=20');
 const target=document.getElementById('retentionHolds');target.replaceChildren();
 if(!values.length)target.append(element('p','No hay protecciones activas en esta página.'));
 for(const value of values){const row=element('article');row.append(element('p',(value.kind==='messages'?'Mensaje':'Estado')+' · '+value.id),element('p',value.reason),protectButton(value.kind,value.id));target.append(row);}
 document.getElementById('holdsPrevious').disabled=holdsOffset===0;document.getElementById('holdsNext').disabled=values.length<20;
}
async function changeProtection(active){
 const form=document.getElementById('retentionForm');if(!form.reportValidity())return;
 const protect=document.getElementById('protectContent'),release=document.getElementById('releaseContent');protect.disabled=release.disabled=true;
 try{
  const csrf=await request('/api/admin/csrf');
  const path='/api/admin/v1/moderation/'+document.getElementById('retentionKind').value+'/'+encodeURIComponent(document.getElementById('retentionId').value.trim())+'/retention-hold';
  await request(path,{method:'POST',headers:{'Content-Type':'application/json',[csrf.headerName]:csrf.token},body:JSON.stringify({active,reason:document.getElementById('retentionReason').value.trim()})});
  await loadHolds();notice.textContent=active?'Protección activa. La limpieza conservará este contenido.':'Protección resuelta. Comienza un plazo nuevo de conservación.';
 }catch(error){notice.textContent=error.message;}finally{protect.disabled=release.disabled=false;}
}
document.getElementById('retentionForm').addEventListener('submit',event=>{event.preventDefault();changeProtection(true);});
document.getElementById('releaseContent').addEventListener('click',()=>changeProtection(false));
document.getElementById('holdsPrevious').addEventListener('click',()=>{holdsOffset=Math.max(0,holdsOffset-20);loadHolds().catch(error=>{notice.textContent=error.message;});});
document.getElementById('holdsNext').addEventListener('click',()=>{holdsOffset+=20;loadHolds().catch(error=>{notice.textContent=error.message;});});

async function loadAppeals(){
 const values=await request('/api/admin/v1/appeals?offset='+appealsOffset+'&limit=20');const target=document.getElementById('appealQueue');target.replaceChildren();
 if(!values.length)target.append(element('p','No hay apelaciones abiertas en esta página.'));
 for(const value of values){const card=element('article');card.append(element('h3',(value.kind==='messages'?'Mensaje · versión '+value.revision:'Estado')+' · '+value.owner),element('p','Solicitud: '+value.reason));
  const detail=element('button','Revisar apelación'),panel=element('section');detail.addEventListener('click',async()=>{detail.disabled=true;panel.replaceChildren();
   try{const item=await request('/api/admin/v1/appeals/'+encodeURIComponent(value.id));panel.append(element('pre',item.text),element('p',item.current?'La versión sigue rechazada.':'El contenido cambió o fue retirado.'),element('p',item.expired?'Estado caducado: no puede volver a publicarse.':''));
    if(!['NONE','TEXT'].includes(item.mediaKind)){const url='/api/admin/v1/appeals/'+encodeURIComponent(item.id)+'/media';const link=element('a','Abrir archivo completo de la apelación');link.href=url;panel.append(link);
     if(item.mediaKind!=='FILE'){const media=element(item.mediaKind==='IMAGE'?'img':item.mediaKind==='VIDEO'?'video':'audio');media.src=url;media.style.maxWidth='100%';if(item.mediaKind==='IMAGE')media.alt='Contenido de la apelación';else{media.controls=true;media.preload='metadata';}panel.append(media);}
    }
    const label=element('label','Explicación para quien apeló'),reason=element('textarea');reason.maxLength=1000;label.append(reason);panel.append(label);
    for(const [outcome,title] of [['UPHELD','Mantener rechazo'],['RETURNED','Devolver a revisión humana'],['SUPERSEDED','Cerrar por cambio o caducidad']]){
     const button=element('button',title);button.disabled=outcome==='RETURNED'&&(!item.current||item.expired)||outcome==='SUPERSEDED'&&item.current&&!item.expired;
     button.addEventListener('click',async()=>{if(!reason.value.trim()){notice.textContent='Explica la resolución para quien apeló.';reason.focus();return;}const buttons=panel.querySelectorAll('button');buttons.forEach(b=>b.disabled=true);
      try{const csrf=await request('/api/admin/csrf');await request('/api/admin/v1/appeals/'+encodeURIComponent(item.id)+'/resolve',{method:'POST',headers:{'Content-Type':'application/json',[csrf.headerName]:csrf.token},body:JSON.stringify({outcome,reason:reason.value.trim(),contentHash:item.contentHash})});await load();notice.textContent='Apelación resuelta. El contenido no se ha publicado.';}
      catch(error){notice.textContent=error.message;detail.disabled=false;button.disabled=false;}
     });panel.append(button);
    }
   }catch(error){notice.textContent=error.message;detail.disabled=false;}
  });card.append(detail,panel);target.append(card);
 }
 document.getElementById('appealsPrevious').disabled=appealsOffset===0;document.getElementById('appealsNext').disabled=values.length<20;
}
document.getElementById('appealsPrevious').addEventListener('click',()=>{appealsOffset=Math.max(0,appealsOffset-20);loadAppeals().catch(error=>{notice.textContent=error.message;});});
document.getElementById('appealsNext').addEventListener('click',()=>{appealsOffset+=20;loadAppeals().catch(error=>{notice.textContent=error.message;});});
