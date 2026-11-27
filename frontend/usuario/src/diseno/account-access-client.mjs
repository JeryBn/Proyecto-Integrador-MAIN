import {boundedRead} from '../alumno/datos/bounded-read.mjs';

class AccessError extends Error {
 constructor(message,status=0,code='REQUEST_FAILED'){super(message);this.status=status;this.code=code;}
}
const uncertain='No pudimos confirmar el resultado. Conserva tu usuario y consulta a Administración antes de repetir la solicitud.';
async function jsonResponse(response){
 if(response.status===204)return {message:"Cuenta activada. Ya puedes iniciar sesión."};
 const declared=Number(response.headers?.get('content-length'));
 if(declared>65536)throw new AccessError('Respuesta inesperada del servidor.',response.status,'INVALID_RESPONSE');
 const text=await response.text();
 if(new TextEncoder().encode(text).byteLength>65536)throw new AccessError('Respuesta inesperada del servidor.',response.status,'INVALID_RESPONSE');
 let value;
 try{value=JSON.parse(text);}catch{throw new AccessError('Respuesta inesperada del servidor.',response.status,'INVALID_RESPONSE');}
 if(!response.ok)throw new AccessError(value?.error?.message||'No pudimos completar la solicitud. Intenta nuevamente.',response.status,value?.error?.code);
 return value;
}
export function createAccountAccessClient({fetcher=(...args)=>fetch(...args),baseUrl='',timeout=15000}={}){
 const endpoint=path=>baseUrl.replace(/\/$/,'')+path;
 return async(path,value)=>{
  let writeStarted=false;
  try{
   return await boundedRead(async signal=>{
    const prepared=await fetcher(endpoint('/login-token'),{credentials:'include',cache:'no-store',signal,headers:{Accept:'application/json'}});
    if(!prepared.ok)throw new AccessError('No pudimos preparar el acceso. Comprueba la conexión con el colegio.',prepared.status);
    const token=await jsonResponse(prepared);
    if(typeof token?.token!=='string'||!token.token)throw new AccessError('No pudimos preparar el acceso. Recarga la página.');
    writeStarted=true;
    return jsonResponse(await fetcher(endpoint('/api/access/'+path),{method:'POST',credentials:'include',cache:'no-store',signal,headers:{Accept:'application/json','Content-Type':'application/json','X-CSRF-TOKEN':token.token},body:JSON.stringify(value)}));
   },{timeout});
  }catch(error){
   if(error instanceof AccessError){if(writeStarted&&error.code==='INVALID_RESPONSE')throw new AccessError(uncertain,0,'UNCERTAIN');throw error;}
   throw new AccessError(writeStarted?uncertain:'No hay conexión con el servidor. Tu formulario se conserva para reintentar.',0,writeStarted?'UNCERTAIN':'NETWORK');
  }
 };
}
