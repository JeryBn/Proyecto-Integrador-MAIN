package pe.tecsup.main;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;
@RestController
public class LoginController {
 @GetMapping("/login-token") public Map<String,String> token(CsrfToken token){return Map.of("parameterName",token.getParameterName(),"token",token.getToken());}
 @GetMapping(value="/favicon.ico",produces="image/svg+xml") public Resource favicon(){return new ClassPathResource("static/favicon.svg");}
}
