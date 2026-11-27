import {useEffect,useState} from 'react';
import {userApi,localDate} from '../datos/user-api.mjs';

export const publicationKinds={NOTICE:'Comunicado',TASK:'Tarea',EXAM:'Examen'};
const fresh=()=>({requestId:crypto.randomUUID(),scope:'',kind:'NOTICE',title:'',instructions:'',dueAt:''});

export function PublicationList({items,onRead,showReadStats=false}) {
  const [opened,setOpened]=useState(null),[error,setError]=useState(''),[busy,setBusy]=useState(false);
  async function open(item) {
    setOpened(item.id);setError('');
    if(onRead&&!item.read){setBusy(true);try{await onRead(item);}catch(e){setError(e.message);}finally{setBusy(false);}}
  }
  return <div className="school-publications-list">
    {!items.length&&<div className="school-empty-state"><h3>Aún no hay publicaciones</h3><p>Los comunicados, tareas y exámenes aparecerán aquí cuando los publique el docente.</p></div>}
    {error&&<p role="alert">{error}</p>}
    {items.map(item=><article className="school-publication" key={item.id}>
      <div className="publication-meta"><span className={`school-pill kind-${item.kind}`}>{publicationKinds[item.kind]}</span><span>{item.course}</span>{onRead&&<span>{item.read?'Leído':'Por leer'}</span>}</div>
      <h3>{item.title}</h3><p>{item.dueAt?`${item.kind==='EXAM'?'Fecha del examen':'Entrega'}: ${localDate(item.dueAt)}`:`Publicado: ${localDate(item.createdAt)}`}</p>
      <button disabled={busy} onClick={()=>opened===item.id?setOpened(null):open(item)}>{opened===item.id?'Cerrar detalle':'Leer publicación'}</button>
      {opened===item.id&&<div className="publication-details"><p>{item.instructions}</p>{error&&onRead&&<button disabled={busy} onClick={()=>open(item)}>Reintentar confirmación de lectura</button>}</div>}
      {showReadStats&&item.readers&&<><p>{item.readers.length} de {item.audience} apoderados leyeron esta publicación.</p>{item.readers.length>0&&<details><summary>Lecturas confirmadas</summary><ul>{item.readers.map((r,i)=><li key={i}>{r.name} · {localDate(r.readAt)}</li>)}</ul></details>}</>}
    </article>)}
  </div>;
}

export function SchoolPublications({profile}) {
  const teacher=profile.rol==='Docente';
  const [items,setItems]=useState(null),[scopes,setScopes]=useState([]),[form,setForm]=useState(fresh),[error,setError]=useState(''),[notice,setNotice]=useState(''),[busy,setBusy]=useState(false),[revision,setRevision]=useState(0),[dateError,setDateError]=useState('');
  useEffect(()=>{let alive=true;setError('');setItems(null);
    Promise.all([userApi.call(teacher?'/api/v1/teaching/publications':'/api/v1/school/publications'),teacher?userApi.call('/api/v1/teaching/publication-scopes'):Promise.resolve([])])
      .then(([i,s])=>{if(alive){setItems(i);setScopes(s);}}).catch(e=>alive&&setError(e.message));
    return()=>{alive=false;};
  },[teacher,revision]);
  const update=(name,value)=>{setForm({...form,[name]:value});if(name==='dueAt'||name==='kind')setDateError('');};
  async function submit(e){e.preventDefault();setError('');setNotice('');
    if(form.kind!=='NOTICE'&&!form.dueAt){setDateError(form.kind==='EXAM'?'La fecha del examen es obligatoria.':'La fecha de entrega es obligatoria.');e.currentTarget.querySelector('input[type=datetime-local]')?.focus();return;}
    if(!e.currentTarget.checkValidity()){e.currentTarget.reportValidity();return;}
    setDateError('');setBusy(true);
    try{const scope=scopes[Number(form.scope)];if(!scope)throw Error('Selecciona el curso y su aula.');
      await userApi.call('/api/v1/teaching/publications',{method:'POST',json:{...form,scope:undefined,courseId:scope.courseId,classroomId:scope.classroomId,dueAt:form.dueAt||null}});
      setNotice(`${publicationKinds[form.kind]} ${form.kind==='TASK'?'publicada':'publicado'} correctamente.`);setForm(fresh());setRevision(v=>v+1);
    }catch(e){setError(e.message);}finally{setBusy(false);}
  }
  return <section className="school-portal"><div className="school-page-heading"><span className="eyebrow">COMUNIDAD EDUCATIVA</span><h1>{teacher?'Publicaciones del aula':'Comunicados y tareas'}</h1><p>{teacher?'Comparte indicaciones claras y consulta las lecturas de los apoderados.':'Consulta los comunicados, tareas y exámenes de tus cursos.'}</p><button onClick={()=>setRevision(v=>v+1)}>Actualizar publicaciones</button></div>
    {error&&<p role="alert">{error}</p>}{notice&&<p role="status" className="school-notice">{notice}</p>}
    {teacher&&<form noValidate onSubmit={submit} className="user-panel"><h2>Nueva publicación</h2><fieldset disabled={busy} className="school-form-grid">
      <label>Curso y destinatarios<select required value={form.scope} onChange={e=>update('scope',e.target.value)}><option value="">Selecciona…</option>{scopes.map((s,i)=><option value={i} key={i}>{s.course} · {s.classroom}</option>)}</select></label>
      <label>Tipo<select aria-label="Tipo" value={form.kind} onChange={e=>update('kind',e.target.value)}>{Object.entries(publicationKinds).map(([k,v])=><option key={k} value={k}>{v}</option>)}</select></label>
      <label>Título<input required maxLength="160" value={form.title} onChange={e=>update('title',e.target.value)}/></label>
      <label>{form.kind==='EXAM'?'Fecha del examen':'Fecha de entrega'}{form.kind==='NOTICE'?' (opcional)':' (obligatoria)'}<input type="datetime-local" required={form.kind!=='NOTICE'} aria-invalid={!!dateError} aria-describedby={dateError?'publication-date-error':undefined} value={form.dueAt} onChange={e=>update('dueAt',e.target.value)}/>{dateError&&<small id="publication-date-error" role="alert">{dateError}</small>}</label>
      <label className="span-all">Indicaciones<textarea required maxLength="2000" value={form.instructions} onChange={e=>update('instructions',e.target.value)}/></label>
      <button type="submit" disabled={busy||!scopes.length}>{busy?'Publicando…':'Publicar'}</button>
    </fieldset></form>}
    {items===null&&!error?<p role="status">Cargando publicaciones…</p>:items&&<PublicationList items={items} showReadStats={teacher}/>}
  </section>;
}
