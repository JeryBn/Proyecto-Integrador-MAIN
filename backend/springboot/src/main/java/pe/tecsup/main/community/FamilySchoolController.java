package pe.tecsup.main.community;

import java.security.Principal;
import java.time.LocalDate;
import java.io.IOException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/family")
public class FamilySchoolController {
 private final FamilySchoolService service;
 private final SchoolReportPdf pdf;
 public FamilySchoolController(FamilySchoolService service,SchoolReportPdf pdf){this.service=service;this.pdf=pdf;}
 private <T> ResponseEntity<T> privateData(T data){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(data);}
 @GetMapping("/students") public ResponseEntity<?> students(Principal p){return privateData(service.students(p.getName()));}
 @GetMapping("/students/{student}/catalog") public ResponseEntity<?> catalog(Principal p,@PathVariable long student){return privateData(service.catalog(p.getName(),student));}
 @GetMapping("/students/{student}/report") public ResponseEntity<?> report(Principal p,@PathVariable long student,@RequestParam int yearId,@RequestParam String periodId,@RequestParam(required=false) Long courseId){return privateData(service.report(p.getName(),student,yearId,periodId,courseId));}
 @GetMapping("/students/{student}/attendance") public ResponseEntity<?> attendance(Principal p,@PathVariable long student,@RequestParam int yearId,@RequestParam LocalDate from,@RequestParam LocalDate to){return privateData(service.attendance(p.getName(),student,yearId,from,to));}
 @GetMapping(value="/students/{student}/report.pdf",produces=MediaType.APPLICATION_PDF_VALUE)
 public ResponseEntity<byte[]> download(Principal p,@PathVariable long student,@RequestParam int yearId,@RequestParam String periodId,@RequestParam(required=false) Long courseId) throws IOException {
  var report=service.report(p.getName(),student,yearId,periodId,courseId);
  return ResponseEntity.ok().cacheControl(CacheControl.noStore())
    .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=libreta-"+student+"-"+yearId+"-"+report.period().position()+".pdf")
    .contentType(MediaType.APPLICATION_PDF).body(pdf.render(report));
 }
}
