import {Aes256Gcm, CipherSuite, DhkemP256HkdfSha256, HkdfSha256} from '@hpke/core';

const suite = new CipherSuite({
  kem: new DhkemP256HkdfSha256(),
  kdf: new HkdfSha256(),
  aead: new Aes256Gcm()
});
const utf8 = new TextEncoder();
const info = utf8.encode('MAIN private message HPKE v1');
const maxPlaintextBytes = 16384;

function authenticatedMetadata({conversationId, messageId, senderDeviceId, recipientDeviceId}) {
  const values = [conversationId, messageId, senderDeviceId, recipientDeviceId];
  if (values.some(value => typeof value !== 'string' || !value || value.length > 128)) {
    throw new TypeError('Faltan identificadores del mensaje.');
  }
  return utf8.encode(JSON.stringify(values));
}

function encode(bytes) {
  return btoa(String.fromCharCode(...new Uint8Array(bytes)))
    .replaceAll('+', '-').replaceAll('/', '_').replace(/=+$/, '');
}

function decode(value, maxBytes) {
  if (typeof value !== 'string' || value.length > Math.ceil(maxBytes * 4 / 3) || !/^[A-Za-z0-9_-]+$/.test(value)) {
    throw new TypeError('El mensaje cifrado tiene un formato inválido.');
  }
  const bytes = Uint8Array.from(atob(value.replaceAll('-', '+').replaceAll('_', '/')), char => char.charCodeAt(0));
  if (encode(bytes) !== value) throw new TypeError('La codificación del mensaje no es válida.');
  return bytes;
}

export function generateChatEncryptionKey() {
  return crypto.subtle.generateKey({name: 'ECDH', namedCurve: 'P-256'}, false, ['deriveBits']);
}

export async function exportChatEncryptionKey(publicKey) {
  return encode(await suite.kem.serializePublicKey(publicKey));
}

export async function importChatEncryptionKey(value) {
  return suite.kem.deserializePublicKey(decode(value, 65));
}

// The caller provides keys already bound to verified device identities.
export async function encryptChatText(text, metadata, senderKey, recipientPublicKey) {
  if (typeof text !== 'string' || !senderKey?.privateKey || !recipientPublicKey) {
    throw new TypeError('Faltan el mensaje o las claves del chat.');
  }
  const plaintext = utf8.encode(text);
  if (!plaintext.length || plaintext.length > maxPlaintextBytes) {
    throw new RangeError('El mensaje supera el tamaño permitido o está vacío.');
  }
  const {enc, ct} = await suite.seal({senderKey, recipientPublicKey, info}, plaintext, authenticatedMetadata(metadata));
  return {version: 1, encapsulatedKey: encode(enc), ciphertext: encode(ct)};
}

export async function decryptChatText(envelope, metadata, recipientKey, senderPublicKey) {
  if (envelope?.version !== 1 || !recipientKey?.privateKey || !senderPublicKey) {
    throw new TypeError('Faltan el mensaje cifrado o las claves del chat.');
  }
  const enc = decode(envelope.encapsulatedKey, 65);
  const ct = decode(envelope.ciphertext, maxPlaintextBytes + 16);
  const bytes = await suite.open({recipientKey, senderPublicKey, enc, info}, ct, authenticatedMetadata(metadata));
  return new TextDecoder('utf-8', {fatal: true}).decode(bytes);
}
