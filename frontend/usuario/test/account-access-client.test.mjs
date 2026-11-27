import {test} from 'node:test';
import assert from 'node:assert/strict';
import {createAccountAccessClient} from '../src/diseno/account-access-client.mjs';
const response=(value,status=200)=>new Response(JSON.stringify(value),{status,headers:{'Content-Type':'application/json'}});

test('el registro usa la base configurada, cookies y CSRF sin repetir el envío',async()=>{
 const calls=[];const client=createAccountAccessClient({baseUrl:'https://school.example.test/',fetcher:async(url,options)=>{calls.push({url,options});return calls.length===1?response({token:'test-token'}):response({emailSent:false,message:'Solicitud registrada.'});}});
 const result=await client('register-family',{username:'synthetic.family'});
 assert.equal(result.emailSent,false);assert.equal(calls.length,2);
 assert.equal(calls[1].url,'https://school.example.test/api/access/register-family');assert.equal(calls[1].options.headers['X-CSRF-TOKEN'],'test-token');assert.equal(calls[1].options.credentials,'include');assert.equal(calls[1].options.cache,'no-store');
});
test('fallar la preparación del acceso no registra una cuenta',async()=>{
 let calls=0;const client=createAccountAccessClient({fetcher:async()=>{calls++;return response({},503);}});
 await assert.rejects(()=>client('register-family',{}),/preparar el acceso/);assert.equal(calls,1);
});
test('un fallo incierto de escritura no reenvía y no anuncia una activación',async()=>{
 let calls=0;const client=createAccountAccessClient({fetcher:async()=>{calls++;if(calls===1)return response({token:'test-token'});throw new TypeError('network failed');}});
 await assert.rejects(()=>client('confirm',{}),error=>error.code==='UNCERTAIN'&&/consulta a Administración/.test(error.message));assert.equal(calls,2);
});
test('el error confirmado del colegio se conserva como mensaje legible',async()=>{
 let calls=0;const client=createAccountAccessClient({fetcher:async()=>++calls===1?response({token:'test-token'}):response({error:{code:'MAIL_NOT_CONFIGURED',message:'Solicita ayuda al colegio.'}},503)});
 await assert.rejects(()=>client('reset-request',{}),error=>error.status===503&&error.message==='Solicita ayuda al colegio.');assert.equal(calls,2);
});
test('la espera del registro vence y no se vuelve a enviar automáticamente',async()=>{
 let calls=0;const client=createAccountAccessClient({timeout:10,fetcher:async(url,options)=>{calls++;if(calls===1)return response({token:'test-token'});return new Promise((resolve,reject)=>options.signal.addEventListener('abort',()=>reject(new DOMException('aborted','AbortError')),{once:true}));}});
 await assert.rejects(()=>client('register-family',{}),error=>error.code==='UNCERTAIN');assert.equal(calls,2);
});
