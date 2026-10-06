-- Base nueva exclusiva de la demostración; no elimina tablas ni otras bases.
CREATE DATABASE IF NOT EXISTS main_s08_jery CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE main_s08_jery;
CREATE TABLE alumno (id BIGINT PRIMARY KEY, usuario VARCHAR(60) UNIQUE NOT NULL, nombre VARCHAR(100) NOT NULL);
CREATE TABLE docente (id BIGINT PRIMARY KEY, nombre VARCHAR(100) NOT NULL);
CREATE TABLE curso (id BIGINT PRIMARY KEY, nombre VARCHAR(100) NOT NULL, docente_id BIGINT NOT NULL REFERENCES docente(id));
CREATE TABLE matricula (alumno_id BIGINT REFERENCES alumno(id), curso_id BIGINT REFERENCES curso(id), PRIMARY KEY(alumno_id,curso_id));
CREATE TABLE actividad (id BIGINT PRIMARY KEY, curso_id BIGINT NOT NULL REFERENCES curso(id), titulo VARCHAR(150) NOT NULL, descripcion VARCHAR(2000) NOT NULL, fecha_limite TIMESTAMP NOT NULL);
CREATE TABLE entrega (alumno_id BIGINT REFERENCES alumno(id), actividad_id BIGINT REFERENCES actividad(id), fecha TIMESTAMP NOT NULL, PRIMARY KEY(alumno_id,actividad_id));


INSERT INTO alumno VALUES (1,'alumno','Alex Demo'),(2,'otro','Sam Demo'),(3,'vacio','Sol Demo');
INSERT INTO docente VALUES (1,'Docente de demostraciÃƒÂ³n'),(2,'Docente de otro curso');
INSERT INTO curso VALUES (1,'ConstrucciÃƒÂ³n y Pruebas de Software',1),(2,'Desarrollo de Aplicaciones Web',1),(3,'Curso privado de otro alumno',2);
INSERT INTO matricula VALUES (1,1),(1,2),(2,3);
INSERT INTO actividad VALUES
(1,1,'Primer avance de MAIN','Demuestra el listado de pendientes y el detalle. Adjunta criterios de aceptaciÃƒÂ³n, pruebas y evidencia real del Sprint 1.',TIMESTAMP '2026-10-06 15:30:00'),
(2,2,'DiseÃƒÂ±o de la interfaz del alumno','Prepara las pantallas de listado y detalle. Considera estados vacÃƒÂ­os, navegaciÃƒÂ³n y adaptaciÃƒÂ³n a mÃƒÂ³vil.',TIMESTAMP '2026-10-08 18:00:00'),
(3,1,'RevisiÃƒÂ³n de criterios de aceptaciÃƒÂ³n','Revisa HU06 y HU07 y relaciona cada criterio con una prueba verificable.',TIMESTAMP '2026-10-05 18:00:00'),
(4,1,'Lectura introductoria de Scrum','Actividad de ejemplo ya completada; no debe aparecer como pendiente.',TIMESTAMP '2026-10-04 18:00:00'),
(5,3,'Actividad de otro alumno','No debe ser visible para Alex Demo.',TIMESTAMP '2026-10-09 18:00:00');
INSERT INTO entrega VALUES (1,4,TIMESTAMP '2026-10-04 17:00:00');
