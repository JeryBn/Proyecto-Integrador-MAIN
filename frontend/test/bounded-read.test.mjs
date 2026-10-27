import test from 'node:test';
import assert from 'node:assert/strict';
import {boundedRead} from '../src/bounded-read.mjs';

const stalled=signal=>new Promise((resolve,reject)=>{
 signal.addEventListener('abort',()=>{const error=new Error('aborted');error.name='AbortError';reject(error);},{once:true});
});

test('lectura colgada vence y otra lectura usa una señal nueva',async()=>{
 let expire,cleared=0,oldSignal;
 const pending=boundedRead(signal=>{oldSignal=signal;return stalled(signal);},
  {schedule:(fn,ms)=>{assert.equal(ms,30000);expire=fn;return 7;},cancel:id=>{assert.equal(id,7);cleared++;}});
 expire();await assert.rejects(pending,error=>error.name==='TimeoutError');
 assert.equal(oldSignal.aborted,true);assert.equal(cleared,1);
 assert.equal(await boundedRead(async signal=>{assert.equal(signal.aborted,false);return 'recuperado';}),'recuperado');
});

test('desmontar cancela la lectura y elimina el temporizador',async()=>{
 const controller=new AbortController();let cleared=0;
 const pending=boundedRead(stalled,{signal:controller.signal,schedule:()=>5,cancel:()=>cleared++});
 controller.abort();await assert.rejects(pending,error=>error.name==='AbortError');assert.equal(cleared,1);
});

test('una cancelación previa no consulta y una respuesta correcta limpia el plazo',async()=>{
 const controller=new AbortController();controller.abort();let calls=0;
 await assert.rejects(boundedRead(async()=>calls++,{signal:controller.signal}),error=>error.name==='AbortError');
 assert.equal(calls,0);let cleared=0;
 assert.equal(await boundedRead(async()=>42,{schedule:()=>3,cancel:()=>cleared++}),42);
 assert.equal(cleared,1);
});
