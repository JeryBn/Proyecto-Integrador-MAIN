package pe.tecsup.main.community;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.io.ByteArrayInputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
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

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:family_school;DB_CLOSE_DELAY=-1","main.admin.username=admin_family","main.admin.password=SyntheticFamily2026!","main.demo.enabled=true"})
@AutoConfigureMockMvc @Transactional
class FamilySchoolIntegrationTest {
 @Autowired JdbcTemplate db;@Autowired IdentityService users;@Autowired FamilySchoolService family;
 @Autowired TeachingSchoolService teaching;@Autowired SchoolPublicationService publications;
 @Autowired SchoolReportPdf pdf;@Autowired GradeService grades;@Autowired MockMvc mvc;
 @BeforeEach void setup() {
  users.create("admin_family",new IdentityService.NewAccount("family_one","Apoderado sintético","FAMILIAR","SyntheticFamily2026!",null,null));
  users.create("admin_family",new IdentityService.NewAccount("family_two","Otra familia","FAMILIAR","SyntheticFamily2026!",null,null));
  users.create("admin_family",new IdentityService.NewAccount("teacher_one","Docente uno","DOCENTE","SyntheticFamily2026!",null,1L));
  users.create("admin_family",new IdentityService.NewAccount("teacher_two","Docente dos","DOCENTE","SyntheticFamily2026!",null,2L));
  db.update("INSERT INTO guardian_link VALUES('family_one',1,'admin_family',CURRENT_TIMESTAMP)");
  db.update("INSERT INTO guardian_link VALUES('family_two',2,'admin_family',CURRENT_TIMESTAMP)");
  db.update("INSERT INTO school_year(id,name) VALUES(2050,'Año escolar 2050')");
  db.update("INSERT INTO school_period VALUES('period-one',2050,'Trimestre 1',1)");
  db.update("INSERT INTO school_period VALUES('period-two',2050,'Trimestre 2',2)");
  db.update("UPDATE school_year SET active_period_id='period-one' WHERE id=2050");
  db.update("INSERT INTO school_classroom VALUES('room-one',2050,'SECUNDARIA',2,'A','NUMERIC',20,11,'A')");
  db.update("INSERT INTO school_classroom VALUES('room-two',2050,'PRIMARIA',4,'B','LITERAL',20,11,'A')");
  db.update("INSERT INTO school_enrollment VALUES('enrollment-one',2050,'room-one',1,CURRENT_TIMESTAMP)");
  db.update("INSERT INTO school_enrollment VALUES('enrollment-two',2050,'room-two',2,CURRENT_TIMESTAMP)");
  db.update("INSERT INTO school_course VALUES('assignment-one','room-one',1,'teacher_one')");
  db.update("INSERT INTO school_course VALUES('assignment-two','room-two',3,'teacher_two')");
 }
 private TeachingSchoolService.Input mark(String enrollment,String period,String name,String score,String achievement,int version){return new TeachingSchoolService.Input(enrollment,period,name,"Resuelve problemas",score==null?null:new BigDecimal(score),achievement,version);}
 @Test void teacherToParentReportPreservesCompetenciesPeriodsAndCorrections() {
  var created=teaching.save("teacher_one","assignment-one",mark("enrollment-one","period-one","Práctica 1","8",null,-1));
  teaching.save("teacher_one","assignment-one",mark("enrollment-one","period-two","Examen 2","16",null,-1));
  var report=family.report("family_one",1,2050,"period-one",null);
  assertEquals(1,report.marks().size());assertEquals("Resuelve problemas",report.marks().getFirst().competency());
  assertEquals("8.00",report.marks().getFirst().score().toPlainString());
  assertEquals("period-one",family.catalog("family_one",1).years().getFirst().activePeriodId());
  teaching.save("teacher_one","assignment-one",mark("enrollment-one","period-one","Práctica 1","14",null,0));
  assertEquals("14.00",family.report("family_one",1,2050,"period-one",1L).marks().getFirst().score().toPlainString());
  assertEquals("14.00",db.queryForObject("SELECT score FROM course_grade WHERE id=?",BigDecimal.class,created.get("id")).toPlainString());
  assertThrows(DomainException.class,()->teaching.save("teacher_one","assignment-one",mark("enrollment-one","period-one","Práctica 1","9",null,0)));
  assertThrows(DomainException.class,()->grades.save("teacher_one",1,new GradeService.Input(1,"2050/1 - Práctica 1",BigDecimal.ONE,new BigDecimal("20"))));
 }
 @Test void literalMarksNeverBecomeNumericalGrades() {
  teaching.save("teacher_two","assignment-two",mark("enrollment-two","period-one","Lectura",null,"B",-1));
  var report=family.report("family_two",2,2050,"period-one",null);
  assertEquals("B",report.marks().getFirst().achievement());assertNull(report.marks().getFirst().score());
  assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM course_grade WHERE student_id=2",Integer.class));
 }
 @Test void verifiedLinksAreRequiredForEveryReadAndRevocationIsImmediate() {
  assertThrows(DomainException.class,()->family.catalog("family_one",2));
  assertThrows(DomainException.class,()->family.report("teacher_one",1,2050,"period-one",null));
  assertThrows(DomainException.class,()->teaching.sheet("teacher_two","assignment-one","period-one"));
  assertThrows(DomainException.class,()->teaching.save("teacher_one","assignment-one",mark("enrollment-two","period-one","Privada","8",null,-1)));
  db.update("DELETE FROM guardian_link WHERE guardian='family_one'");
  assertTrue(family.students("family_one").isEmpty());
  assertThrows(DomainException.class,()->family.report("family_one",1,2050,"period-one",null));
 }
 @Test void filtersRejectOtherCoursesYearsAndForeignPeriods() {
  db.update("INSERT INTO school_year(id,name) VALUES(2051,'Año escolar 2051')");
  db.update("INSERT INTO school_period VALUES('foreign-period',2051,'Trimestre 1',1)");
  assertThrows(DomainException.class,()->family.report("family_one",1,2050,"foreign-period",null));
  assertThrows(DomainException.class,()->family.report("family_one",1,2050,"period-one",3L));
  assertThrows(DomainException.class,()->teaching.save("teacher_one","assignment-one",mark("enrollment-one","foreign-period","No válida","8",null,-1)));
  assertThrows(DomainException.class,()->teaching.save("teacher_one","assignment-one",mark("enrollment-one","period-one","No válida","21",null,-1)));
 }
 @Test void attendanceIsDailyAndBoundToTheStudentsEnrollmentYear() {
  db.update("INSERT INTO school_attendance VALUES('attendance-one','enrollment-one',DATE '2050-03-01','LATE','Llegó tarde','admin_family',0)");
  var list=family.attendance("family_one",1,2050,LocalDate.of(2050,3,1),LocalDate.of(2050,3,31));
  assertEquals(1,list.size());assertEquals("LATE",list.getFirst().status());
  assertThrows(DomainException.class,()->family.attendance("family_one",2,2050,LocalDate.of(2050,3,1),LocalDate.of(2050,3,31)));
  assertThrows(DomainException.class,()->family.attendance("family_one",1,2050,LocalDate.of(2049,3,1),LocalDate.of(2050,3,31)));
 }
 @Test void pdfIsReadableContainsOnlyTheSelectedStudentAndPeriod() throws Exception {
  teaching.save("teacher_one","assignment-one",mark("enrollment-one","period-one","Práctica de geometría","16",null,-1));
  teaching.save("teacher_two","assignment-two",mark("enrollment-two","period-one","Privado",null,"B",-1));
  byte[] bytes=pdf.render(family.report("family_one",1,2050,"period-one",null));
  try(var document=Loader.loadPDF(bytes)) {
   var text=new PDFTextStripper().getText(document);
   assertTrue(text.contains("Práctica de geometría"));assertTrue(text.contains("Trimestre 1"));
   assertTrue(text.contains("Resuelve problemas"));assertFalse(text.contains("Privado"));assertEquals(1,document.getNumberOfPages());
   java.nio.file.Files.write(java.nio.file.Path.of("target/family-report-preview.pdf"),bytes);
   javax.imageio.ImageIO.write(new org.apache.pdfbox.rendering.PDFRenderer(document).renderImageWithDPI(0,110),"PNG",new java.io.File("target/family-report-preview.png"));
  }
  mvc.perform(get("/api/v1/family/students/1/report.pdf").param("yearId","2050").param("periodId","period-one").with(user("family_one").roles("FAMILIAR")))
    .andExpect(status().isOk()).andExpect(content().contentType("application/pdf")).andExpect(header().string("Cache-Control","no-store"));
  mvc.perform(get("/api/v1/family/students/2/report.pdf").param("yearId","2050").param("periodId","period-one").with(user("family_one").roles("FAMILIAR"))).andExpect(status().isForbidden());
 }
 @Test void longReportPaginatesWithoutLosingTheLastEvaluation() throws Exception {
  var report=family.report("family_one",1,2050,"period-one",null);
  var marks=new java.util.ArrayList<FamilySchoolService.Mark>();
  for(int i=0;i<75;i++)marks.add(new FamilySchoolService.Mark("m"+i,1,"Geometría","Resuelve problemas de forma y movimiento","Evaluación "+i+" con indicaciones y criterios de una competencia escolar",new BigDecimal("16"),new BigDecimal("20"),null,"2050-03-01 10:00:00"));
  byte[] bytes=pdf.render(new FamilySchoolService.Report(report.student(),report.year(),report.period(),report.courses(),marks));
  try(var document=Loader.loadPDF(bytes)) {
   assertTrue(document.getNumberOfPages()>1);
   assertTrue(new PDFTextStripper().getText(document).contains("Evaluación 74"));
   for(int i=0;i<document.getNumberOfPages();i++)javax.imageio.ImageIO.write(new org.apache.pdfbox.rendering.PDFRenderer(document).renderImageWithDPI(i,80),"PNG",new java.io.File("target/family-report-long-"+i+".png"));
  }
 }
 @Test void publicationDueDateReadReceiptsAndAudienceAreVerified() {
  var noDate=new SchoolPublicationService.Input(UUID.randomUUID().toString(),1,"room-one","TASK","Tarea","Resolver ejercicios",null);
  assertThrows(DomainException.class,()->publications.publish("teacher_one",noDate));
  var input=new SchoolPublicationService.Input(UUID.randomUUID().toString(),1,"room-one","TASK","Tarea","Resolver ejercicios","2050-03-05T18:00");
  String id=publications.publish("teacher_one",input).get("id");
  assertEquals(id,publications.publish("teacher_one",input).get("id"));
  assertEquals(1,publications.family("family_one",1).size());assertFalse(publications.family("family_one",1).getFirst().read());
  assertTrue(publications.family("family_two",2).isEmpty());
  assertThrows(DomainException.class,()->publications.read("family_two",2,id));
  publications.read("family_one",1,id);publications.read("family_one",1,id);
  var post=publications.teaching("teacher_one").getFirst();assertEquals(1,post.audience());assertEquals(1,post.readers().size());
  assertThrows(DomainException.class,()->publications.publish("teacher_two",input));
  db.update("DELETE FROM guardian_link WHERE guardian='family_one'");
  assertThrows(DomainException.class,()->publications.read("family_one",1,id));
  assertEquals(0,publications.teaching("teacher_one").getFirst().readers().size());
 }
 @Test void unauthenticatedRequestsAndWritesWithoutCsrfAreRejected() throws Exception {
  mvc.perform(get("/api/v1/family/students")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/v1/teaching/publications").with(user("teacher_one").roles("DOCENTE")).contentType("application/json").content("{}")).andExpect(status().isForbidden());
  mvc.perform(get("/bienvenida.html")).andExpect(status().isOk());
 }
 @Test void publicationDatesPreserveSchoolYearsAfter2038AndRejectUnsupportedValues() {
  var input=new SchoolPublicationService.Input(UUID.randomUUID().toString(),1,"room-one","EXAM","Examen","Indicaciones","2050-03-05T18:00");
  String id=publications.publish("teacher_one",input).get("id");
  assertTrue(publications.teaching("teacher_one").stream().filter(p->p.id().equals(id)).findFirst().orElseThrow().dueAt().startsWith("2050-03-05"));
  assertThrows(DomainException.class,()->publications.publish("teacher_one",new SchoolPublicationService.Input(UUID.randomUUID().toString(),1,"room-one","EXAM","Examen","Indicaciones","+10000-03-05T18:00")));
  assertThrows(DomainException.class,()->publications.publish("teacher_one",new SchoolPublicationService.Input(UUID.randomUUID().toString(),1,"room-one","EXAM","Examen","Indicaciones","2050-03-05T18:00:00.001")));
 }
 @Test void studentReadsOwnNumericAndLiteralMarksWithoutChoosingAnotherIdentity() {
  teaching.save("teacher_one","assignment-one",mark("enrollment-one","period-one","Práctica","16",null,-1));
  teaching.save("teacher_two","assignment-two",mark("enrollment-two","period-one","Historia",null,"AD",-1));
  assertEquals(1,family.ownCatalog("alumno").student().id());
  assertEquals("Práctica",family.ownReport("alumno",2050,"period-one",null).marks().getFirst().assessment());
  assertEquals("AD",family.ownReport("otro",2050,"period-one",null).marks().getFirst().achievement());
  assertThrows(DomainException.class,()->family.ownCatalog("family_one"));
  assertThrows(DomainException.class,()->family.ownReport("alumno",2050,"period-one",3L));
 }
}
