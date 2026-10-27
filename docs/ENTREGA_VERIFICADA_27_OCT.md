# Entrega local verificada — 27 octubre 2026

Se generó un APK release firmado desde el commit 964fabb00f3f28f33a97feba821c4670c6ce013b. La compilación limpia ejecutó 54 tareas y terminó correctamente. Se verificaron firma, certificado, paquete pe.tecsup.main.mobile y ausencia de depuración. Versión 0.1.0, código 1. Evidencia pública sin secretos: informes/2026-10-27-revision/android-entrega.json. SHA-256 del APK: 3aea0298776867e7a69267b0b6f4cd35e30bea4da120330cfe9e32345190bf13.

Ubicación local: mobile/app/build/outputs/apk/release/app-release.apk. La clave privada y contraseña protegida permanecen fuera de Git en la carpeta privada Windows indicada en FIRMA_PRIVADA_ANDROID.md. La clave no es la firma debug. No se instaló encima de la aplicación existente ni se borraron sus datos. ADB solo detectó emulator-5554; aceptación en teléfono físico pendiente.

La firma y la compilación no cierran M17 ni S3-05 por completo: todavía falta destino HTTPS y prueba funcional de entrega. No se contrataron servicios, habilitó acceso público a la PC ni gastó dinero. FCM necesita credencial de servidor del proyecto Firebase existente; no hay recepción real de push acreditada. El proveedor IA sigue sin cuota comprobada. La política delegada está documentada y falta implementar y evaluar su ciclo operativo.

El paquete local MAIN_LOCAL_PAMELA.zip se reconstruyó con el servidor actualizado y fuentes del commit 72ee6a756318163215ed02b45cb3fc94cbe2610d; 472 archivos fuente, 39.335.638 bytes, SHA-256 7cd0eac50b007d766df18c7677f18be8420ec67b3580cfff09ef7121d67c2313. Contiene manifiesto y hashes; excluye archivos ignorados, variables privadas, firma y credenciales. Es entrega local de fuentes/servidor, no el APK Android. Se conserva fuera de Git en outputs/main-entrega-local-2026-10-27.

El servidor se volvió a empaquetar después de la última corrección visual de revisión y se reinició junto al entorno compartido. No se repitió la batería completa en este empaquetado: las comprobaciones del incremento son las documentadas en RESUMEN_PARA_REVISORES_27_OCT.md (55 Django, 10 Spring y 8 HTTP/MySQL con proveedor controlado).

T09 y T11 se marcaron completas y movieron a FINALIZADO después de la confirmación expresa del alcance técnico por Jery. No se cambiaron vencimientos históricos ni se declaró aceptación del docente. Las demás tarjetas conservan los criterios pendientes reales.
