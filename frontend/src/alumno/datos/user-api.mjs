import {boundedRead} from './bounded-read.mjs';
export class ApiError extends Error {
  constructor(message,status=0,code='NETWORK_ERROR'){super(message);this.status=status;this.code=code;}
}
export function fileError(file,{status=false}={}){
  if(!file)return '';
  if(!file.size)return 'El archivo está vacío.';
  if(file.size>12*1024*1024)return 'El archivo debe pesar como máximo 12 MB.';
  const allowed=['image/jpeg','image/png'];
  if(!status)allowed.push('text/plain');
  if(!allowed.includes(file.type)&&!file.type.startsWith('video/')&&!file.type.startsWith('audio/'))return 'Usa JPG, PNG, video o audio'+(status?'.':' o TXT UTF-8.');
  if(file.type==='text/plain'&&file.size>1024*1024)return 'El documento TXT debe pesar como máximo 1 MB.';
  return '';
}
export function submissionKey(previous,payload){
  return previous?.payload===payload?previous:{payload,key:crypto.randomUUID()};
}
const maxResponseBytes=1024*1024;
const writeUncertain='No se confirmó el resultado. Conservamos tu borrador; actualiza antes de repetir la operación.';
async function responseText(response){
  const declared=Number(response.headers?.get?.('content-length'));
  if(Number.isFinite(declared)&&declared>maxResponseBytes)throw new ApiError('La respuesta del servidor supera el tamaño permitido.',response.status,'INVALID_RESPONSE');
  if(!response.body?.getReader){
    const text=await response.text();
    if(new TextEncoder().encode(text).byteLength>maxResponseBytes)throw new ApiError('La respuesta del servidor supera el tamaño permitido.',response.status,'INVALID_RESPONSE');
    return text;
  }
  const reader=response.body.getReader(),decoder=new TextDecoder();let bytes=0,text='';
  try{
    while(true){
      const {done,value}=await reader.read();if(done)break;
      bytes+=value.byteLength;
      if(bytes>maxResponseBytes){await reader.cancel();throw new ApiError('La respuesta del servidor supera el tamaño permitido.',response.status,'INVALID_RESPONSE');}
      text+=decoder.decode(value,{stream:true});
    }
    return text+decoder.decode();
  }finally{reader.releaseLock?.();}
}
async function responseValue(response){
  if(!response.body?.getReader&&typeof response.text!=='function'&&typeof response.json==='function')return response.json();
  const text=await responseText(response);return text?JSON.parse(text):null;
}
export function createUserApi({fetcher=(...args)=>fetch(...args),expired=()=>{location.assign('/login.html');},changed=()=>{location.replace('/');},timeout=30000,uploadTimeout=100000,schedule,cancel}={}){
  let identity=null;
  async function readResponse(response){
    if(response.status===401){expired();throw new ApiError('Tu sesión finalizó. Vuelve a ingresar.',401,'UNAUTHENTICATED');}
    let value=null;
    if(response.status!==204){try{value=await responseValue(response);}catch(error){if(error instanceof ApiError)throw error;if(response.ok)throw new ApiError('El servidor respondió con un formato inesperado.',response.status,'INVALID_RESPONSE');}}
    if(response.status===409&&value?.error?.code==='ACCOUNT_CHANGED')changed();
    if(!response.ok)throw new ApiError(value?.error?.message||(response.status===403?'No tienes acceso o la sesión cambió. Actualiza antes de reintentar.':response.status===404?'El contenido ya no está disponible.':'No se pudo completar la operación.'),response.status,value?.error?.code||'REQUEST_FAILED');
    return value;
  }
  async function call(path,{method='GET',json,form,signal}={}){
    const headers={Accept:'application/json'};let writeStarted=false;
    if(identity)headers['X-MAIN-Expected-User']=identity;
    try{
      if(method!=='GET'){
        const token=await call('/api/csrf',{signal});
        headers[token.headerName||'X-CSRF-TOKEN']=token.token;
      }
      if(json!==undefined)headers['Content-Type']='application/json';
      const request=async requestSignal=>{if(method!=='GET')writeStarted=true;return readResponse(await fetcher(path,{method,headers,credentials:'same-origin',cache:'no-store',signal:requestSignal,body:json!==undefined?JSON.stringify(json):form}));};
      const deadline={signal,timeout};if(schedule)deadline.schedule=schedule;if(cancel)deadline.cancel=cancel;
      return await boundedRead(request,deadline);
    }catch(e){
      if(e instanceof ApiError){if(method!=='GET'&&writeStarted&&e.code==='INVALID_RESPONSE')throw new ApiError(writeUncertain,e.status,'NETWORK_ERROR');throw e;}
      if(e.name==='TimeoutError')throw new ApiError(method!=='GET'&&writeStarted?writeUncertain:'El servidor tarda en responder. Volveremos a consultar; también puedes reintentar.',0,method!=='GET'&&writeStarted?'NETWORK_ERROR':'READ_TIMEOUT');
      if(e.name==='AbortError')throw new ApiError('Se canceló la espera. El servidor puede haber recibido el envío; actualiza antes de reintentar con el mismo contenido.',0,'CANCELLED');
      throw new ApiError(method!=='GET'&&writeStarted?writeUncertain:'No hay conexión con el servidor. Conservamos tu borrador para reintentar.');
    }
  }
  async function upload(path,form,{signal,onProgress=()=>{},xhrFactory=()=>new XMLHttpRequest()}={}){
    const token=await call('/api/csrf',{signal});
    if(signal?.aborted)throw new ApiError('Se canceló la espera. Actualiza antes de reintentar.',0,'CANCELLED');
    return new Promise((resolve,reject)=>{
      const xhr=xhrFactory();xhr.open('POST',path);xhr.withCredentials=true;xhr.timeout=uploadTimeout;
      xhr.setRequestHeader('Accept','application/json');xhr.setRequestHeader(token.headerName||'X-CSRF-TOKEN',token.token);
      if(identity)xhr.setRequestHeader('X-MAIN-Expected-User',identity);
      const abort=()=>xhr.abort();
      const done=()=>signal?.removeEventListener('abort',abort);
      xhr.upload.onprogress=e=>{if(e.lengthComputable)onProgress(Math.round(e.loaded*100/e.total));};
      xhr.onload=async()=>{done();try{resolve(await readResponse({status:xhr.status,ok:xhr.status>=200&&xhr.status<300,headers:{get:name=>xhr.getResponseHeader?.(name)},text:async()=>xhr.responseText||''}));}catch(e){reject(e instanceof ApiError&&e.code==='INVALID_RESPONSE'?new ApiError(writeUncertain,e.status,'NETWORK_ERROR'):e);}};
      xhr.onerror=()=>{done();reject(new ApiError('No se confirmó el envío. Actualiza antes de reintentar; conservamos tu borrador.'));};
      xhr.ontimeout=()=>{done();reject(new ApiError(writeUncertain,0,'NETWORK_ERROR'));};
      xhr.onabort=()=>{done();reject(new ApiError('Se canceló la espera. El servidor puede haber recibido el envío; actualiza antes de reintentar con el mismo contenido.',0,'CANCELLED'));};
      signal?.addEventListener('abort',abort,{once:true});xhr.send(form);
    });
  }
  return {call,upload,setIdentity:name=>{identity=name;}};
}
export const userApi=createUserApi();
export const enc=encodeURIComponent;
export const labels={PENDING:'Pendiente de revisión',REJECTED:'No entregado · revisar',CANCELLED:'Cancelado',APPROVED:'Entregado'};
export function localDate(value){if(!value)return '';const normalized=value.includes('T')?value:value.replace(' ','T');const zone=/Z$|[+-]\d{2}:\d{2}$/.test(normalized)?'':'-05:00';const date=new Date(normalized+zone);return Number.isNaN(date.valueOf())?value:new Intl.DateTimeFormat('es-PE',{dateStyle:'medium',timeStyle:'short',timeZone:'America/Lima'}).format(date);}
