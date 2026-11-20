export function withRequestReference(response, message) {
  const id = response.headers?.get?.('X-Request-ID');
  return response.status >= 500 && typeof id === 'string' &&
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id)
    ? `${message} Referencia: ${id}`
    : message;
}
