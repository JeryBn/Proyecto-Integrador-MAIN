// Read-only scheduler: callers supply GET operations, never message mutations.
export function createReadPoller({read,interval=5000,maxDelay=60000,available=()=>true,
  schedule=(fn,ms)=>setTimeout(fn,ms),cancel=id=>clearTimeout(id)}){
  let stopped=false,running=false,timer=null,failures=0,wakePending=false;
  const clear=()=>{if(timer!==null)cancel(timer);timer=null;};
  async function tick(){
    clear();if(stopped)return;
    if(running){wakePending=true;return;}
    if(!available()){return;}
    running=true;
    try{await read();failures=0;}catch{failures=Math.min(failures+1,16);}
    finally{
      running=false;
      if(!stopped){
        const delay=wakePending?0:Math.min(maxDelay,interval*2**failures);
        wakePending=false;timer=schedule(tick,delay);
      }
    }
  }
  return {start:tick,wake:tick,stop(){stopped=true;clear();}};
}
