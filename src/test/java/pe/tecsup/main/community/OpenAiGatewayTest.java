package pe.tecsup.main.community;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class OpenAiGatewayTest {
 @SuppressWarnings("unchecked")
 HttpClient response(int status,String body)throws Exception{
  HttpClient client=mock(HttpClient.class);
  HttpResponse<String> response=mock(HttpResponse.class);
  when(response.statusCode()).thenReturn(status);when(response.body()).thenReturn(body);
  when(client.send(any(HttpRequest.class),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(response);
  return client;
 }
 @Test void parsesCompletedOutputWithoutDependingOnOutputArrayPosition()throws Exception{
  var client=response(200,"{\"status\":\"completed\",\"output\":[{\"type\":\"reasoning\"},{\"content\":[{\"type\":\"output_text\",\"text\":\"Cuatro.\"}]}]}");
  assertEquals("Cuatro.",new OpenAiGateway(new ObjectMapper(),"synthetic-test-value","gpt-5-mini",client).answer("2+2","Cursos: []"));
  var request=org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
  verify(client).send(request.capture(),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
  assertEquals("https://api.openai.com/v1/responses",request.getValue().uri().toString());
  assertEquals(45,request.getValue().timeout().orElseThrow().toSeconds());
 }
 @Test void exhaustedCreditsDifferFromTransientRateLimit()throws Exception{
  for(String code:new String[]{"credit_balance_exhausted","insufficient_quota"}){
   var client=response(429,"{\"error\":{\"code\":\""+code+"\",\"message\":\"sensitive upstream detail\"}}");
   var error=assertThrows(DomainException.class,()->new OpenAiGateway(new ObjectMapper(),"synthetic","gpt-5-mini",client).answer("Hola",""));
   assertEquals("AI_QUOTA_EXHAUSTED",error.code);assertFalse(error.getMessage().contains("sensitive"));
  }
  var rate=response(429,"{\"error\":{\"code\":\"rate_limit_exceeded\"}}");
  assertEquals("AI_RATE_LIMIT",assertThrows(DomainException.class,()->new OpenAiGateway(new ObjectMapper(),"synthetic","gpt-5-mini",rate).answer("Hola","")).code);
 }
 @Test void missingCredentialDoesNotCallProvider(){
  var client=mock(HttpClient.class);
  assertEquals("AI_NOT_CONFIGURED",assertThrows(DomainException.class,()->new OpenAiGateway(new ObjectMapper(),"","gpt-5-mini",client).answer("Hola","")).code);
  verifyNoInteractions(client);
 }
 @Test void incompleteOutputFailsExplicitly()throws Exception{
  var client=response(200,"{\"status\":\"incomplete\",\"output\":[]}");
  assertEquals("AI_INCOMPLETE",assertThrows(DomainException.class,()->new OpenAiGateway(new ObjectMapper(),"synthetic","gpt-5-mini",client).answer("Hola","")).code);
 }
}
