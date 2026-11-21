package pe.tecsup.main.community;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:communication_tests;DB_CLOSE_DELAY=-1","main.admin.username=admin_chat","main.admin.password=SyntheticChatTest2026!","main.chat.encrypted-transport-preview=true","MAIN_TURN_URLS=turn:relay.example.invalid","MAIN_TURN_SECRET=synthetic-turn-test-value"})
@AutoConfigureMockMvc @Transactional
class CommunicationIntegrationTest{
 @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
 @Autowired IdentityService users;@Autowired CommunicationService chat;@Autowired MockMvc mvc;@Autowired JdbcTemplate db;
 final String admin="admin_chat";
 @Test void chatIdentityRequiresPossessionAndIsVisibleOnlyToChatParticipants()throws Exception{
  users.approveContact(admin,"alumno","otro");
  String conversation=chat.direct("alumno","otro"), root="/api/v1/chat-identity";
  UUID device=UUID.randomUUID();
  var generator=java.security.KeyPairGenerator.getInstance("Ed25519");
  var sender=user("alumno").roles("ALUMNO");
  var receiver=user("otro").roles("ALUMNO");
  var outsider=user("vacio").roles("ALUMNO");
  var identity=generator.generateKeyPair();
  String registration=chatIdentityRegistration("alumno",device,identity);
  String unregisterable=chatIdentityRegistration("otro",device,generator.generateKeyPair());
  String path=root+"/devices/"+device;
  mvc.perform(put(path).with(sender).with(csrf()).contentType("application/json").content(unregisterable))
      .andExpect(status().isBadRequest());
  for(int attempt=0;attempt<2;attempt++){
   mvc.perform(put(path).with(sender).with(csrf()).contentType("application/json").content(registration))
       .andExpect(status().isNoContent());
  }
  mvc.perform(put(path).with(sender).with(csrf()).contentType("application/json")
      .content(chatIdentityRegistration("alumno",device,generator.generateKeyPair())))
      .andExpect(status().isConflict());
  var key=Base64.getUrlEncoder().withoutPadding();
  byte[] point=new byte[65];
  point[0]=4;
  String encryption=key.encodeToString(point);
  String binding=chatIdentityRegistration("alumno",device,identity,encryption);
  mvc.perform(put(path).with(sender).with(csrf()).contentType("application/json").content(binding))
      .andExpect(status().isNoContent());
  point[1]=1;
  mvc.perform(put(path).with(sender).with(csrf()).contentType("application/json")
      .content(chatIdentityRegistration("alumno",device,identity,key.encodeToString(point))))
      .andExpect(status().isConflict());
  String listing=root+"/conversations/"+conversation+"/devices";
  mvc.perform(get(listing).with(receiver)).andExpect(status().isOk())
      .andExpect(header().string("Cache-Control","no-store"))
      .andExpect(jsonPath("$[0].deviceId").value(device.toString())).andExpect(jsonPath("$[0].encryptionKey").value(encryption));
  mvc.perform(get(listing).with(outsider)).andExpect(status().isNotFound());
 }

 private String chatIdentityRegistration(String actor,UUID device,java.security.KeyPair keys)throws Exception{
  return chatIdentityRegistration(actor,device,keys,null);
 }

 private String chatIdentityRegistration(String actor,UUID device,java.security.KeyPair keys,String encryption)throws Exception{
  var base64=Base64.getUrlEncoder().withoutPadding();
  byte[] encoded=keys.getPublic().getEncoded();
  String key=base64.encodeToString(Arrays.copyOfRange(encoded,encoded.length-32,encoded.length));
  var signature=java.security.Signature.getInstance("Ed25519");
  signature.initSign(keys.getPrivate());
  String payload=encryption==null?String.join("\n",ChatDeviceIdentity.PROOF_DOMAIN,actor,device.toString(),key)
      :String.join("\n",ChatDeviceIdentity.ENCRYPTION_DOMAIN,actor,device.toString(),key,encryption);
  signature.update(payload
      .getBytes(java.nio.charset.StandardCharsets.UTF_8));
  return json.writeValueAsString(new ChatDeviceIdentity.Registration(key,base64.encodeToString(signature.sign()),encryption));
 }
 @Test void encryptedTransportScopesOpaqueEnvelopesToTheRecipientDevice()throws Exception{
  users.approveContact(admin,"alumno","otro");
  String path="/api/v1/conversations/"+chat.direct("alumno","otro")+"/encrypted";
  UUID senderDevice=UUID.randomUUID(), receiverDevice=UUID.randomUUID(), message=UUID.randomUUID();
  var sender=user("alumno").roles("ALUMNO");
  var receiver=user("otro").roles("ALUMNO");
  byte[] point=new byte[65];
  point[0]=4;
  var encoder=Base64.getUrlEncoder().withoutPadding();
  String key=encoder.encodeToString(point), bytes=encoder.encodeToString(new byte[32]);
  db.update("INSERT INTO chat_device_identity (username,device_id,signing_key,encryption_key) VALUES (?,?,?,?)",
      "alumno",senderDevice.toString(),bytes,key);
  db.update("INSERT INTO chat_device_identity (username,device_id,signing_key,encryption_key) VALUES (?,?,?,?)",
      "otro",receiverDevice.toString(),bytes,key);
  String payload=json.writeValueAsString(new EncryptedChatTransport.Send(message,senderDevice,receiverDevice,1,key,bytes));
  for(int attempt=0;attempt<2;attempt++){
   mvc.perform(post(path).with(sender).with(csrf()).contentType("application/json").content(payload))
       .andExpect(status().isNoContent());
  }
  String inbox=path+"/devices/"+receiverDevice;
  var delivery=mvc.perform(get(inbox).with(receiver)).andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].ciphertext").value(bytes))
      .andExpect(jsonPath("$[0].messageId").value(message.toString())).andReturn();
  String cursor=json.readTree(delivery.getResponse().getContentAsString()).get(0).get("cursor").asText();
  mvc.perform(get(inbox).param("after",cursor).with(receiver)).andExpect(jsonPath("$.length()").value(0));
  mvc.perform(get(inbox).with(sender)).andExpect(status().isNotFound());
  mvc.perform(get(inbox).with(user("vacio").roles("ALUMNO"))).andExpect(status().isNotFound());
 }
 @Test void managedTurnReturnsTheScopedRelayCredentials(){
  var controller=new DirectCalls(db,chat,users,java.time.Clock.systemUTC(),
      "turns:relay.example.invalid:443?transport=tcp","","test-user","test-relay-password");
  var response=controller.ice(()->"alumno");
  assertEquals("no-store",response.getHeaders().getCacheControl());
  var server=response.getBody().iceServers().getFirst();
  assertEquals(List.of("turns:relay.example.invalid:443?transport=tcp"),server.urls());
  assertEquals("test-user",server.username());
  assertEquals("test-relay-password",server.credential());
 }
 @Test void signedIceConfigurationRequiresLoginAndDisablesCaching()throws Exception{
  mvc.perform(get("/api/v1/calls/ice")).andExpect(status().isUnauthorized());
  var result=mvc.perform(get("/api/v1/calls/ice").with(user("alumno").roles("ALUMNO")))
   .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
   .andExpect(jsonPath("$.iceServers[0].urls[0]").value("turn:relay.example.invalid")).andReturn();
  var relay=json.readTree(result.getResponse().getContentAsString()).get("iceServers").get(0);
  var mac=javax.crypto.Mac.getInstance("HmacSHA1");
  var utf8=java.nio.charset.StandardCharsets.UTF_8;
  mac.init(new javax.crypto.spec.SecretKeySpec("synthetic-turn-test-value".getBytes(utf8),"HmacSHA1"));
  assertEquals(Base64.getEncoder().encodeToString(mac.doFinal(relay.get("username").asText().getBytes(utf8))),
      relay.get("credential").asText());
 }
 @Test void onlyDirectCallParticipantsCanExchangeSignalsAndHangUp()throws Exception{
  users.approveContact(admin,"alumno","otro");
  String conversation=chat.direct("alumno","otro"), root="/api/v1/calls";
  UUID id=UUID.randomUUID();
  String signal="v=0\r\ns=-\r\n";
  String offer=json.writeValueAsString(new DirectCalls.Start(id,conversation,"VIDEO",signal));
  var caller=user("alumno").roles("ALUMNO");
  var callee=user("otro").roles("ALUMNO");
  var outsider=user("vacio").roles("ALUMNO");
  for(int i=0;i<2;i++)mvc.perform(post(root).with(caller).with(csrf()).contentType("application/json").content(offer))
      .andExpect(status().isCreated()).andExpect(jsonPath("$.state").value("RINGING"));
  mvc.perform(get(root+"/incoming").with(callee)).andExpect(jsonPath("$[0].id").value(id.toString()));
  mvc.perform(get(root+"/incoming").with(outsider)).andExpect(jsonPath("$.length()").value(0));
  mvc.perform(get(root+"/"+id).with(outsider)).andExpect(status().isNotFound());
  String answer=json.writeValueAsString(new DirectCalls.Answer(signal));
  mvc.perform(put(root+"/"+id+"/answer").with(caller).with(csrf()).contentType("application/json").content(answer))
      .andExpect(status().isNotFound());
  mvc.perform(put(root+"/"+id+"/answer").with(callee).with(csrf()).contentType("application/json").content(answer))
      .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("ACTIVE"));
  mvc.perform(delete(root+"/"+id).with(callee).with(csrf())).andExpect(status().isOk());
  mvc.perform(get(root+"/"+id).with(caller)).andExpect(jsonPath("$.state").value("ENDED")).andExpect(jsonPath("$.offer").doesNotExist());
 }
 @Test void aiDecidesDeliveryAndOnlyPublishesAPlaceholderForRemovedText()throws Exception{
  users.approveContact(admin,"alumno","otro");
  String id=chat.direct("alumno","otro");
  var draft=chat.send("alumno",id,new CommunicationService.Send(UUID.randomUUID().toString(),"Texto sintético de prueba"));
  var pending=new CommunicationService.PendingReview(draft.id(),draft.revision(),draft.contentHash(),draft.text());
  assertTrue(chat.messages("otro",id,0,20).isEmpty());
  mvc.perform(get("/api/admin/v1/moderation/messages").with(user(admin).roles("ADMIN")))
   .andExpect(status().isNotFound());
  chat.applyAutomaticReview(pending,CommunicationService.ChatDecision.EDIT);
  assertTrue(chat.messages("otro",id,0,20).isEmpty());
  assertEquals("REJECTED",chat.message("alumno",draft.id()).state());
  var edited=chat.edit("alumno",draft.id(),new CommunicationService.Edit(1,"Texto sintético corregido"));
  var next=new CommunicationService.PendingReview(edited.id(),edited.revision(),edited.contentHash(),edited.text());
  chat.applyAutomaticReview(pending,CommunicationService.ChatDecision.ALLOW);
  assertTrue(chat.messages("otro",id,0,20).isEmpty());
  chat.applyAutomaticReview(next,CommunicationService.ChatDecision.REMOVE);
  var receiver=chat.message("otro",draft.id());
  assertEquals("REMOVED",receiver.state());
  assertEquals("Mensaje eliminado por lenguaje inapropiado.",receiver.text());
  assertEquals("",receiver.contentHash());
  assertThrows(DomainException.class,()->chat.edit("alumno",draft.id(),new CommunicationService.Edit(2,"Otro intento")));
 }
 @Test void conversationLinkResolvesOnlyForCurrentMember() throws Exception {
  String id=chat.direct("alumno","otro");
  mvc.perform(get("/api/v1/conversations/"+id).with(user("alumno").roles("ALUMNO"))).andExpect(status().isOk()).andExpect(jsonPath("$.kind").value("DIRECT"));
  mvc.perform(get("/api/v1/conversations/"+id).with(user("vacio").roles("ALUMNO"))).andExpect(status().isNotFound());
 }
 @Test void invitationMetadataDoesNotGrantMessagesAndDisappearsWhenDeclined() throws Exception {
  String id=chat.group("alumno","Grupo sintético");chat.invite("alumno",id,"otro");
  assertEquals("INVITED",chat.conversation("otro",id).state());
  mvc.perform(get("/api/v1/conversations/"+id+"/messages").with(user("otro").roles("ALUMNO"))).andExpect(status().isNotFound());
  chat.invitation("otro",id,false);assertThrows(DomainException.class,()->chat.conversation("otro",id));
 }
 @BeforeEach void setup(){
  users.create(admin,new IdentityService.NewAccount("docente_chat","Docente chat","DOCENTE","SyntheticTeacher2026!",null,1L));
  users.create(admin,new IdentityService.NewAccount("familiar_chat","Familiar chat","FAMILIAR","SyntheticFamily2026!",null,null));
  for(String a:List.of("alumno","otro","docente_chat","familiar_chat"))for(String b:List.of("alumno","otro","docente_chat","familiar_chat"))
   if(a.compareTo(b)<0)users.approveContact(admin,a,b);
 }
 CommunicationService.Message send(String conversation,String text){return chat.send("alumno",conversation,new CommunicationService.Send(UUID.randomUUID().toString(),text));}
 void approve(CommunicationService.Message message){chat.review(admin,message.id(),new CommunicationService.Review(message.revision(),message.contentHash(),true,""));}
 @Test void pendingAndRejectedMessagesAreInvisibleToRecipientAndCannotBeSelfApproved()throws Exception{
  String id=chat.direct("alumno","otro");var draft=send(id,"Texto pendiente de revisión.");
  assertTrue(chat.messages("otro",id,0,30).isEmpty());assertEquals("PENDING",chat.messages("alumno",id,0,30).getFirst().state());
  mvc.perform(get("/api/admin/v1/moderation/messages").with(user("alumno").roles("ALUMNO"))).andExpect(status().isForbidden());
  chat.review(admin,draft.id(),new CommunicationService.Review(1,draft.contentHash(),false,"Norma de prueba"));
  assertTrue(chat.messages("otro",id,0,30).isEmpty());
  assertEquals("REJECTED",chat.message("alumno",draft.id()).state());
 }
 @Test void correctedVersionIsReviewedAgainAndOnlyApprovedTextIsDelivered(){
  String id=chat.direct("alumno","otro");var first=send(id,"Original privado");
  chat.review(admin,first.id(),new CommunicationService.Review(1,first.contentHash(),false,"Regla de prueba"));
  var edited=chat.edit("alumno",first.id(),new CommunicationService.Edit(1,"Texto corregido"));
  assertTrue(chat.messages("otro",id,0,30).isEmpty());approve(edited);
  var delivered=chat.messages("otro",id,0,30).getFirst();
  assertEquals("Texto corregido",delivered.text());assertTrue(delivered.correctedBeforeDelivery());assertEquals("",delivered.reason());
  assertFalse(delivered.text().contains("Original"));assertEquals("",delivered.contentHash());
 }
 @Test void postDeliveryEditKeepsOldApprovedVersionUntilNewApproval(){
  String id=chat.direct("alumno","otro");var first=send(id,"Versión visible");approve(first);
  var edited=chat.edit("alumno",first.id(),new CommunicationService.Edit(1,"Cambio aún privado"));
  assertEquals("Versión visible",chat.messages("otro",id,0,30).getFirst().text());
  assertThrows(DomainException.class,()->chat.review(admin,first.id(),new CommunicationService.Review(1,first.contentHash(),true,"")));
  approve(edited);var delivered=chat.messages("otro",id,0,30).getFirst();
  assertEquals("Cambio aún privado",delivered.text());assertTrue(delivered.editedAfterDelivery());assertFalse(delivered.correctedBeforeDelivery());
 }
 @Test void retriesAreIdempotentAndChangedPayloadCannotReuseKey(){
  String id=chat.direct("alumno","otro"),key=UUID.randomUUID().toString();
  var first=chat.send("alumno",id,new CommunicationService.Send(key,"Mensaje único"));
  assertEquals(first.id(),chat.send("alumno",id,new CommunicationService.Send(key,"Mensaje único")).id());
  assertThrows(DomainException.class,()->chat.send("alumno",id,new CommunicationService.Send(key,"Otro texto")));
  assertEquals(1,chat.messages("alumno",id,0,30).size());
 }
 @Test void onlyAuthorizedContactsCanStartAndBlocksPreventApprovalAndDelivery(){
  assertThrows(DomainException.class,()->chat.direct("alumno","vacio"));
  String id=chat.direct("alumno","otro");var draft=send(id,"Texto");
  users.contactPreference("otro","alumno",false,true);
  assertThrows(DomainException.class,()->approve(draft));assertTrue(chat.messages("otro",id,0,30).isEmpty());
  assertThrows(DomainException.class,()->send(id,"Intento bloqueado"));
 }
 @Test void invitationsRequireAcceptanceAndRevocationRemovesAllAccess(){
  String id=chat.group("alumno","Grupo académico");chat.invite("alumno",id,"otro");
  assertThrows(DomainException.class,()->chat.messages("otro",id,0,30));
  chat.invitation("otro",id,true);var draft=send(id,"Mensaje grupal");approve(draft);
  assertEquals(1,chat.messages("otro",id,0,30).size());
  chat.membership("alumno",id,"otro",false,true);
  assertThrows(DomainException.class,()->chat.messages("otro",id,0,30));
  assertThrows(DomainException.class,()->chat.send("otro",id,new CommunicationService.Send(UUID.randomUUID().toString(),"No autorizado")));
 }
 @Test void groupSupportsAllRolesAndLastAdministratorCannotLeave(){
  String id=chat.group("familiar_chat","Familia y docentes");chat.invite("familiar_chat",id,"docente_chat");chat.invitation("docente_chat",id,true);
  assertThrows(DomainException.class,()->chat.membership("familiar_chat",id,"familiar_chat",false,true));
  chat.membership("familiar_chat",id,"docente_chat",true,false);chat.membership("familiar_chat",id,"familiar_chat",false,true);
  assertEquals(1,chat.members("docente_chat",id).size());
 }
 @Test void teachersAndFamiliesAlsoRequireModerationAndForeignAccountsCannotRead(){
  String id=chat.direct("docente_chat","familiar_chat");
  var draft=chat.send("docente_chat",id,new CommunicationService.Send(UUID.randomUUID().toString(),"Consulta docente"));
  assertTrue(chat.messages("familiar_chat",id,0,30).isEmpty());approve(draft);
  assertEquals(1,chat.messages("familiar_chat",id,0,30).size());
  assertThrows(DomainException.class,()->chat.messages("alumno",id,0,30));
 }
 @Test void cancellationAndStaleRevisionCannotBeApproved(){
  String id=chat.direct("alumno","otro");var draft=send(id,"Cancelar");chat.cancel("alumno",draft.id(),1);
  assertThrows(DomainException.class,()->approve(draft));assertTrue(chat.messages("otro",id,0,30).isEmpty());
 }
 @Test void csrfAndPaginationCannotBeBypassed()throws Exception{
  String id=chat.direct("alumno","otro");
  mvc.perform(post("/api/v1/conversations/"+id+"/messages").with(user("alumno").roles("ALUMNO")).contentType("application/json")
   .content("{\"clientKey\":\""+UUID.randomUUID()+"\",\"text\":\"Sin CSRF\"}")).andExpect(status().isForbidden());
  mvc.perform(get("/api/v1/conversations/"+id+"/messages?limit=101").with(user("alumno").roles("ALUMNO"))).andExpect(status().isBadRequest());
 }
 @Test void newGroupMembersCannotReadMessagesFromBeforeTheyJoined(){
  String id=chat.group("alumno","Historial privado");var previous=send(id,"Anterior a la invitación");approve(previous);
  chat.invite("alumno",id,"otro");chat.invitation("otro",id,true);
  var boundary=db.queryForObject("SELECT created_at FROM message WHERE id=?",java.sql.Timestamp.class,previous.id());
  db.update("UPDATE conversation_member SET joined_at=? WHERE conversation_id=? AND username='otro'",boundary,id);
  assertTrue(chat.messages("otro",id,0,30).isEmpty());
  var next=send(id,"Posterior a la incorporación");db.update("UPDATE message SET created_at=? WHERE id=?",boundary,next.id());approve(next);
  assertEquals(1,chat.messages("otro",id,0,30).size());
 }
 @Test void manualReviewInterfaceIsRestrictedToAdministrators()throws Exception{
  mvc.perform(get("/moderation.html").with(user("alumno").roles("ALUMNO"))).andExpect(status().isForbidden());
  mvc.perform(get("/moderation.html").with(user(admin).roles("ADMIN"))).andExpect(status().isOk());
 }
}
