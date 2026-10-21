# Arquitectura MAIN

Actualización del 21/10/2026 según el diagrama aportado nuevamente por Jery. Administración React/Django y usuarios React/Kotlin con Spring Boot comparten una base de datos. Se implementó el flujo administrativo de actividades con esta separación; no se declara migrada toda la administración posterior.

```mermaid
flowchart LR
  AR[Administración React] --> DJ[Django actividades Sprint 1]
  UR[Usuarios React] --> SB[Spring Boot usuarios]
  KM[Android Kotlin] --> SB
  DJ --> DB[(MySQL compartido)]
  SB --> DB
  EX[Administración extendida en transición] -.-> SB
```

## Implementación comprobada el 21 de octubre

- Django sirve el React original y atiende ADMIN, cursos, listado, detalle y creación de actividades. Lee/escribe MySQL directamente; no deriva estas operaciones a Spring.
- Spring sirve la web de usuarios y la API Android. Consulta de actividades limitada a matrícula; entregadas excluidas del listado pendiente. Actividad ajena e inexistente devuelven 404.
- Identidad `main_user` y BCrypt compartidos, sesiones y CSRF independientes. Django revalida rol, activación y versión de contraseña por solicitud.
- Flyway V1–V9 administra negocio. Django usa modelos `managed=False` y solo migra sesiones/registro; no recrea tablas académicas.
- Fechas académicas en hora Lima sin offset, sesión SQL en −05:00. Creación y auditoría Django atómicas.

Entorno local: Spring 8088, Django 8090 y MySQL aislado 3307, loopback. Dato creado por Django leído por Spring y Android con permisos y conservado tras reiniciar MySQL y ambos backends. [Evidencias y límites](REGULARIZACION_SPRINT_12_OCT.md).

La base H2 anterior y su respaldo privado se conservan como ensayos históricos; no se afirma migración de todas sus conversaciones/estados a MySQL.

## Trabajo restante de arquitectura

Los módulos posteriores de cuentas, vínculos, cursos/matrículas y moderación siguen en Spring de compatibilidad. Traslado a Django y paridad completa en M21/M22. Pendientes aceptación del contrato/HU administrativa oficial, HTTPS y dispositivo físico.

[Guía Django](../admin-django/README.md) y [reinicio con configuración privada existente](../scripts/Start-SharedArchitecture.ps1). El iniciador no inicializa ni borra bases y rechaza puertos de otros procesos.

## Antecedente del primer incremento

El contenido siguiente corresponde al estado inicial y conserva sus pendientes históricos; no representa el corte del 21/10 descrito arriba.

## Arquitectura objetivo de las bases 2026-2

La diapositiva 4 presenta: administraciÃ³n con React y Django; usuario con React y Kotlin y backend Spring Boot; ambos backends conectados a una BBDD compartida. Imagen original guardada en evidencia/arquitectura-base-tecsup.png y bases originales en el paquete de fuentes. Esto corrige la lectura textual anterior, que no incluÃ­a el contenido de la imagen.

```mermaid
flowchart LR
 AW[AdministraciÃ³n: React - pendiente] --> DJ[Django - pendiente]
 UW[Usuario: React - pendiente de integrar] --> SB[Spring Boot - implementado]
 KM[Kotlin mÃ³vil - pendiente] --> SB
 DJ --> DB[(Base compartida - integraciÃ³n pendiente)]
 SB --> DB
```

## Componentes implementados en el incremento
```mermaid
flowchart LR
 A[Cliente React/Kotlin - pendiente] --> B[Spring Security: sesiÃ³n y permisos]
 B --> C[API ActividadController]
 C --> D[ActividadService]
 D --> E[(H2: datos sintÃ©ticos)]
```

## Modelo relacional implementado
```mermaid
erDiagram
 ALUMNO ||--o{ MATRICULA : cursa
 CURSO ||--o{ MATRICULA : incluye
 DOCENTE ||--o{ CURSO : dicta
 CURSO ||--o{ ACTIVIDAD : publica
 ALUMNO ||--o{ ENTREGA : registra
 ACTIVIDAD ||--o{ ENTREGA : recibe
 ALUMNO { long id PK }
 DOCENTE { long id PK }
 CURSO { long id PK
 long docente_id FK }
 MATRICULA { long alumno_id PK,FK
 long curso_id PK,FK }
 ACTIVIDAD { long id PK
 long curso_id FK
 datetime fecha_limite }
 ENTREGA { long alumno_id PK,FK
 long actividad_id PK,FK
 datetime fecha }
```

Entrega permite saber si una actividad estÃ¡ completada para cada alumno. El listado filtra por matrÃ­cula e identidad de la sesiÃ³n. Consultas parametrizadas con JdbcTemplate; no se concatena texto de bÃºsqueda a SQL.

## EvoluciÃ³n pendiente
Usuarios persistidos; roles docente/apoderado/administraciÃ³n; aplicaciÃ³n mÃ³vil nativa; funciÃ³n de IA aprobada; servidor y BD persistente; revisiÃ³n de arquitectura con los otros cursos.
