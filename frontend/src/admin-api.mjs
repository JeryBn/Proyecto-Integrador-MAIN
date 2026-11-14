import {boundedRead} from './bounded-read.mjs';

export class ApiError extends Error {
  constructor(status, code, message, fields) {
    super(message);
    this.status = status;
    this.code = code;
    this.fields = fields && typeof fields === 'object' ? fields : {};
  }
}

const readMessage = timedOut => timedOut
  ? 'El servidor tarda en responder. Puedes volver a intentar.'
  : 'No hay conexión con el servidor. Puedes volver a intentar.';
const writeMessage = 'No se confirmó el resultado. Conservamos tus datos; actualiza la sección antes de repetir la operación.';
const maxResponseBytes = 1024 * 1024;

async function responseText(response) {
  const declaredLength = Number(response.headers?.get?.('content-length'));
  if (Number.isFinite(declaredLength) && declaredLength > maxResponseBytes) {
    throw new ApiError(response.status, 'INVALID_RESPONSE', 'La respuesta del servidor supera el tamaño permitido.');
  }

  if (!response.body?.getReader) {
    const text = await response.text();
    if (new TextEncoder().encode(text).byteLength > maxResponseBytes) {
      throw new ApiError(response.status, 'INVALID_RESPONSE', 'La respuesta del servidor supera el tamaño permitido.');
    }
    return text;
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let bytes = 0;
  let text = '';
  try {
    while (true) {
      const {done, value} = await reader.read();
      if (done) break;
      bytes += value.byteLength;
      if (bytes > maxResponseBytes) {
        await reader.cancel();
        throw new ApiError(response.status, 'INVALID_RESPONSE', 'La respuesta del servidor supera el tamaño permitido.');
      }
      text += decoder.decode(value, {stream: true});
    }
    return text + decoder.decode();
  } finally {
    reader.releaseLock?.();
  }
}

async function responseData(response) {
  let text;
  try { text = await responseText(response); }
  catch (error) {
    if (error instanceof ApiError) throw error;
    throw new ApiError(0, 'INVALID_RESPONSE', 'No se pudo leer la respuesta del servidor.');
  }
  if (!text) return null;
  try { return JSON.parse(text); }
  catch {
    if (response.ok) throw new ApiError(response.status, 'INVALID_RESPONSE', 'El servidor respondió con un formato inesperado.');
    return null;
  }
}

export function createAdminApi({
  fetcher = (...args) => fetch(...args),
  timeout = 30000,
  schedule,
  cancel,
} = {}) {
  let csrf = null;

  async function transport(path, options, write = false) {
    const deadline = {timeout};
    if (schedule) deadline.schedule = schedule;
    if (cancel) deadline.cancel = cancel;
    try {
      return await boundedRead(async signal => {
        const response = await fetcher(path, {...options, signal});
        return {response, data: await responseData(response)};
      }, deadline);
    } catch (error) {
      if (error instanceof ApiError) {
        if (write && error.code === 'INVALID_RESPONSE') {
          throw new ApiError(error.status, error.code, writeMessage);
        }
        throw error;
      }
      const timedOut = error?.name === 'TimeoutError';
      throw new ApiError(0, timedOut ? 'TIMEOUT' : 'NETWORK', write ? writeMessage : readMessage(timedOut));
    }
  }

  async function loadCsrf() {
    const {response, data} = await transport('/api/admin/csrf', {
      credentials: 'same-origin',
      cache: 'no-store',
      headers: {Accept: 'application/json'},
    });
    if (!response.ok) {
      const value = data?.error || {};
      throw new ApiError(response.status, value.code || 'SECURITY_TOKEN_ERROR', value.message || 'No se pudo renovar el token de seguridad.');
    }
    if (!data || typeof data.headerName !== 'string' || !/^[A-Za-z0-9-]{1,100}$/.test(data.headerName)
        || typeof data.token !== 'string' || !data.token || data.token.length > 4096) {
      throw new ApiError(response.status, 'INVALID_RESPONSE', 'El servidor respondió con un token de seguridad inválido.');
    }
    csrf = data;
    return data;
  }

  async function api(path, {method = 'GET', json, form} = {}) {
    const headers = {Accept: 'application/json'};
    let body;
    const write = method !== 'GET';
    if (write) {
      if (!csrf) await loadCsrf();
      headers[csrf.headerName] = csrf.token;
      if (json !== undefined) {
        headers['Content-Type'] = 'application/json';
        body = JSON.stringify(json);
      } else if (form) {
        body = new URLSearchParams(form);
      }
    }
    const {response, data} = await transport('/api/admin' + path, {
      method,
      headers,
      body,
      credentials: 'same-origin',
      cache: 'no-store',
    }, write);
    if (!response.ok) {
      const value = data?.error || {};
      throw new ApiError(response.status, value.code || 'ERROR', value.message || 'No se pudo completar la operación.', value.fields || data?.fields);
    }
    return data;
  }

  return {api, loadCsrf};
}

export const {api, loadCsrf} = createAdminApi();
