package pe.tecsup.main.community;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import pe.tecsup.main.MainApplication;
import static org.junit.jupiter.api.Assertions.*;

class PersistenceRestartTest {
 @TempDir Path directory;
 private org.springframework.context.ConfigurableApplicationContext open(){
  return new SpringApplicationBuilder(MainApplication.class).run("--server.port=0",
   "--spring.datasource.url=jdbc:h2:file:"+directory.resolve("persistent").toAbsolutePath()+";DB_CLOSE_ON_EXIT=FALSE",
   "--main.demo.enabled=true","--main.admin.username=admin_restart","--main.admin.password=SyntheticRestart2026!");
 }
 @Test void identitiesSettingsRelationshipsAndTimetableSurviveRestart(){
  String conversation;
  try(var context=open()){
   var users=context.getBean(IdentityService.class);
   users.create("admin_restart",new IdentityService.NewAccount("persistente","Usuario persistido","ALUMNO","SyntheticPersist2026!",81L,null));
   users.create("admin_restart",new IdentityService.NewAccount("tutor_persistente","Tutor persistido","FAMILIAR","SyntheticPersist2026!",null,null));
   users.preferences("persistente",new IdentityService.Preferences(true,false,"PRIVATE"));
   users.verifyGuardian("admin_restart","tutor_persistente",1);
   users.approveContact("admin_restart","persistente","alumno");
   users.contactPreference("persistente","alumno",true,true);
   context.getBean(AcademicService.class).createSlot("admin_restart",new AcademicService.NewSlot(1,2,"10:00","11:00","Persistencia T01"));
   users.approveContact("admin_restart","alumno","otro");
   var chat=context.getBean(CommunicationService.class);
   conversation=chat.direct("alumno","otro");
   var message=chat.send("alumno",conversation,new CommunicationService.Send(java.util.UUID.randomUUID().toString(),"Mensaje persistente sintético"));
   chat.review("admin_restart",message.id(),new CommunicationService.Review(1,message.contentHash(),true,""));
  }
  try(var context=open()){
   var users=context.getBean(IdentityService.class);
   assertEquals("Usuario persistido",users.account("persistente").name());
   assertTrue(users.preferences("persistente").darkTheme());assertFalse(users.preferences("persistente").notifications());
   assertTrue(users.blocked("persistente","alumno"));
   assertEquals(2,context.getBean(AcademicService.class).agenda("tutor_persistente").courses().size());
   assertEquals(1,context.getBean(AcademicService.class).agenda("alumno").timetable().size());
   var encoder=context.getBean(org.springframework.security.crypto.password.PasswordEncoder.class);
   assertTrue(encoder.matches("SyntheticPersist2026!",users.loadUserByUsername("persistente").getPassword()));
   assertEquals("Mensaje persistente sintético",context.getBean(CommunicationService.class).messages("otro",conversation,0,30).getFirst().text());
   assertEquals(1,context.getBean(NotificationService.class).list("otro",0,30).size());
  }
 }
}
