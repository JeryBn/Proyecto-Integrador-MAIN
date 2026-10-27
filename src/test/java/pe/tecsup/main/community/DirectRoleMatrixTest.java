package pe.tecsup.main.community;

import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** All unordered role pairs, with messages checked in both directions. */
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:role_matrix;DB_CLOSE_DELAY=-1",
 "main.admin.username=admin_matrix","main.admin.password=SyntheticMatrix2026!"})
@AutoConfigureMockMvc @Transactional
class DirectRoleMatrixTest {
 @Autowired IdentityService users;
 @Autowired CommunicationService chat;
 @Autowired MockMvc mvc;

 @ParameterizedTest(name="{0} ↔ {1}")
 @CsvSource({"ALUMNO,ALUMNO","ALUMNO,DOCENTE","ALUMNO,FAMILIAR",
             "DOCENTE,DOCENTE","DOCENTE,FAMILIAR","FAMILIAR,FAMILIAR"})
 void bothDirectionsRequireContactAndReview(String firstRole,String secondRole) throws Exception {
  create("matrix_first",firstRole,87001L); create("matrix_second",secondRole,87002L);
  assertThrows(DomainException.class,()->chat.direct("matrix_first","matrix_second"));
  users.approveContact("admin_matrix","matrix_first","matrix_second");
  String id=chat.direct("matrix_first","matrix_second");
  assertEquals(id,chat.direct("matrix_second","matrix_first"));
  direction(id,"matrix_first",firstRole,"matrix_second",secondRole,0);
  direction(id,"matrix_second",secondRole,"matrix_first",firstRole,1);
  mvc.perform(get("/api/v1/conversations/"+id+"/messages")
    .with(user("vacio").roles("ALUMNO"))).andExpect(status().isNotFound());
  users.contactPreference("matrix_second","matrix_first",false,true);
  assertThrows(DomainException.class,()->chat.send("matrix_first",id,
    new CommunicationService.Send(UUID.randomUUID().toString(),"Bloqueado")));
 }

 void create(String name,String role,long identifier) {
  users.create("admin_matrix",new IdentityService.NewAccount(name,"Cuenta sintética",role,
    "SyntheticRole2026!",role.equals("ALUMNO")?identifier:null,role.equals("DOCENTE")?identifier:null));
 }

 void direction(String id,String sender,String senderRole,String recipient,String recipientRole,int previous) throws Exception {
  String path="/api/v1/conversations/"+id+"/messages";
  String key=UUID.randomUUID().toString();
  var input=new CommunicationService.Send(key,"Consulta sintética de "+senderRole);
  var pending=chat.send(sender,id,input);
  assertEquals(pending.id(),chat.send(sender,id,input).id());
  mvc.perform(get(path).with(user(recipient).roles(recipientRole)))
    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(previous));
  mvc.perform(post(path).with(user(sender).roles(senderRole)).contentType("application/json")
    .content("{\"clientKey\":\""+UUID.randomUUID()+"\",\"text\":\"Sin CSRF\"}"))
    .andExpect(status().isForbidden());
  chat.review("admin_matrix",pending.id(),new CommunicationService.Review(
    pending.revision(),pending.contentHash(),true,""));
  var visible=chat.messages(recipient,id,0,30);
  assertEquals(previous+1,visible.size());
  var delivered=visible.stream().filter(message->message.id().equals(pending.id())).findFirst().orElseThrow();
  assertEquals(input.text(),delivered.text());
  assertEquals("",delivered.contentHash()); assertEquals("",delivered.reason());
 }
}
