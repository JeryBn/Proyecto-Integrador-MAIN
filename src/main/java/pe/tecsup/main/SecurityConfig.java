package pe.tecsup.main;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
@Configuration
public class SecurityConfig {
 @Bean PasswordEncoder encoder(){return new BCryptPasswordEncoder();}
 @Bean InMemoryUserDetailsManager users(PasswordEncoder encoder,
   @Value("${main.admin.username:admin}") String adminUsername,
   @Value("${main.admin.password-hash:}") String adminHash,
   @Value("${main.admin.password:}") String adminPassword){
  if (!adminPassword.isBlank()) {
   if (!adminHash.isBlank() || adminPassword.length() < 12)
    throw new IllegalArgumentException("Usa contraseña administrativa de al menos 12 caracteres o hash, no ambos.");
   adminHash = encoder.encode(adminPassword);
  }
  var users = new InMemoryUserDetailsManager(
   User.withUsername("alumno").password(encoder.encode("MainDemo2026!")).roles("ALUMNO").build(),
   User.withUsername("otro").password(encoder.encode("MainDemo2026!")).roles("ALUMNO").build(),
   User.withUsername("vacio").password(encoder.encode("MainDemo2026!")).roles("ALUMNO").build());
  if (!adminHash.isBlank()) {
   if (adminUsername.isBlank() || users.userExists(adminUsername) || !adminHash.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}"))
    throw new IllegalArgumentException("Cuenta administrativa inválida: usa nombre distinto de las demos y hash BCrypt válido.");
   users.createUser(User.withUsername(adminUsername).password(adminHash).roles("ADMIN").build());
  }
  return users;
 }
 @Bean HttpSessionCsrfTokenRepository csrfRepository(){ return new HttpSessionCsrfTokenRepository(); }
 @Bean @Order(1) SecurityFilterChain administrativeSecurity(HttpSecurity http, HttpSessionCsrfTokenRepository csrfRepository) throws Exception {
  http.securityMatcher("/api/admin/**")
   .csrf(c -> c.csrfTokenRepository(csrfRepository))
   .authorizeHttpRequests(a -> a.requestMatchers("/api/admin/csrf", "/api/admin/login").permitAll().anyRequest().hasRole("ADMIN"))
   .formLogin(f -> f.loginProcessingUrl("/api/admin/login")
    .successHandler((req,res,auth) -> {
     // ALUMNO credentials must never grant an administrative session.
     if (auth.getAuthorities().stream().noneMatch(g -> g.getAuthority().equals("ROLE_ADMIN"))) {
      new org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler().logout(req,res,auth);
      adminError(res,403,"FORBIDDEN");
     } else {
      res.setContentType("application/json;charset=UTF-8");
      res.getWriter().write("{\"rol\":\"ADMIN\"}");
     }
    })
    .failureHandler((req,res,ex) -> adminError(res,401,"INVALID_CREDENTIALS")))
   .logout(l -> l.logoutUrl("/api/admin/logout").logoutSuccessHandler((req,res,auth) -> {
    res.setContentType("application/json;charset=UTF-8"); res.getWriter().write("{\"message\":\"Sesión finalizada\"}");
   }))
   .exceptionHandling(e -> e.authenticationEntryPoint((req,res,ex) -> adminError(res,401,"UNAUTHENTICATED"))
    .accessDeniedHandler((req,res,ex) -> adminError(res,403,"FORBIDDEN")));
  return http.build();
 }
 private static void adminError(jakarta.servlet.http.HttpServletResponse res,int status,String code) throws java.io.IOException {
  res.setStatus(status); res.setContentType("application/json;charset=UTF-8");
  res.getWriter().write("{\"error\":{\"code\":\""+code+"\",\"message\":\"Acceso rechazado\"}}");
 }
 @Bean @Order(2) SecurityFilterChain security(HttpSecurity http, HttpSessionCsrfTokenRepository csrfRepository) throws Exception {
  http.csrf(c -> c.csrfTokenRepository(csrfRepository));
  http.authorizeHttpRequests(a->a.requestMatchers("/login.html","/login-token","/app.css","/login.js","/error").permitAll().anyRequest().hasRole("ALUMNO"))
   .formLogin(f->f.loginPage("/login.html").loginProcessingUrl("/login").defaultSuccessUrl("/",true).failureUrl("/login.html?error"))
   .logout(l->l.logoutUrl("/logout").logoutSuccessUrl("/login.html?salida"))
   .exceptionHandling(e->e.defaultAuthenticationEntryPointFor((req,res,ex)->res.sendError(401),new AntPathRequestMatcher("/api/**")));
  return http.build();
 }
}
