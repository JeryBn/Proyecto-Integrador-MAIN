package pe.tecsup.main.community;

import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teaching/courses/{courseId}/activities")
public class TeachingActivityController {
 private final TeachingActivityService activities;
 public TeachingActivityController(TeachingActivityService activities){this.activities=activities;}
 @GetMapping public List<TeachingActivityService.Activity> list(Principal principal,@PathVariable long courseId){return activities.list(principal.getName(),courseId);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED)
 public TeachingActivityService.Activity create(Principal principal,@PathVariable long courseId,@RequestBody TeachingActivityService.Input input){return activities.create(principal.getName(),courseId,input);}
}
