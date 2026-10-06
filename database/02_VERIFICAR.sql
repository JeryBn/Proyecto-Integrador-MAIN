USE main_s08_jery;
SHOW TABLES;
SELECT a.id,a.titulo,c.nombre AS curso,al.usuario,
       CASE WHEN e.actividad_id IS NULL THEN 'Pendiente' ELSE 'Completada' END AS estado
FROM actividad a JOIN curso c ON c.id=a.curso_id
JOIN matricula m ON m.curso_id=c.id JOIN alumno al ON al.id=m.alumno_id
LEFT JOIN entrega e ON e.actividad_id=a.id AND e.alumno_id=al.id
ORDER BY al.usuario,a.id;
