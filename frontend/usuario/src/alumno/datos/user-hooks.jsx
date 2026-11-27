import {useCallback,useEffect,useRef,useState} from 'react';
import {userApi} from './user-api.mjs';
import {createReadPoller} from './read-poller.mjs';

export function useResource(path,{enabled=true,interval=0}={}){
  const [state,setState]=useState({data:null,error:'',loading:true});
  const [revision,setRevision]=useState(0);const serial=useRef(0);
  const reload=useCallback(()=>setRevision(n=>n+1),[]);
  useEffect(()=>{
    const current=++serial.current;
    if(!enabled||!path)return;
    let disposed=false,pending=false;const controller=new AbortController();
    setState({data:null,error:'',loading:true});
    async function load(){
      if(pending||disposed)return;pending=true;
      try{const data=await userApi.call(path,{signal:controller.signal});if(!disposed&&current===serial.current)setState({data,error:'',loading:false});}
      catch(e){if(!disposed&&current===serial.current)setState(old=>({data:[401,403,404].includes(e.status)||e.code==='ACCOUNT_CHANGED'?null:old.data,error:e.message,loading:false}));throw e;}
      finally{pending=false;}
    }
    const poller=interval?createReadPoller({read:load,interval,available:()=>!document.hidden&&navigator.onLine!==false}):null;
    const visible=()=>poller?.wake();
    if(poller){document.addEventListener('visibilitychange',visible);window.addEventListener('focus',visible);window.addEventListener('online',visible);poller.start();}
    else load().catch(()=>{});
    return ()=>{disposed=true;poller?.stop();controller.abort();document.removeEventListener('visibilitychange',visible);window.removeEventListener('focus',visible);window.removeEventListener('online',visible);};
  },[path,enabled,interval,revision]);
  return {...state,reload};
}
export function Feedback({resource}){return <>{resource.loading&&<p role="status">Cargando…</p>}{resource.error&&<div role="alert"><p>{resource.error}</p><button onClick={resource.reload}>Reintentar</button></div>}</>;}

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
      else document.getElementById('contenido-principal')?.focus();
    });
  };
  return {confirm,confirmation:pending&&<Confirmation message={pending.message} onFinish={finish}/>};
}
function Confirmation({message,onFinish}){
  const dialog=useRef(null);
  useEffect(()=>{dialog.current?.showModal();},[]);
  return <dialog ref={dialog} className="confirmation-dialog" aria-label="Confirmar acción" onCancel={e=>{e.preventDefault();onFinish(false);}}>
    <h2>Confirmar acción</h2><p>{message}</p><div className="toolbar">
      <button autoFocus onClick={()=>onFinish(false)}>Cancelar acción</button>
      <button onClick={()=>onFinish(true)}>Confirmar acción</button>
    </div>
  </dialog>;
}
