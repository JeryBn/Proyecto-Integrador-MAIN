import {useCallback,useEffect,useRef,useState} from 'react';
import {userApi} from './user-api.mjs';
import {createReadPoller} from './read-poller.mjs';
export {useConfirmation} from '../../confirmation.jsx';

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
