package pe.tecsup.main.community;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;

@Service
public class ReminderService {
 private final JdbcTemplate db;private final IdentityService users;private final Clock clock;
 public ReminderService(JdbcTemplate db,IdentityService users,Clock clock){this.db=db;this.users=users;this.clock=clock;}
 public record Settings(boolean enabled,int minutesBefore){}
 private void student(String actor){if(!users.active(actor).role().equals("ALUMNO"))throw DomainException.denied();}
 public Settings settings(String actor){student(actor);return db.query("SELECT enabled,minutes_before FROM reminder_preference WHERE owner=?",(r,n)->new Settings(r.getBoolean(1),r.getInt(2)),actor).stream().findFirst().orElse(new Settings(false,60));}
 @Transactional public Settings settings(String actor,Settings value){
  student(actor);if(value==null||!Set.of(15,60,1440).contains(value.minutesBefore()))throw DomainException.invalid("Selecciona 15 minutos, una hora o un día.");
  if(db.update("UPDATE reminder_preference SET enabled=?,minutes_before=? WHERE owner=?",value.enabled(),value.minutesBefore(),actor)==0)
   db.update("INSERT INTO reminder_preference(owner,enabled,minutes_before) VALUES(?,?,?)",actor,value.enabled(),value.minutesBefore());
  return value;
 }
 @Scheduled(fixedDelayString="${main.reminders.interval-ms:60000}") public synchronized void dispatch(){
  var now=LocalDateTime.now(clock);var timestamp=Timestamp.valueOf(now);
  var rows=db.query("""
   SELECT u.username,a.id,a.fecha_limite,p.minutes_before FROM reminder_preference p
   JOIN main_user u ON u.username=p.owner AND u.enabled=TRUE AND u.role='ALUMNO'
   JOIN alumno al ON al.id=u.alumno_id AND al.usuario=u.username
   JOIN matricula m ON m.alumno_id=al.id JOIN actividad a ON a.curso_id=m.curso_id
   WHERE p.enabled=TRUE AND u.notifications=TRUE
   AND a.fecha_limite>? AND a.fecha_limite<=TIMESTAMPADD(MINUTE,p.minutes_before,CAST(? AS DATETIME))
   AND NOT EXISTS(SELECT 1 FROM entrega e WHERE e.alumno_id=al.id AND e.actividad_id=a.id)
   AND NOT EXISTS(SELECT 1 FROM notification n WHERE n.recipient=u.username AND n.kind='REMINDER'
       AND n.activity_id=a.id AND n.reminder_due=a.fecha_limite AND n.reminder_minutes=p.minutes_before)
   ORDER BY a.fecha_limite,u.username LIMIT 500
   """,(r,n)->new Due(r.getString(1),r.getLong(2),r.getTimestamp(3).toLocalDateTime(),r.getInt(4)),timestamp,timestamp);
  for(var item:rows){
   if(item.due().isAfter(now.plusMinutes(item.minutes())))continue;
   String event="reminder:"+PushService.digest(item.owner()+":"+item.activity()+":"+item.due()+":"+item.minutes());
   try{db.update("INSERT INTO notification(id,recipient,sender,kind,event_key,created_at,activity_id,reminder_due,reminder_minutes) VALUES(?,?,?,'REMINDER',?,?,?,?,?)",
    UUID.randomUUID().toString(),item.owner(),item.owner(),event,timestamp,item.activity(),Timestamp.valueOf(item.due()),item.minutes());}
   catch(DuplicateKeyException ignored){/* Another instance already enqueued this deadline. */}
  }
 }
 private record Due(String owner,long activity,LocalDateTime due,int minutes){}
}
