# Cancelación accesible de confirmaciones al 30 de octubre

Corrección de M14 en la confirmación compartida por Perfil, mensajes y estados. Antes, cancelar con Escape eliminaba el diálogo y dejaba el foco en BODY, perdiendo la posición de teclado. El botón que abrió la confirmación se recuerda antes de mostrar el diálogo y recibe de nuevo el foco al terminar. Si ya no existe o está deshabilitado, el contenido principal es el destino alternativo. Los fotogramas pendientes se cancelan al desmontar el componente o abrir una confirmación nueva.

En el navegador real se abrió la confirmación de bloqueo de un contacto sintético. El foco inicial quedó en Cancelar acción; Tab alcanzó Confirmar acción y Shift Tab volvió a Cancelar acción. Se probaron por separado Escape y la activación con Enter del botón Cancelar acción. Ambos cerraron el diálogo y devolvieron el foco a Bloquear Sam Demo. No se activó Confirmar acción.

La prueba se repitió con viewport de 320 píxeles. Una consulta autenticada independiente a Spring y MySQL confirmó que el contacto seguía desbloqueado. Se compilaron los recursos y pasaron las 37 pruebas web existentes; no se afirma que esas pruebas unitarias por sí solas cubran el foco de navegador.

Evidencia: docs/informes/2026-10-30-dialogos/results.json y foco-restaurado-320.png. Las cuentas y contactos son de demostración. No se enviaron reportes, cancelaron mensajes, bloquearon contactos ni modificaron datos de personas reales. La mejora conserva las acciones y validaciones de servidor existentes. No acredita una evaluación completa con lector de pantalla ni una aceptación de terceros.
