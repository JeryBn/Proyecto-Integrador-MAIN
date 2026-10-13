package pe.tecsup.main.administracion;

import java.net.URI;
import java.security.Principal;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService service;
    public AdminController(AdminService service) { this.service = service; }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("parameterName", token.getParameterName(), "headerName", token.getHeaderName(), "token", token.getToken());
    }
    @GetMapping("/perfil")
    public Map<String, String> perfil(Principal principal) { return Map.of("usuario", principal.getName(), "rol", "ADMIN"); }

    @GetMapping("/cursos")
    public AdminService.Pagina<AdminService.CursoAdmin> cursos(@RequestParam(defaultValue="0") int offset,
            @RequestParam(defaultValue="20") int limit) { return service.cursos(offset, limit); }

    @GetMapping("/actividades")
    public AdminService.Pagina<AdminService.ActividadAdmin> actividades(@RequestParam(defaultValue="0") int offset,
            @RequestParam(defaultValue="20") int limit) { return service.actividades(offset, limit); }

    @GetMapping("/actividades/{id}")
    public AdminService.ActividadAdmin detalle(@PathVariable long id) { return service.detalle(id); }

    @PostMapping(value="/actividades", consumes="application/json")
    public ResponseEntity<AdminService.ActividadAdmin> crear(@RequestBody JsonNode body) {
        if (!body.isObject()) throw new AdminApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Se requiere objeto JSON.");
        var fields = new java.util.LinkedHashMap<String,String>();
        body.fieldNames().forEachRemaining(key -> {
            if (!java.util.Set.of("id","cursoId","titulo","descripcion","fechaLimite").contains(key)) fields.put(key,"Campo desconocido.");
        });
        for (String key : java.util.List.of("id","cursoId")) {
            if (body.hasNonNull(key) && (!body.get(key).isIntegralNumber() || !body.get(key).canConvertToLong())) fields.put(key,"Usa un entero JSON.");
        }
        for (String key : java.util.List.of("titulo","descripcion","fechaLimite")) {
            if (body.hasNonNull(key) && !body.get(key).isTextual()) fields.put(key,"Usa texto JSON.");
        }
        if (!fields.isEmpty()) throw new AdminApiException(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","Revisa los campos.",fields);
        var input = new AdminService.NuevaActividad(
            body.hasNonNull("id") ? body.get("id").longValue() : null,
            body.hasNonNull("cursoId") ? body.get("cursoId").longValue() : null,
            body.hasNonNull("titulo") ? body.get("titulo").textValue() : null,
            body.hasNonNull("descripcion") ? body.get("descripcion").textValue() : null,
            body.hasNonNull("fechaLimite") ? body.get("fechaLimite").textValue() : null);
        var item = service.crear(input);
        return ResponseEntity.created(URI.create("/api/admin/actividades/" + item.id())).body(item);
    }
}
