# Firma local de entrega Android

Por delegación de Jery se prepara una identidad técnica de firma nueva, sin compras ni publicación. `mobile/ENTREGA-PRIVADA.ps1 -CrearFirma` crea una clave RSA de 3072 bits y certificado de 25 años fuera de Git, en `%LOCALAPPDATA%\MAIN-private\android`. Se niega a sobrescribir una carpeta existente. La carpeta permite acceso al usuario Windows actual y la contraseña aleatoria se guarda cifrada con DPAPI. No se imprime ni incorpora al APK.

Una segunda ejecución sin parámetros recupera temporalmente la contraseña, ejecuta la compilación limpia y verifica firma, certificado, identidad y ausencia de depuración. Después restaura las variables del proceso. Requiere Java y Android SDK configurados, árbol Git limpio y la misma cuenta Windows. No instalar sobre otra firma ni desinstalar la app del usuario para forzar una actualización.

Guardar una copia privada del keystore y su contraseña recuperable antes de distribuir a terceros. El archivo DPAPI depende de esta cuenta Windows; copiarlo solo a otra máquina no garantiza recuperación. La pérdida de la clave impide actualizar instalaciones con la misma identidad. Los archivos privados nunca se suben a GitHub ni al paquete local.

El manifiesto `entrega.json` deja explícitamente pendientes la aceptación en teléfono físico y el entorno HTTPS. Una firma válida no acredita funcionamiento de FCM, despliegue ni aceptación funcional.
