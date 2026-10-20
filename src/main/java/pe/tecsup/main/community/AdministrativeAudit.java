package pe.tecsup.main.community;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Structured metadata only: never record passwords, tokens or message bodies. */
@Service
public class AdministrativeAudit {
 private final JdbcTemplate db; private final Clock clock;
 public AdministrativeAudit(JdbcTemplate db,Clock clock){this.db=db;this.clock=clock;}
 public record Event(String id,String actor,String action,String subject,String occurredAt){}
 void record(String actor,String action,String subject){
  db.update("INSERT INTO administrative_audit VALUES(?,?,?,?,?)",UUID.randomUUID().toString(),actor,action,subject,Timestamp.valueOf(LocalDateTime.now(clock)));
 }
 List<Event> events(int offset,int limit){
  IdentityService.page(offset,limit);
  return db.query("SELECT * FROM administrative_audit ORDER BY occurred_at DESC,id DESC LIMIT ? OFFSET ?",
   (r,n)->new Event(r.getString("id"),r.getString("actor"),r.getString("action"),r.getString("subject"),r.getTimestamp("occurred_at").toString()),limit,offset);
 }
}
