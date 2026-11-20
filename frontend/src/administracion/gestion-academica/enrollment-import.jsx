import {useState} from 'react';

export function EnrollmentImport({api,courseId,onSaved,onExpire}) {
  const [source,setSource]=useState('');
  const [preview,setPreview]=useState(null);
  const [busy,setBusy]=useState(false);
  const [error,setError]=useState('');
  const [notice,setNotice]=useState('');

  function change(value) {
    setSource(value);setPreview(null);setNotice('');setError('');
  }

  async function loadFile(event) {
    const file=event.target.files?.[0];
    event.target.value='';
    if(!file)return;
    setBusy(true);
    try {
      if(file.size>30000)throw Error('Elige un CSV de hasta 30 KB.');
      change(await file.text());
    } catch(reason) {
      setError(reason.message);
    } finally {
      setBusy(false);
    }
  }

  async function submit(apply) {
    if(busy||!source.trim()||(apply&&!preview?.valid))return;
    setBusy(true);setError('');setNotice('');
    try {
      const result=await api(`/v1/courses/${courseId}/students/import`,
        {method:'POST',json:{csv:source,apply}});
      if(apply){
        setPreview(null);setSource('');
        setNotice(`Matrículas agregadas: ${result.added}.`);
        onSaved();
      } else setPreview(result);
    } catch(reason) {
      if(reason.status===401||reason.status===403)onExpire();
      else {
        if(apply&&reason.status===409)setPreview(null);
        setError(apply&&reason.status===409?'El lote cambió. Genera una nueva vista previa.':reason.message);
      }
    } finally {
      setBusy(false);
    }
  }

  return <details className="card">
    <summary>Importar matrículas</summary>
    <p>Usa un CSV con encabezado «usuario» y una cuenta de estudiante por fila. Hasta 300 filas.</p>
    <label>Archivo CSV <input type="file" accept=".csv,text/csv" disabled={busy} onChange={loadFile}/></label>
    <label>Contenido del CSV <textarea value={source} rows={6} maxLength={30000} disabled={busy}
      placeholder={'usuario\nalumno'} onChange={event=>change(event.target.value)}/></label>
    <button type="button" disabled={busy||!source.trim()} onClick={()=>submit(false)}>Vista previa</button>
    {error&&<p className="err" role="alert">{error}</p>}
    {notice&&<p className="ok" role="status">{notice}</p>}
    {preview&&<>
      <div className="scroll"><table><caption>Resultado por fila</caption>
        <thead><tr><th scope="col">Fila</th><th scope="col">Usuario</th><th scope="col">Resultado</th></tr></thead>
        <tbody>{preview.rows.map(row=><tr key={row.line}><td>{row.line}</td><td>{row.username||'—'}</td><td>{row.detail}</td></tr>)}</tbody>
      </table></div>
      <button type="button" disabled={busy||!preview.valid||!preview.rows.some(row=>row.state==='READY')}
        onClick={()=>submit(true)}>Confirmar matrículas</button>
    </>}
  </details>;
}
