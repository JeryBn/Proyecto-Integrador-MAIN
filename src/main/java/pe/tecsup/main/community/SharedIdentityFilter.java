package pe.tecsup.main.community;

import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Django y Spring revocan las sesiones existentes mediante la misma identidad SQL. */
public final class SharedIdentityFilter extends OncePerRequestFilter {
 private final JdbcTemplate db;
 public SharedIdentityFilter(JdbcTemplate db){this.db=db;}
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
   throws ServletException,IOException {
  var auth=SecurityContextHolder.getContext().getAuthentication();
  if(auth!=null&&auth.isAuthenticated()&&auth.getPrincipal() instanceof SharedIdentity user){
   var matches=db.query("SELECT password_hash,enabled,role FROM main_user WHERE username=?",(r,n)->{
    String role="ROLE_"+r.getString(3);
    return r.getBoolean(2)&&SharedIdentity.version(r.getString(1)).equals(user.passwordVersion())&&
    auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals(role));},user.getUsername());
   if(matches.isEmpty()||!matches.getFirst()){
    var session=request.getSession(false);if(session!=null)session.invalidate();
    SecurityContextHolder.clearContext();response.setStatus(401);response.setContentType("application/json;charset=UTF-8");
    response.setHeader("Cache-Control","no-store");
    response.getWriter().write("{\"error\":{\"code\":\"SESSION_EXPIRED\",\"message\":\"Vuelve a iniciar sesión.\"}}");return;
   }
  }
  String expected=request.getHeader("X-MAIN-Expected-User");
  if(expected!=null&&auth!=null&&auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails&& !expected.equals(auth.getName())){
   response.setStatus(409);response.setContentType("application/json;charset=UTF-8");response.setHeader("Cache-Control","no-store");
   response.getWriter().write("{\"error\":{\"code\":\"ACCOUNT_CHANGED\",\"message\":\"La cuenta cambió en otra pestaña. Vuelve a abrir MAIN.\"}}");return;
  }
  chain.doFilter(request,response);
 }
}
