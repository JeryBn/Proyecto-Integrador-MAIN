package pe.tecsup.main.community;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
public class GradeController {
 private final GradeService grades;
 public GradeController(GradeService grades){this.grades=grades;}
 @GetMapping("/api/v1/grades") public List<GradeService.CourseGrades> own(Principal p){return grades.own(p.getName());}
 @PutMapping({"/api/v1/teaching/courses/{courseId}/grades","/api/admin/v1/courses/{courseId}/grades"})
 public GradeService.Grade save(Principal p,@PathVariable long courseId,@RequestBody GradeService.Input input){return grades.save(p.getName(),courseId,input);}
}
