# Modelo Stitch integrado en COLEGIA

Referencia local: stitch_colegia_school_platform_ui (seis pantallas HTML/capturas y guía Academic Modernism). El paquete inspeccionado no contiene INICIAR-WINDOWS.bat ni servidor Python. No se ejecutaron scripts del modelo.

Se adapta presentación, no se sustituye el sistema por datos de ejemplo: blanco, azul marino, azul glaciar, acentos amarillos; cabecera y lateral; login; panel de alumno con horario real y pendientes; tarjetas del docente y apoderado; directorio administrativo conectado. Los nombres, cifras, grados y porcentajes ficticios de las capturas no se incorporan a la base.

Los indicadores de rol muestran la sesión vigente y no permiten cambiar permisos. El acceso Comunidad llama a Spring y Directivo/Admin a Django. Google/Microsoft, activación automática, recuperación por IA y declaraciones de cifrado del modelo no se presentan como implementadas. No se almacena la contraseña en el navegador.

La nueva apariencia comienza clara. La selección explícita de modo oscuro se conserva en este navegador y se sincroniza con la preferencia existente al cambiarla. No se modifica automáticamente la base al aplicar el diseño.

Las fuentes de diseño compartidas se distribuyen en src/diseno de ambos proyectos para conservar builds Docker independientes. Deben mantenerse iguales ui.jsx, access.jsx y modernism.css salvo adaptaciones específicas de acceso.

## Integración de las plantillas originales

Tras la aclaración de Pamela se importó el árbol HTML original de las pantallas en stitch-templates.json y se conservaron las clases y configuración Tailwind del exportado. Se compilan localmente con Tailwind 3.4.17; no se ejecutan los scripts del prototipo ni se requiere CDN. Logotipo, Inter y Material Symbols se descargaron a public/diseno. Los slots React enlazan cuenta, cursos, notas, asistencia y publicaciones. Se usó la segunda variante del acceso unificado.

Las cifras, nombres y estados del diseño son ejemplos, por lo que se sustituyen por datos autorizados. Mérito y asistencia docente no disponibles aparecen sin resultado. La tarjeta que originalmente indicaba promedio muestra cantidad de evaluaciones y se rotula como tal; no inventa una nota final. Google y Microsoft se muestran deshabilitados hasta integrar su autenticación. La recuperación remite a administración. Las preferencias visuales no cambian permisos.

Verificado en navegador con Spring H2 y Django SQLite aislados: login, vistas de los cuatro roles, cambio de hijo y detalle de competencias, navegación a gestión de cuentas. Los 56 tests web pasaron y ambos builds compilaron. No se modificaron modelos ni migraciones ni se usó la base real para estas pruebas.

Se verificaron además cinco pruebas de sesión y recursos públicos de Spring (sin fallos). Los recursos de diseño públicos no habilitan el acceso a datos académicos.

Prueba final de Django: 102 casos aprobados, incluido el acceso público limitado al logotipo/fuentes y el bloqueo del perfil sin sesión. Empaquetado Spring actualizado con los builds React finales.

## Ajuste visual del 26 de noviembre de 2026

Se alinearon las medidas del lateral y cabecera con el exportado. El alumno muestra las seis opciones del modelo; Horarios y Agenda consulta `/api/v1/agenda` y ordena las actividades disponibles por fecha. Reportes & BI consulta `/api/v1/grades`; muestra una referencia porcentual con igual peso por evaluación, sin sustituir la libreta ni decidir promoción. Las herramientas anteriores se conservan en el menú de cuenta.

El asistente lateral usa un compositor compacto y sugerencias; mantiene control de duplicados, cancelación y errores. La información sobre el tratamiento de consultas se puede desplegar. La inferencia de IA no se probó en este ajuste.

Administración tiene acceso directo a Usuarios, Gestión Escolar, Matrículas y Auditoría. Las acciones previas permanecen en Más herramientas. La auditoría es lectura paginada del endpoint existente; no se modificaron tablas ni permisos.

Verificación: 49 pruebas Usuario y 7 Admin aprobadas, ambos builds React correctos y paquete Spring reconstruido tras detener el proceso que lo utilizaba. Se revisaron alumno y administrador sobre el entorno local, incluidos los nuevos accesos, y docente/apoderado sobre H2 aislado. También se revisó el alumno a 390 px, sin desbordamiento horizontal. La ausencia de horarios, aulas o calificaciones se conserva como estado vacío; las cifras del prototipo no se insertaron en MariaDB.
