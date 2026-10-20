package pe.tecsup.main.community;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:attachment_tests;DB_CLOSE_DELAY=-1","main.admin.username=admin_files","main.admin.password=SyntheticFiles2026!"})
@AutoConfigureMockMvc @Transactional
class MessageAttachmentIntegrationTest {
 @Autowired IdentityService users; @Autowired CommunicationService chat;
 @Autowired MessageAttachments files; @Autowired MockMvc mvc;
 @Autowired org.springframework.jdbc.core.JdbcTemplate db;
 final String admin="admin_files";
 @BeforeEach void setup(){users.approveContact(admin,"alumno","otro");}
 MockMultipartFile txt(String text){return new MockMultipartFile("file","unsafe-name.txt","text/plain",text.getBytes(StandardCharsets.UTF_8));}
 CommunicationService.Message send(String conversation,String key,String text){return chat.sendMedia("alumno",conversation,new CommunicationService.Send(key,""),files.validate(txt(text)));}
 void approve(CommunicationService.Message m){chat.review(admin,m.id(),new CommunicationService.Review(m.revision(),m.contentHash(),true,"",true));}
 @Test void pendingFileRequiresExplicitReviewBeforeRecipientCanDownload()throws Exception{
  String conversation=chat.direct("alumno","otro");var m=send(conversation,UUID.randomUUID().toString(),"Tarea académica");
  assertEquals("FILE",m.attachmentKind());assertEquals("documento.txt",m.attachmentName());
  assertThrows(DomainException.class,()->chat.attachment("otro",m.id(),1,false));
  assertThrows(DomainException.class,()->chat.attachment("vacio",m.id(),1,false));
  assertThrows(DomainException.class,()->chat.attachment("alumno",m.id(),1,true));
  assertArrayEquals("Tarea académica".getBytes(StandardCharsets.UTF_8),chat.attachment(admin,m.id(),1,true).bytes());
  assertThrows(DomainException.class,()->chat.review(admin,m.id(),new CommunicationService.Review(1,m.contentHash(),true,"")));
  approve(m);
  mvc.perform(get("/api/v1/messages/"+m.id()+"/attachment?revision=1").with(user("otro").roles("ALUMNO")))
   .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
   .andExpect(header().string("X-Content-Type-Options","nosniff")).andExpect(content().bytes("Tarea académica".getBytes(StandardCharsets.UTF_8)));
  assertThrows(DomainException.class,()->chat.attachment(admin,m.id(),1,true));
 }
 @Test void retryDoesNotDuplicateOrAcceptDifferentFile(){
  String conversation=chat.direct("alumno","otro"),key=UUID.randomUUID().toString();var m=send(conversation,key,"Uno");
  assertEquals(m.id(),send(conversation,key,"Uno").id());
  assertThrows(DomainException.class,()->send(conversation,key,"Dos"));
  assertThrows(DomainException.class,()->chat.send("alumno",conversation,new CommunicationService.Send(key,"Texto")));
  assertEquals(1,chat.messages("alumno",conversation,0,30).size());
 }
 @Test void captionEditsRequireNewReviewWhileApprovedRevisionStaysReadable(){
  var m=send(chat.direct("alumno","otro"),UUID.randomUUID().toString(),"Documento");approve(m);
  var edited=chat.edit("alumno",m.id(),new CommunicationService.Edit(1,"Nueva explicación"));
  assertNotEquals(m.contentHash(),edited.contentHash());assertEquals(2,edited.revision());
  assertNotNull(chat.attachment("otro",m.id(),1,false));
  assertThrows(DomainException.class,()->chat.attachment("otro",m.id(),2,false));
  assertThrows(DomainException.class,()->approve(m));approve(edited);
  assertThrows(DomainException.class,()->chat.attachment("otro",m.id(),1,false));
  assertNotNull(chat.attachment("otro",m.id(),2,false));
 }
 @Test void rejectionCancellationAndBlockingDenyDownloads(){
  var m=send(chat.direct("alumno","otro"),UUID.randomUUID().toString(),"Documento");
  chat.review(admin,m.id(),new CommunicationService.Review(1,m.contentHash(),false,"Prueba"));
  assertThrows(DomainException.class,()->chat.attachment("otro",m.id(),1,false));
  assertNotNull(chat.attachment("alumno",m.id(),1,false));chat.cancel("alumno",m.id(),1);
  assertThrows(DomainException.class,()->chat.attachment("alumno",m.id(),1,false));
  var second=send(m.conversationId(),UUID.randomUUID().toString(),"Segundo");approve(second);
  users.contactPreference("otro","alumno",false,true);
  assertThrows(DomainException.class,()->chat.attachment("otro",second.id(),1,false));
 }
 @Test void multipartEnforcesCsrfAndRejectsInvalidDocuments()throws Exception{
  String conversation=chat.direct("alumno","otro");
  mvc.perform(multipart("/api/v1/conversations/"+conversation+"/messages").file(txt("Texto")).param("clientKey",UUID.randomUUID().toString()).with(user("alumno").roles("ALUMNO"))).andExpect(status().isForbidden());
  mvc.perform(multipart("/api/v1/conversations/"+conversation+"/messages").file(txt("Texto")).param("clientKey",UUID.randomUUID().toString()).with(user("alumno").roles("ALUMNO")).with(csrf())).andExpect(status().isAccepted());
  assertThrows(DomainException.class,()->files.validate(new MockMultipartFile("file","x.txt","text/plain",new byte[]{(byte)0xff})));
  assertThrows(DomainException.class,()->files.validate(txt("Texto\u0000oculto")));
  assertThrows(DomainException.class,()->files.validate(new MockMultipartFile("file","x.txt","text/plain",new byte[1024*1024+1])));
  assertThrows(DomainException.class,()->files.validate(new MockMultipartFile("file","x.pdf","application/pdf","fake".getBytes(StandardCharsets.UTF_8))));
 }
 @Test void realImageIsSanitizedAndStorageCorruptionIsDetected()throws Exception{
  var image=new java.awt.image.BufferedImage(4,4,java.awt.image.BufferedImage.TYPE_INT_RGB);
  var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",bytes);
  var validated=files.validate(new MockMultipartFile("file","photo.png","image/png",bytes.toByteArray()));
  assertEquals("IMAGE",validated.kind());assertEquals("image/jpeg",validated.mime());
  assertNotNull(javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(validated.bytes())));
  var m=chat.sendMedia("alumno",chat.direct("alumno","otro"),new CommunicationService.Send(UUID.randomUUID().toString(),"Imagen"),validated);approve(m);
  assertArrayEquals(validated.bytes(),chat.attachment("otro",m.id(),1,false).bytes());
  db.update("UPDATE message_attachment_chunk SET payload=? WHERE message_id=?",new byte[]{1,2},m.id());
  assertThrows(IllegalStateException.class,()->chat.attachment("otro",m.id(),1,false));
 }
 @Test void groupRemovalRevokesFileAccess(){
  String group=chat.group("alumno","Archivos de curso");chat.invite("alumno",group,"otro");chat.invitation("otro",group,true);
  var m=send(group,UUID.randomUUID().toString(),"Material");approve(m);
  assertNotNull(chat.attachment("otro",m.id(),1,false));chat.membership("alumno",group,"otro",false,true);
  assertThrows(DomainException.class,()->chat.attachment("otro",m.id(),1,false));
 }
 @Test void dailyLimitRejectsNewFileButAllowsIdempotentRetry(){
  String conversation=chat.direct("alumno","otro"),key=UUID.randomUUID().toString();var m=send(conversation,key,"Material");
  // Simulate an existing account quota without allocating 100 MB in the fixture.
  db.update("UPDATE message_attachment SET byte_size=? WHERE message_id=?",100*1024*1024,m.id());
  assertEquals(m.id(),send(conversation,key,"Material").id());
  assertThrows(DomainException.class,()->send(conversation,UUID.randomUUID().toString(),"Otro"));
  assertEquals(1,chat.messages("alumno",conversation,0,30).size());
 }
}
