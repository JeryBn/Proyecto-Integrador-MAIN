import {test} from 'node:test';
import assert from 'node:assert/strict';
import {exportChatEncryptionKey, generateChatEncryptionKey} from '../src/alumno/comunicacion/chat-envelope.mjs';
import {deviceSafetyCode, verifyChatEncryptionIdentity} from '../src/alumno/comunicacion/chat-device.mjs';

test('both endpoints obtain the same safety code and a key change alters it', async () => {
  const a = {username:'alumno', deviceId:'device-a', signingKey:'key-a'};
  const b = {username:'otro', deviceId:'device-b', signingKey:'key-b'};
  const code = await deviceSafetyCode(a, b);
  assert.match(code, /^(?:[A-F0-9]{4} ){15}[A-F0-9]{4}$/);
  assert.equal(code, await deviceSafetyCode(b, a));
  assert.notEqual(code, await deviceSafetyCode(a, {...b, signingKey:'replacement'}));
  assert.notEqual(code, await deviceSafetyCode(a, {...b, deviceId:'device-c'}));
});

test('the contact verifies the signed encryption key and rejects substitutions', async () => {
  const identity = await crypto.subtle.generateKey('Ed25519', false, ['sign', 'verify']);
  const encryption = await generateChatEncryptionKey();
  const encode = bytes => Buffer.from(bytes).toString('base64url');
  const device = {
    username: 'alumno', deviceId: 'device-a',
    signingKey: encode(await crypto.subtle.exportKey('raw', identity.publicKey)),
    encryptionKey: await exportChatEncryptionKey(encryption.publicKey)
  };
  const payload = new TextEncoder().encode(['MAIN chat encryption key v1', device.username,
    device.deviceId, device.signingKey, device.encryptionKey].join('\n'));
  device.encryptionProof = encode(await crypto.subtle.sign('Ed25519', identity.privateKey, payload));
  const verified = await verifyChatEncryptionIdentity(device);
  assert.equal(await exportChatEncryptionKey(verified), device.encryptionKey);
  const replacement = await generateChatEncryptionKey();
  await assert.rejects(verifyChatEncryptionIdentity({
    ...device, encryptionKey: await exportChatEncryptionKey(replacement.publicKey)
  }));
});
