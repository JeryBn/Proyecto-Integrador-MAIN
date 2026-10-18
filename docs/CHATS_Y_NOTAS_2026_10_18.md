# MAIN — chats para estudiantes y notas por curso

Cambios solicitados el 18/10/2026:
- Inicio conserva cursos, tareas y horario; añade «Mis notas» con evaluaciones por curso.
- Navegación de cinco secciones: Inicio, Chats, Estados, IA y Ajustes. Las tareas se abren desde Inicio.
- Chats usa filas con avatar de iniciales, búsqueda, filtros de privados/grupos y botón «Nuevo chat».
- Nuevo chat permite escoger estudiantes o profesores entre los contactos autorizados. No está limitado a grupos.
- Una conversación privada muestra el nombre del interlocutor, en vez del título genérico «Conversación privada».
- Burbujas alineadas, compositor con botón de envío y opciones de edición desde el mensaje. Se mantiene revisión previa.
- Tocar el encabezado del chat abre información del contacto. Allí están silenciar, bloquear/desbloquear y reportar.
- Bloqueo con confirmación; lista propia en Ajustes → Privacidad → Contactos bloqueados.
- Inicio permanece como pantalla inicial al ingresar. No se muestran estados de conexión, lecturas o calificaciones inventadas.

## Notas
GET /api/v1/grades: solo el alumno autenticado, sin parámetro para elegir otro estudiante.
Devuelve cursos matriculados y evaluaciones con nombre, nota y escala máxima. Cursos sin registros muestran «Sin notas registradas».
No se calcula promedio ni nota final sin reglas acordadas de ponderación.

PUT /api/v1/teaching/courses/{courseId}/grades: profesor asignado al curso, sesión/CSRF y alumno matriculado obligatorios.
PUT /api/admin/v1/courses/{courseId}/grades: mismo registro, para ADMIN.
Cuerpo: {"studentId":1,"assessment":"Práctica 1","score":16,"maximum":20}.
Actualizar la misma evaluación del mismo alumno/curso corrige ese registro. Guarda último responsable y fecha; no constituye historial completo de auditoría.
Acepta cero como nota válida. Rechaza valores negativos, nota mayor que máximo, escala no positiva o más de dos decimales.
La escala se declara por evaluación, no se asume 0–20 para todos los cursos.
Migración V6; sin calificaciones precargadas en la aplicación.

Las pruebas de interfaz registran «Evaluación sintética de revisión» (16/20) en la base local de demostración para comprobar la pantalla. No es una nota real.
Para uso institucional se necesitan las evaluaciones/calificaciones registradas por el docente y la regla de promedio si desean mostrarlo.

## Referencia visual
Se usaron patrones familiares de mensajería manteniendo identidad MAIN, sin importar el historial de WhatsApp ni sus contactos telefónicos.
Referencia de flujo de contacto y bloqueo: https://faq.whatsapp.com/414631957536067
Los permisos de contactos siguen los vínculos verificados de MAIN. No se abrió el directorio a cuentas no autorizadas.
