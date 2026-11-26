package pe.tecsup.main.community;

import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class AccountMailConfiguration {
 @Bean @ConditionalOnProperty(name="MAIN_SMTP_HOST")
 JavaMailSender accountMailSender(@Value("${MAIN_SMTP_HOST}") String host,
   @Value("${MAIN_SMTP_PORT:587}") int port,@Value("${MAIN_SMTP_USERNAME:}") String username,
   @Value("${MAIN_SMTP_PASSWORD:}") String password,@Value("${MAIN_SMTP_STARTTLS:true}") boolean tls){
  var sender=new JavaMailSenderImpl();sender.setHost(host);sender.setPort(port);sender.setUsername(username);sender.setPassword(password);
  var props=sender.getJavaMailProperties();props.setProperty("mail.smtp.auth",Boolean.toString(!username.isBlank()));
  props.setProperty("mail.smtp.starttls.enable",Boolean.toString(tls));props.setProperty("mail.smtp.starttls.required",Boolean.toString(tls));
  props.setProperty("mail.smtp.connectiontimeout","10000");props.setProperty("mail.smtp.timeout","10000");props.setProperty("mail.smtp.writetimeout","10000");
  return sender;
 }
}
