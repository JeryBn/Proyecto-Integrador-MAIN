import {test} from 'node:test';
import assert from 'node:assert/strict';
import {
  decryptChatText, encryptChatText, exportChatEncryptionKey, generateChatEncryptionKey, importChatEncryptionKey
} from '../src/alumno/comunicacion/chat-envelope.mjs';

test('private chat envelopes authenticate sender, recipient and message context', async () => {
  const [sender, recipient, outsider] = await Promise.all(Array.from({length: 3}, generateChatEncryptionKey));
  const recipientPublic = await importChatEncryptionKey(await exportChatEncryptionKey(recipient.publicKey));
  const metadata = {conversationId:'chat', messageId:'message', senderDeviceId:'a', recipientDeviceId:'b'};
  const plaintext = 'Nos vemos después de clase 📚';
  const envelope = await encryptChatText(plaintext, metadata, sender, recipientPublic);

  assert.equal(sender.privateKey.extractable, false);
  assert.equal(recipient.privateKey.extractable, false);
  assert.equal(await decryptChatText(envelope, metadata, recipient, sender.publicKey), plaintext);
  assert.notEqual((await encryptChatText(plaintext, metadata, sender, recipientPublic)).ciphertext, envelope.ciphertext);
  await assert.rejects(decryptChatText(envelope, metadata, outsider, sender.publicKey));
  await assert.rejects(decryptChatText(envelope, metadata, recipient, outsider.publicKey));
  await assert.rejects(decryptChatText(envelope, {...metadata, messageId:'different'}, recipient, sender.publicKey));
  const changed = (envelope.ciphertext[0] === 'A' ? 'B' : 'A') + envelope.ciphertext.slice(1);
  await assert.rejects(decryptChatText({...envelope, ciphertext:changed}, metadata, recipient, sender.publicKey));
});
