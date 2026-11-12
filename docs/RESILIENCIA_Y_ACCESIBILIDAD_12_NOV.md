# Resiliencia y accesibilidad — 12 de noviembre de 2026

## Alcance implementado

- Web de usuarios: límite de errores de renderizado con recuperación, aviso accesible de desconexión y reconexión, conservación del borrador mientras la red no está disponible y mensajes seguros ante fallos de cámara o micrófono.
- API Spring: contrato JSON estable para recurso inexistente, parámetro inválido, conflicto de datos, base no disponible y fallo inesperado, tanto en actividades como en los módulos comunitarios y administrativos. Los detalles técnicos quedan en el registro del servidor y no se exponen a la persona usuaria.
- Administración: se integró el rediseño visual del panel de actividades realizado por Pamela Meneses y se completó su accesibilidad con contraste legible, encabezado principal y región de contexto identificable. La interfaz React incorpora recuperación ante errores de renderizado, avisos accesibles de desconexión y reconexión, y conservación del formulario sin enviar. Django devuelve respuestas JSON estables para rutas API inexistentes y fallos internos; las páginas no API reciben mensajes de texto seguros. Se añadió un ícono local para evitar solicitudes fallidas del navegador.
- Android: mensajes diferenciados para espera agotada, servidor inaccesible, error TLS, interrupción de red, credenciales inválidas y fallos inesperados. La configuración del servidor y los flujos multimedia conservan únicamente mensajes validados; los errores no reconocidos no muestran rutas ni detalles internos.
- Inicio local: el iniciador reconoce procesos propios aunque Windows no informe correctamente el dueño del puerto, evitando levantar instancias duplicadas.
- Accesibilidad web: se corrigieron contrastes, reflujo, foco visible, áreas táctiles y comportamiento con movimiento reducido.

## Comprobaciones automatizadas

| Área | Resultado |
|---|---:|
| Spring | 157 pruebas, sin fallos |
| Django | 82 pruebas, sin fallos |
| Web | 38 pruebas, sin fallos |
| Android | 9 pruebas unitarias, sin fallos; `lintDebug` y `assembleDebug` correctos |
| Arquitectura compartida | 16 comprobaciones HTTP reales después del reinicio, sin fallos |
| Web de usuarios | 18 vistas en Chrome, normal y 200 % emulado: 0 infracciones Axe, 0 errores de consola, 0 desbordamientos globales |
| Administración | 6 estados auditados — acceso normal, desconectado y 200 % emulado; panel autenticado normal y 200 %; formulario nuevo al 200 % — con 0 infracciones Axe y 0 desbordamientos globales; borrador conservado al desconectar y recuperar |

La prueba de desconexión verificó que se muestran avisos comprensibles, la interfaz anuncia la recuperación y el texto aún no enviado permanece intacto. Las consultas `401` que realiza Administración para saber si existe una sesión son el comportamiento esperado antes de mostrar el acceso y no se contabilizan como fallo funcional.

## Límites de esta evidencia

- El 200 % fue emulado en Chrome mediante un ancho CSS equivalente y densidad 2; debe complementarse con zoom manual real.
- Axe detecta problemas automáticos, pero no sustituye una revisión humana con lector de pantalla y navegación completa por teclado.
- Siguen pendientes las validaciones que requieren servicios externos o dispositivos reales, entre ellas FCM real, despliegue público HTTPS y la aceptación manual del equipo.

## Criterio de privacidad

Los mensajes visibles son accionables y no incluyen trazas, consultas SQL, rutas privadas, credenciales ni contenido de respuestas internas. Los registros del servidor conservan la información técnica necesaria para diagnóstico sin enviarla a la interfaz.
