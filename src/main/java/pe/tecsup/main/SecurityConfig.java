package pe.tecsup.main;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
@Configuration
public class SecurityConfig {
 @Bean PasswordEncoder encoder(){return new BCryptPasswordEncoder();}
 @Bean InMemoryUserDetailsManager users(PasswordEncoder encoder){
  return new InMemoryUserDetailsManager(
   User.withUsername("alumno").password(encoder.encode("MainDemo2026!")).roles("ALUMNO").build(),
   User.withUsername("otro").password(encoder.encode("MainDemo2026!")).roles("ALUMNO").build(),
   User.withUsername("vacio").password(encoder.encode("MainDemo2026!")).roles("ALUMNO").build());
 }
 @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
  http.authorizeHttpRequests(a->a.requestMatchers("/login.html","/login-token","/app.css","/login.js","/error").permitAll().anyRequest().hasRole("ALUMNO"))
   .formLogin(f->f.loginPage("/login.html").loginProcessingUrl("/login").defaultSuccessUrl("/",true).failureUrl("/login.html?error"))
   .logout(l->l.logoutUrl("/logout").logoutSuccessUrl("/login.html?salida"))
   .exceptionHandling(e->e.defaultAuthenticationEntryPointFor((req,res,ex)->res.sendError(401),new AntPathRequestMatcher("/api/**")));
  return http.build();
 }
}
