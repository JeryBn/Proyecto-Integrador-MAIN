package pe.tecsup.main.community;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Never falls back to public logs or an API response containing a recovery secret. */
@Component
public class AccountMail {
 private final ObjectProvider<JavaMailSender> senders;
 private final String from;
 public AccountMail(ObjectProvider<JavaMailSender> senders,@Value("${MAIN_ACCOUNTS_MAIL_FROM:}") String from){this.senders=senders;this.from=from;}
 public boolean configured(){return senders.getIfAvailable()!=null&&!from.isBlank();}
 public void requireConfigured(){if(!configured())throw new DomainException(HttpStatus.SERVICE_UNAVAILABLE,"MAIL_NOT_CONFIGURED","El correo del colegio aún no está habilitado. Solicita ayuda a Administración.");}
 public void send(String email,String purpose,String code){
  requireConfigured();
  var message=new SimpleMailMessage();message.setFrom(from);message.setTo(email);
  message.setSubject("COLEGIA · "+(purpose.equals("RESET")?"Recuperar acceso":"Activar tu cuenta"));
  message.setText("Tu código de COLEGIA es:\n\n"+code+"\n\nCaduca en 30 minutos y solo se puede usar una vez. No lo compartas. Si no solicitaste este cambio, ignora este correo.");
  try{senders.getObject().send(message);}catch(org.springframework.mail.MailException e){throw new DomainException(HttpStatus.SERVICE_UNAVAILABLE,"MAIL_UNAVAILABLE","No pudimos enviar el correo. Intenta más tarde o solicita ayuda al colegio.");}
 }
}
