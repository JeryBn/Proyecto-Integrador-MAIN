package pe.tecsup.main;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class MainApplication {
 public static void main(String[] args) { SpringApplication.run(MainApplication.class,args); }
}
