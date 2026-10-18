package pe.tecsup.main.community;
import java.math.BigDecimal;
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
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:grades_test;DB_CLOSE_DELAY=-1","main.admin.username=admin_grades","main.admin.password=SyntheticGrades2026!"})
@AutoConfigureMockMvc @Transactional
class GradeIntegrationTest{
 @Autowired GradeService grades;@Autowired IdentityService users;@Autowired MockMvc mvc;
 GradeService.Input input(long student,String label,String score,String maximum){return new GradeService.Input(student,label,new BigDecimal(score),new BigDecimal(maximum));}
 @BeforeEach void setup(){users.create("admin_grades",new IdentityService.NewAccount("teacher_grades","Profesor","DOCENTE","SyntheticTeacher2026!",null,1L));}
 @Test void eachStudentSeesOnlyOwnGradesAndEnrollment(){
  grades.save("teacher_grades",1,input(1,"Examen","16","20"));
  grades.save("admin_grades",3,input(2,"Privado","9","10"));
  assertEquals(2,grades.own("alumno").size());assertEquals("16.00",grades.own("alumno").getFirst().grades().isEmpty()?grades.own("alumno").getLast().grades().getFirst().score().toPlainString():grades.own("alumno").getFirst().grades().getFirst().score().toPlainString());
  assertEquals(1,grades.own("otro").size());assertEquals("Privado",grades.own("otro").getFirst().grades().getFirst().assessment());
  assertTrue(grades.own("vacio").isEmpty());
 }
 @Test void studentsCannotWriteAndTeachersCannotGradeOtherCourses(){
  assertThrows(DomainException.class,()->grades.save("alumno",1,input(1,"Examen","16","20")));
  assertThrows(DomainException.class,()->grades.save("teacher_grades",3,input(2,"Examen","16","20")));
  assertThrows(DomainException.class,()->grades.save("teacher_grades",1,input(2,"No matriculado","16","20")));
  assertThrows(DomainException.class,()->grades.own("teacher_grades"));
 }
 @Test void scaleAndCorrectionAreExplicitWithoutInventedAverage(){
  var first=grades.save("teacher_grades",1,input(1,"Práctica","0","10"));
  var corrected=grades.save("teacher_grades",1,input(1,"Práctica","8.5","10"));
  assertEquals(first.id(),corrected.id());assertEquals(new BigDecimal("8.5"),corrected.score());
  for(String score:new String[]{"-1","21","1.234"})assertThrows(DomainException.class,()->grades.save("teacher_grades",1,input(1,"Error",score,"20")));
  assertThrows(DomainException.class,()->grades.save("teacher_grades",1,input(1,"Error","0","0")));
 }
 @Test void csrfAndAnonymousAccessAreDenied()throws Exception{
  mvc.perform(get("/api/v1/grades")).andExpect(status().isUnauthorized());
  mvc.perform(put("/api/v1/teaching/courses/1/grades").with(user("teacher_grades").roles("DOCENTE")).contentType("application/json").content("{}")).andExpect(status().isForbidden());
  mvc.perform(get("/api/v1/grades").with(user("alumno").roles("ALUMNO"))).andExpect(status().isOk()).andExpect(jsonPath("$[0].course").exists());
 }
}
