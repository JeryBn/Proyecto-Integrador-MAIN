import {useCallback, useEffect, useState} from 'react';
import {createRoot} from 'react-dom/client';
import './admin.css';
import './admin-design.css';
import {CommunityAdmin} from './community-admin.jsx';
import {AcademicAdmin} from './academic-admin.jsx';
import {ReviewerPortal} from './reviewer-portal.jsx';
import {AppErrorBoundary, ConnectionStatus} from './resilience.jsx';
import {api, loadCsrf} from './admin-api.mjs';

/* ---------- Acceso a la API ---------- */
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
      onOk(perfil);
    } catch (x) {
      setErr(x.status === 401 ? 'Usuario o contraseña incorrectos.'
        : x.status === 403 ? 'Esta cuenta no tiene acceso de administración.' : x.message);
      setP('');
      if (x.status) {
        try { await loadCsrf(); } catch { /* se reintenta al enviar */ }
      }
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
function Lista({onOpen, onExpire, reload, onNew}) {
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
      <div className="section-heading"><div><span className="eyebrow">GESTIÓN ACADÉMICA</span><h2>Actividades <span className="count-badge">{data.total}</span></h2><p>Organiza las actividades y consulta la información de tus cursos.</p></div><button className="primary" onClick={onNew}>+ Nueva actividad</button></div>
      <div className="scroll">
        <table>
          <thead><tr><th>ID</th><th>Título</th><th>Curso</th><th>Docente</th><th>Fecha límite</th><th><span className="sr">Acciones</span></th></tr></thead>
          <tbody>
            {data.items.map(a => (
              <tr key={a.id}>
                <td className="record-id">{a.id}</td><td className="activity-title">{a.titulo}</td><td>{a.curso}</td><td>{a.docente}</td><td className="deadline-cell">{fmtFecha(a.fechaLimite)}</td>
                <td><button className="link detail-link" onClick={() => onOpen(a.id)} aria-label={`Ver detalle de ${a.titulo}`}>Ver detalle <span aria-hidden="true">↗</span></button></td>
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
function NavIcon({kind}) {
  const paths = {
    list:'M8 6h12M8 12h12M8 18h12M3 6h.01M3 12h.01M3 18h.01',
    new:'M12 5v14M5 12h14',
    community:'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M18 8a4 4 0 0 1 0 8M22 21v-2a4 4 0 0 0-3-3.87M13 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0',
    academic:'M3 4h7a3 3 0 0 1 3 3v14a4 4 0 0 0-4-2H3ZM21 4h-5a3 3 0 0 0-3 3v14a4 4 0 0 1 4-2h4Z',
    review:'m12 3 8 4v5c0 5-8 9-8 9s-8-4-8-9V7Zm-4 9 3 3 5-6'
  };
  return <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={paths[kind]}/></svg>;
}
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
      try { await loadCsrf(); const p = await api('/perfil'); setUser(p); setAuth('in'); }
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
  if (user.rol==='REVIEWER'||user.mustChangePassword) return <ReviewerPortal profile={user} api={api} onUpdated={setUser} onLogout={logout} notice={notice}/>;

  return (
    <div className="admin-shell">
      <a className="skip-link" href="#admin-content">Saltar al contenido</a>
      <header className="admin-sidebar">
        <div className="admin-brand"><span className="brand-mark" aria-hidden="true">M</span><div><strong>MAIN<span className="brand-dot">.</span></strong><small>Portal administrativo</small></div></div>
        <p className="nav-label">ESPACIO DE TRABAJO</p>
        <nav aria-label="Administración">
          <button className="link" aria-current={['list','detail'].includes(view.name)?'page':undefined} onClick={() => { setNotice(''); setView({name: 'list'}); setReload(reload + 1); }}><NavIcon kind="list"/>Actividades</button>
          <button className="link" aria-current={view.name==='new'?'page':undefined} onClick={() => { setNotice(''); setView({name: 'new'}); }}><NavIcon kind="new"/>Nueva actividad</button>
          <button className="link" aria-current={view.name==='community'?'page':undefined} onClick={() => setView({name: 'community'})}><NavIcon kind="community"/>Cuentas y vínculos</button>
          <button className="link" aria-current={view.name==='academic'?'page':undefined} onClick={() => setView({name: 'academic'})}><NavIcon kind="academic"/>Cursos y matrículas</button>
          <a href="/moderation.html"><NavIcon kind="review"/>Revisar contenido</a>
        </nav>
        <div className="sidebar-note"><span aria-hidden="true">✦</span><strong>Todo conectado</strong><p>Actividades, cursos y comunidad en un mismo espacio.</p></div>
        <div className="sidebar-account"><span className="user-avatar" aria-hidden="true">{user.usuario?.slice(0,1).toUpperCase()}</span><div><strong>{user.usuario}</strong><small>Administración</small></div></div>
        <button className="logout-button" onClick={logout}>Cerrar sesión <span aria-hidden="true">↗</span></button>
      </header>
      <div className="admin-workspace">
      <div className="admin-topbar" role="region" aria-label="Contexto administrativo"><span>MAIN <span className="breadcrumb-divider">/</span> Administración</span><span className="workspace-tag"><span aria-hidden="true"/>Espacio académico</span></div>
      <main id="admin-content" tabIndex={-1}>
        <h1 className="sr">Administración MAIN</h1>
        {view.name === 'academic' && <AcademicAdmin api={api} onExpire={expire} />}
        {notice && view.name !== 'detail' && <p role="status">{notice}</p>}
        {view.name === 'community' && <CommunityAdmin api={api} onExpire={expire} currentUser={user.usuario} />}
        {view.name === 'list' && <Lista reload={reload} onExpire={expire} onNew={()=>{setNotice('');setView({name:'new'});}} onOpen={id => { setNotice(''); setView({name: 'detail', id}); }} />}
        {view.name === 'detail' && <Detalle id={view.id} notice={notice} onExpire={expire} onBack={() => { setNotice(''); setView({name: 'list'}); setReload(reload + 1); }} />}
        {view.name === 'new' && <Formulario onExpire={expire} onBack={() => setView({name: 'list'})}
          onCreated={id => { setNotice('Actividad creada correctamente.'); setView({name: 'detail', id}); }} />}
      </main>
      <footer className="admin-footer">MAIN · Gestión académica <span>Un espacio para aprender y conectar.</span></footer>
      </div>
    </div>
  );
}

createRoot(document.getElementById('root')).render(
  <AppErrorBoundary><ConnectionStatus/><App /></AppErrorBoundary>
);
