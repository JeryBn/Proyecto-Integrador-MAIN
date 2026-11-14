package pe.tecsup.main.community;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BoundedResponseBodyTest {
 private static final class Subscription implements Flow.Subscription {
  long requested;boolean cancelled;
  @Override public void request(long count){requested+=count;}
  @Override public void cancel(){cancelled=true;}
 }
 private static HttpResponse.BodySubscriber<String> subscriber(int maxBytes){return BoundedResponseBody.utf8(maxBytes).apply(null);}

 @Test void joinsUtf8ChunksWithoutChangingCharacters(){
  var subscriber=subscriber(16);var subscription=new Subscription();subscriber.onSubscribe(subscription);
  subscriber.onNext(List.of(ByteBuffer.wrap("á".getBytes(StandardCharsets.UTF_8)),ByteBuffer.wrap("rbol".getBytes(StandardCharsets.UTF_8))));subscriber.onComplete();
  assertEquals("árbol",subscriber.getBody().toCompletableFuture().join());assertEquals(2,subscription.requested);assertFalse(subscription.cancelled);
 }
 @Test void acceptsTheExactByteLimit(){
  var subscriber=subscriber(4);subscriber.onSubscribe(new Subscription());subscriber.onNext(List.of(ByteBuffer.wrap(new byte[4])));subscriber.onComplete();
  assertEquals(4,subscriber.getBody().toCompletableFuture().join().length());
 }
 @Test void cancelsBeforeKeepingAnOversizedResponse(){
  var subscriber=subscriber(4);var subscription=new Subscription();subscriber.onSubscribe(subscription);subscriber.onNext(List.of(ByteBuffer.wrap(new byte[5])));
  var error=assertThrows(CompletionException.class,()->subscriber.getBody().toCompletableFuture().join());
  assertInstanceOf(IOException.class,error.getCause());assertTrue(subscription.cancelled);
 }
 @Test void propagatesAnUpstreamFailure(){
  var subscriber=subscriber(8);var subscription=new Subscription();subscriber.onSubscribe(subscription);subscriber.onError(new IOException("fallo sintético"));
  var error=assertThrows(CompletionException.class,()->subscriber.getBody().toCompletableFuture().join());
  assertInstanceOf(IOException.class,error.getCause());assertTrue(subscription.cancelled);
 }
}
