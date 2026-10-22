# Cierre pendiente de Jery — sprint 2

Corte: 22 de octubre de 2026. Las tarjetas del sprint 3 con fecha 27/10 organizan pendientes; no cierran retroactivamente el sprint 2.

## Preparación completada

El cliente ya admite firma de entrega con las cuatro variables `MAIN_SIGNING_*` y exige HTTPS fuera de debug. `mobile/PREPARAR-ENTREGA.ps1` completa el procedimiento: exige Git limpio, recompila desde cero, verifica la firma contra una huella aprobada, rechaza APK depurable o de otra aplicación y genera `entrega.json` con commit, versión, certificado y SHA-256 del APK. El directorio de salida está ignorado por Git.

Este procedimiento está preparado, pero no se ha ejecutado con una firma de entrega. No demuestra instalación en teléfono, conectividad HTTPS, aceptación ni funcionamiento real de IA. No incluye cambios a backend, web, push, despliegue o moderación de Pamela/Michael.

## Firma y generación del paquete (M17)

1. Jery proporciona o crea una firma propia con Android Studio → Generate Signed Bundle / APK. Conservar copia de seguridad privada; perderla puede impedir actualizar la aplicación.
2. Mantener keystore fuera del repositorio y guardar contraseñas en un gestor privado. No enviar secretos al chat, Trello ni Git. Las extensiones `.jks` y `.keystore` ya están ignoradas, pero eso no protege formatos alternativos ni archivos copiados por error.
3. Configurar en la sesión local privada `MAIN_SIGNING_STORE_FILE`, `MAIN_SIGNING_STORE_PASSWORD`, `MAIN_SIGNING_KEY_ALIAS`, `MAIN_SIGNING_KEY_PASSWORD`. No usar transcripciones de terminal para registrar los valores. Gradle debe usar el JDK configurado para el proyecto y Build Tools 36.0.0.
4. Obtener la huella SHA-256 del certificado elegido desde la herramienta de firma/keystore y aprobarla como identidad de entrega. La huella es pública; la clave privada y sus contraseñas no.
5. Desde `mobile`, ejecutar `./PREPARAR-ENTREGA.ps1 -ExpectedCertificateSha256 '<huella SHA-256 aprobada>'`. El script limpia los resultados de compilaciones anteriores.
6. Distribuir juntos `app/build/outputs/apk/release/app-release.apk` y `entrega.json` por el canal acordado. El manifiesto acredita artefacto/firma, no aceptación funcional ni publicación en GitHub.

La aplicación solicita el origen HTTPS en acceso; introducir la URL entregada por Michael, sin rutas ni credenciales. No contiene una URL de producción inventada. El paquete debug instalado puede requerir desinstalación si su firma difiere: no hacerlo sin acordar la pérdida de datos locales.

## Hoja de aceptación en teléfono físico

Registrar modelo/versión Android, versión/commit y SHA-256 del paquete, entorno, fecha, resultado esperado, resultado observado y captura sin datos personales. Usar cuentas y archivos sintéticos autorizados. Cada caso empieza **PENDIENTE**.

| Caso | Resultado esperado | Estado inicial |
|---|---|---|
| Instalación de entrega | Paquete firmado correcto abre en el teléfono | Pendiente |
| Acceso y salida | Cuenta correcta, contraseña incorrecta rechazada, salida invalida sesión | Pendiente |
| Inicio académico | Cursos, tareas y detalle autorizados, regreso conserva contexto | Pendiente |
| Notas del alumno | Solo calificaciones de cursos autorizados | Pendiente |
| Notas del profesor | Registro/edición únicamente en cursos asignados | Pendiente |
| Privados y grupos | Interlocutor correcto; permisos y bloqueos respetados | Pendiente |
| Adjuntos y estados | Selección, reproducción, audiencia y expiración correctas según contrato entregado | Pendiente |
| Cámara/permisos | Conceder, denegar y cancelar no bloquean la app | Pendiente |
| Red y sesión | Error recuperable al desconectar; sesión no se cruza entre usuarios | Pendiente |
| IA real | Respuesta del proveedor con contexto autorizado o error exacto de cuota | Pendiente |

Pruebas locales/emulador históricas no sustituyen estos casos. Push, cargas, backend, moderación automática y operación siguen con Michael; revisión visual/web sigue con Pamela. Registrar fallos de esas dependencias en sus tarjetas, sin modificar sus módulos.

## Qué necesita aportar Jery para cerrar

| Dependencia | Aporte exacto | Cierre posterior |
|---|---|---|
| M12 IA | Habilitar saldo/cuota en el proyecto de la clave ya creada; confirmar que está habilitado, sin compartir la clave | Ejecutar consulta sintética y conservar respuesta/error; no basta tener clave |
| M17 firma | Ubicación local del keystore y alias; configurar contraseñas privadamente y aprobar huella del certificado | Generar paquete y verificar firma |
| M17 teléfono | Teléfono Android 8+ conectado por USB, depuración autorizada y permiso para instalar; indicar si se pueden reemplazar instalaciones anteriores | Ejecutar y documentar casos reales; pedir aceptación |
| M16 entorno | Elegir proveedor/destino y presupuesto, o confirmar un servidor existente; Michael entrega URL HTTPS y acceso operativo seguro | Conectar Android y verificar contra el entorno entregado |
| M20 política | Nombre/rol del revisor autorizado y suplente; número de días de conservación y regla de borrado de rechazados, pendientes y auditoría | Documentar aprobación y entregar decisión a responsables de implementación |
| M01/M02/T10/M18 | Equipo confirma alcance, responsable de migraciones, contratos y aceptación por historia | Registrar acuerdo real; no sustituirlo con una propuesta |

La documentación, preparación de firma y correcciones propias Android pueden avanzar sin tocar módulos ajenos. Ni pagos, decisiones institucionales, aceptación humana ni un teléfono inexistente se resuelven con código.
