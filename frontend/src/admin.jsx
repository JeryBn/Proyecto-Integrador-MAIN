import {useCallback, useEffect, useState} from 'react';
import {createRoot} from 'react-dom/client';
import './admin.css';
import {CommunityAdmin} from './community-admin.jsx';
import {AcademicAdmin} from './academic-admin.jsx';
const activityScope = document.documentElement.dataset.adminScope === 'activities';
const academicScope = document.documentElement.dataset.adminScope === 'academic';

/* ---------- Acceso a la API ---------- */
let csrf = null;
async function loadCsrf() {
  const r = await fetch('/api/admin/csrf', {credentials: 'same-origin'});
  if (!r.ok) throw new Error('No se pudo obtener el token de seguridad.');
  csrf = await r.json();
}
class ApiError extends Error {
  constructor(status, code, message, fields) {
    super(message); this.status = status; this.code = code; this.fields = fields || {};
  }
}
async function api(path, {method = 'GET', json, form} = {}) {
  const headers = {Accept: 'application/json'};
  let body;
  if (method !== 'GET') {
    if (!csrf) await loadCsrf();
    headers[csrf.headerName] = csrf.token;
    if (json !== undefined) { headers['Content-Type'] = 'application/json'; body = JSON.stringify(json); }
    if (form) body = new URLSearchParams(form);
  }
  let r;
  try { r = await fetch('/api/admin' + path, {method, headers, body, credentials: 'same-origin'}); }
  catch { throw new ApiError(0, 'NETWORK', 'No hay conexión con el servidor.'); }
  const text = await r.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { /* sin JSON */ }
  if (!r.ok) {
    const e = data?.error || {};
    throw new ApiError(r.status, e.code || 'ERROR', e.message || 'Ocurrió un error.', e.fields || data?.fields);
  }
  return data;
}
const sessionLost = x => x.status === 401 || x.status === 403;
const fmtFecha = s => (s ? String(s).replace('T', ' ').slice(0, 16) : '');

/* ---------- Login ---------- */
function Login({notice, onOk}) {
  const [u, setU] = useState('');
  const [p, setP] = useState('');
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(e) {
    e.preventDefault(); setErr(''); setBusy(true);
    try {
      // La página puede seguir abierta tras reiniciar servidor o cambiar sesión en otra pestaña.
      await loadCsrf();
      await api('/login', {method: 'POST', form: {username: u, password: p}});
      await loadCsrf(); // la sesión cambió: hay que pedir un token nuevo
      const perfil = await api('/perfil');
      onOk(perfil.usuario);
    } catch (x) {
      setErr(x.status === 401 ? 'Usuario o contraseña incorrectos.'
        : x.status === 403 ? 'Esta cuenta no tiene acceso de administración.' : x.message);
      setP('');
      try { await loadCsrf(); } catch { /* se reintenta al enviar */ }
    } finally { setBusy(false); }
  }

  return (
    <main className="card narrow">
      <h1>Administración</h1>
      {notice && <p className="ok" role="status">{notice}</p>}
      <form onSubmit={submit}>
        <label htmlFor="u">Usuario</label>
        <input id="u" value={u} onChange={e => setU(e.target.value)} autoComplete="username" required />
        <label htmlFor="p">Contraseña</label>
        <input id="p" type="password" value={p} onChange={e => setP(e.target.value)} autoComplete="current-password" required />
        {err && <p className="err" role="alert">{err}</p>}
        <button type="submit" disabled={busy}>{busy ? 'Ingresando…' : 'Ingresar'}</button>
      </form>
    </main>
  );
}

/* ---------- Listado ---------- */
const PAGE = 10;
function Lista({onOpen, onExpire, reload}) {
  const [off, setOff] = useState(0);
  const [data, setData] = useState(null);
  const [err, setErr] = useState('');

  useEffect(() => {
    let alive = true;
    setData(null); setErr('');
    api(`/actividades?offset=${off}&limit=${PAGE}`)
      .then(d => alive && setData(d))
      .catch(x => { if (alive) (sessionLost(x) ? onExpire() : setErr(x.message)); });
    return () => { alive = false; };
  }, [off, reload]);

  if (err) return <p className="err" role="alert">{err}</p>;
  if (!data) return <p role="status">Cargando actividades…</p>;
  if (data.total === 0) return <p>Aún no hay actividades registradas.</p>;
  const hasPrev = off > 0, hasNext = off + PAGE < data.total;
  return (
    <section>
      <h2>Actividades ({data.total})</h2>
      <div className="scroll">
        <table>
          <thead><tr><th>ID</th><th>Título</th><th>Curso</th><th>Docente</th><th>Fecha límite</th><th><span className="sr">Acciones</span></th></tr></thead>
          <tbody>
            {data.items.map(a => (
              <tr key={a.id}>
                <td>{a.id}</td><td>{a.titulo}</td><td>{a.curso}</td><td>{a.docente}</td><td>{fmtFecha(a.fechaLimite)}</td>
                <td><button className="link" onClick={() => onOpen(a.id)} aria-label={`Ver detalle de ${a.titulo}`}>Ver</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="pager">
        <button disabled={!hasPrev} onClick={() => setOff(off - PAGE)}>Anterior</button>
        <span>{off + 1}–{Math.min(off + PAGE, data.total)} de {data.total}</span>
        <button disabled={!hasNext} onClick={() => setOff(off + PAGE)}>Siguiente</button>
      </div>
    </section>
  );
}

/* ---------- Detalle ---------- */
function Detalle({id, notice, onBack, onExpire}) {
  const [a, setA] = useState(null);
  const [err, setErr] = useState('');
  useEffect(() => {
    let alive = true;
    setA(null); setErr('');
    api(`/actividades/${id}`)
      .then(d => alive && setA(d))
      .catch(x => { if (alive) (sessionLost(x) ? onExpire() : setErr(x.message)); });
    return () => { alive = false; };
  }, [id]);

  return (
    <section>
      <button className="link" onClick={onBack}>← Volver al listado</button>
      {notice && <p className="ok" role="status">{notice}</p>}
      {err && <p className="err" role="alert">{err}</p>}
      {!a && !err && <p role="status">Cargando…</p>}
      {a && (
        <article className="card">
          <h2>{a.titulo}</h2>
          <dl>
            <dt>ID</dt><dd>{a.id}</dd>
            <dt>Curso</dt><dd>{a.curso} (ID {a.cursoId})</dd>
            <dt>Docente</dt><dd>{a.docente}</dd>
            <dt>Fecha límite</dt><dd>{fmtFecha(a.fechaLimite)}</dd>
            <dt>Descripción</dt><dd>{a.descripcion}</dd>
          </dl>
        </article>
      )}
    </section>
  );
}

/* ---------- Formulario ---------- */
const num = v => {
  const s = v.trim();
  if (s === '') return null;
  if (!/^\d+$/.test(s)) return s;            // el servidor responde "Usa un entero JSON"
  const n = Number(s);
  return Number.isSafeInteger(n) ? n : s;
};
function Campo({name, label, error, children}) {
  return (
    <div className="campo">
      <label htmlFor={name}>{label}</label>
      {children}
      {error && <p className="err" id={name + '-err'}>{error}</p>}
    </div>
  );
}
function Formulario({onCreated, onBack, onExpire}) {
  const [cursos, setCursos] = useState(null);
  const [f, setF] = useState({id: '', cursoId: '', titulo: '', descripcion: '', fechaLimite: ''});
  const [fields, setFields] = useState({});
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);
  const set = k => e => setF({...f, [k]: e.target.value});

  useEffect(() => {
    let alive = true;
    api('/cursos?offset=0&limit=100')
      .then(d => alive && setCursos(d.items))
      .catch(x => { if (alive) (sessionLost(x) ? onExpire() : setErr(x.message)); });
    return () => { alive = false; };
  }, []);

  async function submit(e) {
    e.preventDefault(); setErr(''); setFields({}); setBusy(true);
    const body = {
      id: num(f.id), cursoId: num(f.cursoId), titulo: f.titulo, descripcion: f.descripcion,
      fechaLimite: f.fechaLimite.length === 16 ? f.fechaLimite + ':00' : f.fechaLimite
    };
    try {
      const item = await api('/actividades', {method: 'POST', json: body});
      onCreated(item.id);
    } catch (x) {
      if (x.status === 401) return onExpire();
      setFields(x.fields);
      setErr(x.status === 409 ? x.message : Object.keys(x.fields).length ? 'Revisa los campos marcados.' : x.message);
    } finally { setBusy(false); }
  }

  const inv = k => ({'aria-invalid': !!fields[k], 'aria-describedby': fields[k] ? k + '-err' : undefined});
  return (
    <section>
      <button className="link" onClick={onBack}>← Volver al listado</button>
      <h2>Nueva actividad</h2>
      <form className="card" onSubmit={submit} noValidate>
        {err && <p className="err" role="alert">{err}</p>}
        <Campo name="id" label="ID de la actividad (entero positivo)" error={fields.id}>
          <input id="id" inputMode="numeric" value={f.id} onChange={set('id')} {...inv('id')} />
        </Campo>
        <Campo name="cursoId" label="Curso" error={fields.cursoId}>
          <select id="cursoId" value={f.cursoId} onChange={set('cursoId')} disabled={!cursos} {...inv('cursoId')}>
            <option value="">{cursos ? 'Selecciona un curso' : 'Cargando cursos…'}</option>
            {(cursos || []).map(c => <option key={c.id} value={c.id}>{c.nombre} — {c.docente}</option>)}
          </select>
        </Campo>
        <Campo name="titulo" label="Título (máx. 150)" error={fields.titulo}>
          <input id="titulo" value={f.titulo} onChange={set('titulo')} {...inv('titulo')} />
        </Campo>
        <Campo name="descripcion" label="Descripción (máx. 2000)" error={fields.descripcion}>
          <textarea id="descripcion" rows="4" value={f.descripcion} onChange={set('descripcion')} {...inv('descripcion')} />
        </Campo>
        <Campo name="fechaLimite" label="Fecha límite (hora de Lima)" error={fields.fechaLimite}>
          <input id="fechaLimite" type="datetime-local" value={f.fechaLimite} onChange={set('fechaLimite')} {...inv('fechaLimite')} />
        </Campo>
        <button type="submit" disabled={busy}>{busy ? 'Guardando…' : 'Crear actividad'}</button>
      </form>
    </section>
  );
}

/* ---------- App ---------- */
function App() {
  const [auth, setAuth] = useState('checking');
  const [user, setUser] = useState('');
  const [notice, setNotice] = useState('');
  const [view, setView] = useState({name: 'list'});
  const [reload, setReload] = useState(0);

  const expire = useCallback(() => {
    setAuth('out'); setView({name: 'list'}); setNotice('Tu sesión expiró. Ingresa de nuevo.');
    loadCsrf().catch(() => {});
  }, []);

  useEffect(() => {
    (async () => {
      try { await loadCsrf(); const p = await api('/perfil'); setUser(p.usuario); setAuth('in'); }
      catch { setAuth('out'); }
    })();
  }, []);

  async function logout() {
    setNotice('');
    try {
      // Otra pestaña puede haber renovado la sesión y el token CSRF.
      await loadCsrf();
      try { await api('/logout', {method: 'POST'}); }
      catch (x) {
        if (x.status !== 403) throw x;
        await loadCsrf();
        await api('/logout', {method: 'POST'});
      }
    } catch (x) {
      if (x.status !== 401) {
        setNotice('No se pudo confirmar el cierre de sesión. Comprueba la conexión y vuelve a intentarlo.');
        return;
      }
    }
    setAuth('out'); setView({name: 'list'}); setNotice('Sesión cerrada.');
    try { await loadCsrf(); } catch { /* se reintenta al ingresar */ }
  }

  if (auth === 'checking') return <p role="status" className="card narrow">Cargando…</p>;
  if (auth === 'out') return <Login notice={notice} onOk={u => { setUser(u); setNotice(''); setAuth('in'); }} />;

  return (
    <>
      <header>
        <strong>Administración · MAIN</strong>
        <nav>
          <button className="link" onClick={() => { setNotice(''); setView({name: 'list'}); setReload(reload + 1); }}>Actividades</button>
          <button className="link" onClick={() => { setNotice(''); setView({name: 'new'}); }}>Nueva actividad</button>
          {!activityScope && <>{!academicScope && <button className="link" onClick={() => setView({name: 'community'})}>Cuentas y vínculos</button>}
          <button className="link" onClick={() => setView({name: 'academic'})}>Cursos y matrículas</button>
          {!academicScope && <a href="/moderation.html">Revisar contenido</a>}</>}
        </nav>
        <span>{user}</span>
        <button onClick={logout}>Cerrar sesión</button>
      </header>
      <main>
        {view.name === 'academic' && <AcademicAdmin api={api} onExpire={expire} />}
        {notice && view.name !== 'detail' && <p role="status">{notice}</p>}
        {view.name === 'community' && <CommunityAdmin api={api} onExpire={expire} currentUser={user} />}
        {view.name === 'list' && <Lista reload={reload} onExpire={expire} onOpen={id => { setNotice(''); setView({name: 'detail', id}); }} />}
        {view.name === 'detail' && <Detalle id={view.id} notice={notice} onExpire={expire} onBack={() => { setNotice(''); setView({name: 'list'}); setReload(reload + 1); }} />}
        {view.name === 'new' && <Formulario onExpire={expire} onBack={() => setView({name: 'list'})}
          onCreated={id => { setNotice('Actividad creada correctamente.'); setView({name: 'detail', id}); }} />}
      </main>
    </>
  );
}

createRoot(document.getElementById('root')).render(<App />);
