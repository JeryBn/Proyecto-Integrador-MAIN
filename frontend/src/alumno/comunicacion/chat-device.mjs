import {userApi} from '../datos/user-api.mjs';

import {exportChatEncryptionKey, generateChatEncryptionKey, importChatEncryptionKey} from './chat-envelope.mjs';

const registrations = new Map();
const proofDomain = 'MAIN chat encryption key v1';

function base64url(bytes) {
  return btoa(String.fromCharCode(...new Uint8Array(bytes)))
    .replaceAll('+', '-').replaceAll('/', '_').replace(/=+$/, '');
}

function openIdentityStore() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open('main-chat-identity', 1);
    request.onupgradeneeded = () => request.result.createObjectStore('devices');
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

function accountDevice(database, username, candidate) {
  return new Promise((resolve, reject) => {
    const transaction = database.transaction('devices', candidate ? 'readwrite' : 'readonly');
    const store = transaction.objectStore('devices');
    const request = store.get(username);
    let device;
    request.onsuccess = () => {
      device = request.result;
      if (!device && candidate) {
        device = candidate;
        store.add(device, username);
      } else if (device && candidate && !device.encryptionKeys) {
        device = {...device, encryptionKeys: candidate.encryptionKeys};
        store.put(device, username);
      }
    };
    transaction.oncomplete = () => resolve(device);
    transaction.onabort = () => reject(transaction.error);
  });
}

async function storedDevice(username) {
  const database = await openIdentityStore();
  try {
    const existing = await accountDevice(database, username);
    if (existing?.encryptionKeys) return existing;

    const encryptionKeys = await generateChatEncryptionKey();
    if (existing) return await accountDevice(database, username, {...existing, encryptionKeys});
    const keys = await crypto.subtle.generateKey('Ed25519', false, ['sign', 'verify']);
    const candidate = {
      id: crypto.randomUUID(),
      privateKey: keys.privateKey,
      encryptionKeys,
      signingKey: base64url(await crypto.subtle.exportKey('raw', keys.publicKey))
    };
    return await accountDevice(database, username, candidate);
  } finally {
    database.close();
  }
}

async function register(username) {
  const {id, privateKey, signingKey, encryptionKeys} = await storedDevice(username);
  const encryptionKey = await exportChatEncryptionKey(encryptionKeys.publicKey);
  const payload = new TextEncoder().encode([proofDomain, username, id, signingKey, encryptionKey].join('\n'));
  const proof = base64url(await crypto.subtle.sign('Ed25519', privateKey, payload));
  await userApi.call('/api/v1/chat-identity/devices/' + id, {
    method: 'PUT',
    json: {signingKey, proof, encryptionKey}
  });
  return {username, deviceId: id, signingKey, encryptionKey};
}

export function registerChatDevice(username) {
  if (!username || !globalThis.crypto?.subtle || !globalThis.indexedDB) return Promise.resolve();
  if (!registrations.has(username)) {
    const pending = register(username).catch(error => {
      registrations.delete(username);
      throw error;
    });
    registrations.set(username, pending);
  }
  return registrations.get(username);
}

export async function deviceSafetyCode(first, second) {
  const serialize = ({username, deviceId, signingKey, encryptionKey}) => JSON.stringify([username, deviceId, signingKey, encryptionKey]);
  const values = [serialize(first), serialize(second)].sort();
  const payload = new TextEncoder().encode(JSON.stringify(['MAIN device comparison v2', ...values]));
  const digest = await crypto.subtle.digest('SHA-256', payload);
  return Array.from(new Uint8Array(digest), byte => byte.toString(16).padStart(2, '0'))
    .join('').toUpperCase().match(/.{4}/g).join(' ');
}

export async function chatSafetyCodes(username, conversationId, contactUsername, signal) {
  const current = await registerChatDevice(username);
  if (!current) throw new Error('Este navegador no puede verificar dispositivos.');
  const devices = await userApi.call('/api/v1/chat-identity/conversations/' +
    encodeURIComponent(conversationId) + '/devices', {signal});
  const peers = devices.filter(device => device.username === contactUsername && device.encryptionKey);
  return Promise.all(peers.map(async device => {
    await verifyChatEncryptionIdentity(device);
    return {deviceId: device.deviceId, code: await deviceSafetyCode(current, device)};
  }));
}

export async function verifyChatEncryptionIdentity(device) {
  const {username, deviceId, signingKey, encryptionKey, encryptionProof} = device;
  const decode = value => Uint8Array.from(atob(value.replaceAll('-', '+').replaceAll('_', '/')), char => char.charCodeAt(0));
  const verifier = await crypto.subtle.importKey('raw', decode(signingKey), 'Ed25519', false, ['verify']);
  const payload = new TextEncoder().encode([proofDomain, username, deviceId, signingKey, encryptionKey].join('\n'));
  if (!await crypto.subtle.verify('Ed25519', verifier, decode(encryptionProof), payload)) {
    throw new Error('No se pudo verificar la clave de cifrado de tu contacto.');
  }
  return importChatEncryptionKey(encryptionKey);
}