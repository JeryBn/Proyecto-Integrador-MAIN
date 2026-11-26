CREATE TABLE alumno (id BIGINT PRIMARY KEY, usuario VARCHAR(60) UNIQUE NOT NULL, nombre VARCHAR(100) NOT NULL);
CREATE TABLE docente (id BIGINT PRIMARY KEY, nombre VARCHAR(100) NOT NULL);
CREATE TABLE curso (id BIGINT PRIMARY KEY, nombre VARCHAR(100) NOT NULL, docente_id BIGINT NOT NULL, FOREIGN KEY(docente_id) REFERENCES docente(id));
CREATE TABLE matricula (alumno_id BIGINT NOT NULL, curso_id BIGINT NOT NULL, PRIMARY KEY(alumno_id,curso_id), FOREIGN KEY(alumno_id) REFERENCES alumno(id), FOREIGN KEY(curso_id) REFERENCES curso(id));
CREATE TABLE actividad (id BIGINT PRIMARY KEY, curso_id BIGINT NOT NULL, titulo VARCHAR(150) NOT NULL, descripcion VARCHAR(2000) NOT NULL, fecha_limite TIMESTAMP NOT NULL, FOREIGN KEY(curso_id) REFERENCES curso(id));
CREATE TABLE entrega (alumno_id BIGINT NOT NULL, actividad_id BIGINT NOT NULL, fecha TIMESTAMP NOT NULL, PRIMARY KEY(alumno_id,actividad_id), FOREIGN KEY(alumno_id) REFERENCES alumno(id), FOREIGN KEY(actividad_id) REFERENCES actividad(id));
