package pe.tecsup.main.community;
import com.fasterxml.jackson.databind.*;
import java.net.*;import java.net.http.*;import java.nio.file.*;import java.nio.charset.StandardCharsets;
import java.security.*;import java.security.spec.PKCS8EncodedKeySpec;import java.time.*;import java.util.*;
import org.springframework.stereotype.Component;import org.springframework.beans.factory.annotation.Value;
@Component
public class FcmGateway {
 public enum Outcome { ACCEPTED, RETRY, INVALID_TOKEN }
 private final ObjectMapper json;private final String credentials;private final HttpClient client;
 private String accessToken="";private Instant accessExpiry=Instant.EPOCH;
 public FcmGateway(ObjectMapper json,@Value("${MAIN_FCM_CREDENTIALS_FILE:}") String credentials){this.json=json;this.credentials=credentials;this.client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build();}
 public boolean ready(){return !credentials.isBlank()&&Files.isRegularFile(Path.of(credentials));}
 private static String b64(byte[] bytes){return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
 private synchronized String token(JsonNode account)throws Exception{
  if(Instant.now().isBefore(accessExpiry))return accessToken;
  if(!account.path("type").asText().equals("service_account"))throw new IllegalArgumentException();
  String email=account.path("client_email").asText();if(!email.endsWith(".gserviceaccount.com"))throw new IllegalArgumentException();
  long now=Instant.now().getEpochSecond();
  String unsigned=b64(json.writeValueAsBytes(Map.of("alg","RS256","typ","JWT")))+"."+b64(json.writeValueAsBytes(Map.of("iss",email,"scope","https://www.googleapis.com/auth/firebase.messaging","aud","https://oauth2.googleapis.com/token","iat",now,"exp",now+3600)));
  String pem=account.path("private_key").asText().replace("-----BEGIN PRIVATE KEY-----","").replace("-----END PRIVATE KEY-----","").replaceAll("\\s","");
  var key=KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
  Signature signer=Signature.getInstance("SHA256withRSA");signer.initSign(key);signer.update(unsigned.getBytes(StandardCharsets.US_ASCII));
  String assertion=unsigned+"."+b64(signer.sign());
  var request=HttpRequest.newBuilder(URI.create("https://oauth2.googleapis.com/token")).timeout(Duration.ofSeconds(15)).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion="+URLEncoder.encode(assertion,StandardCharsets.UTF_8))).build();
  var response=client.send(request,HttpResponse.BodyHandlers.ofString());if(response.statusCode()!=200)throw new IllegalStateException();
  var body=json.readTree(response.body());accessToken=body.path("access_token").asText();if(accessToken.isBlank())throw new IllegalStateException();
  accessExpiry=Instant.now().plusSeconds(Math.max(0,Math.min(3600,body.path("expires_in").asInt())-120));return accessToken;
 }
 public static Map<String,Object> payload(String token,String binding,String delivery){
  // No title, sender, message body, course, conversation or user identifier goes to FCM.
  return Map.of("message",Map.of("token",token,"data",Map.of("event","MAIN_UPDATE","binding",binding,"delivery",delivery),"android",Map.of("priority","high","ttl","300s","collapse_key","main_updates")));
 }
 public Outcome send(String deviceToken,String binding,String delivery){
  try{var account=json.readTree(Files.readString(Path.of(credentials)));String project=account.path("project_id").asText();if(!project.matches("[a-z][a-z0-9-]{4,62}"))return Outcome.RETRY;
   var request=HttpRequest.newBuilder(URI.create("https://fcm.googleapis.com/v1/projects/"+project+"/messages:send")).timeout(Duration.ofSeconds(15)).header("Authorization","Bearer "+token(account)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload(deviceToken,binding,delivery)))).build();
   var response=client.send(request,HttpResponse.BodyHandlers.ofString());if(response.statusCode()==200)return Outcome.ACCEPTED;
   var error=json.readTree(response.body()).path("error");for(var detail:error.path("details"))if(detail.path("errorCode").asText().equals("UNREGISTERED"))return Outcome.INVALID_TOKEN;
   if(response.statusCode()==401||response.statusCode()==403)accessExpiry=Instant.EPOCH;
   return Outcome.RETRY;
  }catch(InterruptedException e){Thread.currentThread().interrupt();return Outcome.RETRY;}catch(Exception e){return Outcome.RETRY;}
 }
}
