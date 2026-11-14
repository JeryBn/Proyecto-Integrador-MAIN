# Mejora de experiencia institucional — 14 de noviembre de 2026

## Resultado implementado

- Interfaz visual institucional adaptable para escritorio y teléfono, con navegación clara, panel académico, estados vacíos, carga, reintento y mensajes de error seguros.
- Chats y novedades rediseñados con una organización familiar para mensajería moderna, sin copiar la identidad visual de otra aplicación.
- Alumno: listado y detalle de actividades, búsqueda y filtros, borrador real, entrega, edición posterior y protección frente a cambios simultáneos.
- Docente: panel con cursos, estudiantes, evaluaciones y alertas; registro/edición de notas; publicación de actividades en cursos propios; conteo separado de entregas y borradores.
- Cuenta de demostración docente `docente` y cuenta de estudiante `alumno`. La contraseña de demostración se presenta solo en la pantalla local de acceso.
- Vista móvil comprobada a 390 × 844 px; el menú inferior conserva etiquetas legibles y los formularios se reorganizan en una columna.

## Seguridad y reglas verificadas

- Un alumno no puede usar las operaciones docentes.
- Un docente no puede consultar ni publicar actividades en cursos ajenos.
- Título, indicaciones y fecha límite se validan nuevamente en el servidor.
- Una actividad publicada aparece únicamente a estudiantes matriculados en el curso.
- Las entregas en borrador no cuentan como entregadas.
- Creaciones y cambios académicos relevantes quedan registrados en auditoría.

## Evidencia ejecutada

- 173 pruebas Spring: 0 fallos.
- 52 pruebas web: 0 fallos.
- Compilación web y paquete ejecutable: correctos.
- Recorrido real en navegador: docente publica “Entrega final del proyecto”; el estudiante matriculado la recibe en su lista con estado pendiente.
- Salud local: `/actuator/health` responde correctamente en el puerto 8088.

## Límites externos que permanecen

No se declara cierre global mientras falten el servidor HTTPS público, teléfono Android físico, firma release recuperable, Firebase/FCM real, cuota de IA y aceptación humana del equipo/docente. El tablero académico de Trello tampoco se modifica desde una integración que no tenga acceso al tablero correcto.
