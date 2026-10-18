package pe.tecsup.main.community;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:communication_tests;DB_CLOSE_DELAY=-1","main.admin.username=admin_chat","main.admin.password=SyntheticChatTest2026!"})
@AutoConfigureMockMvc @Transactional
class CommunicationIntegrationTest{
 @Autowired IdentityService users;@Autowired CommunicationService chat;@Autowired MockMvc mvc;
 final String admin="admin_chat";
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
  String id=chat.group("alumno","Historial privado");approve(send(id,"Anterior a la invitación"));
  chat.invite("alumno",id,"otro");chat.invitation("otro",id,true);
  assertTrue(chat.messages("otro",id,0,30).isEmpty());
  approve(send(id,"Posterior a la incorporación"));assertEquals(1,chat.messages("otro",id,0,30).size());
 }
 @Test void manualReviewInterfaceIsRestrictedToAdministrators()throws Exception{
  mvc.perform(get("/moderation.html").with(user("alumno").roles("ALUMNO"))).andExpect(status().isForbidden());
  mvc.perform(get("/moderation.html").with(user(admin).roles("ADMIN"))).andExpect(status().isOk());
 }
}
