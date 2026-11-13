import test from 'node:test';
import assert from 'node:assert/strict';
import {createAdminApi} from '../src/admin-api.mjs';

const response = (status, value, text) => ({
  status,
  ok: status >= 200 && status < 300,
  text: async () => text ?? (value === null ? '' : JSON.stringify(value)),
});
const csrf = response(200, {headerName: 'X-CSRFToken', token: 'synthetic-token'});

test('fallo del token de seguridad no muestra el mensaje técnico del navegador', async () => {
  const client = createAdminApi({fetcher: async () => { throw new TypeError('Failed to fetch at private-host'); }});
  await assert.rejects(client.loadCsrf(), error => error.code === 'NETWORK'
    && error.message === 'No hay conexión con el servidor. Puedes volver a intentar.'
    && !error.message.includes('private-host'));
});

test('respuesta inválida del token produce un error estable', async () => {
  const client = createAdminApi({fetcher: async () => response(200, null, '<html>proxy interno</html>')});
  await assert.rejects(client.loadCsrf(), error => error.code === 'INVALID_RESPONSE'
    && error.message === 'El servidor respondió con un formato inesperado.');
});

test('escritura colgada vence sin recomendar un reenvío ciego', async () => {
  let calls = 0, expire;
  const client = createAdminApi({
    fetcher: async (path, {signal}) => {
      if (++calls === 1) return csrf;
      return new Promise((resolve, reject) => signal.addEventListener('abort', () => {
        const error = new Error('private timeout detail'); error.name = 'AbortError'; reject(error);
      }, {once: true}));
    },
    schedule: fn => { expire = fn; return 7; },
    cancel: () => {},
  });
  await client.loadCsrf();
  const pending = client.api('/v1/courses', {method: 'POST', json: {}});
  expire();
  await assert.rejects(pending, error => error.code === 'TIMEOUT'
    && error.message.includes('No se confirmó el resultado')
    && error.message.includes('antes de repetir'));
});

test('fallo de red durante una escritura conserva un resultado incierto seguro', async () => {
  let calls = 0;
  const client = createAdminApi({fetcher: async () => {
    if (++calls === 1) return csrf;
    throw new TypeError('socket closed at private-host');
  }});
  await client.loadCsrf();
  await assert.rejects(client.api('/v1/courses', {method: 'POST', json: {}}), error => error.code === 'NETWORK'
    && error.message.includes('Conservamos tus datos')
    && !error.message.includes('private-host'));
});
