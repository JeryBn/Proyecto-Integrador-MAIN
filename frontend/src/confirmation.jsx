import {useEffect,useRef,useState} from 'react';
import {Icon} from './ui-icons.jsx';

export function useConfirmation(){
  const [pending,setPending]=useState(null);
  const restoreFrame=useRef(null);
  useEffect(()=>()=>{if(restoreFrame.current!==null)cancelAnimationFrame(restoreFrame.current);},[]);
  const confirm=(message,action)=>{
    if(restoreFrame.current!==null){cancelAnimationFrame(restoreFrame.current);restoreFrame.current=null;}
    const returnTo=document.activeElement;
    setPending(current=>current||{message,action,returnTo});
  };
  const finish=approved=>{
    const action=pending?.action,returnTo=pending?.returnTo;
    setPending(null);if(approved)action?.();
    restoreFrame.current=requestAnimationFrame(()=>{
      restoreFrame.current=null;
      if(returnTo?.isConnected&&!returnTo.disabled)returnTo.focus();
      else (document.getElementById('contenido-principal')||document.getElementById('admin-content'))?.focus();
    });
  };
  return {confirm,confirmation:pending&&<Confirmation message={pending.message} onFinish={finish}/>};
}

function Confirmation({message,onFinish}){
  const dialog=useRef(null);
  useEffect(()=>{dialog.current?.showModal();},[]);
  return <dialog ref={dialog} className="confirmation-dialog" aria-label="Confirmar acción" onCancel={event=>{event.preventDefault();onFinish(false);}}>
    <div className="confirmation-heading"><span><Icon name="alert" size={23}/></span><div><small>ACCIÓN IMPORTANTE</small><h2>¿Quieres continuar?</h2></div></div><p>{message}</p><div className="confirmation-note"><Icon name="shield" size={15}/> MAIN protege tus cambios y te pedirá confirmación en acciones sensibles.</div><div className="confirmation-actions">
      <button autoFocus onClick={()=>onFinish(false)}>No, volver</button>
      <button className="confirm-action" onClick={()=>onFinish(true)}>Sí, continuar</button>
    </div>
  </dialog>;
}
