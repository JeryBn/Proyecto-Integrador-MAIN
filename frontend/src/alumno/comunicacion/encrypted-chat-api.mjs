import {createLocalChatModerator} from './local-chat-moderation.mjs';
import {userApi} from '../datos/user-api.mjs';
import {decryptChatText, encryptChatText} from './chat-envelope.mjs';
import {deviceSafetyCode, registerChatDevice, storedChatDevice, verifyChatEncryptionIdentity} from './chat-device.mjs';

const route = conversationId => '/api/v1/conversations/' + encodeURIComponent(conversationId) + '/encrypted';

async function chatContext(username, conversationId, signal, group = false) {
  const self = await registerChatDevice(username);
  if (!self) throw new Error('El dispositivo no admite chat cifrado.');
  const local = await storedChatDevice(username);
  if (local.id !== self.deviceId) throw new Error('La identidad local cambió. Actualiza la conversación.');
  if (group) {
    const roster = await userApi.call(route(conversationId) + '/group-recipients', {signal});
    return {self, keyPair: local.encryptionKeys, directory: roster.devices, revision: roster.revision};
  }
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

export async function readEncryptedChatTexts({username, conversationId, after = 0, verifiedCodes, signal, group = false}) {
  const context = await chatContext(username, conversationId, signal, group);
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


export async function groupChatSafetyCodes(username, conversationId, signal) {
  const context = await chatContext(username, conversationId, signal, true);
  return Promise.all(context.directory.map(async peer => {
    await verifyChatEncryptionIdentity(peer);
    return {deviceId:peer.deviceId, username:peer.username, code:await deviceSafetyCode(context.self, peer)};
  }));
}

async function prepareGroup({username, conversationId, text, verifiedCodes, moderateLocally, signal}) {
  const context = await chatContext(username, conversationId, signal, true);
  const recipients = await Promise.all(context.directory.map(async peer => ({
    peer, key: await checkedPeer(context, peer.username, peer.deviceId, verifiedCodes)
  })));
  const decision = await moderateLocally(text, {signal});
  if (decision !== 'APPROVE') {
    throw new Error(decision === 'EDIT' ? 'Edita el mensaje antes de enviarlo.' : 'El mensaje no se puede enviar.');
  }
  signal?.throwIfAborted();
  const messageId = crypto.randomUUID(), senderDeviceId = context.self.deviceId;
  const envelopes = await Promise.all(recipients.map(async ({peer, key}) => {
    const {encapsulatedKey, ciphertext} = await encryptChatText(text, {
      conversationId, messageId, senderDeviceId, recipientDeviceId:peer.deviceId
    }, context.keyPair, key);
    return {username:peer.username, deviceId:peer.deviceId, encapsulatedKey, ciphertext};
  }));
  return {messageId, senderDeviceId, revision:context.revision, envelopes};
}

export async function openEncryptedChatSession({username, conversationId, contactUsername, verifiedCodes, signal, onDownload, group = false}) {
  const moderator = await createLocalChatModerator({signal, onDownload});
  return {
    prepare: (text, recipientDeviceId, requestSignal) => (group ? prepareGroup : prepareEncryptedChatText)({
      username, conversationId, contactUsername, recipientDeviceId, text, verifiedCodes,
      moderateLocally: moderator.moderate, signal: requestSignal
    }),
    send: (envelope, requestSignal) => group
      ? userApi.call(route(conversationId) + '/group', {method:'POST', json:envelope, signal:requestSignal})
      : sendEncryptedChatEnvelope(conversationId, envelope, requestSignal),
    read: (after, requestSignal) => readEncryptedChatTexts({
      username, conversationId, after, verifiedCodes, signal: requestSignal, group
    }),
    close: moderator.destroy
  };
}