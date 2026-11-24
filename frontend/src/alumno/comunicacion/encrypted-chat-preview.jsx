import {useEffect, useRef, useState} from 'react';
import {chatSafetyCodes} from './chat-device.mjs';
import {groupChatSafetyCodes, openEncryptedChatSession} from './encrypted-chat-api.mjs';

export function EncryptedChatPreview({username, conversationId, contactUsername, group = false}) {
  const session = useRef(null);
  const controller = useRef(null);
  const pending = useRef(null);
  const verified = useRef(new Set());
  const [devices, setDevices] = useState([]);
  const [confirmation, setConfirmation] = useState('');
  const [selection, setSelection] = useState('');
  const [text, setText] = useState('');
  const [messages, setMessages] = useState([]);
  const [cursor, setCursor] = useState(0);
  const [working, setWorking] = useState(false);
  const [ready, setReady] = useState(false);
  const [notice, setNotice] = useState('');
  useEffect(() => () => {
    controller.current?.abort();
    session.current?.close();
  }, []);

  const deviceOption = device => JSON.stringify([device.username ?? contactUsername, device.deviceId]);
  const selected = devices.find(device => deviceOption(device) === selection);
  const normalized = value => value.replace(/\s/g, '').toUpperCase();

  async function run(action) {
    if (controller.current) return;
    const task = new AbortController();
    controller.current = task;
    setWorking(true);
    setNotice('');
    try {
      await action(task.signal);
    } catch (error) {
      if (!task.signal.aborted) setNotice(error.message);
    } finally {
      controller.current = null;
      if (!task.signal.aborted) setWorking(false);
    }
  }

  async function start() {
    await run(async signal => {
      const opened = await openEncryptedChatSession({username, conversationId, contactUsername,
        verifiedCodes: verified.current, signal, group, onDownload: value => {
          if (!signal.aborted) setNotice('Descargando la IA local… ' + Math.round(value * 100) + '%');
        }});
      if (signal.aborted) {
        opened.close();
        return;
      }
      session.current = opened;
      setReady(true);
      setNotice('');
    });
  }

  async function send(event) {
    event.preventDefault();
    await run(async signal => {
      const envelope = pending.current ?? await session.current.prepare(text, selected.deviceId, signal);
      pending.current = envelope;
      try {
        await session.current.send(envelope, signal);
      } catch (error) {
        if (error.code === 'GROUP_DEVICES_CHANGED') pending.current = null;
        throw error;
      }
      pending.current = null;
      setText('');
      setMessages(previous => [...previous, {messageId: envelope.messageId, sender: username, text}]);
    });
  }

  return <section className="card-form" aria-label="Prueba de chat cifrado">
    <h3>Chat cifrado de prueba</h3>
    <p>Solo texto. Comparen los códigos por otro canal antes de enviar.</p>
    <button disabled={working || !!pending.current} onClick={() => run(async signal => {
      const result = group ? await groupChatSafetyCodes(username, conversationId, signal)
        : await chatSafetyCodes(username, conversationId, contactUsername, signal);
      setDevices(result);
      setSelection(result.length ? deviceOption(result[0]) : '');
      if (!result.length) setNotice('Tu contacto debe abrir esta conversación desde su dispositivo.');
    })}>Cargar códigos</button>
    {devices.length > 0 && <><label>Dispositivo del contacto
      <select value={selection} disabled={working || !!pending.current} onChange={event => {
        setSelection(event.target.value);
        setConfirmation('');
      }}>{devices.map(device => <option key={deviceOption(device)} value={deviceOption(device)}>{device.username ? device.username + " · " : ""}{device.code.slice(0, 9)}</option>)}</select>
    </label>
    {selected && <p><code style={{overflowWrap:'anywhere'}}>{selected.code}</code></p>}
    {selected && !verified.current.has(selected.code) && <><label>Código recibido por otro canal
      <input value={confirmation} onChange={event => setConfirmation(event.target.value)}/>
    </label><button disabled={working || normalized(confirmation) !== normalized(selected.code)}
      onClick={() => {
        verified.current.add(selected.code);
        setConfirmation('');
        setNotice('Código confirmado para esta sesión.');
      }}>Confirmar código</button></>}</>}
    {!ready && <button disabled={working} onClick={start}>Activar IA local</button>}
    {ready && selected && verified.current.has(selected.code) && <>
      <form onSubmit={send}>
        <label>Mensaje cifrado<textarea value={text} maxLength={4000} disabled={working || !!pending.current}
          onChange={event => setText(event.target.value)}/></label>
        <button disabled={working || !text.trim() || (group && devices.some(device => !verified.current.has(device.code)))}>{pending.current ? 'Reintentar envío' : 'Enviar cifrado'}</button>
      </form>
      <button disabled={working || !!pending.current} onClick={() => run(async signal => {
        const incoming = await session.current.read(cursor, signal);
        if (incoming.length) {
          setMessages(previous => [...previous, ...incoming]);
          setCursor(incoming.at(-1).cursor);
        } else {
          setNotice('No hay mensajes nuevos.');
        }
      })}>Recibir mensajes cifrados</button>
    </>}
    {notice && <p role="status">{notice}</p>}
    <ul className="message-list" aria-label="Mensajes cifrados">{messages.map(message =>
      <li className="message-bubble" key={message.sender + ':' + message.messageId}><strong>{message.sender}</strong>
        <p className="message-text">{message.text}</p></li>)}</ul>
  </section>;
}
