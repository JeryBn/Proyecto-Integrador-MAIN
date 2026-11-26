package pe.tecsup.main.community;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public access recovery shares main_user, BCrypt and the existing session invalidation. */
@Service
public class AccountAccessService {
 private final JdbcTemplate db;private final PasswordEncoder passwords;private final Clock clock;private final AccountMail mail;
 public AccountAccessService(JdbcTemplate db,PasswordEncoder passwords,Clock clock,AccountMail mail){this.db=db;this.passwords=passwords;this.clock=clock;this.mail=mail;}
 public static String digest(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 private static String version(String hash,String role){return digest(hash+"\0"+role);}
 private LocalDateTime now(){return LocalDateTime.now(clock);}
 private static String email(String value){String e=IdentityService.text(value,254,"correo").toLowerCase(Locale.ROOT);if(!e.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))throw DomainException.invalid("Escribe un correo válido.");return e;}
 private static String username(String value){String u=IdentityService.text(value,60,"usuario");if(!u.matches("[a-zA-Z0-9._-]{3,60}"))throw DomainException.invalid("Usuario: 3 a 60 letras, números, puntos o guiones.");return u;}
 private static void password(String value){if(value==null||value.length()<12||value.getBytes(StandardCharsets.UTF_8).length>72)throw DomainException.invalid("La contraseña necesita al menos 12 caracteres y como máximo 72 bytes UTF-8.");}
 private void issue(String user,String email,String purpose,String hash,String role){
  byte[] bytes=new byte[24];new SecureRandom().nextBytes(bytes);String code=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);var time=now();
  db.update("UPDATE account_access_token SET used_at=? WHERE username=? AND purpose=? AND used_at IS NULL",Timestamp.valueOf(time),user,purpose);
  db.update("INSERT INTO account_access_token(id,username,token_hash,purpose,delivery,credential_version,expires_at,created_at) VALUES(?,?,?,?,?,?,?,?)",UUID.randomUUID().toString(),user,digest(code),purpose,"EMAIL",version(hash,role),Timestamp.valueOf(time.plusMinutes(30)),Timestamp.valueOf(time));
  mail.send(email,purpose,code);
 }
 @Transactional public void requestReset(String identifier){
  mail.requireConfigured();String value=IdentityService.text(identifier,254,"usuario o correo");
  var rows=db.queryForList("SELECT u.username,u.password_hash,u.role,c.email FROM main_user u JOIN account_contact c ON c.username=u.username WHERE (u.username=? OR c.email=?) AND u.enabled=TRUE AND u.archived=FALSE AND c.verified_at IS NOT NULL",value,value.toLowerCase(Locale.ROOT));
  if(rows.size()==1){var r=rows.getFirst();issue((String)r.get("username"),(String)r.get("email"),"RESET",(String)r.get("password_hash"),(String)r.get("role"));}
 }
 @Transactional public boolean registerFamily(String user,String name,String address,String secret){
  user=username(user);name=IdentityService.text(name,100,"nombre");address=email(address);password(secret);
  // Public registration has no role parameter and never creates a guardian_link.
  if(db.queryForObject("SELECT COUNT(*) FROM main_user WHERE username=?",Long.class,user)>0||db.queryForObject("SELECT COUNT(*) FROM account_contact WHERE email=?",Long.class,address)>0)
   throw new DomainException(org.springframework.http.HttpStatus.CONFLICT,"ACCOUNT_EXISTS","El usuario o correo ya está registrado. Recupera tu acceso o consulta al colegio.");
  String hash=passwords.encode(secret);var time=Timestamp.valueOf(now());
  try{
   db.update("INSERT INTO main_user(username,display_name,role,password_hash,enabled,archived) VALUES(?,?,'FAMILIAR',?,FALSE,FALSE)",user,name,hash);
   db.update("INSERT INTO account_contact(username,email,created_at,updated_at) VALUES(?,?,?,?)",user,address,time,time);
  }catch(DuplicateKeyException e){throw new DomainException(org.springframework.http.HttpStatus.CONFLICT,"ACCOUNT_EXISTS","El usuario o correo ya está registrado.");}
  if(mail.configured()){issue(user,address,"VERIFY_FAMILY",hash,"FAMILIAR");return true;}
  return false;
 }
 @Transactional public void confirm(String user,String code,String intent,String secret){
  user=username(user);if(code==null||!code.matches("[A-Za-z0-9_-]{32}"))throw invalidCode();
  var accounts=db.queryForList("SELECT username,password_hash,role,enabled,archived FROM main_user WHERE username=? FOR UPDATE",user);
  if(accounts.isEmpty())throw invalidCode();var a=accounts.getFirst();
  var tokens=db.queryForList("SELECT * FROM account_access_token WHERE username=? AND token_hash=? AND used_at IS NULL FOR UPDATE",user,digest(code));
  if(tokens.isEmpty())throw invalidCode();var t=tokens.getFirst();String purpose=(String)t.get("purpose"),role=(String)a.get("role");
  if(!((Timestamp)t.get("expires_at")).toLocalDateTime().isAfter(now())||!Objects.equals(t.get("credential_version"),version((String)a.get("password_hash"),role))||Boolean.TRUE.equals(a.get("archived"))||Objects.equals(a.get("archived"),1))throw invalidCode();
  boolean enabled=Boolean.TRUE.equals(a.get("enabled"))||Objects.equals(a.get("enabled"),1);
  if(intent.equals("reset")&&!purpose.equals("RESET")||intent.equals("activate")&&!Set.of("ACTIVATE","VERIFY_FAMILY","VERIFY_CONTACT").contains(purpose))throw invalidCode();
  if(purpose.equals("RESET")&&!enabled)throw invalidCode();
  if(purpose.equals("VERIFY_FAMILY")&&!role.equals("FAMILIAR"))throw invalidCode();
  var time=Timestamp.valueOf(now());
  if(!purpose.equals("VERIFY_CONTACT")){
   password(secret);String encoded=passwords.encode(secret);
   db.update("UPDATE main_user SET password_hash=?,enabled=TRUE,password_change_required=FALSE WHERE username=?",encoded,user);
  }
  if(t.get("delivery").equals("EMAIL"))db.update("UPDATE account_contact SET verified_at=?,updated_at=? WHERE username=?",time,time,user);
  db.update("UPDATE account_access_token SET used_at=? WHERE username=? AND used_at IS NULL",time,user);
 }
 private static DomainException invalidCode(){return DomainException.invalid("Código inválido, vencido o ya utilizado. Solicita uno nuevo al colegio.");}
}
