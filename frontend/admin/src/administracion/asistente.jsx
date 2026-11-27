import {useRef, useState} from 'react';
import {createAdminApi} from '../admin-api.mjs';

const assistant = createAdminApi({timeout: 45000});

export function SchoolAssistant({onExpire}) {
  const [question, setQuestion] = useState('');
  const [reply, setReply] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const inFlight = useRef(false);

  async function submit(event) {
    event.preventDefault();
    if (inFlight.current || !question.trim()) return;
    inFlight.current = true;
    setBusy(true);
    setError('');
    setReply(null);
    try {
      await assistant.loadCsrf();
      setReply(await assistant.api('/v1/assistant', {method: 'POST', json: {question: question.trim()}}));
    } catch (failure) {
      if (failure.status === 401) onExpire();
      else setError(failure.message);
    } finally {
      inFlight.current = false;
      setBusy(false);
    }
  }

  return (
    <section className="card" aria-labelledby="school-assistant-title">
      <h2 id="school-assistant-title">Asistente COLEGIA</h2>
      <p>Haz consultas de gestión escolar o prepara un borrador de comunicado. Evita incluir datos personales.</p>
      <form onSubmit={submit}>
        <label>Consulta de gestión escolar
          <textarea required rows={5} maxLength={2000} value={question} disabled={busy}
            onChange={(event) => setQuestion(event.target.value)} />
        </label>
        <button type="submit" disabled={busy || !question.trim()}>Consultar</button>
      </form>
      {busy && <p role="status">Preparando la respuesta…</p>}
      {error && <p role="alert" className="err">{error}</p>}
      {reply && <article aria-live="polite">
        <h3>Respuesta</h3>
        <p style={{whiteSpace: 'pre-wrap'}}>{reply.answer}</p>
        <small>{reply.model}</small>
      </article>}
    </section>
  );
}
