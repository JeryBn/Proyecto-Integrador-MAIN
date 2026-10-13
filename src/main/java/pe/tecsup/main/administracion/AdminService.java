package pe.tecsup.main.administracion;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final JdbcTemplate db;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss")
        .withResolverStyle(ResolverStyle.STRICT);
    private static final String SELECT = """
        SELECT a.id,a.curso_id,a.titulo,a.descripcion,a.fecha_limite,c.nombre curso,d.nombre docente
        FROM actividad a JOIN curso c ON c.id=a.curso_id JOIN docente d ON d.id=c.docente_id
        """;

    public AdminService(JdbcTemplate db) { this.db = db; }
    public record NuevaActividad(Long id, Long cursoId, String titulo, String descripcion, String fechaLimite) {}
    public record ActividadAdmin(long id, long cursoId, String curso, String docente,
                                 String titulo, String descripcion, LocalDateTime fechaLimite) {}
    public record CursoAdmin(long id, String nombre, String docente) {}
    public record Pagina<T>(List<T> items, long total, int offset, int limit) {}

    private ActividadAdmin map(java.sql.ResultSet r) throws java.sql.SQLException {
        return new ActividadAdmin(r.getLong("id"), r.getLong("curso_id"), r.getString("curso"),
            r.getString("docente"), r.getString("titulo"), r.getString("descripcion"),
            r.getTimestamp("fecha_limite").toLocalDateTime());
    }

    private void pagination(int offset, int limit) {
        if (offset < 0 || offset > 1_000_000 || limit < 1 || limit > 100)
            throw new AdminApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "offset: 0..1000000; limit: 1..100.");
    }

    public Pagina<CursoAdmin> cursos(int offset, int limit) {
        pagination(offset, limit);
        var items = db.query("SELECT c.id,c.nombre,d.nombre docente FROM curso c JOIN docente d ON d.id=c.docente_id ORDER BY c.id LIMIT ? OFFSET ?",
            (r,n) -> new CursoAdmin(r.getLong("id"), r.getString("nombre"), r.getString("docente")), limit, offset);
        return new Pagina<>(items, db.queryForObject("SELECT COUNT(*) FROM curso", Long.class), offset, limit);
    }

    public Pagina<ActividadAdmin> actividades(int offset, int limit) {
        pagination(offset, limit);
        var items = db.query(SELECT + " ORDER BY a.fecha_limite,a.id LIMIT ? OFFSET ?", (r,n) -> map(r), limit, offset);
        return new Pagina<>(items, db.queryForObject("SELECT COUNT(*) FROM actividad", Long.class), offset, limit);
    }

    public ActividadAdmin detalle(long id) {
        return db.query(SELECT + " WHERE a.id=?", (r,n) -> map(r), id).stream().findFirst()
            .orElseThrow(() -> new AdminApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Actividad no disponible."));
    }

    @Transactional
    public ActividadAdmin crear(NuevaActividad input) {
        var fields = new LinkedHashMap<String, String>();
        // React numbers must stay exact; existing schema uses non-generated BIGINT IDs.
        if (input.id() == null || input.id() <= 0 || input.id() > 9_007_199_254_740_991L)
            fields.put("id", "Entero positivo seguro para JavaScript requerido.");
        if (input.cursoId() == null || input.cursoId() <= 0 || input.cursoId() > 9_007_199_254_740_991L)
            fields.put("cursoId", "Entero positivo seguro para JavaScript requerido.");
        if (input.titulo() == null || input.titulo().isBlank() || input.titulo().strip().length() > 150)
            fields.put("titulo", "Texto de 1 a 150 caracteres requerido.");
        if (input.descripcion() == null || input.descripcion().isBlank() || input.descripcion().strip().length() > 2000)
            fields.put("descripcion", "Texto de 1 a 2000 caracteres requerido.");
        LocalDateTime date = null;
        try {
            if (input.fechaLimite() == null || input.fechaLimite().length() != 19) throw new DateTimeParseException("Formato", "", 0);
            date = LocalDateTime.parse(input.fechaLimite(), DATE);
        } catch (DateTimeParseException ex) {
            fields.put("fechaLimite", "Fecha Lima obligatoria: YYYY-MM-DDTHH:mm:ss, sin offset.");
        }
        if (!fields.isEmpty()) throw new AdminApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Revisa los campos.", fields);
        if (db.queryForObject("SELECT COUNT(*) FROM curso WHERE id=?", Long.class, input.cursoId()) == 0)
            throw new AdminApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Curso inexistente.", java.util.Map.of("cursoId", "Selecciona un curso existente."));
        try {
            db.update("INSERT INTO actividad (id,curso_id,titulo,descripcion,fecha_limite) VALUES (?,?,?,?,?)",
                input.id(), input.cursoId(), input.titulo().strip(), input.descripcion().strip(), Timestamp.valueOf(date));
        } catch (DataIntegrityViolationException ex) {
            throw new AdminApiException(HttpStatus.CONFLICT, "DATA_CONFLICT", "Identificador duplicado o conflicto de curso. No se sobrescribió la actividad.");
        }
        return detalle(input.id());
    }
}
