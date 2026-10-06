package pe.tecsup.main;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
public class ActividadService {
 private final JdbcTemplate db;
 private final Clock clock;
 public ActividadService(JdbcTemplate db, Clock clock){this.db=db;this.clock=clock;}
 public record Actividad(long id,String titulo,String descripcion,String curso,String docente,LocalDateTime fechaLimite,boolean vencida,boolean completada){}
 private static final String SELECT = """
 SELECT a.id,a.titulo,a.descripcion,a.fecha_limite,c.nombre curso,d.nombre docente,
 CASE WHEN e.actividad_id IS NULL THEN FALSE ELSE TRUE END completada
 FROM actividad a JOIN curso c ON c.id=a.curso_id JOIN docente d ON d.id=c.docente_id
 JOIN matricula m ON m.curso_id=c.id JOIN alumno al ON al.id=m.alumno_id
 LEFT JOIN entrega e ON e.actividad_id=a.id AND e.alumno_id=al.id
 WHERE al.usuario=?
 """;
 boolean estaVencida(LocalDateTime fecha,boolean completada){
  return !completada && fecha.isBefore(LocalDateTime.now(clock));
 }
 private Actividad map(java.sql.ResultSet r) throws java.sql.SQLException{
  var fecha=r.getTimestamp("fecha_limite").toLocalDateTime();
  boolean completada=r.getBoolean("completada");
  return new Actividad(r.getLong("id"),r.getString("titulo"),r.getString("descripcion"),r.getString("curso"),r.getString("docente"),fecha,!completada&&estaVencida(fecha,completada),completada);
 }
 public List<Actividad> pendientes(String usuario){
  return db.query(SELECT+" AND e.actividad_id IS NULL ORDER BY a.fecha_limite,a.id",(r,n)->map(r),usuario);
 }
 public Actividad detalle(String usuario,long id){
  return db.query(SELECT+" AND a.id=?",(r,n)->map(r),usuario,id).stream().findFirst()
    .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Actividad no disponible"));
 }
 public Map<String,String> perfil(String usuario){
  return db.query("SELECT nombre FROM alumno WHERE usuario=?",(r,n)->Map.of("usuario",usuario,"nombre",r.getString("nombre"),"rol","Alumno"),usuario).stream().findFirst().orElseThrow();
 }
}
