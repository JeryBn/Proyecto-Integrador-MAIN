import {useEffect,useRef,useState} from 'react';
import {userApi} from '../datos/user-api.mjs';
import {useResource} from '../datos/user-hooks.jsx';

const path='/api/v1/calls';

export function requestCall(conversationId,kind){
  window.dispatchEvent(new CustomEvent('main:call',{detail:{conversationId,kind}}));
}

function gathered(peer,signal){
  if(peer.iceGatheringState==='complete')return Promise.resolve();
  return new Promise((resolve,reject)=>{
    const timer=setTimeout(()=>finish(new Error('No se pudo preparar la conexión de la llamada.')),8000);
    const changed=()=>{if(peer.iceGatheringState==='complete')finish();};
    const aborted=()=>finish(new DOMException('Cancelado','AbortError'));
    function finish(error){
      clearTimeout(timer);
      peer.removeEventListener('icegatheringstatechange',changed);
      signal.removeEventListener('abort',aborted);
      if(error)reject(error);else resolve();
    }
    peer.addEventListener('icegatheringstatechange',changed);
    signal.addEventListener('abort',aborted,{once:true});
    if(signal.aborted)aborted();else changed();
  });
}

function failure(error){
  if(error.name==='NotAllowedError')return 'Permite el acceso al micrófono o la cámara para continuar.';
  if(error.name==='NotFoundError')return 'El navegador no encontró un micrófono o cámara.';
  if(error.name==='NotReadableError')return 'El micrófono o la cámara están ocupados o no están disponibles.';
  return error.message||'No se pudo conectar la llamada.';
}

export function CallPanel({username}){
  const [state,setState]=useState(null),[error,setError]=useState(''),[muted,setMuted]=useState(false);
  const incoming=useResource(path+'/incoming',{interval:2000});
  const current=useRef(null),local=useRef(null),remote=useRef(null);
  const [streams,setStreams]=useState({local:null,remote:null});
  const invite=!state?incoming.data?.[0]:null;
  const available=window.isSecureContext&&typeof RTCPeerConnection==='function'&&!!navigator.mediaDevices?.getUserMedia;

  useEffect(()=>{
    if(remote.current&&remote.current.srcObject!==streams.remote)remote.current.srcObject=streams.remote;
    if(local.current&&local.current.srcObject!==streams.local)local.current.srcObject=streams.local;
  },[streams,state?.kind]);

  function dispose(session,notify=true){
    session.controller.abort();
    clearTimeout(session.poll);
    clearTimeout(session.connectTimeout);
    session.peer?.close();
    session.stream?.getTracks().forEach(track=>track.stop());
    if(notify&&session.id)userApi.call(path+'/'+session.id,{method:'DELETE'}).catch(()=>{});
    if(current.current===session){
      current.current=null;
      setStreams({local:null,remote:null});
      setState(null);
      setMuted(false);
      incoming.reload();
    }
  }

  function poll(session){
    if(current.current!==session)return;
    session.poll=setTimeout(async()=>{
      let call;
      try{call=await userApi.call(path+'/'+session.id,{signal:session.controller.signal});}
      catch(reason){
        if(current.current!==session)return;
        if([401,403,404].includes(reason.status)){dispose(session,false);return;}
        poll(session);
        return;
      }
      if(current.current!==session)return;
      if(call.state==='ENDED'){dispose(session,false);return;}
      if(session.outgoing&&call.answer&&!session.peer.remoteDescription){
        setState({kind:call.kind,label:'Conectando…'});
        clearTimeout(session.connectTimeout);
        session.connectTimeout=setTimeout(()=>{
          if(current.current!==session)return;
          setError('No se pudo establecer la conexión entre los dispositivos.');
          dispose(session);
        },20000);
        try{await session.peer.setRemoteDescription({type:'answer',sdp:call.answer});}
        catch(reason){if(current.current===session){setError(failure(reason));dispose(session);}return;}
      }
      poll(session);
    },1800);
  }

  async function connect(kind,conversationId,invitation){
    if(current.current)return;
    if(!available){setError('Las llamadas requieren un navegador compatible y una conexión HTTPS o local.');return;}
    const session={id:invitation?.id||crypto.randomUUID(),controller:new AbortController(),outgoing:!invitation};
    current.current=session;
    setState({kind,label:'Preparando llamada…'});
    setError('');
    try{
      session.stream=await navigator.mediaDevices.getUserMedia({audio:true,video:kind==='VIDEO'});
      if(current.current!==session){session.stream.getTracks().forEach(track=>track.stop());return;}
      const configuration=await userApi.call(path+'/ice',{signal:session.controller.signal});
      if(current.current!==session)return;
      session.peer=new RTCPeerConnection(configuration);
      const peer=session.peer;
      const remoteStream=new MediaStream();
      setStreams({local:session.stream,remote:remoteStream});
      peer.ontrack=({track})=>remoteStream.addTrack(track);
      peer.onconnectionstatechange=()=>{
        if(current.current!==session)return;
        if(peer.connectionState==='connected'){
          clearTimeout(session.connectTimeout);
          setState({kind,label:'En llamada'});
        }else if(peer.connectionState==='disconnected'){
          setState({kind,label:'Recuperando conexión…'});
          clearTimeout(session.connectTimeout);
          session.connectTimeout=setTimeout(()=>{
            if(current.current===session){setError('La conexión se interrumpió. Puedes volver a llamar.');dispose(session);}
          },15000);
        }else if(peer.connectionState==='failed'){
          setError('No pudimos conectar los dispositivos.');
          dispose(session);
        }
      };
      for(const track of session.stream.getTracks())peer.addTrack(track,session.stream);
      if(invitation)await peer.setRemoteDescription({type:'offer',sdp:invitation.offer});
      await peer.setLocalDescription(invitation?await peer.createAnswer():await peer.createOffer());
      await gathered(peer,session.controller.signal);
      if(current.current!==session)return;
      const sdp=peer.localDescription.sdp;
      if(invitation)await userApi.call(path+'/'+session.id+'/answer',{method:'PUT',json:{sdp},signal:session.controller.signal});
      else await userApi.call(path,{method:'POST',json:{id:session.id,conversationId,kind,sdp},signal:session.controller.signal});
      if(current.current!==session)return;
      if(peer.connectionState!=='connected'){
        setState({kind,label:invitation?'Conectando…':'Llamando…'});
        clearTimeout(session.connectTimeout);
        session.connectTimeout=setTimeout(()=>{
          if(current.current!==session)return;
          setError(invitation||peer.remoteDescription
            ?'No se pudo establecer la conexión entre los dispositivos.'
            :'La otra persona no contestó.');
          dispose(session);
        },invitation?20000:59000);
      }
      poll(session);
    }catch(reason){
      if(current.current===session){setError(failure(reason));dispose(session);}
    }
  }

  useEffect(()=>{
    const start=event=>connect(event.detail.kind,event.detail.conversationId);
    window.addEventListener('main:call',start);
    return ()=>{
      window.removeEventListener('main:call',start);
      if(current.current)dispose(current.current);
    };
  },[username,available]);

  async function reject(){
    try{await userApi.call(path+'/'+invite.id,{method:'DELETE'});incoming.reload();}
    catch(reason){setError(failure(reason));}
  }

  if(!invite&&!state&&!error)return null;
  return <section className="main-call" aria-label="Llamadas de COLEGIA MAIN">
    {error&&<div role="alert">{error} <button onClick={()=>setError('')}>Cerrar aviso</button></div>}
    {invite&&<><p>{invite.callerName} · {invite.kind==='VIDEO'?'Videollamada':'Llamada de audio'}</p>
      <button className="primary-action" onClick={()=>connect(invite.kind,invite.conversationId,invite)}>Aceptar</button>
      <button onClick={reject}>Rechazar</button></>}
    {state&&<>
      <p role="status">{state.label}</p>
      {state.kind==='VIDEO'?<div className="call-video"><video ref={remote} autoPlay playsInline aria-label="Video remoto"/>
        <video ref={local} autoPlay playsInline muted aria-label="Vista previa de tu cámara"/></div>:
        <audio ref={remote} autoPlay/>}
      <button disabled={!streams.local} aria-pressed={muted} onClick={()=>{
        streams.local.getAudioTracks().forEach(track=>{track.enabled=muted;});setMuted(!muted);
      }}>{muted?'Activar micrófono':'Silenciar'}</button>
      <button onClick={()=>{if(current.current)dispose(current.current);}}>Colgar</button>
    </>}
  </section>;
}
