import {test} from 'node:test';
import assert from 'node:assert/strict';
import {deviceSafetyCode} from '../src/alumno/comunicacion/chat-device.mjs';

test('both endpoints obtain the same safety code and a key change alters it', async () => {
  const a = {username:'alumno', deviceId:'device-a', signingKey:'key-a'};
  const b = {username:'otro', deviceId:'device-b', signingKey:'key-b'};
  const code = await deviceSafetyCode(a, b);
  assert.match(code, /^(?:[A-F0-9]{4} ){15}[A-F0-9]{4}$/);
  assert.equal(code, await deviceSafetyCode(b, a));
  assert.notEqual(code, await deviceSafetyCode(a, {...b, signingKey:'replacement'}));
  assert.notEqual(code, await deviceSafetyCode(a, {...b, deviceId:'device-c'}));
});
