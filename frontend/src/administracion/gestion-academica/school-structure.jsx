import {useEffect,useState} from 'react';

const endpoint='/v1/school-structure';

export function SchoolStructure({api,onExpire,courses}) {
  const [catalog,setCatalog]=useState(null);
  const [year,setYear]=useState('');
  const [revision,setRevision]=useState(0);
  const [error,setError]=useState('');
  const [notice,setNotice]=useState('');
  const [busy,setBusy]=useState(false);
  const [loading,setLoading]=useState(true);

  function showError(reason) {
    if (reason.status===401||reason.status===403) onExpire();
    else setError(reason.message);
  }

  useEffect(()=>{
    let active=true;
    setLoading(true);
    api(endpoint+(year?'?year='+encodeURIComponent(year):''))
      .then(data=>{if(active)setCatalog(data);})
      .catch(reason=>{if(active){setCatalog(null);showError(reason);}})
      .finally(()=>{if(active)setLoading(false);});
    return ()=>{active=false;};
  },[api,year,revision]);

  async function save(event,kind) {
    event.preventDefault();
    if (busy||loading)return;
    const form=event.currentTarget;
    const fields=Object.fromEntries(new FormData(form));
    const data={...fields,kind,year:Number(fields.year||catalog?.selectedYear)};
    for(const name of ['number','courseId'])if(name in data)data[name]=Number(data[name]);
    setBusy(true);setError('');setNotice('');
    try {
      await api(endpoint,{method:'POST',json:data});
      if(kind==='year')setYear(String(data.year));
      form.reset();
      setRevision(value=>value+1);
      setNotice('Cambios guardados.');
    } catch(reason){
      showError(reason);
    } finally {
      setBusy(false);
    }
  }

  const sections=catalog?.sections||[];
  return <section className="card" aria-label="Estructura escolar">
    <h2>Estructura escolar</h2>
    {loading&&<p role="status">Cargando estructura…</p>}
    {error&&<p className="err" role="alert">{error} <button type="button" onClick={()=>{setError('');setRevision(value=>value+1);}}>Actualizar</button></p>}
    {notice&&<p className="ok" role="status">{notice}</p>}
    <details>
      <summary>Crear año escolar</summary>
      <form onSubmit={event=>save(event,'year')}><fieldset disabled={busy||loading}>
        <label>Año <input name="year" type="number" min="1970" max="2200" required/></label>
        <label>Inicio <input name="start" type="date" required/></label>
        <label>Fin <input name="end" type="date" required/></label>
        <button>Guardar año</button>
      </fieldset></form>
    </details>
    {!!catalog?.years.length&&<label>Año escolar <select value={year||catalog.selectedYear} disabled={busy||loading}
      onChange={event=>{setYear(event.target.value);setError('');setNotice('');}}>
      {catalog.years.map(item=><option key={item.year} value={item.year}>{item.year}</option>)}
    </select></label>}
    {!!catalog?.selectedYear&&<>
      <details><summary>Períodos ({catalog.periods.length})</summary>
        <ul>{catalog.periods.map(item=><li key={item.number}>{item.name} · {item.startsOn} a {item.endsOn}</li>)}</ul>
        <form onSubmit={event=>save(event,'period')}><fieldset disabled={busy||loading}>
          <label>Número <input name="number" type="number" min="1" max="12" required/></label>
          <label>Nombre <input name="name" maxLength="60" required/></label>
          <label>Inicio <input name="start" type="date" required/></label>
          <label>Fin <input name="end" type="date" required/></label>
          <button>Guardar período</button>
        </fieldset></form>
      </details>
      <details><summary>Grados y secciones ({sections.length})</summary>
        <ul>{sections.map(item=><li key={item.id}>{item.grade} · {item.label}</li>)}</ul>
        <form onSubmit={event=>save(event,'section')}><fieldset disabled={busy||loading}>
          <label>Grado <input name="grade" maxLength="32" required/></label>
          <label>Sección <input name="label" maxLength="16" required/></label>
          <button>Guardar sección</button>
        </fieldset></form>
      </details>
      <details><summary>Cursos vinculados ({catalog.assignments.length})</summary>
        <ul>{catalog.assignments.map(item=>{
          const section=sections.find(value=>value.id===item.sectionId);
          return <li key={item.courseId}>{item.course} · {section?.grade} {section?.label}</li>;
        })}</ul>
        <form onSubmit={event=>save(event,'assignment')}><fieldset disabled={busy||loading||!sections.length||!courses.length}>
          <label>Curso <select name="courseId" required defaultValue="">
            <option value="">Selecciona un curso</option>{courses.map(course=><option key={course.id} value={course.id}>{course.name} · {course.id}</option>)}
          </select></label>
          <label>Sección <select name="sectionId" required defaultValue="">
            <option value="">Selecciona una sección</option>{sections.map(item=><option key={item.id} value={item.id}>{item.grade} · {item.label}</option>)}
          </select></label>
          <button>Vincular curso</button>
        </fieldset></form>
      </details>
    </>}
  </section>;
}
