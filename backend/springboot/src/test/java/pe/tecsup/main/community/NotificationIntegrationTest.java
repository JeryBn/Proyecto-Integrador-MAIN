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
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:notice_tests;DB_CLOSE_DELAY=-1","main.admin.username=admin_notice","main.admin.password=SyntheticNotices2026!"})
@AutoConfigureMockMvc @Transactional
class NotificationIntegrationTest {
 @Autowired IdentityService users;@Autowired CommunicationService chat;@Autowired NotificationService notices;@Autowired MockMvc mvc;
 @BeforeEach void setup(){users.approveContact("admin_notice","alumno","otro");}
 CommunicationService.Message send(String id){return chat.send("alumno",id,new CommunicationService.Send(UUID.randomUUID().toString(),"Texto privado de prueba"));}
 void approve(CommunicationService.Message m){chat.review("admin_notice",m.id(),new CommunicationService.Review(m.revision(),m.contentHash(),true,""));}
 @Test void onlyApprovedMessageCreatesOneNoticeAndNoPrivateTextIsExposed()throws Exception{
  String conversation=chat.direct("alumno","otro");var message=send(conversation);
  assertTrue(notices.list("otro",0,30).isEmpty());approve(message);
  assertEquals(1,notices.list("otro",0,30).size());
  var edited=chat.edit("alumno",message.id(),new CommunicationService.Edit(1,"Nuevo texto"));approve(edited);
  assertEquals(1,notices.list("otro",0,30).size());
  var notice=notices.list("otro",0,30).getFirst();assertFalse(notice.read());
  notices.read("otro",notice.id());notices.read("otro",notice.id());assertTrue(notices.list("otro",0,30).getFirst().read());
  assertThrows(DomainException.class,()->notices.read("alumno",notice.id()));
  mvc.perform(get("/api/v1/notifications").with(user("otro").roles("ALUMNO"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Texto privado"))));
 }
 @Test void mutedDisabledPreferencesAndBlocksSuppressNotifications(){
  String id=chat.direct("alumno","otro");
  users.contactPreference("otro","alumno",true,false);approve(send(id));assertTrue(notices.list("otro",0,30).isEmpty());
  users.contactPreference("otro","alumno",false,false);
  users.preferences("otro",new IdentityService.Preferences(false,false,"CONTACTS"));approve(send(id));assertTrue(notices.list("otro",0,30).isEmpty());
  users.preferences("otro",new IdentityService.Preferences(false,true,"CONTACTS"));approve(send(id));assertEquals(1,notices.list("otro",0,30).size());
  users.contactPreference("otro","alumno",false,true);assertTrue(notices.list("otro",0,30).isEmpty());
 }
 @Test void invitationAndMembershipRevocationAreRechecked(){
  String id=chat.group("alumno","Grupo");chat.invite("alumno",id,"otro");chat.invite("alumno",id,"otro");
  assertEquals(1,notices.list("otro",0,30).size());chat.invitation("otro",id,true);assertTrue(notices.list("otro",0,30).isEmpty());
  approve(send(id));assertEquals(1,notices.list("otro",0,30).size());
  chat.membership("alumno",id,"otro",false,true);assertTrue(notices.list("otro",0,30).isEmpty());
 }
 @Test void readRequiresCsrfAndPaginationIsBounded()throws Exception{
  mvc.perform(post("/api/v1/notifications/missing/read").with(user("otro").roles("ALUMNO")).contentType("application/json").content("{}")).andExpect(status().isForbidden());
  mvc.perform(get("/api/v1/notifications?limit=101").with(user("otro").roles("ALUMNO"))).andExpect(status().isBadRequest());
 }
}
