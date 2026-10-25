import test from 'node:test';
import assert from 'node:assert/strict';
import {createReadPoller} from '../src/read-poller.mjs';
function fixture(read,available=()=>true){
 const timers=new Map();let id=0;
 const poller=createReadPoller({read,available,schedule:(fn,ms)=>{timers.set(++id,{fn,ms});return id;},cancel:key=>timers.delete(key)});
 const next=async()=>{const [key,timer]=timers.entries().next().value;timers.delete(key);await timer.fn();};
 return {poller,timers,next};
}
test('lecturas fallidas retroceden hasta 60s y éxito restaura 5s',async()=>{
 let fails=true;const f=fixture(async()=>{if(fails)throw Error('sin red');});
 await f.poller.start();assert.equal([...f.timers.values()][0].ms,10000);
 for(const delay of [20000,40000,60000,60000]){await f.next();assert.equal([...f.timers.values()][0].ms,delay);}
 fails=false;await f.next();assert.equal([...f.timers.values()][0].ms,5000);f.poller.stop();assert.equal(f.timers.size,0);
});
test('online/foco recupera sin lecturas simultáneas ni timers duplicados',async()=>{
 let release,calls=0;const f=fixture(()=>{calls++;return new Promise(resolve=>{release=resolve;});});
 const first=f.poller.start();await f.poller.wake();await f.poller.wake();assert.equal(calls,1);
 release();await first;assert.equal(f.timers.size,1);assert.equal([...f.timers.values()][0].ms,0);
 f.poller.stop();await f.poller.wake();assert.equal(calls,1);
});
test('pestaña oculta o equipo offline pausa; volver reactiva y stop descarta timers',async()=>{
 let available=false,calls=0;const f=fixture(async()=>{calls++;},()=>available);
 await f.poller.start();assert.equal(calls,0);assert.equal(f.timers.size,0);
 available=true;await f.poller.wake();assert.equal(calls,1);assert.equal(f.timers.size,1);
 available=false;await f.next();assert.equal(calls,1);assert.equal(f.timers.size,0);
 available=true;await f.poller.wake();assert.equal(calls,2);f.poller.stop();assert.equal(f.timers.size,0);
});
test('desmontar durante una petición no programa otra',async()=>{
 let release;const f=fixture(()=>new Promise(resolve=>{release=resolve;}));
 const waiting=f.poller.start();f.poller.stop();release();await waiting;assert.equal(f.timers.size,0);
});
