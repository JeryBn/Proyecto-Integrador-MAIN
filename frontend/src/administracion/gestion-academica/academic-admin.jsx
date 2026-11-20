import {EnrollmentImport} from './enrollment-import.jsx';
import {SchoolStructure} from './school-structure.jsx';
import { useEffect, useState } from 'react';

const TAMANO_PAGINA = 20;
const FORMULARIO_VACIO = { id: '', name: '', teacherUsername: '', expectedVersion: null };

const etiquetasDeAuditoria = {
  ACCOUNT_ENABLED: 'Cuenta habilitada',
  ACTIVITY_CREATED: 'Actividad creada',
  ACTIVITY_UPDATED: 'Actividad editada',
  ACCOUNT_CREATED: 'Cuenta creada',
  ACCOUNT_DISABLED: 'Cuenta deshabilitada',
  PASSWORD_RESET: 'Contraseña restablecida',
  GUARDIAN_VERIFIED: 'Parentesco verificado',
  GUARDIAN_REVOKED: 'Parentesco revocado',
  CONTACT_APPROVED: 'Contacto autorizado',
  COURSE_CREATED: 'Curso creado',
  COURSE_UPDATED: 'Curso editado',
  ENROLLMENT_GRANTED: 'Matrícula registrada',
  ENROLLMENT_REVOKED: 'Matrícula revocada',
  TIMETABLE_CREATED: 'Horario registrado',
  TIMETABLE_REMOVED: 'Horario retirado',
  GRADE_CREATED: 'Nota registrada',
  GRADE_UPDATED: 'Nota editada',
  GROUP_CREATED: 'Grupo creado',
  GROUP_INVITED: 'Invitación a grupo',
  GROUP_INVITATION_ACCEPTED: 'Invitación aceptada',
  GROUP_INVITATION_REJECTED: 'Invitación rechazada',
  GROUP_MEMBER_REMOVED: 'Miembro retirado',
  GROUP_MEMBER_ROLE_CHANGED: 'Rol de miembro editado',
  MESSAGE_APPROVED: 'Mensaje aprobado',
  MESSAGE_REJECTED: 'Mensaje rechazado',
  STATUS_APPROVED: 'Estado aprobado',
  STATUS_REJECTED: 'Estado rechazado',
};

import { useConfirmation } from '../../confirmation.jsx';

export function AcademicAdmin({ api, onExpire }) {
  const { confirm, confirmation } = useConfirmation();
  const [cursos, setCursos] = useState([]);
  const [auditoria, setAuditoria] = useState([]);
  const [estudiantes, setEstudiantes] = useState([]);
  const [inicioCursos, setInicioCursos] = useState(0);
  const [inicioAuditoria, setInicioAuditoria] = useState(0);
  const [inicioEstudiantes, setInicioEstudiantes] = useState(0);
  const [cursoSeleccionado, setCursoSeleccionado] = useState('');
  const [revision, setRevision] = useState(0);
  const [error, setError] = useState('');
  const [aviso, setAviso] = useState('');
  const [guardando, setGuardando] = useState(false);
  const [cargando, setCargando] = useState(true);
  const [usuarioEstudiante, setUsuarioEstudiante] = useState('');
  const [formulario, setFormulario] = useState(FORMULARIO_VACIO);
  const [editando, setEditando] = useState(false);

  function mostrarError(errorDeApi) {
    if (errorDeApi.status === 401 || errorDeApi.status === 403) {
      onExpire();
      return;
    }

    setError(errorDeApi.message);
  }

  useEffect(() => {
    let pantallaActiva = true;

    async function cargarCursosYAuditoria() {
      setCargando(true);

      try {
        const [listaCursos, historial] = await Promise.all([
          api(`/v1/courses?offset=${inicioCursos}&limit=${TAMANO_PAGINA}`),
          api(`/v1/audit?offset=${inicioAuditoria}&limit=${TAMANO_PAGINA}`),
        ]);

        if (pantallaActiva) {
          setCursos(listaCursos);
          setAuditoria(historial);
        }
      } catch (errorDeApi) {
        if (pantallaActiva) {
          mostrarError(errorDeApi);
        }
      } finally {
        if (pantallaActiva) {
          setCargando(false);
        }
      }
    }

    cargarCursosYAuditoria();
    return () => {
      pantallaActiva = false;
    };
  }, [inicioCursos, inicioAuditoria, revision]);

  useEffect(() => {
    let pantallaActiva = true;
    setEstudiantes([]);

    async function cargarEstudiantes() {
      if (!cursoSeleccionado) {
        return;
      }

      try {
        const listaEstudiantes = await api(
          `/v1/courses/${cursoSeleccionado}/students?offset=${inicioEstudiantes}&limit=${TAMANO_PAGINA}`,
        );
        if (pantallaActiva) {
          setEstudiantes(listaEstudiantes);
        }
      } catch (errorDeApi) {
        if (pantallaActiva) {
          mostrarError(errorDeApi);
        }
      }
    }

    cargarEstudiantes();
    return () => {
      pantallaActiva = false;
    };
  }, [cursoSeleccionado, inicioEstudiantes, revision]);

  function actualizarPantalla() {
    setRevision((revisionActual) => revisionActual + 1);
  }

  async function guardar(accion, mensaje, alCompletar) {
    setGuardando(true);
    setError('');
    setAviso('');

    try {
      await accion();
      setAviso(mensaje);
      if (alCompletar) {
        alCompletar();
      }
      actualizarPantalla();
    } catch (errorDeApi) {
      mostrarError(errorDeApi);
    } finally {
      setGuardando(false);
    }
  }

  function prepararEdicion(curso) {
    setEditando(true);
    setFormulario({
      id: String(curso.id),
      name: curso.name,
      teacherUsername: '',
      expectedVersion: curso.version,
    });
    setAviso('Indica el usuario docente para confirmar la asignación.');
  }

  function prepararCursoNuevo() {
    setEditando(false);
    setFormulario(FORMULARIO_VACIO);
  }

  function seleccionarCurso(curso) {
    setCursoSeleccionado(String(curso.id));
    setInicioEstudiantes(0);
  }

  function guardarCurso(evento) {
    evento.preventDefault();

    const id = Number(formulario.id);
    if (!Number.isSafeInteger(id) || id < 1) {
      setError('Usa un ID entero positivo.');
      return;
    }

    let ruta = '/v1/courses';
    let metodo = 'POST';
    if (editando) {
      ruta = `/v1/courses/${id}`;
      metodo = 'PUT';
    }

    guardar(
      () => api(ruta, { method: metodo, json: { ...formulario, id } }),
      'Curso guardado.',
      prepararCursoNuevo,
    );
  }

  function matricularEstudiante(evento) {
    evento.preventDefault();

    guardar(
      () =>
        api(`/v1/courses/${cursoSeleccionado}/students`, {
          method: 'PUT',
          json: { username: usuarioEstudiante, enrolled: true },
        }),
      'Estudiante matriculado.',
      () => setUsuarioEstudiante(''),
    );
  }

  function revocarMatricula(estudiante) {
    const mensaje = `¿Revocar la matrícula de ${estudiante.name} en el curso ${cursoSeleccionado}?`;
    confirm(mensaje, () => guardar(
      () =>
        api(`/v1/courses/${cursoSeleccionado}/students`, {
          method: 'PUT',
          json: { username: estudiante.username, enrolled: false },
        }),
      'Matrícula revocada.',
    ));
  }

  return (
    <section aria-label="Administración académica">
      {confirmation}
      <SchoolStructure api={api} onExpire={onExpire} courses={cursos}/>
      <h2>Cursos y matrículas</h2>
      {error && (
        <p role="alert" className="err">
          {error}
        </p>
      )}
      {aviso && (
        <p role="status" className="ok">
          {aviso}
        </p>
      )}
      <button disabled={guardando} onClick={actualizarPantalla}>
        Actualizar cursos y auditoría
      </button>

      {cargando ? (
        <p role="status">Cargando…</p>
      ) : (
        <div className="scroll">
          <table>
            <caption>Cursos registrados</caption>
            <thead>
              <tr>
                <th>ID</th>
                <th>Curso</th>
                <th>Docente</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {cursos.map((curso) => (
                <tr key={curso.id}>
                  <td>{curso.id}</td>
                  <td>{curso.name}</td>
                  <td>{curso.teacher}</td>
                  <td>
                    <button disabled={guardando} onClick={() => prepararEdicion(curso)}>
                      Editar {curso.name}
                    </button>
                    <button disabled={guardando} onClick={() => seleccionarCurso(curso)}>
                      Matrículas de {curso.name}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {!cursos.length && <p>No hay cursos en esta página.</p>}
        </div>
      )}

      <div className="pager">
        <button
          disabled={guardando || inicioCursos === 0}
          onClick={() => setInicioCursos(inicioCursos - TAMANO_PAGINA)}
        >
          Cursos anteriores
        </button>
        <span>Página {inicioCursos / TAMANO_PAGINA + 1}</span>
        <button
          disabled={guardando || cursos.length < TAMANO_PAGINA}
          onClick={() => setInicioCursos(inicioCursos + TAMANO_PAGINA)}
        >
          Cursos siguientes
        </button>
      </div>

      <fieldset className="card" disabled={guardando}>
        <legend>{editando ? 'Editar curso' : 'Crear curso'}</legend>
        <form onSubmit={guardarCurso}>
          <label>
            ID del curso
            <input
              required
              inputMode="numeric"
              readOnly={editando}
              value={formulario.id}
              onChange={(evento) => setFormulario({ ...formulario, id: evento.target.value })}
            />
          </label>
          <label>
            Nombre del curso
            <input
              required
              maxLength="100"
              value={formulario.name}
              onChange={(evento) => setFormulario({ ...formulario, name: evento.target.value })}
            />
          </label>
          <label>
            Usuario del docente responsable
            <input
              required
              value={formulario.teacherUsername}
              onChange={(evento) =>
                setFormulario({ ...formulario, teacherUsername: evento.target.value })
              }
            />
          </label>
          <p>
            Solo cuentas docentes activas. Si el curso cambió en otra sesión, actualiza y vuelve a
            seleccionarlo. Para reasignar un curso con horario, ajusta antes sus clases.
          </p>
          <button type="submit">{guardando ? 'Guardando…' : 'Guardar curso'}</button>
          <button type="button" onClick={prepararCursoNuevo}>
            Nuevo curso
          </button>
        </form>
      </fieldset>

      {cursoSeleccionado && (
        <section className="card">
          <h3>Matrículas del curso {cursoSeleccionado}</h3>
          <EnrollmentImport key={cursoSeleccionado} api={api} courseId={cursoSeleccionado} onExpire={onExpire}
            onSaved={()=>setRevision(value=>value+1)}/>
          <p>
            Revocar una matrícula retira acceso a actividades y notas del curso; conserva el
            historial académico. No elimina contactos ni conversaciones independientes.
          </p>
          <ul>
            {estudiantes.map((estudiante) => (
              <li key={estudiante.studentId}>
                {estudiante.name} ({estudiante.username}){' '}
                <button disabled={guardando} onClick={() => revocarMatricula(estudiante)}>
                  Revocar matrícula de {estudiante.name}
                </button>
              </li>
            ))}
          </ul>
          {!estudiantes.length && <p>No hay estudiantes en esta página.</p>}
          <div className="pager">
            <button
              disabled={guardando || inicioEstudiantes === 0}
              onClick={() => setInicioEstudiantes(inicioEstudiantes - TAMANO_PAGINA)}
            >
              Estudiantes anteriores
            </button>
            <button
              disabled={guardando || estudiantes.length < TAMANO_PAGINA}
              onClick={() => setInicioEstudiantes(inicioEstudiantes + TAMANO_PAGINA)}
            >
              Estudiantes siguientes
            </button>
          </div>
          <form onSubmit={matricularEstudiante}>
            <label>
              Usuario del estudiante
              <input
                required
                value={usuarioEstudiante}
                disabled={guardando}
                onChange={(evento) => setUsuarioEstudiante(evento.target.value)}
              />
            </label>
            <button type="submit" disabled={guardando}>
              Matricular estudiante
            </button>
          </form>
        </section>
      )}

      <h3>Auditoría de cambios</h3>
      <p>
        Acceso exclusivo de administración. Registra actor, acción, objeto y fecha; no muestra
        contraseñas, claves ni contenido de mensajes.
      </p>
      <div className="scroll">
        <table>
          <thead>
            <tr>
              <th>Fecha</th>
              <th>Actor</th>
              <th>Acción</th>
              <th>Objeto</th>
            </tr>
          </thead>
          <tbody>
            {auditoria.map((evento) => (
              <tr key={evento.id}>
                <td>{evento.occurredAt}</td>
                <td>{evento.actor}</td>
                <td>{etiquetasDeAuditoria[evento.action] || evento.action}</td>
                <td>{evento.activityTitle ? `${evento.activityTitle} (ID ${evento.subject})` : evento.subject}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {!auditoria.length && (
        <p>
          No hay cambios registrados en esta página. Los eventos anteriores a esta actualización no
          se reconstruyen.
        </p>
      )}
      <div className="pager">
        <button
          disabled={guardando || inicioAuditoria === 0}
          onClick={() => setInicioAuditoria(inicioAuditoria - TAMANO_PAGINA)}
        >
          Auditoría anterior
        </button>
        <span>Página {inicioAuditoria / TAMANO_PAGINA + 1}</span>
        <button
          disabled={guardando || auditoria.length < TAMANO_PAGINA}
          onClick={() => setInicioAuditoria(inicioAuditoria + TAMANO_PAGINA)}
        >
          Auditoría siguiente
        </button>
      </div>
    </section>
  );
}
