// Runs only when explicitly invoked by the loopback Python fixture. No cookies in evidence.
import http from 'node:http';
import assert from 'node:assert/strict';
import {performance} from 'node:perf_hooks';
import {createReadPoller} from '../frontend/src/read-poller.mjs';
import {createUserApi} from '../frontend/src/user-api.mjs';
let input='';for await (const chunk of process.stdin) input+=chunk;
const fixture=JSON.parse(input);
assert.match(fixture.path,/^\/api\/v1\/conversations\/[a-zA-Z0-9-]+\/messages$/);
let offline=false,failures=0,reads=0,active=0,maxActive=0,rows=[],delays=[];
const proxy=http.createServer(async(req,res)=>{
  if(req.method!=='GET'||req.url!==fixture.path){res.writeHead(403);res.end();return;}
  reads++;active++;maxActive=Math.max(maxActive,active);
  try{
    if(offline){failures++;req.socket.destroy();return;}
    const upstream=await fetch('http://127.0.0.1:8088'+req.url,{headers:{cookie:fixture.cookie}});
    res.writeHead(upstream.status,{'Content-Type':'application/json'});res.end(await upstream.text());
  }catch{res.writeHead(502);res.end();}finally{active--;}
});
await new Promise(resolve=>proxy.listen(0,'127.0.0.1',resolve));
const base='http://127.0.0.1:'+proxy.address().port;
const api=createUserApi({fetcher:(path,options)=>fetch(base+path,options),expired:()=>assert.fail('session expired'),changed:()=>assert.fail('account changed')});
const poller=createReadPoller({read:async()=>{rows=await api.call(fixture.path);},schedule:(fn,ms)=>{delays.push(ms);return setTimeout(fn,ms);}});
try{
  await poller.start();assert.equal(rows.length,1);
  offline=true;const started=performance.now();await poller.wake();
  await new Promise(resolve=>setTimeout(resolve,75000));
  const outageMs=Math.round(performance.now()-started);
  assert.ok(failures>=3);assert.ok(delays.includes(60000));
  assert.equal(rows.length,1,'failed read must retain last confirmed view');
  offline=false;const resumed=performance.now();await poller.wake();
  const recoveryMs=Math.round(performance.now()-resumed);
  assert.equal(rows.length,2,'recover missed message after reconnect');
  assert.equal(new Set(rows.map(r=>r.id)).size,2);assert.ok(rows.every(r=>r.state==='APPROVED'));
  assert.equal(delays.at(-1),5000);assert.equal(maxActive,1);
  poller.stop();const stoppedReads=reads;await new Promise(resolve=>setTimeout(resolve,5500));
  assert.equal(reads,stoppedReads,'stop must prevent later polls');
  process.stdout.write(JSON.stringify({passed:10,outageMs,recoveryMs,failedRequests:failures,reads,maxActive,
    recoveredMessages:rows.length,duplicates:0,maxRetryDelayMs:Math.max(...delays),stoppedWithoutFurtherReads:true,
    fault:'loopback proxy closes GET connections for 75 real seconds; backend remains running',
    physicalNetworkTest:false,androidTest:false,pushTest:false}));
}finally{poller.stop();proxy.closeAllConnections();await new Promise(resolve=>proxy.close(resolve));}
