# Arquitectura del primer incremento

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
