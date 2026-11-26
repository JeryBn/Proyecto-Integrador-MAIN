package pe.tecsup.main.community;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;

class OllamaGatewayTest {
 @Test void disabledDoesNotNeedProviderOrKey(){
  var gateway=new OpenAiGateway(new ObjectMapper(),"","unused",java.net.http.HttpClient.newHttpClient());
  ReflectionTestUtils.setField(gateway,"provider","disabled");
  assertEquals("AI_NOT_CONFIGURED",assertThrows(DomainException.class,()->gateway.answer("Hola","Cursos: Geometría")).code);
 }
 @Test void optionalAdapterUsesConfiguredModelAndAuthorizedContext()throws Exception{
  var received=new AtomicReference<String>();
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/api/chat",exchange->{received.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
   byte[] body="{\"done\":true,\"message\":{\"content\":\"Explicación de prueba.\"}}".getBytes(StandardCharsets.UTF_8);
   exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();});
  server.start();
  try{
   var json=new ObjectMapper();var gateway=new OpenAiGateway(json,"","unused",java.net.http.HttpClient.newHttpClient());
   ReflectionTestUtils.setField(gateway,"provider","ollama");
   ReflectionTestUtils.setField(gateway,"ollamaUrl","http://127.0.0.1:"+server.getAddress().getPort());
   assertEquals("Explicación de prueba.",gateway.answer("Explica un triángulo","Cursos: Geometría"));
   var body=json.readTree(received.get());assertEquals("gemma3:4b",body.path("model").asText());assertFalse(body.path("stream").asBoolean());
   assertTrue(body.path("messages").get(1).path("content").asText().contains("Cursos: Geometría"));
  }finally{server.stop(0);}
 }
}
