package pe.tecsup.main.community;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.http.HttpStatus;

@Service
public class AssistantService {
 private final JdbcTemplate db;private final IdentityService identities;private final AcademicService academic;private final OpenAiGateway provider;private final TransactionTemplate tx;
 public AssistantService(JdbcTemplate db,IdentityService identities,AcademicService academic,OpenAiGateway provider,PlatformTransactionManager tm){this.db=db;this.identities=identities;this.academic=academic;this.provider=provider;this.tx=new TransactionTemplate(tm);}
 public record Input(String clientKey,String question){}
 public record Reply(String clientKey,String answer,String model){}
 public Reply ask(String actor,Input input){
  identities.active(actor);String question=IdentityService.text(input.question(),2000,"consulta");
  String context=context(actor);
  try{UUID.fromString(input.clientKey());}catch(Exception e){throw DomainException.invalid("clientKey UUID requerido.");}
  String hash;try{hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((context+"\n"+question).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException();}
  String cached=tx.execute(status->{
   db.queryForObject("SELECT username FROM main_user WHERE username=? FOR UPDATE",String.class,actor);
   db.update("DELETE FROM assistant_request WHERE username=? AND created_at<?",actor,java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(86400)));
   var previous=db.queryForList("SELECT * FROM assistant_request WHERE username=? AND client_key=?",actor,input.clientKey());
   if(!previous.isEmpty()){
    var row=previous.getFirst();
    if(!hash.equals(row.get("QUESTION_HASH"))&&!hash.equals(row.get("question_hash")))throw conflict("La clave pertenece a otra consulta.");
    String state=Objects.toString(row.getOrDefault("STATE",row.get("state")));
    if(state.equals("COMPLETED"))return Objects.toString(row.getOrDefault("ANSWER",row.get("answer")));
    throw conflict("La consulta ya fue procesada o sigue en curso. Para una consulta nueva usa otra clave.");
   }
   Long used=db.queryForObject("SELECT COUNT(*) FROM assistant_request WHERE username=? AND created_at>=?",Long.class,actor,java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(86400)));
   if(used>=20)throw new DomainException(HttpStatus.TOO_MANY_REQUESTS,"AI_DAILY_LIMIT","Límite: 20 consultas por cuenta en 24 horas.");
   db.update("INSERT INTO assistant_request(username,client_key,question_hash,state) VALUES(?,?,?,'PENDING')",actor,input.clientKey(),hash);return null;
  });
  if(cached!=null)return new Reply(input.clientKey(),cached,provider.model());
  try{
   String answer=provider.answer(question,context);
   identities.active(actor);
   if(!context.equals(context(actor)))throw conflict("Cambió tu acceso académico durante la consulta. Solicita una respuesta nueva.");
   db.update("UPDATE assistant_request SET state='COMPLETED',answer=? WHERE username=? AND client_key=?",answer,actor,input.clientKey());
   return new Reply(input.clientKey(),answer,provider.model());
  }catch(RuntimeException ex){db.update("UPDATE assistant_request SET state='FAILED' WHERE username=? AND client_key=?",actor,input.clientKey());throw ex;}
 }
 private String context(String actor){
  var agenda=academic.agenda(actor);
  return "Cursos: "+agenda.courses().stream().limit(30).map(AcademicService.Course::name).toList()+"; Horarios: "+
   agenda.timetable().stream().limit(50).map(s->s.course()+" día "+s.dayOfWeek()+" "+s.startsAt()+"-"+s.endsAt()).toList();
 }
 private static DomainException conflict(String message){return new DomainException(HttpStatus.CONFLICT,"AI_REQUEST_EXISTS",message);}
}
