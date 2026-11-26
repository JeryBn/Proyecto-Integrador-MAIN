// Each read gets its own deadline; a timed-out request must not poison retries.
export async function boundedRead(read,{signal,timeout=30000,
 schedule=(fn,ms)=>setTimeout(fn,ms),cancel=id=>clearTimeout(id)}={}){
 const controller=new AbortController();let timedOut=false,timer;
 const abort=()=>controller.abort();
 if(signal?.aborted)abort();
 else signal?.addEventListener('abort',abort,{once:true});
 try{
  if(controller.signal.aborted){const error=new Error('Cancelled');error.name='AbortError';throw error;}
  timer=schedule(()=>{timedOut=true;controller.abort();},timeout);
  return await read(controller.signal);
 }catch(error){
  if(timedOut){const deadline=new Error('Read deadline exceeded');deadline.name='TimeoutError';throw deadline;}
  throw error;
 }finally{
  if(timer!==undefined)cancel(timer);
  signal?.removeEventListener('abort',abort);
 }
}
