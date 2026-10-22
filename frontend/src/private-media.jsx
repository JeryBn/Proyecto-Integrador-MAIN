import {useState} from 'react';
export function PrivateMedia({kind,path,name='Archivo privado',active=true}){
 const [failed,setFailed]=useState(false),[attempt,setAttempt]=useState(0);
 if(!active||kind==='NONE'||kind==='TEXT')return null;
 if(failed)return <div role="alert"><p>El archivo no está disponible o tu acceso cambió. Actualiza la conversación o el estado.</p><button onClick={()=>{setFailed(false);setAttempt(n=>n+1);}}>Reintentar archivo</button></div>;
 if(kind==='FILE')return <a className="file-link" href={path} download>{name} · Descargar TXT</a>;
 const source=path+(path.includes('?')?'&':'?')+'view='+attempt;
 return kind==='IMAGE'?<img className="private-media" src={source} alt={name} loading="lazy" onError={()=>setFailed(true)}/>:kind==='VIDEO'?<video className="private-media" src={source} controls playsInline preload="metadata" onError={()=>setFailed(true)}/>:<audio className="private-media" src={source} controls preload="metadata" onError={()=>setFailed(true)}/>;
}
