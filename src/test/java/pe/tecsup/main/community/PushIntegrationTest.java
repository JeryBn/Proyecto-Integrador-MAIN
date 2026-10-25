package pe.tecsup.main.community;
import java.util.*;import java.time.*;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.session.SessionRegistry;import org.springframework.security.core.userdetails.User;
import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:push_tests;DB_CLOSE_DELAY=-1","MAIN_PUSH_ENABLED=true","MAIN_PUSH_ENCRYPTION_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=","main.push.interval-ms=3600000","main.admin.username=admin_push","main.admin.password=SyntheticPushTest2026!"})
@AutoConfigureMockMvc @Transactional
class PushIntegrationTest {
 @Autowired PushService push;@Autowired NotificationService notices;@Autowired CommunicationService chat;@Autowired IdentityService users;
 @Autowired PushTokenCipher cipher;@Autowired JdbcTemplate db;@Autowired SessionRegistry sessions;@Autowired MockMvc mvc;
 @MockitoBean FcmGateway gateway;
 String device,session,token;Object principal;
 @BeforeEach void prepare(){when(gateway.ready()).thenReturn(true);when(gateway.send(anyString(),anyString(),anyString())).thenReturn(FcmGateway.Outcome.ACCEPTED);
  users.approveContact("admin_push","alumno","otro");device=UUID.randomUUID().toString();session=UUID.randomUUID().toString();token="synthetic-fcm-token-"+UUID.randomUUID();
  principal=User.withUsername("otro").password("unused").roles("ALUMNO").build();sessions.registerNewSession(session,principal);
 }
 @AfterEach void cleanup(){sessions.removeSessionInformation(session);}
 String register(){return push.register("otro",session,1800,new PushService.Registration(device,token)).get("binding");}
 CommunicationService.Message pending(){String c=chat.direct("alumno","otro");return chat.send("alumno",c,new CommunicationService.Send(UUID.randomUUID().toString(),"Contenido privado sintético"));}
 void approve(CommunicationService.Message m){chat.review("admin_push",m.id(),new CommunicationService.Review(m.revision(),m.contentHash(),true,""));}
 @Test void registrationEncryptsTokenAndBindsSession(){String binding=register();assertEquals(binding,register());var stored=db.queryForMap("SELECT * FROM push_device WHERE id=?",device);assertFalse(stored.toString().contains(token));assertFalse(stored.toString().contains(session));assertEquals(token,cipher.decrypt((String)stored.get("TOKEN_CIPHER"),device));}
 @Test void pendingNeverSendsAndApprovalIsDeduplicated(){register();var m=pending();push.dispatch();verify(gateway,never()).send(anyString(),anyString(),anyString());approve(m);push.dispatch();push.dispatch();verify(gateway,times(1)).send(eq(token),anyString(),anyString());assertEquals("ACCEPTED",db.queryForObject("SELECT state FROM push_delivery WHERE device_id=?",String.class,device));}
 @Test void revokedPreferencesOrAlreadyReadNoticeDoNotSend(){register();var m=pending();approve(m);var item=notices.list("otro",0,30).getFirst();notices.read("otro",item.id());push.dispatch();verify(gateway,never()).send(anyString(),anyString(),anyString());}
 @Test void blockedContactPreventsQueuedPush(){register();var m=pending();approve(m);db.update("INSERT INTO contact_preference(owner,subject,muted,blocked) VALUES('otro','alumno',FALSE,TRUE)");push.dispatch();verify(gateway,never()).send(anyString(),anyString(),anyString());}
 @Test void expiredSessionRemovesDevice(){register();var m=pending();approve(m);sessions.getSessionInformation(session).expireNow();push.dispatch();assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM push_device WHERE id=?",Integer.class,device));verify(gateway,never()).send(anyString(),anyString(),anyString());}
 @Test void invalidFcmTokenIsRemoved(){register();var m=pending();approve(m);when(gateway.send(anyString(),anyString(),anyString())).thenReturn(FcmGateway.Outcome.INVALID_TOKEN);push.dispatch();assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM push_device WHERE id=?",Integer.class,device));}
 @RepeatedTest(10) void retryWaitsAndDoesNotPretendDelivery(){register();var m=pending();approve(m);when(gateway.send(anyString(),anyString(),anyString())).thenReturn(FcmGateway.Outcome.RETRY);push.dispatch();push.dispatch();assertEquals("PENDING",db.queryForObject("SELECT state FROM push_delivery WHERE device_id=?",String.class,device));verify(gateway,times(1)).send(anyString(),anyString(),anyString());}
 @Test void ownerCannotDeleteAnotherDeviceOrRebindItsId(){register();push.remove("alumno",device);assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM push_device WHERE id=?",Integer.class,device));assertThrows(DomainException.class,()->push.register("alumno","another",1800,new PushService.Registration(device,token)));}
 @Test void registrationRequiresCsrfAndSession()throws Exception{mvc.perform(post("/api/v1/push/devices").with(user("otro").roles("ALUMNO")).contentType("application/json").content("{}" )).andExpect(status().isForbidden());}
 @Test void fcmPayloadNeverContainsConversationContent(){var payload=FcmGateway.payload("token","binding","delivery");String serialized=payload.toString();assertFalse(serialized.contains("notification="));assertFalse(serialized.contains("Contenido"));assertTrue(serialized.contains("MAIN_UPDATE"));}
 @Test void cipherRejectsWrongDeviceAndTampering(){String encrypted=cipher.encrypt("synthetic-token",device);assertThrows(IllegalStateException.class,()->cipher.decrypt(encrypted,UUID.randomUUID().toString()));assertThrows(IllegalStateException.class,()->cipher.decrypt(encrypted+"broken",device));}
}
