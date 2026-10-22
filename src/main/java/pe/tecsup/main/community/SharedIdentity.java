package pe.tecsup.main.community;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/** Conserva una huella de versión después de borrar las credenciales del principal. */
public final class SharedIdentity extends User {
 private final String passwordVersion;
 public SharedIdentity(String name,String hash,boolean enabled,Collection<? extends GrantedAuthority> authorities){
  super(name,hash,enabled,true,true,true,authorities);passwordVersion=version(hash);
 }
 public String passwordVersion(){return passwordVersion;}
 public static String version(String hash){
  try{return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(hash.getBytes(StandardCharsets.UTF_8)));}
  catch(java.security.NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
 }
}
