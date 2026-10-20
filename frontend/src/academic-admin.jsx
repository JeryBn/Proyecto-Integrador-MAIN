import {useEffect,useState} from 'react';
const actionLabels={ACCOUNT_CREATED:'Cuenta creada',ACCOUNT_DISABLED:'Cuenta deshabilitada',PASSWORD_RESET:'Contraseña restablecida',GUARDIAN_VERIFIED:'Parentesco verificado',GUARDIAN_REVOKED:'Parentesco revocado',CONTACT_APPROVED:'Contacto autorizado',COURSE_CREATED:'Curso creado',COURSE_UPDATED:'Curso editado',ENROLLMENT_GRANTED:'Matrícula registrada',ENROLLMENT_REVOKED:'Matrícula revocada',TIMETABLE_CREATED:'Horario registrado',TIMETABLE_REMOVED:'Horario retirado',GRADE_CREATED:'Nota registrada',GRADE_UPDATED:'Nota editada',GROUP_CREATED:'Grupo creado',GROUP_INVITED:'Invitación a grupo',GROUP_INVITATION_ACCEPTED:'Invitación aceptada',GROUP_INVITATION_REJECTED:'Invitación rechazada',GROUP_MEMBER_REMOVED:'Miembro retirado',GROUP_MEMBER_ROLE_CHANGED:'Rol de miembro editado',MESSAGE_APPROVED:'Mensaje aprobado',MESSAGE_REJECTED:'Mensaje rechazado',STATUS_APPROVED:'Estado aprobado',STATUS_REJECTED:'Estado rechazado'};

export function AcademicAdmin({api,onExpire}) {
  const [courses,setCourses]=useState([]),[audit,setAudit]=useState([]),[students,setStudents]=useState([]);
  const [offset,setOffset]=useState(0),[auditOffset,setAuditOffset]=useState(0),[studentOffset,setStudentOffset]=useState(0);
  const [selected,setSelected]=useState(''),[revision,setRevision]=useState(0),[error,setError]=useState(''),[notice,setNotice]=useState('');
  const [busy,setBusy]=useState(false),[loading,setLoading]=useState(true),[student,setStudent]=useState('');
  const blank={id:'',name:'',teacherUsername:'',expectedVersion:null};
  const [form,setForm]=useState(blank),[editing,setEditing]=useState(false);
  const fail=e=>{if(e.status===401||e.status===403)onExpire();else setError(e.message);};
  useEffect(()=>{
    let active=true;setLoading(true);
    Promise.all([api(`/v1/courses?offset=${offset}&limit=20`),api(`/v1/audit?offset=${auditOffset}&limit=20`)])
      .then(([c,a])=>{if(active){setCourses(c);setAudit(a);}}).catch(e=>{if(active)fail(e);}).finally(()=>{if(active)setLoading(false);});
    return ()=>{active=false;};
  },[offset,auditOffset,revision]);
  useEffect(()=>{
    let active=true;setStudents([]);
    if(selected)api(`/v1/courses/${selected}/students?offset=${studentOffset}&limit=20`).then(s=>{if(active)setStudents(s);}).catch(e=>{if(active)fail(e);});
    return ()=>{active=false;};
  },[selected,studentOffset,revision]);
  async function save(action,message,done){
    setBusy(true);setError('');setNotice('');
    try{await action();setNotice(message);done?.();setRevision(v=>v+1);}
    catch(e){fail(e);}finally{setBusy(false);}
  }
  return <section aria-label="Administración académica"><h2>Cursos y matrículas</h2>
    {error && <p role="alert" className="err">{error}</p>}{notice && <p role="status" className="ok">{notice}</p>}
    <button disabled={busy} onClick={()=>setRevision(v=>v+1)}>Actualizar cursos y auditoría</button>
    {loading?<p role="status">Cargando…</p>:<div className="scroll"><table><caption>Cursos registrados</caption><thead><tr><th>ID</th><th>Curso</th><th>Docente</th><th>Acciones</th></tr></thead><tbody>
      {courses.map(c=><tr key={c.id}><td>{c.id}</td><td>{c.name}</td><td>{c.teacher}</td><td>
        <button disabled={busy} onClick={()=>{setEditing(true);setForm({id:String(c.id),name:c.name,teacherUsername:'',expectedVersion:c.version});setNotice('Indica el usuario docente para confirmar la asignación.');}}>Editar {c.name}</button>
        <button disabled={busy} onClick={()=>{setSelected(String(c.id));setStudentOffset(0);}}>Matrículas de {c.name}</button>
      </td></tr>)}</tbody></table>{!courses.length&&<p>No hay cursos en esta página.</p>}</div>}
    <div className="pager"><button disabled={busy||offset===0} onClick={()=>setOffset(offset-20)}>Cursos anteriores</button><span>Página {offset/20+1}</span><button disabled={busy||courses.length<20} onClick={()=>setOffset(offset+20)}>Cursos siguientes</button></div>
    <fieldset className="card" disabled={busy}><legend>{editing?'Editar curso':'Crear curso'}</legend><form onSubmit={e=>{
      e.preventDefault();const id=Number(form.id);if(!Number.isSafeInteger(id)||id<1){setError('Usa un ID entero positivo.');return;}
      const body={...form,id};save(()=>api('/v1/courses'+(editing?'/'+id:''),{method:editing?'PUT':'POST',json:body}),'Curso guardado.',()=>{setEditing(false);setForm(blank);});
    }}><label>ID del curso<input required inputMode="numeric" readOnly={editing} value={form.id} onChange={e=>setForm({...form,id:e.target.value})}/></label>
      <label>Nombre del curso<input required maxLength="100" value={form.name} onChange={e=>setForm({...form,name:e.target.value})}/></label>
      <label>Usuario del docente responsable<input required value={form.teacherUsername} onChange={e=>setForm({...form,teacherUsername:e.target.value})}/></label>
      <p>Solo cuentas docentes activas. Si el curso cambió en otra sesión, actualiza y vuelve a seleccionarlo. Para reasignar un curso con horario, ajusta antes sus clases.</p>
      <button type="submit">{busy?'Guardando…':'Guardar curso'}</button><button type="button" onClick={()=>{setEditing(false);setForm(blank);}}>Nuevo curso</button>
    </form></fieldset>
    {selected && <section className="card"><h3>Matrículas del curso {selected}</h3>
      <p>Revocar una matrícula retira acceso a actividades y notas del curso; conserva el historial académico. No elimina contactos ni conversaciones independientes.</p>
      <ul>{students.map(s=><li key={s.studentId}>{s.name} ({s.username}) <button disabled={busy} onClick={()=>{
        if(window.confirm(`¿Revocar la matrícula de ${s.name} en el curso ${selected}?`))save(()=>api(`/v1/courses/${selected}/students`,{method:'PUT',json:{username:s.username,enrolled:false}}),'Matrícula revocada.');
      }}>Revocar matrícula de {s.name}</button></li>)}</ul>{!students.length&&<p>No hay estudiantes en esta página.</p>}
      <div className="pager"><button disabled={busy||studentOffset===0} onClick={()=>setStudentOffset(studentOffset-20)}>Estudiantes anteriores</button><button disabled={busy||students.length<20} onClick={()=>setStudentOffset(studentOffset+20)}>Estudiantes siguientes</button></div>
      <form onSubmit={e=>{e.preventDefault();save(()=>api(`/v1/courses/${selected}/students`,{method:'PUT',json:{username:student,enrolled:true}}),'Estudiante matriculado.',()=>setStudent(''));}}>
        <label>Usuario del estudiante<input required value={student} disabled={busy} onChange={e=>setStudent(e.target.value)}/></label><button type="submit" disabled={busy}>Matricular estudiante</button>
      </form>
    </section>}
    <h3>Auditoría de cambios</h3><p>Acceso exclusivo de administración. Registra actor, acción, objeto y fecha; no muestra contraseñas, claves ni contenido de mensajes.</p>
    <div className="scroll"><table><thead><tr><th>Fecha</th><th>Actor</th><th>Acción</th><th>Objeto</th></tr></thead><tbody>{audit.map(a=><tr key={a.id}><td>{a.occurredAt}</td><td>{a.actor}</td><td>{actionLabels[a.action]||a.action}</td><td>{a.subject}</td></tr>)}</tbody></table></div>
    {!audit.length&&<p>No hay cambios registrados en esta página. Los eventos anteriores a esta actualización no se reconstruyen.</p>}
    <div className="pager"><button disabled={busy||auditOffset===0} onClick={()=>setAuditOffset(auditOffset-20)}>Auditoría anterior</button><span>Página {auditOffset/20+1}</span><button disabled={busy||audit.length<20} onClick={()=>setAuditOffset(auditOffset+20)}>Auditoría siguiente</button></div>
  </section>;
}
