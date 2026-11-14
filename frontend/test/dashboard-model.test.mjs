import test from 'node:test';
import assert from 'node:assert/strict';
import {nextClass,progressPercent,recentNotices} from '../src/dashboard-model.mjs';

test('progressPercent normaliza evaluaciones con escalas diferentes', () => {
  assert.equal(progressPercent([{grades:[{score:18,maximum:20},{score:80,maximum:100}]}]),85);
  assert.equal(progressPercent([]),null);
});

test('nextClass elige la siguiente sesión de la semana', () => {
  const now=new Date('2026-11-16T10:00:00'); // lunes
  const result=nextClass([
    {id:1,dayOfWeek:1,startsAt:'09:00',course:'Pasada'},
    {id:2,dayOfWeek:1,startsAt:'13:00',course:'Siguiente'},
    {id:3,dayOfWeek:2,startsAt:'08:00',course:'Mañana'}
  ],now);
  assert.equal(result.course,'Siguiente');
});

test('recentNotices ordena y limita los avisos', () => {
  const result=recentNotices([
    {id:1,createdAt:'2026-11-10T10:00:00'},
    {id:2,createdAt:'2026-11-12T10:00:00'},
    {id:3,createdAt:'2026-11-11T10:00:00'}
  ],2);
  assert.deepEqual(result.map(item=>item.id),[2,3]);
});
