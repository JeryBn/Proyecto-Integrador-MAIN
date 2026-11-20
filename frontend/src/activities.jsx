import {useEffect,useState} from 'react';
import {userApi,localDate} from './alumno/datos/user-api.mjs';
import {Icon} from './ui-icons.jsx';

function ActivityCard({activity:a}){
 return <article className="modern-activity-card">
  <span className={'activity-mark '+(a.completada?'complete':a.vencida?'late':'')}><Icon name={a.completada?'checkCircle':a.vencida?'alert':'clipboard'} size={21}/></span>
  <div className="activity-card-main"><span className="course-label">{a.curso}</span><h3>{a.titulo}</h3><p><Icon name="clock" size={14}/> Entrega: {localDate(a.fechaLimite)}</p></div>
  <div className="activity-card-side"><span className={'badge '+(a.completada?'complete':a.vencida?'late':'')}>{a.completada?'Entregada':a.vencida?'Fuera de plazo':'Pendiente'}</span><a className="activity-open" href={'#actividad/'+a.id}>{a.completada?'Ver entrega':'Abrir actividad'} <Icon name="arrowRight" size={17}/></a></div>
 </article>;
}

export function Activities({items,loading,error,onReload}){
 const [search,setSearch]=useState(''),[filter,setFilter]=useState('all');
 const pending=items.filter(item=>!item.completada);
 const shown=items.filter(item=>(filter==='all'||(filter==='late'?item.vencida:filter==='complete'?item.completada:!item.completada))&&(item.titulo+' '+item.curso).toLocaleLowerCase('es').includes(search.toLocaleLowerCase('es').trim()));
 return <section className="activities-workspace">
  <header className="section-hero compact-hero"><div><span className="eyebrow">ORGANIZA TU SEMANA</span><h1>Actividades</h1><p>Revisa prioridades, fechas límite, entregas e indicaciones desde un solo lugar.</p></div><div className="hero-summary"><span>{pending.length}</span><small>pendientes</small></div></header>
  <section className="workspace-card activity-browser">
   <header className="browser-toolbar"><div><h2>Tu lista de trabajo</h2><p>Ordenada por la fecha de entrega más cercana.</p></div><label className="modern-search"><Icon name="search" size={18}/><span className="sr-only">Buscar actividades</span><input type="search" value={search} onChange={event=>setSearch(event.target.value)} placeholder="Buscar actividad o curso"/></label></header>
   <div className="filter-toolbar"><div className="segmented-control" aria-label="Filtrar actividades">{[['all','Todas'],['next','Pendientes'],['late','Vencidas'],['complete','Entregadas']].map(([value,label])=><button key={value} className={filter===value?'selected':''} aria-pressed={filter===value} onClick={()=>setFilter(value)}>{label}<span>{value==='all'?items.length:value==='late'?items.filter(item=>item.vencida).length:value==='complete'?items.filter(item=>item.completada).length:pending.length}</span></button>)}</div><button className="refresh-action" disabled={loading} onClick={onReload}><Icon name="refresh" size={17}/>{loading?'Actualizando…':'Actualizar'}</button></div>
   {loading&&<div className="list-skeleton" role="status">Cargando tus actividades…</div>}
   {error&&<div className="inline-error" role="alert"><p>{error}</p><button onClick={onReload}>Reintentar</button></div>}
   {!loading&&!error&&!shown.length&&<div className="dashboard-empty centered"><span><Icon name="checkCircle" size={30}/></span><div><strong>{items.length?'No encontramos coincidencias':'Todo está en orden'}</strong><p>{items.length?'Prueba otra búsqueda o filtro.':'No tienes actividades pendientes.'}</p></div></div>}
   {!loading&&!error&&<div className="modern-activity-list">{shown.map(item=><ActivityCard key={item.id} activity={item}/>)}</div>}
  </section>
 </section>;
}

export function ActivityDetail({id}){
 const [activity,setActivity]=useState(null),[submission,setSubmission]=useState(null),[error,setError]=useState('');
 const [submissionError,setSubmissionError]=useState(''),[submissionNotice,setSubmissionNotice]=useState(''),[saving,setSaving]=useState(false);
 useEffect(()=>{let active=true;setActivity(null);setSubmission(null);setError('');Promise.all([userApi.call('/api/actividades/'+id),userApi.call('/api/actividades/'+id+'/entrega')]).then(([nextActivity,nextSubmission])=>{if(active){setActivity(nextActivity);setSubmission({...nextSubmission,note:nextSubmission.note||'',evidenceUrl:nextSubmission.evidenceUrl||''});}}).catch(reason=>{if(active)setError(reason.message)});return()=>{active=false};},[id]);
 async function saveSubmission(event,status='SUBMITTED'){event.preventDefault();if(saving)return;setSaving(true);setSubmissionError('');setSubmissionNotice('');try{const saved=await userApi.call('/api/actividades/'+id+'/entrega',{method:'PUT',json:{note:submission.note,evidenceUrl:submission.evidenceUrl,status,expectedRevision:submission.revision}});setSubmission({...saved,note:saved.note||'',evidenceUrl:saved.evidenceUrl||''});setActivity(current=>({...current,completada:saved.submitted,vencida:saved.submitted?false:current.vencida}));setSubmissionNotice(status==='DRAFT'?'Borrador guardado. Puedes continuar más tarde.':saved.revision===1?'Entrega registrada correctamente.':'Cambios de la entrega guardados.');}catch(reason){setSubmissionError(reason.message);}finally{setSaving(false);}}
 if(error)return <section className="workspace-card detail-error" role="alert"><span><Icon name="alert" size={30}/></span><h1>No pudimos abrir esta actividad</h1><p>{error}</p><a href="#actividades">Volver a actividades</a></section>;
 if(!activity)return <section className="workspace-card detail-loading" role="status">Preparando la actividad…</section>;
 const a=activity;
 return <section className="activity-detail-modern" aria-label="Detalle de actividad">
  <a className="back-link" href="#actividades"><Icon name="arrowLeft" size={17}/>Volver a actividades</a>
  <header className="activity-detail-hero"><div><span className="eyebrow">{a.curso}</span><h1>{a.titulo}</h1><p>Publicada por {a.docente}</p></div><span className={'detail-status '+(a.completada?'complete':a.vencida?'late':'')}><Icon name={a.completada?'checkCircle':a.vencida?'alert':'clock'} size={18}/>{a.completada?'Entregada':a.vencida?'Fuera de plazo':'Pendiente'}</span></header>
  <div className="activity-detail-grid">
   <main className="activity-detail-main"><section className="workspace-card activity-instructions"><div className="content-heading"><span><Icon name="file"/></span><div><span className="section-kicker">INDICACIONES</span><h2>Lo que debes realizar</h2></div></div><div className="rich-instructions">{a.descripcion}</div><div className="academic-tip"><Icon name="spark"/><div><strong>Prepárate antes de entregar</strong><p>Lee las indicaciones, revisa tus materiales y confirma que tu trabajo cumpla cada criterio solicitado.</p></div></div></section>
   {submission&&<section className="workspace-card submission-card"><div className="content-heading"><span><Icon name={submission.submitted?'checkCircle':'send'}/></span><div><span className="section-kicker">{submission.submitted?'ENTREGA REGISTRADA':submission.saved?'BORRADOR GUARDADO':'ENTREGA DIGITAL'}</span><h2>{submission.submitted?'Revisa o actualiza tu evidencia':submission.saved?'Continúa tu entrega':'Presenta tu actividad'}</h2></div></div>{submission.saved&&<div className="submission-meta"><span><Icon name="clock" size={15}/>{submission.submitted?'Enviada: '+localDate(submission.submittedAt):'Última edición: '+localDate(submission.updatedAt)}</span><span>Versión {submission.revision}</span></div>}<form onSubmit={saveSubmission}><fieldset disabled={saving}><label>Nota para el docente<textarea maxLength="2000" value={submission.note} onChange={event=>setSubmission({...submission,note:event.target.value})} placeholder="Resume tu trabajo, decisiones o puntos importantes…"/></label><label>Enlace de evidencia<input type="url" maxLength="1000" value={submission.evidenceUrl} onChange={event=>setSubmission({...submission,evidenceUrl:event.target.value})} placeholder="https://drive.google.com/…"/></label><small>Incluye una nota, un enlace HTTPS o ambos. No compartas credenciales ni información sensible.</small>{submissionError&&<p role="alert">{submissionError}</p>}{submissionNotice&&<p className="success-message" role="status"><Icon name="checkCircle" size={17}/>{submissionNotice}</p>}<div className="submission-actions">{!submission.submitted&&<button type="button" className="secondary-action" disabled={saving||(!submission.note.trim()&&!submission.evidenceUrl.trim())} onClick={event=>saveSubmission(event,'DRAFT')}><Icon name="file" size={17}/>Guardar borrador</button>}<button className="primary-action" disabled={saving||(!submission.note.trim()&&!submission.evidenceUrl.trim())}><Icon name="send" size={17}/>{saving?'Guardando…':submission.submitted?'Actualizar entrega':'Entregar actividad'}</button></div></fieldset></form></section>}</main>
   <aside className="activity-detail-aside">
    <section className="workspace-card deadline-card"><span className="round-icon"><Icon name="calendar"/></span><span className="section-kicker">FECHA LÍMITE</span><strong>{localDate(a.fechaLimite)}</strong><p>{a.completada?'Tu entrega está registrada. Puedes actualizarla mientras la actividad permanezca disponible.':a.vencida?'El plazo registrado ya terminó. La institución podrá definir si acepta una entrega tardía.':'Organiza tu avance para entregar con tiempo.'}</p></section>
    <section className="workspace-card preparation-card"><h2>Antes de continuar</h2><ul><li><Icon name="checkCircle"/>Revisa las indicaciones</li><li><Icon name="checkCircle"/>Prepara tus archivos</li><li><Icon name="checkCircle"/>Comprueba el formato solicitado</li></ul><a href="#ia"><Icon name="spark"/>Pedir ayuda académica</a></section>
   </aside>
  </div>
 </section>;
}
