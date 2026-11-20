import {useEffect,useState} from 'react';
import {userApi} from '../datos/user-api.mjs';
import {useResource} from '../datos/user-hooks.jsx';

const base='/api/v1/contact-codes';
const sixDigits=/^[0-9]{6}$/;

export function ContactCodes({active,canAdd,onAccepted}) {
  const [own,setOwn]=useState(null),[code,setCode]=useState('');
  const [busy,setBusy]=useState(false),[feedback,setFeedback]=useState(null);
  const requests=useResource(base+'/requests',{enabled:active&&canAdd,interval:5000});
  useEffect(()=>{
    if(!active)return;
    const controller=new AbortController();
    userApi.call(base+'/mine',{method:'POST',json:{},signal:controller.signal})
      .then(value=>{if(!controller.signal.aborted)setOwn(value);})
      .catch(error=>{if(!controller.signal.aborted)setFeedback({error:true,text:error.message});});
    return ()=>controller.abort();
  },[active]);

  async function run(action,success){
    setBusy(true);setFeedback(null);
    try{setFeedback(await action()||{text:success});}
    catch(error){setFeedback({error:true,text:error.message});}
    finally{setBusy(false);}
  }

  async function readImage(file){
    if(!file)return;
    await run(async()=>{
      if(file.size>5*1024*1024)throw new Error('Elige una imagen de hasta 5 MB.');
      const image=await createImageBitmap(file,{resizeWidth:1024});
      try{
        const matches=await new globalThis.BarcodeDetector({formats:['qr_code']}).detect(image);
        const value=matches.find(item=>sixDigits.test(item.rawValue))?.rawValue;
        if(!value)throw new Error('No se encontró un código de contacto.');
        setCode(value);
      }finally{image.close();}
    },'QR leído. Confirma el envío.');
  }

  return <section className="workspace-card contacts-card">
    <h2>Agregar compañeros</h2>
    {own&&<details><summary>Mi código: <strong>{own.code}</strong></summary>
      <svg width="144" height="144" viewBox={`0 0 ${own.qr.length} ${own.qr.length}`} role="img" aria-label="QR de mi código de contacto" shapeRendering="crispEdges" style={{background:'white'}}>
        <path fill="black" d={own.qr.flatMap((row,y)=>Array.from(row,(pixel,x)=>pixel==='1'?`M${x} ${y}h1v1h-1z`:'')).join('')}/>
      </svg>
    </details>}
    {canAdd&&<form onSubmit={event=>{
      event.preventDefault();
      if(!sixDigits.test(code)||busy)return;
      run(async()=>{
        const {status}=await userApi.call(base+'/requests',{method:'POST',json:{code}});
        switch(status){
          case 'SENT':setCode('');return {text:'Solicitud enviada. Tu compañero puede aceptarla en Avisos.'};
          case 'PENDING':return {text:'Ya enviaste una solicitud. Está pendiente de respuesta.'};
          case 'INCOMING':requests.reload();return {text:'Este compañero ya te envió una solicitud. Puedes aceptarla abajo.'};
          case 'ALREADY_CONTACT':return {text:'Este compañero ya está en tus contactos.'};
          case 'OWN_CODE':throw new Error('Ese es tu propio código.');
          default:throw new Error('No se pudo enviar la solicitud. Revisa que el código sea de un estudiante de tu institución.');
        }
      });
    }}>
      <label>Código de 6 dígitos <input inputMode="numeric" pattern="[0-9]{6}" value={code}
        onChange={event=>setCode(event.target.value.replace(/[^0-9]/g,'').slice(0,6))}/></label>
      <button disabled={busy||!sixDigits.test(code)}>Enviar solicitud</button>
      {typeof globalThis.BarcodeDetector==='function'&&<label>Leer QR de una imagen
        <input type="file" accept="image/*" disabled={busy} onChange={event=>{readImage(event.target.files[0]);event.target.value='';}}/>
      </label>}
    </form>}
    {feedback&&<p role={feedback.error?'alert':'status'}>{feedback.text}</p>}
    {requests.error&&<p role="alert">{requests.error} <button onClick={requests.reload}>Reintentar</button></p>}
    {canAdd&&<h3>Solicitudes recibidas</h3>}
    {requests.data?.length===0&&<p>No tienes solicitudes pendientes.</p>}
    {!!requests.data?.length&&<ul>{requests.data.map(request=><li key={request.sender}>
      <strong>{request.name}</strong>{[true,false].map(accept=><button key={String(accept)} disabled={busy}
        onClick={()=>run(async()=>{
          await userApi.call(base+'/requests/'+encodeURIComponent(request.sender),{method:'POST',json:{accept}});
          requests.reload();if(accept)onAccepted();
        },accept?'Contacto agregado.':'Solicitud rechazada.')}>{accept?'Aceptar':'Rechazar'}</button>)}
    </li>)}</ul>}
  </section>;
}
