# Contratos v1 para integración — M22

Autenticación: sesión Spring y cookies; iniciar con login-token, formulario login y comprobar perfil.
GET /api/csrf devuelve token, parameterName, headerName. Toda mutación JSON exige el token vigente en headerName.
401 finaliza sesión; 403 deniega; 404 oculta recurso inexistente o no autorizado; 409 indica conflicto de versión/idempotencia; 400 valida datos.
Errores de dominio: {"error":{"code":"CODIGO","message":"mensaje público"}}. Errores de seguridad previos al controlador pueden no seguir ese cuerpo.
No conservar claves API ni credenciales de usuario en el cliente. Android tiene una cookie jar por sesión y borra contenido al salir.

## Cuenta y agenda
GET /api/v1/profile → usuario,nombre,rol (contrato compatible con /api/perfil).
GET/PUT /api/v1/preferences → darkTheme:boolean,notifications:boolean,statusAudience:CONTACTS|PRIVATE.
GET /api/v1/contacts → username,name,role,muted,blocked de contactos autorizados.
PUT /api/v1/contacts/{username}/preferences → muted,blocked.
POST /api/v1/contacts/{username}/reports → reason; devuelve id.
GET /api/v1/agenda → courses,timetable,students. Alcance del usuario autenticado; no permite elegir identidad ajena.
Administración /api/admin/v1: usuarios, recuperación/inhabilitación, parentescos, contactos verificados, reportes y horarios. Rol ADMIN; el usuario normal no aprueba su propio acceso.

## Conversaciones
GET /api/v1/conversations?offset=0&limit=30 → id,kind:DIRECT|GROUP,title,administrator,state.
POST /api/v1/conversations/direct {username} → id; exige contacto verificado, sin bloqueo.
POST /api/v1/conversations/groups {title} → id.
POST /api/v1/conversations/{id}/invitations {username}.
POST /api/v1/conversations/{id}/invitation {accept:boolean}.
GET /api/v1/conversations/{id}/members → username,name,role,state,administrator.
PUT /api/v1/conversations/{id}/members/{username} {administrator,remove}.
Nuevos miembros solo acceden a mensajes creados desde su aceptación. El último administrador no puede salir sin sucesor.

## Mensajes y revisión
GET /api/v1/conversations/{id}/messages?offset=0&limit=30. Página de mensajes recientes; cada página está en orden cronológico.
POST /api/v1/conversations/{id}/messages {clientKey:UUID,text} → Message.
PUT /api/v1/messages/{id} {expectedRevision,text} → Message.
POST /api/v1/messages/{id}/cancel {expectedRevision}.
Message: id,conversationId,sender,senderName,revision,text,state,reason,correctedBeforeDelivery,editedAfterDelivery,createdAt,contentHash.
Autor ve última revisión y motivo; receptor solo última revisión aprobada. Hash/motivo de revisión no se exponen al receptor.
Todos los roles pasan por revisión. Pendiente/rechazado no se entrega. Edición posterior mantiene visible la versión aprobada hasta aprobar el cambio.
GET /api/admin/v1/moderation/messages?offset=0&limit=30 → cola de últimas revisiones pendientes.
POST /api/admin/v1/moderation/messages/{id}/review {revision,contentHash,approved,reason}.
La interfaz humana se encuentra en /moderation.html y requiere ADMIN. Una clave de OpenAI configurada no activa aprobación automática.

## Avisos e IA
Ver IA_Y_DESPLIEGUE.md para los contratos, límites, privacidad y errores.
Paginación soportada por conversaciones, mensajes, cola y avisos: offset>=0, limit entre1 y100.
Contactos/agenda no tienen todavía paginación; evitar suponer que toda colección es paginada.
Timestamps locales actuales de chat no incluyen zona explícita; acordar ISO con zona para integración de producción antes de M24.
Los adjuntos y estados todavía no tienen contrato implementado. No simularlos desde web ni Android.

## Propiedad de datos
Spring Boot y Flyway son la implementación de este checkout. No hay servicio Django integrado.
No ejecutar dos migradores sobre las mismas tablas. Flyway rechaza por defecto una base no vacía sin su historial.
Coordinar con Michael antes de baselinar MySQL; hacer copia y probar restauración. Mantener rutas HU06/HU07 para los clientes existentes.
