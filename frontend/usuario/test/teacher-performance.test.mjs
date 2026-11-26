import test from 'node:test';
import assert from 'node:assert/strict';
import {periodOf, performanceRows} from '../src/alumno/academico/teacher-performance.mjs';

test('normaliza escalas distintas y da igual peso a cada evaluación',()=>{
  const [row]=performanceRows([{studentId:1,grades:[{score:8,maximum:10},{score:12,maximum:20}]}]);
  assert.equal(row.percent,70);
  assert.equal(row.status,'within');
});
test('una nota cero requiere apoyo; no tener notas es un estado diferente',()=>{
  const rows=performanceRows([{grades:[{score:0,maximum:20}]},{grades:[]}]);
  assert.equal(rows[0].status,'support');
  assert.equal(rows[0].percent,0);
  assert.equal(rows[1].status,'missing');
  assert.equal(rows[1].percent,null);
});
test('filtra por año y periodo sin atribuir fechas a notas antiguas',()=>{
  const roster=[{grades:[{assessment:'2026/1 - Examen',score:8,maximum:20},{assessment:'2026/2 - Examen',score:16,maximum:20},{assessment:'Examen antiguo',score:20,maximum:20}]}];
  assert.equal(performanceRows(roster,'2026/1')[0].percent,40);
  assert.equal(performanceRows(roster,'2026/2')[0].percent,80);
  assert.equal(performanceRows(roster,'unassigned')[0].percent,100);
  assert.equal(periodOf('2026/5 - Fuera de rango'),'unassigned');
});
test('ignora notas y escalas inválidas y respeta el límite de apoyo',()=>{
  const roster=[{grades:[{score:null,maximum:20},{score:10,maximum:0},{score:30,maximum:20},{score:11,maximum:20}]}];
  assert.equal(performanceRows(roster)[0].count,1);
  assert.equal(performanceRows(roster,'all',55)[0].status,'within');
  assert.equal(performanceRows(roster,'all',60)[0].status,'support');
});
