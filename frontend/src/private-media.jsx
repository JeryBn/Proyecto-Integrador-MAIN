import {useState,useRef,useEffect} from 'react';
export function PrivateMedia({kind,path,name='Archivo privado',active=true,onEnded}){
 const [failed,setFailed]=useState(false),[attempt,setAttempt]=useState(0);
 const media=useRef(null);
 useEffect(()=>{const hide=()=>{if(document.hidden)media.current?.pause();};document.addEventListener('visibilitychange',hide);return()=>{document.removeEventListener('visibilitychange',hide);media.current?.pause();};},[path,kind]);
 if(!active||kind==='NONE'||kind==='TEXT')return null;
 if(failed)return <div role="alert"><p>El archivo no está disponible o tu acceso cambió. Actualiza la conversación o el estado.</p><button onClick={()=>{setFailed(false);setAttempt(n=>n+1);}}>Reintentar archivo</button></div>;
 if(kind==='FILE')return <a className="file-link" href={path} download>{name} · Descargar TXT</a>;
 const source=path+(path.includes('?')?'&':'?')+'view='+attempt;
 return kind==='IMAGE'?<img className="private-media" src={source} alt={name} loading="lazy" onError={()=>setFailed(true)}/>:kind==='VIDEO'?<video ref={media} className="private-media" src={source} controls playsInline preload="metadata" onEnded={onEnded} onError={()=>setFailed(true)}/>:<audio ref={media} className="private-media" src={source} controls preload="metadata" onEnded={onEnded} onError={()=>setFailed(true)}/>;
}
