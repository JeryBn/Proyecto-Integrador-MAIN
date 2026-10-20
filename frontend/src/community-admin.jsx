import {useEffect, useState} from 'react';

export function CommunityAdmin({api, onExpire, currentUser}) {
  const [users, setUsers] = useState([]), [offset, setOffset] = useState(0);
  const [error, setError] = useState(''), [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false), [loading, setLoading] = useState(true);
  const [revision, setRevision] = useState(0), [reports, setReports] = useState([]);
  const [account, setAccount] = useState({username:'', name:'', role:'ALUMNO', password:'', academicId:''});
  const [guardian, setGuardian] = useState(''), [student, setStudent] = useState('');
  const [first, setFirst] = useState(''), [second, setSecond] = useState('');
  const [target, setTarget] = useState(''), [password, setPassword] = useState('');
  const fail = e => { if (e.status === 401 || e.status === 403) onExpire(); else setError(e.message); };
  useEffect(() => {
    let active = true; setLoading(true); setError('');
    Promise.all([api(`/v1/users?offset=${offset}&limit=20`), api(`/v1/reports?offset=${offset}&limit=20`)])
      .then(([u,r]) => {if(active) {setUsers(u); setReports(r);}})
      .catch(e => {if(active) fail(e);}).finally(() => {if(active) setLoading(false);});
    return () => {active = false;};
  }, [offset, revision]);
  async function save(action, message, clear) {
    setBusy(true); setError(''); setNotice('');
    try {await action(); clear?.(); setNotice(message); setRevision(v=>v+1);}
    catch(e) {fail(e);} finally {setBusy(false);}
  }
  const field = (key, label, type='text') => <label>{label}<input type={type} value={account[key]} required
    autoComplete={type === 'password' ? 'new-password' : 'off'} onChange={e=>setAccount({...account,[key]:e.target.value})}/></label>;
  return <section aria-label="Administración de la comunidad">
    <h2>Cuentas y vínculos</h2>
    <p>Las cuentas, contactos y parentescos se comparten con la app. Verifica la identidad antes de autorizar un vínculo.</p>
    {error && <p role="alert" className="err">{error}</p>}
    {notice && <p role="status" className="ok">{notice}</p>}
    {loading ? <p role="status">Cargando comunidad…</p> : <>
      <div className="scroll"><table><caption>Cuentas registradas</caption><thead><tr><th>Usuario</th><th>Nombre</th><th>Rol</th><th>Estado</th><th>Acción</th></tr></thead>
        <tbody>{users.map(u=><tr key={u.username}><td>{u.username}</td><td>{u.name}</td><td>{u.role}</td><td>{u.enabled?'Activa':'Deshabilitada'}</td><td>
          <button disabled={busy || !u.enabled || u.username === currentUser} onClick={()=>{
            if(window.confirm(`¿Deshabilitar la cuenta ${u.username}? Se cerrarán sus sesiones.`))
              save(()=>api(`/v1/users/${encodeURIComponent(u.username)}/disable`,{method:'POST'}),'Cuenta deshabilitada.');
          }}>Deshabilitar</button></td></tr>)}</tbody></table></div>
      {!users.length && <p>No hay cuentas en esta página.</p>}
      <h3>Reportes de contactos</h3>
      {!reports.length ? <p>No hay reportes en esta página.</p> : <ul>{reports.map((r,i)=><li key={i}><dl>{Object.entries(r).map(([k,v])=><div key={k}><dt>{k}</dt><dd>{String(v ?? '')}</dd></div>)}</dl></li>)}</ul>}
      <div className="pager"><button disabled={busy || offset===0} onClick={()=>setOffset(offset-20)}>Anterior</button><span>Página {offset/20+1}</span><button disabled={busy || (users.length<20 && reports.length<20)} onClick={()=>setOffset(offset+20)}>Siguiente</button></div>
    </>}
    <fieldset disabled={busy} className="card"><legend>Crear cuenta</legend><form onSubmit={e=>{e.preventDefault();
      const id=Number(account.academicId);
      if(account.role!=='FAMILIAR' && (!Number.isSafeInteger(id)||id<1)) {setError('Usa un identificador académico entero positivo.');return;}
      save(()=>api('/v1/users',{method:'POST',json:{username:account.username,name:account.name,role:account.role,password:account.password,
        alumnoId:account.role==='ALUMNO'?id:null,docenteId:account.role==='DOCENTE'?id:null}}),'Cuenta creada.',()=>setAccount({...account,password:'',username:'',name:'',academicId:''}));
    }}>
      {field('username','Usuario')}{field('name','Nombre completo')}
      <label>Rol<select value={account.role} onChange={e=>setAccount({...account,role:e.target.value,academicId:''})}><option value="ALUMNO">Estudiante</option><option value="DOCENTE">Docente</option><option value="FAMILIAR">Familiar</option></select></label>
      {account.role!=='FAMILIAR' && field('academicId','Identificador académico','number')}
      {field('password','Contraseña inicial (12 caracteres como mínimo)','password')}
      <button type="submit">Crear cuenta</button>
    </form></fieldset>
    <fieldset disabled={busy} className="card"><legend>Parentesco verificado</legend><form onSubmit={e=>{e.preventDefault();const id=Number(student);
      if(!Number.isSafeInteger(id)||id<1){setError('Identificador de estudiante inválido.');return;}
      save(()=>api('/v1/guardians',{method:'POST',json:{guardian,studentId:id}}),'Parentesco verificado.');
    }}><label>Usuario familiar<input required value={guardian} onChange={e=>setGuardian(e.target.value)}/></label>
      <label>ID del estudiante<input required type="number" min="1" step="1" value={student} onChange={e=>setStudent(e.target.value)}/></label>
      <button type="submit">Verificar parentesco</button>
      <button type="button" onClick={()=>{
        if(!guardian || !Number.isSafeInteger(Number(student)) || Number(student)<1){setError('Indica familiar y estudiante válidos.');return;}
        if(window.confirm('¿Revocar este parentesco y su acceso académico?')) save(()=>api(`/v1/guardians/${encodeURIComponent(guardian)}/${Number(student)}`,{method:'DELETE'}),'Parentesco revocado.');
      }}>Revocar parentesco</button>
    </form></fieldset>
    <fieldset disabled={busy} className="card"><legend>Autorizar contacto</legend><form onSubmit={e=>{e.preventDefault();save(()=>api('/v1/contacts',{method:'POST',json:{first,second}}),'Contacto autorizado.');}}>
      <label>Primer usuario<input required value={first} onChange={e=>setFirst(e.target.value)}/></label><label>Segundo usuario<input required value={second} onChange={e=>setSecond(e.target.value)}/></label><button type="submit">Autorizar contacto</button>
    </form></fieldset>
    <fieldset disabled={busy} className="card"><legend>Restablecer contraseña</legend><form onSubmit={e=>{e.preventDefault();
      if(window.confirm(`¿Restablecer la contraseña de ${target} y cerrar sus sesiones?`)) save(()=>api(`/v1/users/${encodeURIComponent(target)}/reset-password`,{method:'POST',json:{password}}),'Contraseña restablecida.',()=>setPassword(''));
    }}><label>Usuario<input required value={target} onChange={e=>setTarget(e.target.value)}/></label><label>Nueva contraseña<input required type="password" minLength="12" autoComplete="new-password" value={password} onChange={e=>setPassword(e.target.value)}/></label><button type="submit">Restablecer contraseña</button></form></fieldset>
  </section>;
}
