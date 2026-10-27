import {useState} from 'react';
import {useResource,Feedback} from './user-hooks.jsx';
import {userApi} from './user-api.mjs';
export function ReminderSettings({active}){
 const resource=useResource('/api/v1/reminders/preferences',{enabled:active});
 const [busy,setBusy]=useState(false),[error,setError]=useState('');
 async function save(value){setBusy(true);setError('');try{await userApi.call('/api/v1/reminders/preferences',{method:'PUT',json:value});resource.reload();}catch(error){setError(error.message);}finally{setBusy(false);}}
 if(!active)return null;
 const value=resource.data;
 return <section aria-label="Recordatorios de tareas"><h2>Recordatorios de tareas</h2><Feedback resource={resource}/>{error&&<p role="alert">{error}</p>}
  {value&&<fieldset disabled={busy||resource.loading}><legend>Avisarme antes del vencimiento</legend>
   <label><input type="checkbox" checked={value.enabled} onChange={e=>save({...value,enabled:e.target.checked})}/> Activar recordatorios</label>
   <label>Anticipación <select value={value.minutesBefore} onChange={e=>save({...value,minutesBefore:Number(e.target.value)})}><option value="15">15 minutos</option><option value="60">Una hora</option><option value="1440">Un día</option></select></label>
  </fieldset>}
  <p>Solo tareas pendientes de tus cursos. Activa también Avisos dentro de MAIN. Los avisos del teléfono requieren push configurado y permiso.</p>
 </section>;
}
