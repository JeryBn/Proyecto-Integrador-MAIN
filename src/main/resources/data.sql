INSERT INTO alumno VALUES (1,'alumno','Alex Demo'),(2,'otro','Sam Demo'),(3,'vacio','Sol Demo');
INSERT INTO docente VALUES (1,'Docente de demostraciÃ³n'),(2,'Docente de otro curso');
INSERT INTO curso VALUES (1,'ConstrucciÃ³n y Pruebas de Software',1),(2,'Desarrollo de Aplicaciones Web',1),(3,'Curso privado de otro alumno',2);
INSERT INTO matricula VALUES (1,1),(1,2),(2,3);
INSERT INTO actividad VALUES
(1,1,'Primer avance de MAIN','Demuestra el listado de pendientes y el detalle. Adjunta criterios de aceptaciÃ³n, pruebas y evidencia real del Sprint 1.',TIMESTAMP '2026-10-06 15:30:00'),
(2,2,'DiseÃ±o de la interfaz del alumno','Prepara las pantallas de listado y detalle. Considera estados vacÃ­os, navegaciÃ³n y adaptaciÃ³n a mÃ³vil.',TIMESTAMP '2026-10-08 18:00:00'),
(3,1,'RevisiÃ³n de criterios de aceptaciÃ³n','Revisa HU06 y HU07 y relaciona cada criterio con una prueba verificable.',TIMESTAMP '2026-10-05 18:00:00'),
(4,1,'Lectura introductoria de Scrum','Actividad de ejemplo ya completada; no debe aparecer como pendiente.',TIMESTAMP '2026-10-04 18:00:00'),
(5,3,'Actividad de otro alumno','No debe ser visible para Alex Demo.',TIMESTAMP '2026-10-09 18:00:00');
INSERT INTO entrega VALUES (1,4,TIMESTAMP '2026-10-04 17:00:00');
