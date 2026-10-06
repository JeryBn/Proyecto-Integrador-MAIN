package pe.tecsup.main;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class TimeConfig {
 @Bean Clock relojPeru(){return Clock.system(ZoneId.of("America/Lima"));}
}
