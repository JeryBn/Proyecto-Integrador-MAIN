import {userApi} from '../datos/user-api.mjs';

const registrations = new Map();
const proofDomain = 'MAIN chat device identity v1';

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
    if (existing) return existing;

    const keys = await crypto.subtle.generateKey('Ed25519', false, ['sign', 'verify']);
    const candidate = {
      id: crypto.randomUUID(),
      privateKey: keys.privateKey,
      signingKey: base64url(await crypto.subtle.exportKey('raw', keys.publicKey))
    };
    return await accountDevice(database, username, candidate);
  } finally {
    database.close();
  }
}

async function register(username) {
  const {id, privateKey, signingKey} = await storedDevice(username);
  const payload = new TextEncoder().encode([proofDomain, username, id, signingKey].join('\n'));
  const proof = base64url(await crypto.subtle.sign('Ed25519', privateKey, payload));
  await userApi.call('/api/v1/chat-identity/devices/' + id, {
    method: 'PUT',
    json: {signingKey, proof}
  });
  return {username, deviceId: id, signingKey};
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
  const serialize = ({username, deviceId, signingKey}) => JSON.stringify([username, deviceId, signingKey]);
  const values = [serialize(first), serialize(second)].sort();
  const payload = new TextEncoder().encode(JSON.stringify(['MAIN device comparison v1', ...values]));
  const digest = await crypto.subtle.digest('SHA-256', payload);
  return Array.from(new Uint8Array(digest), byte => byte.toString(16).padStart(2, '0'))
    .join('').toUpperCase().match(/.{4}/g).join(' ');
}

export async function chatSafetyCodes(username, conversationId, contactUsername, signal) {
  const current = await registerChatDevice(username);
  if (!current) throw new Error('Este navegador no puede verificar dispositivos.');
  const devices = await userApi.call('/api/v1/chat-identity/conversations/' +
    encodeURIComponent(conversationId) + '/devices', {signal});
  return Promise.all(devices.filter(device => device.username === contactUsername).map(async device => ({
    deviceId: device.deviceId,
    code: await deviceSafetyCode(current, device)
  })));
}
