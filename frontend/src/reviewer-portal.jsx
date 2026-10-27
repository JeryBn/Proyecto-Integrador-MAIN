import {useState} from 'react';
export function ReviewerPortal({profile,api,onUpdated,onLogout,notice}){
 const [current,setCurrent]=useState(''),[next,setNext]=useState(''),[repeat,setRepeat]=useState(''),[busy,setBusy]=useState(false),[error,setError]=useState(''),[saved,setSaved]=useState(false);
 async function submit(event){event.preventDefault();if(busy)return;setError('');setSaved(false);
  if(next!==repeat){setError('Las contraseñas nuevas no coinciden.');return;}
  setBusy(true);
  try{const value=await api('/reviewer/password',{method:'POST',json:{currentPassword:current,newPassword:next}});setCurrent('');setNext('');setRepeat('');onUpdated(value);setSaved(true);}
  catch(error){setError(error.message);}finally{setBusy(false);}
 }
 return <main className="card narrow"><h1>Revisión de contenido</h1><p>{profile.nombre} · {profile.usuario}</p>
  <p>Este acceso permite revisar contenido y resolver apelaciones. No permite gestionar notas, matrículas, usuarios ni consultar la auditoría general.</p>
  {notice&&<p role="status">{notice}</p>}{saved&&<p role="status">Contraseña personal guardada. Ya puedes entrar a revisión.</p>}
  {profile.mustChangePassword?<p>Antes de revisar contenido, sustituye la clave temporal por una contraseña personal. No compartas tu contraseña con otro revisor.</p>:<p><a href="/moderation.html">Entrar a moderación y apelaciones</a></p>}
  <form onSubmit={submit}><h2>{profile.mustChangePassword?'Configura tu contraseña':'Cambiar mi contraseña'}</h2>
   <label>Contraseña actual o temporal<input type="password" autoComplete="current-password" value={current} onChange={e=>setCurrent(e.target.value)} required disabled={busy}/></label>
   <label>Nueva contraseña personal<input type="password" autoComplete="new-password" minLength={12} maxLength={72} value={next} onChange={e=>setNext(e.target.value)} required disabled={busy}/></label>
   <label>Repetir contraseña nueva<input type="password" autoComplete="new-password" minLength={12} maxLength={72} value={repeat} onChange={e=>setRepeat(e.target.value)} required disabled={busy}/></label>
   <p>Usa al menos doce caracteres y una contraseña distinta de la temporal. Las contraseñas no se guardan en esta página ni en el navegador por la aplicación.</p>
   {error&&<p role="alert" className="err">{error}</p>}<button disabled={busy}>{busy?'Guardando…':'Guardar contraseña personal'}</button>
  </form><button onClick={onLogout} disabled={busy}>Cerrar sesión</button>
 </main>;
}
