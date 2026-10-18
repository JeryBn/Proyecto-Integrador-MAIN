package pe.tecsup.main.community;
import pe.tecsup.main.SecurityConfig;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;

@Component @Order(0)
public class IdentityBootstrap implements ApplicationRunner {
 private final IdentityService identities;private final JdbcTemplate db;private final DataSource source;
 private final PasswordEncoder encoder;private final SecurityConfig security;
 @Value("${main.demo.enabled:false}") private boolean demo;
 @Value("${main.admin.username:admin}") private String admin;
 @Value("${main.admin.password-hash:}") private String hash;
 @Value("${main.admin.password:}") private String password;
 public IdentityBootstrap(IdentityService identities,JdbcTemplate db,DataSource source,PasswordEncoder encoder,SecurityConfig security){
  this.identities=identities;this.db=db;this.source=source;this.encoder=encoder;this.security=security;
 }
 @Override public void run(ApplicationArguments arguments){
  var configured=security.users(encoder,admin,hash,password);
  if(demo){
   if(db.queryForObject("SELECT COUNT(*) FROM alumno",Long.class)==0)
    new ResourceDatabasePopulator(new ClassPathResource("data.sql")).execute(source);
   for(var row:db.queryForList("SELECT id,usuario,nombre FROM alumno WHERE usuario IN ('alumno','otro','vacio')")){
    String username=(String)row.get("usuario");
    identities.bootstrap(username,(String)row.get("nombre"),"ALUMNO",configured.loadUserByUsername(username).getPassword(),((Number)row.get("id")).longValue(),null);
   }
  }
  if(configured.userExists(admin)){
   var account=configured.loadUserByUsername(admin);
   if(account.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN")))
    identities.bootstrap(admin,admin,"ADMIN",account.getPassword(),null,null);
  }
 }
}
