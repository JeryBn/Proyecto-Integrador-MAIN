# COLEGIA · Sistema escolar MAIN

Plataforma web para alumnos, docentes, apoderados y administradores.

## Cómo funciona

- **React Usuario → Spring Boot:** login, tareas, agenda, notas, asistencia y libreta PDF.
- **React Admin → Django:** cuentas, activación, vínculos familiares, aulas, cursos y matrículas.
- **Una base compartida:** Spring aplica las migraciones Flyway; Django administra sus tablas internas. El acceso usa sesiones, cookies y CSRF, con permisos comprobados en el servidor.

```text
backend/
  springboot/       API escolar Java y migraciones
  django-admin/     API administrativa Python
frontend/
  usuario/          React para alumno, docente y apoderado
  admin/            React para Administración
database/           Datos de demostración
docs/               Documentación técnica
scripts/            Herramientas y pruebas
docker-compose.yml  Ejecución del conjunto
.env.example        Configuración de ejemplo
```

`mobile/` se conserva y no se necesita para ejecutar la web.

## Ejecutar en otra laptop

Instalar **Git y Docker Desktop**, abrir Docker y usar contenedores Linux. No hace falta instalar Java, Python ni MySQL por separado.

**Rama de entrega: `web/frontend-backend`.** Ejecutar en PowerShell:

```powershell
git clone --branch web/frontend-backend https://github.com/JeryBn/Proyecto-Integrador-MAIN.git
cd Proyecto-Integrador-MAIN
Copy-Item .env.example .env
```

Abrir `.env` y cambiar `DB_PASSWORD`, `DB_ROOT_PASSWORD`, `DJANGO_SECRET_KEY` y `ADMIN_PASSWORD`. La clave del administrador debe tener al menos 12 caracteres. Dejar `AI_PROVIDER=disabled` para arrancar sin IA.

```powershell
docker compose config --quiet
docker compose up -d --build --wait --wait-timeout 300
```

| Portal | Dirección |
|---|---|
| Comunidad Escolar | http://localhost:5173/login.html |
| Administración | http://localhost:5174/admin.html |

El administrador entra con `ADMIN_USERNAME` y `ADMIN_PASSWORD` de `.env`.

**Demostración opcional:** ejecutar una sola vez sobre una base nueva de pruebas:

```powershell
docker compose exec django-admin python manage.py seed_colegia
```

Crea cuentas básicas como `alumno`, `docente` y `apoderado`, con clave `ColegiaDemo2026!`. No incluye todos los registros de las capturas; rechaza bases con alumnos. **Clonar el código no copia los datos de otra laptop.**

Para nuevas cuentas, Administración genera una invitación y la persona usa **Activar cuenta**. El docente necesita cursos asignados, el alumno matrícula y el apoderado un vínculo familiar autorizado. El correo automático está pendiente de configuración; se pueden entregar códigos privados.

Para revisar o detener:

```powershell
docker compose ps
docker compose logs --tail 100 springboot django-admin
docker compose down
```

`down` conserva los datos; `down -v` elimina el volumen. No subir `.env` ni datos personales a GitHub.

## Trabajo en equipo y estado

Cada compañero usa su identidad de Git y una rama propia. Guardar cambios en commits concretos (`feat:`, `fix:` o `docs:`), hacer `push` y abrir un Pull Request para revisión. Conservar los autores del historial anterior.

En **Docker**: MySQL 8 interno **3306**, Spring **8081**, Django **8000**. El entorno local de Pamela usa MariaDB **3312** y Spring **8088**. Los frontends usan **5173/5174** en ambos modos; no se necesitan las rutas privadas de su laptop para Docker.

**Comprobado:** portales y acceso en el entorno local, compilaciones y configuración de Compose. **Pendiente:** arranque completo de Docker con MySQL 8 vacío y persistencia tras reiniciar; unificar las tareas nuevas del docente con los pendientes y agenda anteriores. Aún no se certifica la instalación completa en otra laptop.

Detalles: [arquitectura](docs/arquitectura/arquitectura.md), [API](docs/api/endpoints.md), [base de datos](database/README.md) y [verificaciones](docs/verificacion-colegia.md).
