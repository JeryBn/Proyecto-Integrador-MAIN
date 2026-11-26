INSERT INTO alumno VALUES (1,'alumno','Alex Demo'),(2,'otro','Sam Demo'),(3,'vacio','Sol Demo');
INSERT INTO docente VALUES (1,'Docente de demostración'),(2,'Docente de otro curso');
INSERT INTO curso(id,nombre,docente_id) VALUES (1,'Geometría',1),
(2,'Trigonometría',1),
(3,'Historia',2),
(4,'Comprensión lectora',1),
(5,'Aritmética',1),
(6,'Álgebra',1),
(7,'Comunicación',1),
(8,'Biología',1),
(9,'Física',1),
(10,'Química',1),
(11,'Inglés',1),
(12,'Educación Física',1),
(13,'Arte y Cultura',1),
(14,'Desarrollo Personal, Ciudadanía y Cívica',1),
(15,'Educación para el Trabajo',1);
INSERT INTO matricula VALUES (1,1),(1,2),(2,3);
INSERT INTO actividad VALUES
(1,1,'Primer avance de MAIN','Demuestra el listado de pendientes y el detalle. Adjunta criterios de aceptación, pruebas y evidencia real del Sprint 1.',TIMESTAMP '2026-10-06 15:30:00'),
(2,2,'Diseño de la interfaz del alumno','Prepara las pantallas de listado y detalle. Considera estados vacíos, navegación y adaptación a móvil.',TIMESTAMP '2026-10-08 18:00:00'),
(3,1,'Revisión de criterios de aceptación','Revisa HU06 y HU07 y relaciona cada criterio con una prueba verificable.',TIMESTAMP '2026-10-05 18:00:00'),
(4,1,'Lectura introductoria de Scrum','Actividad de ejemplo ya completada; no debe aparecer como pendiente.',TIMESTAMP '2026-10-04 18:00:00'),
(5,3,'Actividad de otro alumno','No debe ser visible para Alex Demo.',TIMESTAMP '2026-10-09 18:00:00');
INSERT INTO entrega VALUES (1,4,TIMESTAMP '2026-10-04 17:00:00');
