export const captureLimit=12*1024*1024;
export function captureError(error){
 return error?.name==='NotAllowedError'?'No se autorizó la cámara o el micrófono. Puedes adjuntar un archivo.':
 error?.name==='NotFoundError'?'No hay cámara o micrófono disponible. Puedes adjuntar un archivo.':
 error?.message||'No se pudo iniciar la captura.';
}
export function createCaptureSession({getMedia,Recorder,onStream,onFile,onError,
 setTimer=(fn,ms)=>setTimeout(fn,ms),clearTimer=id=>clearTimeout(id)}){
 let generation=0,stream=null,recorder=null,timer=null,active=false,parts=[],bytes=0;
 function stopTracks(value){value?.getTracks().forEach(track=>track.stop());}
 function cancel(){generation++;active=false;if(timer!==null)clearTimer(timer);timer=null;
   const old=recorder;recorder=null;if(old&&old.state!=='inactive')old.stop();stopTracks(stream);stream=null;parts=[];onStream(null);}
 async function start(kind){
   cancel();const request=++generation;active=true;
   try{
     const next=await getMedia({video:kind!=='audio'?{facingMode:'environment',width:{ideal:1280},height:{ideal:720}}:false,audio:kind!=='photo'});
     if(request!==generation){stopTracks(next);return;}
     stream=next;onStream(next);
     if(kind==='photo')return;
     if(!Recorder)throw Error('Este navegador no permite grabar. Puedes adjuntar un archivo.');
     const types=kind==='audio'?['audio/webm;codecs=opus','audio/mp4']:['video/webm;codecs=vp8,opus','video/mp4'];
     const mimeType=types.find(type=>Recorder.isTypeSupported(type));
     if(!mimeType)throw Error('No hay un formato de grabación compatible. Puedes adjuntar un archivo.');
     const current=new Recorder(stream,{mimeType,videoBitsPerSecond:1000000,audioBitsPerSecond:64000});recorder=current;parts=[];bytes=0;
     current.ondataavailable=e=>{if(request!==generation||!e.data.size)return;bytes+=e.data.size;
       if(bytes>captureLimit){cancel();onError('La captura superó 12 MB. Intenta una grabación más corta.');return;}parts.push(e.data);};
     current.onerror=()=>{if(request===generation){cancel();onError('La grabación falló. Puedes volver a intentar o adjuntar un archivo.');}};
     current.onstop=()=>{if(request!==generation)return;
       const blob=new Blob(parts,{type:mimeType});cancel();
       if(blob.size)onFile(new File([blob],'captura.'+(mimeType.includes('mp4')?(kind==='audio'?'m4a':'mp4'):'webm'),{type:mimeType}));
       else onError('La grabación quedó vacía. Intenta de nuevo.');};
     current.start(250);timer=setTimer(()=>finish(),55000);
   }catch(error){if(request===generation){cancel();onError(captureError(error));}}
 }
 function finish(){if(active&&recorder?.state==='recording')recorder.stop();}
 return {start,finish,cancel};
}
