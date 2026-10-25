import {useEffect,useRef,useState} from 'react';
import {createCaptureSession} from './capture-session.mjs';
import {fileError} from './user-api.mjs';

export function MediaCapture({onFile,disabled=false}){
 const [kind,setKind]=useState(null),[error,setError]=useState(''),[stream,setStream]=useState(null);
 const camera=useRef(null),session=useRef(null),alive=useRef(true),callback=useRef(onFile);callback.current=onFile;
 useEffect(()=>{
   alive.current=true;
   session.current=createCaptureSession({getMedia:constraints=>navigator.mediaDevices.getUserMedia(constraints),Recorder:window.MediaRecorder,
    onStream:value=>{if(alive.current)setStream(value);},onError:message=>{if(alive.current){setError(message);setKind(null);}},
    onFile:file=>{if(!alive.current)return;const invalid=fileError(file);if(invalid)setError(invalid);else callback.current(file);setKind(null);}});
   const hide=()=>{if(document.hidden){session.current.cancel();setKind(null);}};
   document.addEventListener('visibilitychange',hide);
   return()=>{alive.current=false;session.current.cancel();document.removeEventListener('visibilitychange',hide);};
 },[]);
 useEffect(()=>{if(camera.current)camera.current.srcObject=stream;},[stream,kind]);
 useEffect(()=>{if(disabled){session.current?.cancel();setKind(null);}},[disabled]);
 const supported=window.isSecureContext&&navigator.mediaDevices?.getUserMedia;
 function begin(value){setError('');setKind(value);session.current.start(value);}
 function cancel(){session.current.cancel();setKind(null);}
 function photo(){
   const video=camera.current;if(!video?.videoWidth){setError('Espera a que la cámara muestre una imagen.');return;}
   const canvas=document.createElement('canvas');canvas.width=video.videoWidth;canvas.height=video.videoHeight;
   canvas.getContext('2d').drawImage(video,0,0);const current=session.current;
   canvas.toBlob(blob=>{if(!alive.current||camera.current?.srcObject!==stream||stream.getTracks().every(track=>track.readyState==='ended'))return;
     if(!blob){setError('No se pudo preparar la foto.');return;}
     const file=new File([blob],'foto-main.jpg',{type:'image/jpeg'}),invalid=fileError(file);
     if(invalid){setError(invalid);return;}current.cancel();setKind(null);callback.current(file);
   },'image/jpeg',.85);
 }
 return <div className="capture-panel" role="group" aria-label="Capturar contenido">
  <p>Captura opcional. El navegador pedirá permiso al comenzar. Revisa el adjunto antes de enviarlo; seguirá sujeto a moderación.</p>
  {!supported?<p>Cámara y micrófono requieren HTTPS y un navegador compatible. Puedes seleccionar un archivo.</p>:!kind?<div className="toolbar">
   <button type="button" disabled={disabled} onClick={()=>begin('photo')}>Tomar foto</button>
   <button type="button" disabled={disabled} onClick={()=>begin('video')}>Grabar video con audio</button>
   <button type="button" disabled={disabled} onClick={()=>begin('audio')}>Grabar audio</button>
  </div>:<>
   <p role="status">{stream?(kind==='photo'?'Cámara lista.':'Grabando; se detiene a los 55 segundos o 12 MB.'):'Esperando permiso del navegador…'}</p>
   {stream&&kind!=='audio'&&<video ref={camera} className="private-media" autoPlay muted playsInline aria-label="Vista previa de cámara"/>}
   {stream&&(kind==='photo'?<button type="button" onClick={photo}>Guardar foto como adjunto</button>:<button type="button" onClick={()=>session.current.finish()}>Terminar grabación</button>)}
   <button type="button" onClick={cancel}>Cancelar captura</button>
  </>}{error&&<p role="alert">{error}</p>}
 </div>;
}
