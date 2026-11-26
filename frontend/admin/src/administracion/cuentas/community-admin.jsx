import {AccountInvitations} from '../../diseno/account-invitations.jsx';
import { useEffect, useRef, useState } from 'react';

export function CommunityAdmin({ api, onExpire, currentUser, focusCreate = false }) {
  const [users, setUsers] = useState([]),
    [offset, setOffset] = useState(0);
  const [error, setError] = useState(''),
    [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false),
    [loading, setLoading] = useState(true);
  const createAccount = useRef(null), didFocusCreate = useRef(false);
  useEffect(() => {
    if (!focusCreate || loading || didFocusCreate.current || !createAccount.current) return;
    didFocusCreate.current = true;
    createAccount.current.scrollIntoView({behavior: 'instant', block: 'start'});
    createAccount.current.querySelector('input')?.focus({preventScroll: true});
  }, [focusCreate, loading]);
  const [archived, setArchived] = useState(false);
  const [pending, setPending] = useState(null);
  const [showPassword, setShowPassword] = useState(false);
  const dialog = useRef(null);
  useEffect(() => { if (pending && dialog.current && !dialog.current.open) dialog.current.showModal(); }, [pending]);
  function selectAction(user, action) {
    setError(''); setNotice(''); setPassword(''); setShowPassword(false);
    setPending({user, action});
  }
  const [revision, setRevision] = useState(0),
    [reports, setReports] = useState([]);
  const [account, setAccount] = useState({
    username: '',
    name: '',
    role: 'ALUMNO',
    password: '',
    academicId: '',
    activationRequired: true,
  });
  const [guardian, setGuardian] = useState(''),
    [student, setStudent] = useState('');
  const [first, setFirst] = useState(''),
    [second, setSecond] = useState('');
  const [password, setPassword] = useState('');
  const fail = (e) => {
    if (e.status === 401) onExpire();
    else setError(e.message);
  };
  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    Promise.all([
      api(`/v1/users?offset=${offset}&limit=20&archived=${archived}`),
      api(`/v1/reports?offset=${offset}&limit=20`),
    ])
      .then(([u, r]) => {
        if (active) {
          setUsers(u);
          setReports(r);
        }
      })
      .catch((e) => {
        if (active) fail(e);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [offset, revision, archived]);
  async function save(action, message, clear) {
    setBusy(true);
    setError('');
    setNotice('');
    try {
      await action();
      clear?.();
      setNotice(message);
      setRevision((v) => v + 1);
    } catch (e) {
      fail(e);
    } finally {
      setBusy(false);
    }
  }
  const field = (key, label, type = 'text') => (
    <label>
      {label}
      <input
        type={type}
        value={account[key]}
        required
        minLength={key === 'password' ? 12 : key === 'username' ? 3 : undefined}
        maxLength={key === 'username' ? 60 : key === 'name' ? 100 : undefined}
        autoComplete={type === 'password' ? 'new-password' : 'off'}
        onChange={(e) => setAccount({ ...account, [key]: e.target.value })}
      />
    </label>
  );
  return (
    <section aria-label="Administración de la comunidad">
      <h2>Cuentas y vínculos</h2>
      <p>
        Las cuentas, contactos y parentescos se comparten con la app. Verifica la identidad antes de
        autorizar un vínculo.
      </p>
      <AccountInvitations api={api} users={users} onExpire={onExpire}/>
      <label>Listado de cuentas<select value={archived ? 'archived' : 'current'} onChange={e=>{setArchived(e.target.value==='archived');setOffset(0);}}><option value="current">Cuentas registradas</option><option value="archived">Cuentas retiradas</option></select></label>
      {pending && <dialog ref={dialog} className="account-dialog" aria-labelledby="account-action-title" onCancel={e=>{if(busy)e.preventDefault();else setPending(null);}}>
        <form onSubmit={e=>{
          e.preventDefault();
          const messages={enable:'Cuenta habilitada.',disable:'Cuenta deshabilitada.',archive:'Cuenta retirada del listado. Se conservaron sus notas e historial.',restore:'Cuenta restaurada al listado. Habilítala para permitir el ingreso.','reset-password':'Contraseña restablecida.'};
          save(()=>api(`/v1/users/${encodeURIComponent(pending.user.username)}/${pending.action}`,{method:'POST',...(pending.action==='reset-password'?{json:{password}}:{})}),messages[pending.action],()=>{setPending(null);setPassword('');});
        }}>
          <h3 id="account-action-title">{{enable:'Habilitar cuenta',disable:'Deshabilitar cuenta',archive:'Retirar cuenta del listado',restore:'Restaurar cuenta','reset-password':'Restablecer contraseña'}[pending.action]}</h3>
          <p><strong>{pending.user.name}</strong> · {pending.user.username}</p>
          {pending.action==='archive'&&<p>Se bloqueará su acceso y saldrá de Cuentas registradas. Sus notas, matrículas e historial se conservarán. Podrás encontrarla en Cuentas retiradas.</p>}
          {pending.action==='disable'&&<p>Esta cuenta perderá el acceso. Podrás habilitarla nuevamente desde la lista.</p>}
          {pending.action==='restore'&&<p>Volverá a la lista de cuentas registradas y permanecerá deshabilitada hasta que la habilites.</p>}
          {pending.action==='enable'&&<p>Esta cuenta podrá volver a ingresar al portal.</p>}
          {pending.action==='reset-password'&&<><p>La contraseña anterior no se puede consultar. Establece una nueva; las sesiones anteriores perderán el acceso.</p><label>Nueva contraseña<input required type={showPassword?'text':'password'} minLength="12" autoComplete="new-password" value={password} onChange={e=>setPassword(e.target.value)}/></label><label><input type="checkbox" checked={showPassword} onChange={e=>setShowPassword(e.target.checked)}/> Mostrar nueva contraseña</label></>}
          {error&&<p role="alert" className="err">{error}</p>}
          <div className="account-actions"><button type="button" disabled={busy} autoFocus onClick={()=>{setPending(null);setPassword('');}}>Cancelar</button><button type="submit" disabled={busy}>{busy?'Guardando…':'Confirmar'}</button></div>
        </form>
      </dialog>}
      {error && (
        <p role="alert" className="err">
          {error}
        </p>
      )}
      {notice && (
        <p role="status" className="ok">
          {notice}
        </p>
      )}
      {loading ? (
        <p role="status">Cargando comunidad…</p>
      ) : (
        <>
          <div className="scroll">
            <table>
              <caption>Cuentas registradas</caption>
              <thead>
                <tr>
                  <th>Usuario</th>
                  <th>Nombre</th>
                  <th>Rol</th>
                  <th>Estado</th>
                  <th>Acción</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.username}>
                    <td>{u.username}</td>
                    <td>{u.name}</td>
                    <td>{u.role}</td>
                    <td>{u.archived ? 'Retirada' : u.enabled ? 'Activa' : 'Deshabilitada'}</td>
                    <td>
                      {!u.archived&&<button
                        disabled={
                          busy ||
                          ['ADMIN', 'REVIEWER'].includes(u.role) ||
                          u.username === currentUser
                        }
                        onClick={() => {
                          if (
                            !u.archived
                          )
                            selectAction(u,u.enabled ? 'disable' : 'enable');
                        }}
                      >
                        {u.enabled ? 'Deshabilitar' : 'Habilitar'}
                      </button>}
                      {!['ADMIN','REVIEWER'].includes(u.role)&&u.username!==currentUser&&<div className="account-actions">
                        {!u.archived&&<button disabled={busy||!u.enabled} onClick={()=>selectAction(u,'reset-password')}>Restablecer contraseña</button>}
                        <button disabled={busy} onClick={()=>selectAction(u,u.archived?'restore':'archive')}>{u.archived?'Restaurar':'Retirar del listado'}</button>
                      </div>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {!users.length && <p>No hay cuentas en esta página.</p>}
          <h3>Reportes de contactos</h3>
          {!reports.length ? (
            <p>No hay reportes en esta página.</p>
          ) : (
            <ul>
              {reports.map((r, i) => (
                <li key={i}>
                  <dl>
                    {Object.entries(r).map(([k, v]) => (
                      <div key={k}>
                        <dt>{k}</dt>
                        <dd>{String(v ?? '')}</dd>
                      </div>
                    ))}
                  </dl>
                </li>
              ))}
            </ul>
          )}
          <div className="pager">
            <button disabled={busy || offset === 0} onClick={() => setOffset(offset - 20)}>
              Anterior
            </button>
            <span>Página {offset / 20 + 1}</span>
            <button
              disabled={busy || (users.length < 20 && reports.length < 20)}
              onClick={() => setOffset(offset + 20)}
            >
              Siguiente
            </button>
          </div>
        </>
      )}
      <fieldset disabled={busy} className="card" id="create-school-account" ref={createAccount} tabIndex={-1}>
        <legend>Crear cuenta</legend>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            const id = Number(account.academicId);
            if (account.role !== 'FAMILIAR' && (!Number.isSafeInteger(id) || id < 1)) {
              setError('Usa un identificador académico entero positivo.');
              return;
            }
            save(
              () =>
                api('/v1/users', {
                  method: 'POST',
                  json: {
                    username: account.username,
                    name: account.name,
                    role: account.role,
                    password: account.activationRequired ? Array.from(crypto.getRandomValues(new Uint8Array(24)),v=>v.toString(16).padStart(2,'0')).join('') : account.password,
                    activationRequired: account.activationRequired,
                    alumnoId: account.role === 'ALUMNO' ? id : null,
                    docenteId: account.role === 'DOCENTE' ? id : null,
                  },
                }),
              account.activationRequired ? 'Cuenta creada pendiente de activación. Selecciónala en Activación y recuperación para generar su invitación.' : 'Cuenta creada.',
              () =>
                setAccount({ ...account, password: '', username: '', name: '', academicId: '' }),
            );
          }}
        >
          {field('username', 'Usuario')}
          {field('name', 'Nombre completo')}
          <label>
            Rol
            <select
              value={account.role}
              onChange={(e) => setAccount({ ...account, role: e.target.value, academicId: '' })}
            >
              <option value="ALUMNO">Estudiante</option>
              <option value="DOCENTE">Docente</option>
              <option value="FAMILIAR">Familiar</option>
            </select>
          </label>
          {account.role !== 'FAMILIAR' && field('academicId', 'Identificador académico', 'number')}
          <label className="account-contact-check"><input type="checkbox" checked={account.activationRequired} onChange={e=>setAccount({...account,activationRequired:e.target.checked})}/> La persona debe activar su cuenta y elegir su propia contraseña</label>
          {!account.activationRequired&&<>{field('password', 'Contraseña inicial (12 caracteres como mínimo)', 'password')}<p>{account.password.length} caracteres escritos · mínimo 12.</p></>}
          {error && <p role="alert" className="err">{error}</p>}
          {notice && <p role="status" className="ok">{notice}</p>}
          <button type="submit">{busy ? 'Creando cuenta…' : 'Crear cuenta'}</button>
        </form>
      </fieldset>
      <fieldset disabled={busy} className="card">
        <legend>Parentesco verificado</legend>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            const id = Number(student);
            if (!Number.isSafeInteger(id) || id < 1) {
              setError('Identificador de estudiante inválido.');
              return;
            }
            save(
              () => api('/v1/guardians', { method: 'POST', json: { guardian, studentId: id } }),
              'Parentesco verificado.',
            );
          }}
        >
          <label>
            Usuario familiar
            <input required value={guardian} onChange={(e) => setGuardian(e.target.value)} />
          </label>
          <label>
            ID del estudiante
            <input
              required
              type="number"
              min="1"
              step="1"
              value={student}
              onChange={(e) => setStudent(e.target.value)}
            />
          </label>
          <button type="submit">Verificar parentesco</button>
          <button
            type="button"
            onClick={() => {
              if (!guardian || !Number.isSafeInteger(Number(student)) || Number(student) < 1) {
                setError('Indica familiar y estudiante válidos.');
                return;
              }
              if (window.confirm('¿Revocar este parentesco y su acceso académico?'))
                save(
                  () =>
                    api(`/v1/guardians/${encodeURIComponent(guardian)}/${Number(student)}`, {
                      method: 'DELETE',
                    }),
                  'Parentesco revocado.',
                );
            }}
          >
            Revocar parentesco
          </button>
        </form>
      </fieldset>
      <fieldset disabled={busy} className="card">
        <legend>Autorizar contacto</legend>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            save(
              () => api('/v1/contacts', { method: 'POST', json: { first, second } }),
              'Contacto autorizado.',
            );
          }}
        >
          <label>
            Primer usuario
            <input required value={first} onChange={(e) => setFirst(e.target.value)} />
          </label>
          <label>
            Segundo usuario
            <input required value={second} onChange={(e) => setSecond(e.target.value)} />
          </label>
          <button type="submit">Autorizar contacto</button>
        </form>
      </fieldset>
    </section>
  );
}
