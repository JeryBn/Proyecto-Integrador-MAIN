package pe.tecsup.main;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
class ActividadFechaTest {
 private final Clock reloj=Clock.fixed(Instant.parse("2026-10-06T20:30:00Z"),ZoneId.of("America/Lima"));
 private final ActividadService servicio=new ActividadService(new JdbcTemplate(),reloj);
 @Test void antesDelLimiteNoEstaVencida(){assertFalse(servicio.estaVencida(LocalDateTime.of(2026,10,6,15,31),false));}
 @Test void enElLimiteNoEstaVencida(){assertFalse(servicio.estaVencida(LocalDateTime.of(2026,10,6,15,30),false));}
 @Test void despuesDelLimiteEstaVencida(){assertTrue(servicio.estaVencida(LocalDateTime.of(2026,10,6,15,29),false));}
 @Test void completadaNoEstaVencida(){assertFalse(servicio.estaVencida(LocalDateTime.of(2026,10,6,15,29),true));}
}
