import {useState} from 'react';
import {chatSafetyCodes} from './chat-device.mjs';

export function ChatSafetyCodes({username, conversationId, contactUsername}) {
  const [open, setOpen] = useState(false);
  const [codes, setCodes] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function toggle() {
    if (open) {
      setOpen(false);
      return;
    }
    setOpen(true);
    setLoading(true);
    setError('');
    setCodes([]);
    try {
      setCodes(await chatSafetyCodes(username, conversationId, contactUsername));
    } catch (failure) {
      setError(failure.message || 'No se pudieron obtener los códigos de comparación.');
    } finally {
      setLoading(false);
    }
  }

  return <div className="card-form">
    <button type="button" aria-expanded={open} disabled={loading} onClick={toggle}>
      {open ? 'Ocultar códigos' : 'Comparar dispositivos'}
    </button>
    {open && <div aria-live="polite">
      {loading ? <p>Preparando códigos…</p> : error ? <p role="alert">{error}</p> :
        codes.length === 0 ? <p>Tu contacto aún no ha registrado un dispositivo.</p> : <>
          <p>Compara el código con tu contacto en persona o por otro canal. Busquen el mismo código en ambos dispositivos.</p>
          <ul>{codes.map(({deviceId, code}) => <li key={deviceId}><code style={{overflowWrap:'anywhere'}}>{code}</code></li>)}</ul>
        </>}
    </div>}
  </div>;
}
