import {test} from 'node:test';
import assert from 'node:assert/strict';
import {prepareEncryptedChatText, sendEncryptedChatEnvelope} from '../src/alumno/comunicacion/encrypted-chat-api.mjs';
import {userApi} from '../src/alumno/datos/user-api.mjs';

test('encrypted chat refuses to prepare an outgoing message without local moderation', async () => {
  await assert.rejects(prepareEncryptedChatText({text:'Prueba'}), /moderación local/);
});

test('the transport receives only a prepared encrypted envelope', async mock => {
  const envelope = {messageId:'m', senderDeviceId:'a', recipientDeviceId:'b', version:1,
    encapsulatedKey:'enc', ciphertext:'cipher'};
  const call = mock.mock.method(userApi, 'call', async () => null);
  await sendEncryptedChatEnvelope('chat', {...envelope, text:'Texto que debe quedarse en el dispositivo'});
  assert.deepEqual(call.mock.calls[0].arguments, ['/api/v1/conversations/chat/encrypted',
    {method:'POST', json:envelope, signal:undefined}]);
});
