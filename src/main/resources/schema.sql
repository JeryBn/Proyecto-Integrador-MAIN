CREATE TABLE alumno (id BIGINT PRIMARY KEY, usuario VARCHAR(60) UNIQUE NOT NULL, nombre VARCHAR(100) NOT NULL);
CREATE TABLE docente (id BIGINT PRIMARY KEY, nombre VARCHAR(100) NOT NULL);
CREATE TABLE curso (id BIGINT PRIMARY KEY, nombre VARCHAR(100) NOT NULL, docente_id BIGINT NOT NULL REFERENCES docente(id));
CREATE TABLE matricula (alumno_id BIGINT REFERENCES alumno(id), curso_id BIGINT REFERENCES curso(id), PRIMARY KEY(alumno_id,curso_id));
CREATE TABLE actividad (id BIGINT PRIMARY KEY, curso_id BIGINT NOT NULL REFERENCES curso(id), titulo VARCHAR(150) NOT NULL, descripcion VARCHAR(2000) NOT NULL, fecha_limite TIMESTAMP NOT NULL);
CREATE TABLE entrega (alumno_id BIGINT REFERENCES alumno(id), actividad_id BIGINT REFERENCES actividad(id), fecha TIMESTAMP NOT NULL, PRIMARY KEY(alumno_id,actividad_id));
