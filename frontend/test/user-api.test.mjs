import {test} from 'node:test';
import assert from 'node:assert/strict';
import {createUserApi,fileError,submissionKey,localDate} from '../src/user-api.mjs';
const response=(status,value)=>({status,ok:status>=200&&status<300,json:async()=>value});
test('renueva CSRF, protege identidad y no almacena respuestas privadas',async()=>{
 const calls=[];const api=createUserApi({fetcher:async(path,options)=>{calls.push({path,options});return response(200,path==='/api/csrf'?{token:'fresh',headerName:'X-CSRF-TOKEN'}:{saved:true});}});
 api.setIdentity('student');await api.call('/api/v1/preferences',{method:'PUT',json:{darkTheme:true}});
 assert.equal(calls.length,2);assert.equal(calls[1].options.headers['X-CSRF-TOKEN'],'fresh');assert.equal(calls[0].options.headers['X-MAIN-Expected-User'],'student');assert.equal(calls[1].options.headers['X-MAIN-Expected-User'],'student');assert.equal(calls[1].options.cache,'no-store');assert.equal(calls[1].options.credentials,'same-origin');
});
test('sesión revocada redirige y no reintenta la mutación',async()=>{let expired=0,calls=0;const api=createUserApi({expired:()=>expired++,fetcher:async()=>{calls++;return response(401,{});}});await assert.rejects(()=>api.call('/api/v1/messages',{method:'POST',json:{}}),e=>e.status===401);assert.equal(expired,1);assert.equal(calls,1);});
test('cambio de cuenta descarta la operación antes de escribir',async()=>{let changed=0,calls=0;const api=createUserApi({changed:()=>changed++,fetcher:async()=>{calls++;return response(409,{error:{code:'ACCOUNT_CHANGED',message:'Cambió la cuenta'}});}});await assert.rejects(()=>api.call('/api/v1/preferences',{method:'PUT',json:{}}),e=>e.code==='ACCOUNT_CHANGED');assert.equal(changed,1);assert.equal(calls,1);});
test('no reenvía automáticamente un POST con resultado incierto',async()=>{let calls=0;const api=createUserApi({fetcher:async()=>{calls++;if(calls===1)return response(200,{token:'csrf'});throw Error('offline');}});await assert.rejects(()=>api.call('/api/v1/conversations',{method:'POST',json:{}}),e=>e.code==='NETWORK_ERROR');assert.equal(calls,2);});
test('clave idempotente se conserva solo para contenido idéntico',()=>{const initial=submissionKey(null,'same');assert.deepEqual(submissionKey(initial,'same'),initial);assert.notEqual(submissionKey(initial,'changed').key,initial.key);});
test('validación de archivos aplica límites distintos de mensajes y estados',()=>{assert.match(fileError({size:0,type:'image/png'}),/vacío/);assert.match(fileError({size:13*1024*1024,type:'image/png'}),/12 MB/);assert.match(fileError({size:2*1024*1024,type:'text/plain'}),/1 MB/);assert.equal(fileError({size:32,type:'text/plain'}),'');assert.match(fileError({size:32,type:'text/plain'},{status:true}),/JPG/);assert.match(fileError({size:32,type:'text/html'}),/Usa/);assert.equal(fileError({size:32,type:'video/mp4'}),'');});
test('fechas SQL Lima e instantes ISO representan el mismo momento',()=>{assert.equal(localDate('2026-10-22 10:00:00.0'),localDate('2026-10-22T15:00:00Z'));});
test('subida transmite CSRF e identidad y confirma progreso antes del resultado',async()=>{
 const seen={};let progress=0;
 const api=createUserApi({fetcher:async()=>response(200,{token:'token',headerName:'X-CSRF-TOKEN'})});api.setIdentity('student');
 const xhr={upload:{},open:(method,path)=>Object.assign(seen,{method,path}),setRequestHeader:(k,v)=>seen[k]=v,send(){this.upload.onprogress({lengthComputable:true,loaded:5,total:10});this.status=202;this.responseText='{"id":"receipt"}';queueMicrotask(()=>this.onload());}};
 const result=await api.upload('/api/v1/messages',new FormData(),{xhrFactory:()=>xhr,onProgress:p=>{progress=p;}});
 assert.equal(result.id,'receipt');assert.equal(progress,50);assert.equal(seen['X-MAIN-Expected-User'],'student');assert.equal(seen['X-CSRF-TOKEN'],'token');
});
test('cancelar subida conserva un resultado incierto sin reenvío automático',async()=>{
 const controller=new AbortController();let sends=0;
 const api=createUserApi({fetcher:async()=>response(200,{token:'token'})});
 const xhr={upload:{},open(){},setRequestHeader(){},send(){sends++;queueMicrotask(()=>controller.abort());},abort(){this.onabort();}};
 await assert.rejects(()=>api.upload('/upload',new FormData(),{signal:controller.signal,xhrFactory:()=>xhr}),e=>e.code==='CANCELLED'&&e.message.includes('puede haber recibido'));
 assert.equal(sends,1);
});
test('subida revocada no devuelve contenido ni anuncia éxito',async()=>{
 let expired=0;const api=createUserApi({expired:()=>expired++,fetcher:async()=>response(200,{token:'token'})});
 const xhr={upload:{},open(){},setRequestHeader(){},send(){this.status=401;this.responseText='{}';queueMicrotask(()=>this.onload());}};
 await assert.rejects(()=>api.upload('/upload',new FormData(),{xhrFactory:()=>xhr}),e=>e.status===401);assert.equal(expired,1);
});
test('rechaza una respuesta de lectura declarada por encima de 1 MiB',async()=>{
 const api=createUserApi({fetcher:async()=>({status:200,ok:true,headers:{get:()=>String(1024*1024+1)},text:async()=>{throw Error('no debe leerse');}})});
 await assert.rejects(()=>api.call('/api/perfil'),e=>e.code==='INVALID_RESPONSE'&&e.message.includes('tamaño permitido'));
});
test('una respuesta excesiva después de escribir queda como resultado incierto',async()=>{
 let calls=0;const api=createUserApi({fetcher:async()=>{calls++;return calls===1?response(200,{token:'csrf'}):{status:200,ok:true,headers:{get:()=>String(1024*1024+1)},text:async()=>''};}});
 await assert.rejects(()=>api.call('/api/v1/preferences',{method:'PUT',json:{darkTheme:true}}),e=>e.code==='NETWORK_ERROR'&&e.message.includes('No se confirmó'));
 assert.equal(calls,2);
});
test('el plazo de escritura abarca la lectura completa sin reenviar',async()=>{
 let calls=0,fire;const schedule=fn=>{fire=fn;return 1;};
 const api=createUserApi({schedule,cancel:()=>{},fetcher:async(path,options)=>{calls++;if(calls===1)return response(200,{token:'csrf'});return {status:200,ok:true,headers:{get:()=>null},body:{getReader:()=>({read:()=>new Promise((resolve,reject)=>{const fail=()=>{const error=Error('aborted');error.name='AbortError';reject(error);};if(options.signal.aborted)fail();else options.signal.addEventListener('abort',fail,{once:true});}),cancel:async()=>{},releaseLock(){}})}};}});
 const pending=api.call('/api/v1/preferences',{method:'PUT',json:{darkTheme:true}});await new Promise(resolve=>setImmediate(resolve));fire();
 await assert.rejects(()=>pending,e=>e.code==='NETWORK_ERROR'&&e.message.includes('No se confirmó'));assert.equal(calls,2);
});
test('una confirmación de subida excesiva no anuncia éxito ni reenvía',async()=>{
 let sends=0;const api=createUserApi({fetcher:async()=>response(200,{token:'token'})});
 const xhr={upload:{},open(){},setRequestHeader(){},send(){sends++;this.status=201;this.responseText='x'.repeat(1024*1024+1);queueMicrotask(()=>this.onload());}};
 await assert.rejects(()=>api.upload('/upload',new FormData(),{xhrFactory:()=>xhr}),e=>e.code==='NETWORK_ERROR'&&e.message.includes('No se confirmó'));assert.equal(sends,1);
});
