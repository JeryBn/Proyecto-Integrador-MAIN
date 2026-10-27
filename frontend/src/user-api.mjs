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
export function createUserApi({fetcher=(...args)=>fetch(...args),expired=()=>{location.assign('/login.html');},changed=()=>{location.replace('/');}}={}){
  let identity=null;
  async function readResponse(response){
    if(response.status===401){expired();throw new ApiError('Tu sesión finalizó. Vuelve a ingresar.',401,'UNAUTHENTICATED');}
    let value=null;
    if(response.status!==204){try{value=await response.json();}catch{if(response.ok)throw new ApiError('El servidor respondió con un formato inesperado.',response.status,'INVALID_RESPONSE');}}
    if(response.status===409&&value?.error?.code==='ACCOUNT_CHANGED')changed();
    if(!response.ok)throw new ApiError(value?.error?.message||(response.status===403?'No tienes acceso o la sesión cambió. Actualiza antes de reintentar.':response.status===404?'El contenido ya no está disponible.':'No se pudo completar la operación.'),response.status,value?.error?.code||'REQUEST_FAILED');
    return value;
  }
  async function call(path,{method='GET',json,form,signal}={}){
    const headers={Accept:'application/json'};
    if(identity)headers['X-MAIN-Expected-User']=identity;
    try{
      if(method!=='GET'){
        const token=await call('/api/csrf',{signal});
        headers[token.headerName||'X-CSRF-TOKEN']=token.token;
      }
      if(json!==undefined)headers['Content-Type']='application/json';
      const request=async requestSignal=>readResponse(await fetcher(path,{method,headers,credentials:'same-origin',cache:'no-store',signal:requestSignal,body:json!==undefined?JSON.stringify(json):form}));
      return await (method==='GET'?boundedRead(request,{signal}):request(signal));
    }catch(e){
      if(e instanceof ApiError)throw e;
      if(e.name==='TimeoutError')throw new ApiError('El servidor tarda en responder. Volveremos a consultar; también puedes reintentar.',0,'READ_TIMEOUT');
      if(e.name==='AbortError')throw new ApiError('Se canceló la espera. El servidor puede haber recibido el envío; actualiza antes de reintentar con el mismo contenido.',0,'CANCELLED');
      throw new ApiError('No hay conexión con el servidor. Conservamos tu borrador para reintentar.');
    }
  }
  async function upload(path,form,{signal,onProgress=()=>{},xhrFactory=()=>new XMLHttpRequest()}={}){
    const token=await call('/api/csrf',{signal});
    if(signal?.aborted)throw new ApiError('Se canceló la espera. Actualiza antes de reintentar.',0,'CANCELLED');
    return new Promise((resolve,reject)=>{
      const xhr=xhrFactory();xhr.open('POST',path);xhr.withCredentials=true;
      xhr.setRequestHeader('Accept','application/json');xhr.setRequestHeader(token.headerName||'X-CSRF-TOKEN',token.token);
      if(identity)xhr.setRequestHeader('X-MAIN-Expected-User',identity);
      const abort=()=>xhr.abort();
      const done=()=>signal?.removeEventListener('abort',abort);
      xhr.upload.onprogress=e=>{if(e.lengthComputable)onProgress(Math.round(e.loaded*100/e.total));};
      xhr.onload=async()=>{done();try{resolve(await readResponse({status:xhr.status,ok:xhr.status>=200&&xhr.status<300,json:async()=>JSON.parse(xhr.responseText)}));}catch(e){reject(e);}};
      xhr.onerror=()=>{done();reject(new ApiError('No se confirmó el envío. Actualiza antes de reintentar; conservamos tu borrador.'));};
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
