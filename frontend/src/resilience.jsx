import React, {Component, useEffect, useRef, useState} from 'react';

export class AppErrorBoundary extends Component {
  state = {failed: false};

  static getDerivedStateFromError() {
    return {failed: true};
  }

  componentDidCatch(error, info) {
    // Do not log form contents, responses, URLs or user identifiers.
    console.error('MAIN_RENDER_FAILED', {
      type: error?.name || 'Error',
      componentStack: info.componentStack,
    });
  }

  render() {
    if (!this.state.failed) return this.props.children;
    return <main className="fatal-error" role="alert">
      <span className="logo" aria-hidden="true">M</span>
      <h1>No pudimos mostrar esta sección</h1>
      <p>Tu cuenta y tus datos no se modificaron. Intenta abrir la sección nuevamente; si el problema continúa, recarga MAIN.</p>
      <div className="toolbar">
        <button onClick={() => this.setState({failed: false})}>Intentar de nuevo</button>
        <button onClick={() => location.reload()}>Recargar MAIN</button>
      </div>
    </main>;
  }
}

export function ConnectionStatus() {
  const [status, setStatus] = useState(() => navigator.onLine === false ? 'offline' : 'online');
  const wasOffline = useRef(status === 'offline');
  const timer = useRef(null);

  useEffect(() => {
    const clear = () => { if (timer.current !== null) clearTimeout(timer.current); timer.current = null; };
    const offline = () => { clear(); wasOffline.current = true; setStatus('offline'); };
    const online = () => {
      clear();
      if (!wasOffline.current) { setStatus('online'); return; }
      wasOffline.current = false;
      setStatus('recovered');
      timer.current = setTimeout(() => { timer.current = null; setStatus('online'); }, 5000);
    };
    window.addEventListener('offline', offline);
    window.addEventListener('online', online);
    return () => { clear(); window.removeEventListener('offline', offline); window.removeEventListener('online', online); };
  }, []);

  if (status === 'online') return null;
  return <div className={'connection-banner ' + status} role="status" aria-live="polite">
    {status === 'offline'
      ? 'Sin conexión. Conservaremos tus borradores; actualiza antes de repetir un envío.'
      : 'Conexión restablecida. Ya puedes actualizar la sección.'}
  </div>;
}
