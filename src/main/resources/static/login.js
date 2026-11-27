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
  return token.token;
}
refreshToken().then(()=>submit.disabled=false).catch(()=>{
  message.textContent='No pudimos preparar el acceso. Pulsa Ingresar para reintentar.';
  submit.disabled=false;
});

const activation=document.querySelector('#activation');
if (activation) activation.addEventListener('submit', async event => {
  event.preventDefault();
  const status=document.querySelector('#activation-message');
  const button=activation.querySelector('button[type="submit"]');
  const credentials=Object.fromEntries(new FormData(activation));
  button.disabled=true;
  status.textContent='';
  try {
    const token=await refreshToken();
    const response=await fetch(activation.action, {
      method:'POST',
      credentials:'same-origin',
      headers:{'Content-Type':'application/json','X-CSRF-TOKEN':token},
      body:JSON.stringify(credentials)
    });
    if (!response.ok) {
      const result=await response.json().catch(() => null);
      status.textContent=result?.error?.message || 'No pudimos activar la cuenta. Intenta nuevamente.';
      return;
    }
    activation.reset();
    form.elements.namedItem('username').value=credentials.username;
    status.textContent='Cuenta activada. Ya puedes ingresar con tu nueva contraseña.';
  } catch {
    status.textContent='No pudimos conectar con MAIN. Intenta nuevamente.';
  } finally {
    button.disabled=false;
  }
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

