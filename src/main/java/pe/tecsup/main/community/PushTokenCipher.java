package pe.tecsup.main.community;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
@Component
public class PushTokenCipher {
 private final byte[] key;
 public PushTokenCipher(@Value("${MAIN_PUSH_ENCRYPTION_KEY:}") String value){
  try{key=value.isBlank()?new byte[0]:Base64.getDecoder().decode(value);}catch(Exception e){throw new IllegalArgumentException("MAIN_PUSH_ENCRYPTION_KEY debe ser Base64.");}
  if(key.length!=0&&key.length!=32)throw new IllegalArgumentException("MAIN_PUSH_ENCRYPTION_KEY necesita 32 bytes.");
 }
 public boolean ready(){return key.length==32;}
 public String encrypt(String token,String device){
  try{if(!ready())throw new IllegalStateException();byte[] iv=new byte[12];new SecureRandom().nextBytes(iv);
   Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,iv));c.updateAAD(device.getBytes(StandardCharsets.UTF_8));
   return Base64.getEncoder().encodeToString(iv)+"."+Base64.getEncoder().encodeToString(c.doFinal(token.getBytes(StandardCharsets.UTF_8)));
  }catch(Exception e){throw new IllegalStateException("No se pudo proteger el registro push.");}
 }
 public String decrypt(String value,String device){
  try{String[] p=value.split("\\.");Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,Base64.getDecoder().decode(p[0])));c.updateAAD(device.getBytes(StandardCharsets.UTF_8));
   return new String(c.doFinal(Base64.getDecoder().decode(p[1])),StandardCharsets.UTF_8);
  }catch(Exception e){throw new IllegalStateException("Registro push no válido.");}
 }
}
