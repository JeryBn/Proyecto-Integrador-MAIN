import {createLocalChatModerator} from './local-chat-moderation.mjs';
import {userApi} from '../datos/user-api.mjs';
import {decryptChatText, encryptChatText} from './chat-envelope.mjs';
import {deviceSafetyCode, registerChatDevice, storedChatDevice, verifyChatEncryptionIdentity} from './chat-device.mjs';

const route = conversationId => '/api/v1/conversations/' + encodeURIComponent(conversationId) + '/encrypted';

async function chatContext(username, conversationId, signal) {
  const self = await registerChatDevice(username);
  if (!self) throw new Error('El dispositivo no admite chat cifrado.');
  const local = await storedChatDevice(username);
  if (local.id !== self.deviceId) throw new Error('La identidad local cambió. Actualiza la conversación.');
  const directory = await userApi.call('/api/v1/chat-identity/conversations/' +
    encodeURIComponent(conversationId) + '/devices', {signal});
  return {self, keyPair: local.encryptionKeys, directory};
}

async function checkedPeer(context, username, deviceId, verifiedCodes) {
  const peer = context.directory.find(device => device.username === username && device.deviceId === deviceId);
  if (!peer || !peer.encryptionKey || !verifiedCodes.has(await deviceSafetyCode(context.self, peer))) {
    throw new Error('Compara el código de este dispositivo con tu contacto antes de continuar.');
  }
  return verifyChatEncryptionIdentity(peer);
}

export async function prepareEncryptedChatText({
  username, conversationId, contactUsername, recipientDeviceId, text, verifiedCodes, moderateLocally, signal
}) {
  if (typeof moderateLocally !== 'function') {
    throw new Error('La moderación local del chat todavía no está disponible.');
  }
  const context = await chatContext(username, conversationId, signal);
  const publicKey = await checkedPeer(context, contactUsername, recipientDeviceId, verifiedCodes);
  const decision = await moderateLocally(text, {signal});
  if (decision !== 'APPROVE') {
    throw new Error(decision === 'EDIT' ? 'Edita el mensaje antes de enviarlo.' : 'El mensaje no se puede enviar.');
  }
  signal?.throwIfAborted();
  const messageId = crypto.randomUUID();
  const senderDeviceId = context.self.deviceId;
  const envelope = await encryptChatText(text,
    {conversationId, messageId, senderDeviceId, recipientDeviceId}, context.keyPair, publicKey);
  return {messageId, senderDeviceId, recipientDeviceId, ...envelope};
}

export function sendEncryptedChatEnvelope(conversationId, envelope, signal) {
  // Retain the prepared envelope if a request is retried.
  const {messageId, senderDeviceId, recipientDeviceId, version, encapsulatedKey, ciphertext} = envelope;
  const json = {messageId, senderDeviceId, recipientDeviceId, version, encapsulatedKey, ciphertext};
  return userApi.call(route(conversationId), {method: 'POST', json, signal});
}

export async function readEncryptedChatTexts({username, conversationId, after = 0, verifiedCodes, signal}) {
  const context = await chatContext(username, conversationId, signal);
  const recipientDeviceId = context.self.deviceId;
  const path = route(conversationId) + '/devices/' + encodeURIComponent(recipientDeviceId) + '?after=' + after;
  const envelopes = await userApi.call(path, {signal});
  return Promise.all(envelopes.map(async envelope => {
    const {cursor, messageId, sender, senderDeviceId} = envelope;
    const publicKey = await checkedPeer(context, sender, senderDeviceId, verifiedCodes);
    const text = await decryptChatText(envelope,
      {conversationId, messageId, senderDeviceId, recipientDeviceId}, context.keyPair, publicKey);
    return {cursor, messageId, sender, text};
  }));
}


export async function openEncryptedChatSession({username, conversationId, contactUsername, verifiedCodes, signal, onDownload}) {
  const moderator = await createLocalChatModerator({signal, onDownload});
  return {
    prepare: (text, recipientDeviceId, requestSignal) => prepareEncryptedChatText({
      username, conversationId, contactUsername, recipientDeviceId, text, verifiedCodes,
      moderateLocally: moderator.moderate, signal: requestSignal
    }),
    send: (envelope, requestSignal) => sendEncryptedChatEnvelope(conversationId, envelope, requestSignal),
    read: (after, requestSignal) => readEncryptedChatTexts({
      username, conversationId, after, verifiedCodes, signal: requestSignal
    }),
    close: moderator.destroy
  };
}