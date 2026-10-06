const message=document.querySelector('#login-message');
const params=new URLSearchParams(location.search);
if(params.has('error'))message.textContent='Usuario o contraseña incorrectos. Intenta nuevamente.';
if(params.has('salida'))message.textContent='Cerraste tu sesión correctamente.';
fetch('/login-token').then(r=>{if(!r.ok)throw Error();return r.json()}).then(t=>{const input=document.querySelector('#csrf');input.name=t.parameterName;input.value=t.token;document.querySelector('#submit').disabled=false}).catch(()=>message.textContent='No pudimos preparar el acceso. Recarga la página.');

