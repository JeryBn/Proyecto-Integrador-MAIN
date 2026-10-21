const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const vm = require('node:vm');
const source = readFileSync(require('node:path').resolve(__dirname, '../../src/main/resources/static/login.js'), 'utf8');
const flush = () => new Promise(resolve => setImmediate(resolve));

function page(fetch) {
  const message = {textContent:''}, button = {disabled:true}, input = {};
  let handler, submitted = 0;
  const form = {addEventListener: (_, callback) => {handler = callback;}};
  const elements = {'#login-message':message,'#submit':button,'#csrf':input,'#login':form};
  vm.runInNewContext(source, {document:{querySelector:selector=>elements[selector]}, location:{search:''},
    URLSearchParams, fetch, HTMLFormElement:{prototype:{submit(){submitted++;}}}});
  return {message,button,input,send:()=>handler({preventDefault(){}}),count:()=>submitted};
}
const token = value => ({ok:true,json:async()=>({parameterName:'_csrf',token:value})});

test('renueva el token al enviar una página que ya estaba abierta', async()=>{
  let calls = 0;
  const ui = page(async()=>token(++calls === 1 ? 'viejo' : 'nuevo'));
  await flush(); assert.equal(ui.input.value,'viejo');
  await ui.send();
  assert.equal(ui.input.value,'nuevo'); assert.equal(ui.input.name,'_csrf'); assert.equal(ui.count(),1);
});
test('fallo de red conserva el formulario y permite reintentar', async()=>{
  let calls = 0;
  const ui = page(async()=>{if(++calls===2)throw Error('Sin conexión');return token('actual');});
  await flush(); await ui.send();
  assert.equal(ui.count(),0); assert.equal(ui.button.disabled,false); assert.match(ui.message.textContent,/conexión/);
  await ui.send(); assert.equal(ui.count(),1);
});
test('fallo inicial del token no deja el acceso bloqueado', async()=>{
  let calls=0;
  const ui=page(async()=>++calls===1 ? {ok:false} : token('actual'));
  await flush(); assert.equal(ui.button.disabled,false);
  await ui.send(); assert.equal(ui.count(),1);
});
test('doble envío mientras se renueva el token hace una sola solicitud', async()=>{
  let calls=0, resolve;
  const ui=page(async()=>++calls===1 ? token('viejo') : new Promise(done=>{resolve=done;}));
  await flush(); const pending=ui.send(); await ui.send();
  assert.equal(calls,2); assert.equal(ui.count(),0);
  resolve(token('nuevo')); await pending; assert.equal(ui.count(),1);
});
