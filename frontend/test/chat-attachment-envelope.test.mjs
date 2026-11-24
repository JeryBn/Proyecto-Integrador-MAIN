import {test} from 'node:test';
import assert from 'node:assert/strict';
import {generateChatEncryptionKey} from '../src/alumno/comunicacion/chat-envelope.mjs';
import {decryptChatAttachment, encryptChatAttachment} from '../src/alumno/comunicacion/chat-attachment-envelope.mjs';

test('the encrypted attachment hides its name and content and rejects substitutions', async () => {
  const [sender, recipient, outsider] = await Promise.all(Array.from({length:3}, generateChatEncryptionKey));
  const file = new File(['Documento de prueba 📚'], 'apuntes.txt', {type:'text/plain'});
  const context = {conversationId:'chat', messageId:'message', senderDeviceId:'a', recipientDeviceId:'b'};
  const envelope = await encryptChatAttachment(file, context, sender, recipient.publicKey);
  assert.equal(envelope.ciphertext.type, 'application/octet-stream');
  assert.equal(envelope.ciphertext.size, file.size + 16);
  assert.ok(!JSON.stringify(envelope).includes(file.name));
  assert.notEqual(await envelope.ciphertext.text(), await file.text());
  const result = await decryptChatAttachment(envelope, context, recipient, sender.publicKey);
  assert.equal(result.name, file.name);
  assert.equal(result.type, file.type);
  assert.equal(await result.text(), await file.text());
  await assert.rejects(decryptChatAttachment(envelope, context, outsider, sender.publicKey));
  await assert.rejects(decryptChatAttachment(envelope, {...context,messageId:'other'}, recipient, sender.publicKey));
  const corrupted = new Uint8Array(await envelope.ciphertext.arrayBuffer());
  corrupted[0] ^= 1;
  await assert.rejects(decryptChatAttachment({...envelope,ciphertext:new Blob([corrupted])}, context, recipient, sender.publicKey));
});
