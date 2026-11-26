package pe.tecsup.main.community;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

final class BoundedResponseBody {
 private BoundedResponseBody() {}

 static HttpResponse.BodyHandler<String> utf8(int maxBytes) {
  if(maxBytes<=0)throw new IllegalArgumentException("El límite debe ser positivo.");
  return ignored->new Utf8Subscriber(maxBytes);
 }

 private static final class Utf8Subscriber implements HttpResponse.BodySubscriber<String> {
  private final int maxBytes;
  private final ByteArrayOutputStream output;
  private final CompletableFuture<String> body=new CompletableFuture<>();
  private Flow.Subscription subscription;
  private int size;
  private boolean finished;

  private Utf8Subscriber(int maxBytes){this.maxBytes=maxBytes;this.output=new ByteArrayOutputStream(Math.min(maxBytes,8192));}

  @Override public CompletionStage<String> getBody(){return body;}
  @Override public void onSubscribe(Flow.Subscription value){
   if(subscription!=null){value.cancel();return;}
   subscription=value;value.request(1);
  }
  @Override public void onNext(List<ByteBuffer> chunks){
   if(finished)return;
   try{
    for(ByteBuffer chunk:chunks){
     int count=chunk.remaining();
     if(count>maxBytes-size){fail(new IOException("La respuesta HTTP supera el límite permitido."));return;}
     byte[] bytes=new byte[count];chunk.get(bytes);output.write(bytes);size+=count;
    }
    subscription.request(1);
   }catch(Exception error){fail(error);}
  }
  @Override public void onError(Throwable error){fail(error);}
  @Override public void onComplete(){
   if(finished)return;finished=true;
   body.complete(new String(output.toByteArray(),StandardCharsets.UTF_8));
  }
  private void fail(Throwable error){
   if(finished)return;finished=true;
   if(subscription!=null)subscription.cancel();
   body.completeExceptionally(error);
  }
 }
}
