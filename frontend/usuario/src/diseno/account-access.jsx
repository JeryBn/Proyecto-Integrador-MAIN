import React,{useEffect,useRef,useState} from 'react';
import {Material} from './stitch-renderer.jsx';
import {createAccountAccessClient} from './account-access-client.mjs';

const submitAccess=createAccountAccessClient({baseUrl:import.meta.env.VITE_API_URL||''});

export function AccountAccess({mode,onClose}){
 const dialog=useRef(null),[step,setStep]=useState(mode==='activate'?'confirm':'request'),[busy,setBusy]=useState(false),[error,setError]=useState(''),[notice,setNotice]=useState(''),[visible,setVisible]=useState(false);
 const [form,setForm]=useState({username:'',name:'',email:'',identifier:'',code:'',password:'',confirmation:''});
 useEffect(()=>{const node=dialog.current;node?.showModal();return()=>node?.close();},[]);
 const field=(key,label,type='text',extra={})=><label>{label}<input required disabled={busy} type={type} value={form[key]} onChange={e=>setForm(old=>({...old,[key]:e.target.value}))} {...extra}/></label>;
 async function send(event){
  event.preventDefault();if(busy)return;setError('');setNotice('');
  if((step==='confirm'||mode==='family')&&form.password!==form.confirmation){setError('Las contraseñas no coinciden.');return;}
  setBusy(true);
  try{
   if(step==='confirm'){
    const result=await submitAccess('activate',{username:form.username.trim(),code:form.code.trim(),password:form.password});
    setForm(old=>({...old,password:'',confirmation:'',code:''}));setNotice(result.message);setStep('done');
   }else if(mode==='family'){
    const result=await submitAccess('register-family',{username:form.username.trim(),name:form.name.trim(),email:form.email.trim(),password:form.password});setNotice(result.message);setStep(result.emailSent?'confirm':'pending');
   }else{
    const result=await submitAccess('reset-request',{identifier:form.identifier.trim()});setNotice(result.message);setStep('confirm');
   }
  }catch(e){setError(e.message);}finally{setBusy(false);}
 }
 const title=step==='done'?'Tu acceso está listo':mode==='family'?'Crear cuenta de apoderado':mode==='reset'?'Recuperar mi contraseña':'Activar mi cuenta';
 return <dialog ref={dialog} className="colegia-access-dialog" aria-labelledby="account-access-title" onCancel={e=>{if(busy)e.preventDefault();else onClose();}}>
  <header><span className="access-dialog-icon"><Material>{mode==='family'?'family_restroom':mode==='reset'?'lock_reset':'how_to_reg'}</Material></span><button className="access-dialog-close" type="button" aria-label="Cerrar" disabled={busy} onClick={onClose}><Material>close</Material></button></header>
  <h2 id="account-access-title">{title}</h2>
  <p>{mode==='family'?'El colegio verificará el vínculo con tus hijos antes de mostrar sus notas y asistencia.':mode==='reset'?'Usa el correo verificado por el colegio. Si todavía no tienes uno registrado, contacta a Administración.':'Utiliza el usuario y el código de invitación que recibiste del colegio. No necesitas crear otra cuenta.'}</p>
  {notice&&<p className="access-dialog-notice" role="status">{notice}</p>}{error&&<p className="access-dialog-error" role="alert">{error}</p>}
  {step==='done'||step==='pending'?<><button className="access-dialog-primary" type="button" onClick={onClose}>Volver a iniciar sesión <Material>arrow_forward</Material></button>{step==='pending'&&<button className="access-dialog-link" type="button" onClick={()=>setStep('confirm')}>Ya tengo un código del colegio</button>}</>:<form onSubmit={send}>
   {step==='request'&&mode==='reset'&&field('identifier','Usuario o correo verificado','text',{autoComplete:'username',maxLength:254})}
   {(mode==='family'||step==='confirm')&&field('username','Usuario institucional','text',{autoComplete:'username',minLength:3,maxLength:60})}
   {step==='request'&&mode==='family'&&<>{field('name','Nombre completo','text',{autoComplete:'name',maxLength:100})}{field('email','Correo electrónico','email',{autoComplete:'email',maxLength:254})}</>}
   {step==='confirm'&&field('code','Código de activación o recuperación','text',{autoComplete:'one-time-code',maxLength:32,minLength:32,spellCheck:false})}
   {(mode==='family'||step==='confirm')&&<>{field('password','Nueva contraseña',visible?'text':'password',{autoComplete:'new-password',minLength:12})}{field('confirmation','Confirmar contraseña',visible?'text':'password',{autoComplete:'new-password',minLength:12})}<label className="access-dialog-check"><input type="checkbox" disabled={busy} checked={visible} onChange={e=>setVisible(e.target.checked)}/> Mostrar contraseña</label><small>Usa al menos 12 caracteres. No compartas tu contraseña ni tu código.</small></>}
   <button className="access-dialog-primary" type="submit" disabled={busy}>{busy?'Procesando…':step==='confirm'?'Confirmar y activar acceso':mode==='family'?'Solicitar mi cuenta':'Enviar código'}<Material>arrow_forward</Material></button>
   {mode!=='family'&&step==='request'&&<button className="access-dialog-link" type="button" onClick={()=>setStep('confirm')}>Ya tengo un código</button>}
  </form>}
 </dialog>;
}
