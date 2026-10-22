import {test} from 'node:test';
import assert from 'node:assert/strict';
import {statusTextColor,ownerPlaylist,canPublish} from '../src/status-model.mjs';
test('los fondos claros y oscuros conservan contraste legible',()=>{assert.equal(statusTextColor('#ffffff'),'#000000');assert.equal(statusTextColor('#000000'),'#ffffff');assert.match(statusTextColor('invalid'),/^#(?:ffffff|000000)$/);});
test('la secuencia contiene únicamente los estados del contacto y no modifica la respuesta',()=>{const feed=[{id:'b',owner:'one',createdAt:'2026-10-22T11:00:00Z'},{id:'other',owner:'two',createdAt:'2026-10-22T10:00:00Z'},{id:'a',owner:'one',createdAt:'2026-10-22T10:00:00Z'}];assert.deepEqual(ownerPlaylist(feed,'one').map(s=>s.id),['a','b']);assert.equal(feed[0].id,'b');});
test('solo estudiantes y docentes muestran la creación de estados',()=>{assert.equal(canPublish('Alumno'),true);assert.equal(canPublish('Docente'),true);assert.equal(canPublish('Familiar'),false);assert.equal(canPublish('ADMIN'),false);});
