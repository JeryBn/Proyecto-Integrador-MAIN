import {useState} from 'react';
import {performanceRows, periodOf} from './teacher-performance.mjs';
import './teacher-performance.css';

const format = value => new Intl.NumberFormat('es-PE', {maximumFractionDigits:1}).format(value);
const labels = {missing:'Sin notas', support:'Necesita apoyo', within:'Dentro del criterio'};

export function TeacherPerformance({roster, courseName}) {
  const [period, setPeriod] = useState('all');
  const [studentId, setStudent] = useState('');
  const [threshold, setThreshold] = useState('55');
  const periods = [...new Set(roster.flatMap(s => s.grades.map(g => periodOf(g.assessment))))].sort();
  const valid = threshold.trim() !== '' && Number.isFinite(Number(threshold)) && Number(threshold)>=0 && Number(threshold)<=100;
  const rows = performanceRows(roster, period, valid ? Number(threshold) : 55)
    .filter(s => !studentId || String(s.studentId)===studentId);
  const evaluated = rows.filter(s=>s.percent!==null);
  const mean = evaluated.length ? evaluated.reduce((sum,s)=>sum+s.percent,0)/evaluated.length : null;
  return <section className="teacher-performance user-panel" aria-label="Gráfica de rendimiento del docente">
    <span className="eyebrow">SEGUIMIENTO DE MIS ESTUDIANTES</span>
    <h2>Rendimiento · {courseName}</h2>
    <div className="performance-filters">
      <label>Periodo de las evaluaciones<select value={period} onChange={e=>setPeriod(e.target.value)}>
        <option value="all">Todas las evaluaciones</option>
        {periods.map(p=><option key={p} value={p}>{p==='unassigned'?'Sin periodo registrado':`Año ${p.split('/')[0]} · periodo ${p.split('/')[1]}`}</option>)}
      </select></label>
      <label>Estudiante<select aria-label="Estudiante" value={studentId} onChange={e=>setStudent(e.target.value)}>
        <option value="">Todos los estudiantes</option>
        {roster.map(s=><option key={s.studentId} value={s.studentId}>{s.name}</option>)}
      </select></label>
      <label>Apoyo si el promedio es menor que (%)<input type="number" min="0" max="100" step="0.1" value={threshold} onChange={e=>setThreshold(e.target.value)}/></label>
    </div>
    {!valid && <p role="alert">Indica un criterio entre 0 y 100 para mostrar el seguimiento.</p>}
    <p>Promedio orientativo: cada evaluación se convierte a porcentaje y tiene el mismo peso. El promedio del grupo da el mismo peso a cada estudiante con notas. El criterio de esta vista no modifica las calificaciones ni la promoción escolar.</p>
    {valid && <>
      <div className="performance-summary">
        <article><span>Promedio del grupo seleccionado</span><strong>{mean===null?'Sin notas':`${format(mean)} %`}</strong></article>
        <article><span>Con notas</span><strong>{evaluated.length}</strong></article>
        <article><span>Necesitan apoyo</span><strong>{rows.filter(s=>s.status==='support').length}</strong></article>
        <article><span>Sin notas</span><strong>{rows.filter(s=>s.status==='missing').length}</strong></article>
      </div>
      {!rows.length && <p>No hay estudiantes matriculados para esta selección.</p>}
      <ul className="performance-chart" aria-label="Promedio por estudiante">
        {rows.map(s=><li key={s.studentId} className={`performance-row ${s.status}`}>
          <div className="performance-label"><strong>{s.name}</strong><span>{s.percent===null?'Sin notas':`${format(s.percent)} % · ${format(s.percent/5)} / 20`}</span></div>
          <div className="performance-track">{s.percent!==null&&<span role="meter" aria-label={`Promedio de ${s.name}`} aria-valuemin={0} aria-valuemax={100} aria-valuenow={s.percent} style={{width:`${s.percent}%`}}/>}<i aria-hidden="true" style={{left:`${threshold}%`}}/></div>
          <small>{labels[s.status]} · {s.count} evaluación(es)</small>
        </li>)}
      </ul>
      <p className="performance-legend">Azul: dentro del criterio · Ámbar: necesita apoyo · Línea vertical: criterio de {threshold} %. “Sin notas” no significa desaprobado.</p>
    </>}
    <p>Esta gráfica utiliza las notas numéricas disponibles en este curso. Las evaluaciones sin periodo se identifican por separado.</p>
  </section>;
}
