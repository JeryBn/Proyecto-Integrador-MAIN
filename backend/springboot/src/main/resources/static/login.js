const message=document.querySelector('#login-message');
const params=new URLSearchParams(location.search);
if(params.has('error'))message.textContent='Usuario o contraseña incorrectos. Intenta nuevamente.';
if(params.has('salida'))message.textContent='Cerraste tu sesión correctamente.';
const form=document.querySelector('#login');
const submit=document.querySelector('#submit');
async function refreshToken() {
  const response=await fetch('/login-token',{cache:'no-store',credentials:'same-origin'});
  if(!response.ok) throw Error('No pudimos preparar el acceso. Intenta nuevamente.');
  const token=await response.json();
  const input=document.querySelector('#csrf');
  input.name=token.parameterName;
  input.value=token.token;
}
refreshToken().then(()=>submit.disabled=false).catch(()=>{
  message.textContent='No pudimos preparar el acceso. Pulsa Ingresar para reintentar.';
  submit.disabled=false;
});
form.addEventListener('submit',async event=>{
  event.preventDefault();
  if(submit.disabled) return;
  submit.disabled=true;
  message.textContent='';
  try {
    // Otra pestaña o el vencimiento de sesión pueden invalidar el token inicial.
    await refreshToken();
    HTMLFormElement.prototype.submit.call(form);
  } catch(error) {
    message.textContent=error.message || 'No hay conexión. Intenta nuevamente.';
    submit.disabled=false;
  }
});

