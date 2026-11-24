import {decryptChatText, encryptChatText} from './chat-envelope.mjs';

export const maxEncryptedAttachmentBytes = 12 * 1024 * 1024;
const encode = bytes => btoa(String.fromCharCode(...new Uint8Array(bytes)));
const decode = value => Uint8Array.from(atob(value), char => char.charCodeAt(0));
const utf8 = new TextEncoder();

function attachmentContext(message, attachmentId) {
  if (typeof attachmentId !== 'string' || !/^[0-9a-f-]{36}$/i.test(attachmentId)) {
    throw new TypeError('El identificador del adjunto no es válido.');
  }
  const context = {...message, messageId: attachmentId};
  const aad = utf8.encode(JSON.stringify(['MAIN chat attachment v1',
    message.conversationId, message.messageId, message.senderDeviceId, message.recipientDeviceId, attachmentId]));
  return {context, aad};
}

export async function encryptChatAttachment(file, message, senderKey, recipientPublicKey) {
  if (!file || file.size < 1 || file.size > maxEncryptedAttachmentBytes ||
      typeof file.name !== 'string' || !file.name || file.name.length > 255 || file.type.length > 150) {
    throw new TypeError('El archivo tiene un nombre o tamaño no admitido.');
  }
  const attachmentId = crypto.randomUUID();
  const {context, aad} = attachmentContext(message, attachmentId);
  const rawKey = crypto.getRandomValues(new Uint8Array(32));
  try {
    const nonce = crypto.getRandomValues(new Uint8Array(12));
    const key = await crypto.subtle.importKey('raw', rawKey, 'AES-GCM', false, ['encrypt']);
    const wrapped = await encryptChatText(JSON.stringify({
      key: encode(rawKey), nonce: encode(nonce), name: file.name, type: file.type,
      size: file.size, messageId: message.messageId
    }), context, senderKey, recipientPublicKey);
    const ciphertext = await crypto.subtle.encrypt({name:'AES-GCM', iv:nonce, additionalData:aad},
      key, await file.arrayBuffer());
    return {attachmentId, wrapped, ciphertext: new Blob([ciphertext], {type:'application/octet-stream'})};
  } finally {
    rawKey.fill(0);
  }
}

export async function decryptChatAttachment({attachmentId, wrapped, ciphertext}, message, recipientKey, senderPublicKey) {
  if (!(ciphertext instanceof Blob) || ciphertext.size < 17 || ciphertext.size > maxEncryptedAttachmentBytes + 16) {
    throw new TypeError('El adjunto cifrado no tiene un tamaño válido.');
  }
  const {context, aad} = attachmentContext(message, attachmentId);
  const metadata = JSON.parse(await decryptChatText(wrapped, context, recipientKey, senderPublicKey));
  if (metadata.messageId !== message.messageId || metadata.size !== ciphertext.size - 16 ||
      typeof metadata.name !== 'string' || !metadata.name || metadata.name.length > 255 ||
      typeof metadata.type !== 'string' || metadata.type.length > 150 ||
      typeof metadata.key !== 'string' || metadata.key.length !== 44 ||
      typeof metadata.nonce !== 'string' || metadata.nonce.length !== 16) {
    throw new Error('El adjunto no corresponde a este mensaje.');
  }
  const rawKey = decode(metadata.key);
  try {
    if (rawKey.length !== 32) throw new Error('La clave del adjunto no es válida.');
    const key = await crypto.subtle.importKey('raw', rawKey, 'AES-GCM', false, ['decrypt']);
    const bytes = await crypto.subtle.decrypt({name:'AES-GCM', iv:decode(metadata.nonce), additionalData:aad},
      key, await ciphertext.arrayBuffer());
    return new File([bytes], metadata.name, {type:metadata.type});
  } finally {
    rawKey.fill(0);
  }
}
