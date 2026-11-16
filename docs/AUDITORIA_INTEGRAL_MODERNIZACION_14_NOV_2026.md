# Auditoría integral de MAIN y modernización institucional — 14 de noviembre de 2026

## Conclusión ejecutiva

MAIN ya no se presenta como una colección de pantallas de curso. La rama `feat/institutional-experience` ofrece una experiencia institucional coherente para estudiante, docente y administración, conserva las reglas de seguridad y está respaldada por regresiones de servidor, web, administración y Android.

El producto técnicamente ejecutable está completo hasta el límite del entorno disponible. El 16 de noviembre se añadió una verificación en un teléfono Android físico. Lo que permanece abierto depende de cuentas de terceros, infraestructura pública, credenciales privadas, firma release o aceptación humana; no corresponde simular esas evidencias ni cerrar tarjetas en nombre de otras personas.

## Evaluación por experiencia

| Área | Situación actual | Resultado de la revisión |
|---|---|---|
| Acceso | Presentación institucional, cuentas de demostración y errores seguros | Adecuado para demostración y adaptable a identidad real |
| Alumno | Inicio con prioridades, cursos, actividades, entregas, notas, avisos y accesos rápidos | Flujo académico completo y comprensible |
| Docente | Métricas propias, cursos asignados, estudiantes, calificaciones y publicación de actividades | Separación real por rol; no es un alumno con etiquetas diferentes |
| Chats | Bandeja de dos paneles, búsqueda, filtros, contactos, grupos, burbujas, adjuntos, captura y progreso | Experiencia moderna comparable con aplicaciones de mensajería, manteniendo moderación previa |
| Estados | Historias de 24 horas, visor inmersivo, compositor con vista previa, color, audiencia, multimedia y captura | Experiencia actual, con privacidad explícita y sin publicar antes de aprobación |
| Perfil | Preferencias, privacidad, contactos, recordatorios, apelaciones y canal confidencial de reporte | Controles sensibles comprensibles y protegidos |
| MAIN IA | Espacio de consulta con sugerencias, privacidad, límites y errores seguros | Interfaz lista; respuesta real depende de cuota del proveedor |
| Administración | Actividades, cuentas, vínculos, cursos, matrículas, auditoría y moderación | Herramienta institucional funcional; las confirmaciones nativas fueron sustituidas por diálogos accesibles |
| Android | Material 3, navegación, actividades, mensajes, estados, avisos y asistente | Compila y pasa análisis; instalación y seis pruebas instrumentadas aprobadas en un Motorola con Android 15; faltan release, servicios externos y aceptación humana |

## Cambios implementados durante la modernización

1. Campus institucional con navegación lateral/escritorio y navegación inferior/móvil.
2. Paneles diferentes para estudiante y docente, con datos y acciones autorizadas por rol.
3. Entregas académicas con borrador, evidencia, revisión y control de concurrencia.
4. Creación docente de actividades dentro de sus cursos asignados.
5. Chats modernizados: selector de contactos, creación de grupos, compositor compacto, adjuntos, captura, progreso, edición y privacidad.
6. Estados modernizados: lista tipo historias, visor, compositor con vista previa y controles multimedia.
7. Perfil modernizado, incluido reporte confidencial con contador y advertencias de privacidad.
8. Confirmaciones sensibles unificadas en usuario y administración, con foco restaurado y soporte de teclado.
9. Corrección de navegación móvil y eliminación de desbordamientos horizontales detectados en prueba real.
10. Manejo profesional de error, desconexión, reintento, sesión revocada, respuestas sobredimensionadas y resultados inciertos.

## Verificación ejecutada en esta revisión

| Componente | Resultado |
|---|---|
| Spring Boot | 173 pruebas, 0 fallos |
| Web React | 52 pruebas, 0 fallos; compilación correcta |
| Administración Django | 92 pruebas, 0 fallos |
| Android | 13 pruebas unitarias, 0 fallos; `lintDebug` y `assembleDebug` correctos |
| Android físico (16/11) | Instalación y recorrido manual correctos; 5/5 pruebas instrumentadas generales, 1/1 con fuente al 200 % y APK release firmado/no depurable instalado |
| APK de desarrollo | Generado correctamente (`app-debug.apk`) |
| Despliegue Oracle Cloud | Contrato, red privada, TLS, secretos, salud y respaldos validados |
| Preflight de entrega | 7 de 12 requisitos disponibles al confirmar estos cambios; 5 bloqueos externos identificados con mensajes accionables |
| Prueba visual | Chats y estados revisados en móvil/escritorio; confirmación sensible probada; sin desborde global móvil |

## Riesgos técnicos y operativos restantes

No se detectó un defecto de código que impida continuar con la puesta en producción. Los riesgos restantes son operativos:

- La aplicación pública todavía no existe: falta la VM gratuita, origen HTTPS y variables MySQL de producción.
- Android ya acredita instalación debug, permiso de notificaciones, segundo plano/reanudación, rotación, fuente al 200 %, recuperación local y una instalación release firmada/no depurable. Aún faltan respaldo externo recuperable, push real, material multimedia sintético y recorrido contra HTTPS final.
- Las notificaciones push requieren dos configuraciones privadas: Firebase Android y cuenta de servicio FCM del servidor.
- Existe una identidad release nueva protegida con DPAPI y un APK firmado verificado. Antes de distribuirlo falta el respaldo externo recuperable de clave y contraseña.
- MAIN IA necesita una clave con cuota activa. La interfaz y los límites ya están preparados.
- Moderación requiere cuentas operativas reales, confirmación de responsables y aceptación de la política de conservación.
- La aceptación visual, de accesibilidad y académica debe realizarla una persona; las pruebas automáticas no pueden firmar por el equipo o docente.

## Interacción necesaria de Jery

1. Crear o facilitar acceso a una VM Oracle Cloud Always Free y elegir el subdominio gratuito.
2. Respaldar fuera de esta PC la nueva identidad Android y guardar su contraseña en un gestor privado antes de distribuir el APK.
3. Descargar de Firebase el `google-services.json` correcto y una cuenta de servicio FCM, manteniéndolos fuera de Git.
4. Habilitar cuota del proyecto de OpenAI si se desea demostrar MAIN IA con respuestas reales.
5. Confirmar responsables de moderación y coordinar la aceptación de Pamela, Michael y el docente.

## Criterio de cierre

El código puede considerarse listo para una ronda final de aceptación. El proyecto completo solo debe marcarse como terminado cuando el mismo commit se despliegue por HTTPS, se pruebe en un teléfono con firma release y servicios reales, y las personas responsables registren su aceptación.
