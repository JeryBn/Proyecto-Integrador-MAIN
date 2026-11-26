package pe.tecsup.main.community;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:appeals_tests;DB_CLOSE_DELAY=-1","main.admin.username=appeal_admin","main.admin.password=SyntheticAppealTest2026!"})
@AutoConfigureMockMvc @Transactional
class AppealIntegrationTest {
 @Autowired IdentityService users; @Autowired CommunicationService chat; @Autowired AppealService appeals; @Autowired MockMvc mvc; @Autowired JdbcTemplate db;
 CommunicationService.Message rejected(){users.approveContact("appeal_admin","alumno","otro");String id=chat.direct("alumno","otro");var m=chat.send("alumno",id,new CommunicationService.Send(UUID.randomUUID().toString(),"Texto sintético apelado"));chat.review("appeal_admin",m.id(),new CommunicationService.Review(1,m.contentHash(),false,"Regla sintética"));return m;}
 AppealService.Input input(CommunicationService.Message m){return new AppealService.Input("messages",m.id(),m.revision(),m.contentHash(),"Solicito revisión del contexto educativo.");}
 @Test void ownerCreatesOnceAndOtherUsersCannotSeeOrAppeal(){var m=rejected();var first=appeals.create("alumno",input(m));assertEquals("OPEN",first.state());assertEquals(first.id(),appeals.create("alumno",input(m)).id());assertEquals(1,appeals.list("alumno",0,30).size());assertTrue(appeals.list("otro",0,30).isEmpty());assertThrows(DomainException.class,()->appeals.create("otro",input(m)));assertTrue(chat.messages("otro",m.conversationId(),0,30).isEmpty());}
 @Test void pendingApprovedPurgedAndOldRevisionsCannotBeAppealed(){var m=rejected();chat.edit("alumno",m.id(),new CommunicationService.Edit(1,"Nueva versión"));assertThrows(DomainException.class,()->appeals.create("alumno",input(m)));var fresh=chat.message("alumno",m.id());assertThrows(DomainException.class,()->appeals.create("alumno",input(fresh)));chat.review("appeal_admin",m.id(),new CommunicationService.Review(2,fresh.contentHash(),false,"Prueba"));db.update("UPDATE message_revision SET purged_at=CURRENT_TIMESTAMP WHERE message_id=? AND revision=2",m.id());assertThrows(DomainException.class,()->appeals.create("alumno",input(fresh)));}
 @Test void retryNeverReplacesReasonOrReopensResolvedAppeal(){var m=rejected();var first=appeals.create("alumno",input(m));db.update("UPDATE moderation_appeal SET state='UPHELD',decision_reason='Decisión sintética' WHERE id=?",first.id());var retried=appeals.create("alumno",new AppealService.Input("messages",m.id(),1,m.contentHash(),"Otro motivo"));assertEquals(first.reason(),retried.reason());assertEquals("UPHELD",retried.state());}
 @Test void disabledAccountAndInvalidReasonAreRejected(){var m=rejected();assertThrows(DomainException.class,()->appeals.create("alumno",new AppealService.Input("messages",m.id(),1,m.contentHash()," ")));users.disable("appeal_admin","alumno");assertThrows(DomainException.class,()->appeals.create("alumno",input(m)));}
 @Test void csrfAuthenticationAndPaginationAreEnforced()throws Exception{mvc.perform(get("/api/v1/appeals")).andExpect(status().isUnauthorized());mvc.perform(post("/api/v1/appeals").with(user("alumno").roles("ALUMNO")).contentType("application/json").content("{} ")).andExpect(status().isForbidden());mvc.perform(get("/api/v1/appeals?limit=1000").with(user("alumno").roles("ALUMNO"))).andExpect(status().isBadRequest());}
 @Test void rejectedExpiredStatusCanBeAppealedWithoutBeingRepublished(){String id=UUID.randomUUID().toString(),hash="a".repeat(64);db.update("INSERT INTO status_update(id,owner,client_key,body,background,audience,kind,state,content_hash,created_at,expires_at) VALUES(?,'alumno',?,'Sintético','#008800','PRIVATE','TEXT','REJECTED',?,CURRENT_TIMESTAMP,DATEADD('DAY',-1,CURRENT_TIMESTAMP))",id,UUID.randomUUID().toString(),hash);assertEquals("OPEN",appeals.create("alumno",new AppealService.Input("statuses",id,0,hash,"Contexto educativo")).state());assertEquals("REJECTED",db.queryForObject("SELECT state FROM status_update WHERE id=?",String.class,id));}
}
