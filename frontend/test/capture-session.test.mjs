import test from 'node:test';
import assert from 'node:assert/strict';
import {createCaptureSession,captureLimit} from '../src/capture-session.mjs';
function fixture(getMedia){
 let stopped=0,stream,recorder,timer,cleared=0;const files=[],errors=[];
 const device={getTracks:()=>[{stop(){stopped++;}}]};
 class Recorder{
  static isTypeSupported(type){return type.includes('webm');}
  constructor(s,options){assert.equal(s,device);this.mimeType=options.mimeType;recorder=this;this.state='inactive';}
  start(){this.state='recording';}
  stop(){this.state='inactive';queueMicrotask(()=>this.onstop?.());}
  emit(data){this.ondataavailable({data});}
 }
 const session=createCaptureSession({getMedia:getMedia|| (async()=>device),Recorder,onStream:s=>{stream=s;},onFile:f=>files.push(f),onError:e=>errors.push(e),setTimer:(fn,ms)=>{assert.equal(ms,55000);timer=fn;return 1;},clearTimer:()=>{cleared++;}});
 return {session,files,errors,device,get recorder(){return recorder;},get stream(){return stream;},get stopped(){return stopped;},get cleared(){return cleared;},finishTimer:()=>timer()};
}
const settle=()=>new Promise(resolve=>queueMicrotask(resolve));
test('captura solo pide permisos explícitos y entrega archivo local al terminar',async()=>{
 let constraints;const f=fixture(async value=>{constraints=value;return f.device;});
 assert.equal(f.stream,undefined);await f.session.start('audio');assert.equal(constraints.video,false);assert.equal(constraints.audio,true);assert.equal(f.files.length,0);
 f.recorder.emit(new Blob(['audio sintetico']));f.session.finish();await settle();
 assert.equal(f.files.length,1);assert.match(f.files[0].type,/audio\/webm/);assert.equal(f.stream,null);assert.equal(f.stopped,1);assert.equal(f.cleared,1);
});
test('cancelar mientras el permiso está pendiente detiene el flujo tardío',async()=>{
 let resolve;const f=fixture(()=>new Promise(done=>{resolve=done;}));const starting=f.session.start('photo');f.session.cancel();resolve(f.device);await starting;
 assert.equal(f.stopped,1);assert.equal(f.stream,null);assert.equal(f.files.length,0);
});
test('cancelar grabación descarta contenido sin adjuntar ni publicar',async()=>{
 const f=fixture();await f.session.start('video');f.recorder.emit(new Blob(['video']));f.session.cancel();await settle();assert.equal(f.files.length,0);assert.equal(f.stopped,1);
});
test('límite de tamaño descarta captura y detiene dispositivos',async()=>{
 const f=fixture();await f.session.start('video');f.recorder.emit({size:captureLimit+1});await settle();assert.equal(f.files.length,0);assert.match(f.errors[0],/12 MB/);assert.equal(f.stopped,1);
});
test('límite temporal produce adjunto y termina tracks',async()=>{
 const f=fixture();await f.session.start('video');f.recorder.emit(new Blob(['video']));f.finishTimer();await settle();assert.equal(f.files.length,1);assert.equal(f.stopped,1);
});
test('permiso rechazado explica alternativa y no entrega archivo',async()=>{
 const f=fixture(async()=>{throw Object.assign(Error(),{name:'NotAllowedError'});});await f.session.start('photo');assert.match(f.errors[0],/No se autorizó/);assert.equal(f.files.length,0);
});
test('foto no solicita micrófono y no graba automáticamente',async()=>{
 let constraints;const f=fixture(async value=>{constraints=value;return f.device;});await f.session.start('photo');assert.equal(constraints.audio,false);assert.equal(f.recorder,undefined);f.session.cancel();assert.equal(f.stopped,1);
});
