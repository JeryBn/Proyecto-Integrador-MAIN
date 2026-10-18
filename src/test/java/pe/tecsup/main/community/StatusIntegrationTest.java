package pe.tecsup.main.community;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:statuses_test;DB_CLOSE_DELAY=-1","main.admin.username=status_admin","main.admin.password=SyntheticStatus2026!"})
@AutoConfigureMockMvc @Transactional class StatusIntegrationTest {
 @Autowired StatusService statuses;@Autowired StatusMedia media;@Autowired IdentityService users;@Autowired JdbcTemplate db;@Autowired MockMvc mvc;
 @BeforeEach void setup(){users.approveContact("status_admin","alumno","otro");users.create("status_admin",new IdentityService.NewAccount("family_status","Familia","FAMILIAR","SyntheticFamily2026!",null,null));}
 StatusService.Update create(String audience){return statuses.create("alumno",UUID.randomUUID().toString(),"Estado sintético","#087F68",audience,null);}
 void approve(StatusService.Update s){statuses.review("status_admin",s.id(),new StatusService.Review(s.contentHash(),true,""));}
 @Test void pendingAndRejectedNeverReachContactsOrMedia(){
  var s=create("CONTACTS");assertEquals(1,statuses.feed("alumno").size());assertTrue(statuses.feed("otro").isEmpty());
  assertThrows(DomainException.class,()->statuses.get("otro",s.id()));assertThrows(DomainException.class,()->statuses.media("otro",s.id(),false));
  statuses.review("status_admin",s.id(),new StatusService.Review(s.contentHash(),false,"Amenaza"));assertTrue(statuses.feed("otro").isEmpty());assertEquals("REJECTED",statuses.get("alumno",s.id()).state());
 }
 @Test void approvedAudienceBlocksExpirationAndDeleteAreEnforced(){
  var s=create("CONTACTS");approve(s);assertEquals(1,statuses.feed("otro").size());assertTrue(statuses.feed("vacio").isEmpty());
  statuses.seen("otro",s.id());statuses.seen("otro",s.id());assertEquals(1,statuses.get("alumno",s.id()).views());
  users.contactPreference("otro","alumno",false,true);assertTrue(statuses.feed("otro").isEmpty());users.contactPreference("otro","alumno",false,false);
  users.preferences("alumno",new IdentityService.Preferences(false,true,"PRIVATE"));assertTrue(statuses.feed("otro").isEmpty());users.preferences("alumno",new IdentityService.Preferences(false,true,"CONTACTS"));
  db.update("UPDATE status_update SET expires_at=? WHERE id=?",java.sql.Timestamp.valueOf(LocalDateTime.now().minusHours(1)),s.id());assertTrue(statuses.feed("otro").isEmpty());assertThrows(DomainException.class,()->statuses.get("alumno",s.id()));
  var other=create("CONTACTS");approve(other);statuses.delete("alumno",other.id());assertThrows(DomainException.class,()->statuses.get("otro",other.id()));
 }
 @Test void familyCannotPublishAndPrivateNeverReachesContacts(){
  assertThrows(DomainException.class,()->statuses.create("family_status",UUID.randomUUID().toString(),"No permitido","#087F68","CONTACTS",null));
  var s=create("PRIVATE");approve(s);assertTrue(statuses.feed("otro").isEmpty());
  assertThrows(DomainException.class,()->statuses.delete("otro",s.id()));
 }
 @Test void reviewChecksHashAndIdempotenceDoesNotDuplicate(){
  String key=UUID.randomUUID().toString();var first=statuses.create("alumno",key,"Prueba","#087F68","CONTACTS",null);
  assertEquals(first.id(),statuses.create("alumno",key,"Prueba","#087F68","CONTACTS",null).id());
  assertThrows(DomainException.class,()->statuses.create("alumno",key,"Otro texto","#087F68","CONTACTS",null));
  assertThrows(DomainException.class,()->statuses.review("status_admin",first.id(),new StatusService.Review("wrong",true,"")));
  approve(first);assertThrows(DomainException.class,()->approve(first));
 }
 @Test void imagesAreDecodedReencodedAndDownloadRequiresApproval()throws Exception{
  var bitmap=new java.awt.image.BufferedImage(3,3,java.awt.image.BufferedImage.TYPE_INT_RGB);var out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(bitmap,"png",out);
  var validated=media.validate(new MockMultipartFile("file","test.png","image/png",out.toByteArray()));assertEquals("IMAGE",validated.kind());assertNotNull(javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(validated.bytes())));
  var s=statuses.create("alumno",UUID.randomUUID().toString(),"Imagen","#087F68","CONTACTS",validated);
  assertThrows(DomainException.class,()->statuses.media("otro",s.id(),false));approve(s);assertArrayEquals(validated.bytes(),statuses.media("otro",s.id(),false));
  statuses.delete("alumno",s.id());assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM status_media_chunk WHERE status_id=?",Integer.class,s.id()));
  assertThrows(DomainException.class,()->media.validate(new MockMultipartFile("file","fake.jpg","image/jpeg","not image".getBytes())));
 }
 @Test void cameraExifOrientationIsAppliedBeforeMetadataRemoval()throws Exception{
  var bitmap=new java.awt.image.BufferedImage(6,4,java.awt.image.BufferedImage.TYPE_INT_RGB);var out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(bitmap,"jpg",out);byte[] jpeg=out.toByteArray();
  for(int orientation=1;orientation<=8;orientation++){
   var metadata=java.nio.ByteBuffer.allocate(36).order(java.nio.ByteOrder.BIG_ENDIAN);
   metadata.put((byte)255).put((byte)225).putShort((short)34).put(new byte[]{'E','x','i','f',0,0,'M','M'}).putShort((short)42).putInt(8).putShort((short)1).putShort((short)274).putShort((short)3).putInt(1).putShort((short)orientation).putShort((short)0).putInt(0);
   var photo=new java.io.ByteArrayOutputStream();photo.write(jpeg,0,2);photo.write(metadata.array());photo.write(jpeg,2,jpeg.length-2);
   var validated=media.validate(new MockMultipartFile("file","camera.jpg","image/jpeg",photo.toByteArray()));var decoded=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(validated.bytes()));
   assertEquals(orientation>=5?4:6,decoded.getWidth());assertEquals(orientation>=5?6:4,decoded.getHeight());
  }
 }
 @Test void completeVideoAndAudioAreValidatedAndTruncatedFilesFail()throws Exception{
  String binaries=System.getenv("MAIN_MEDIA_FFMPEG_DIRECTORY");
  if(binaries==null||binaries.isBlank()){
   assertThrows(DomainException.class,()->media.validate(new MockMultipartFile("file","bad.mp4","video/mp4",new byte[]{1,2,3})));return;
  }
  var folder=java.nio.file.Files.createTempDirectory("main-status-test-");
  try{
   var video=folder.resolve("test.mp4");var audio=folder.resolve("test.m4a");String executable=java.nio.file.Path.of(binaries,"ffmpeg"+(System.getProperty("os.name").startsWith("Windows")?".exe":"")).toString();
   var process=new ProcessBuilder(executable,"-nostdin","-v","error","-f","lavfi","-i","color=c=green:s=160x120:d=1","-f","lavfi","-i","sine=frequency=440:duration=1","-c:v","libx264","-c:a","aac","-shortest",video.toString()).redirectErrorStream(true).redirectOutput(folder.resolve("generate.log").toFile()).start();assertTrue(process.waitFor(30,java.util.concurrent.TimeUnit.SECONDS));assertEquals(0,process.exitValue());
   var validated=media.validate(new MockMultipartFile("file","test.mp4","video/mp4",java.nio.file.Files.readAllBytes(video)));assertEquals("VIDEO",validated.kind());assertTrue(validated.bytes().length>0);
   process=new ProcessBuilder(executable,"-nostdin","-v","error","-f","lavfi","-i","sine=frequency=440:duration=1","-c:a","aac",audio.toString()).redirectErrorStream(true).redirectOutput(folder.resolve("audio.log").toFile()).start();assertTrue(process.waitFor(30,java.util.concurrent.TimeUnit.SECONDS));assertEquals(0,process.exitValue());
   assertEquals("AUDIO",media.validate(new MockMultipartFile("file","test.m4a","audio/mp4",java.nio.file.Files.readAllBytes(audio))).kind());
   assertThrows(DomainException.class,()->media.validate(new MockMultipartFile("file","fake.mp4","video/mp4","not a video".getBytes())));
  }finally{try(var files=java.nio.file.Files.list(folder)){for(var file:files.toList())java.nio.file.Files.delete(file);}java.nio.file.Files.delete(folder);}
 }
 @Test void statusApiRequiresSessionCsrfAndAdminReview()throws Exception{
  mvc.perform(get("/api/v1/statuses")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/v1/statuses/x/delete").with(user("alumno").roles("ALUMNO"))).andExpect(status().isForbidden());
  mvc.perform(get("/api/admin/v1/moderation/statuses").with(user("alumno").roles("ALUMNO"))).andExpect(status().isForbidden());
  mvc.perform(multipart("/api/v1/statuses").param("clientKey",UUID.randomUUID().toString()).param("text","Estado").param("background","#087F68").param("audience","CONTACTS").with(user("alumno").roles("ALUMNO")).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("PENDING"));
 }
}
