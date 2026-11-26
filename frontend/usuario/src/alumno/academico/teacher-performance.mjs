export function periodOf(assessment) {
  return /^(\d{4}\/[1-4]) - /.exec(assessment || '')?.[1] || 'unassigned';
}

// Cada evaluación tiene el mismo peso, después de normalizar su escala.
export function performanceRows(roster, period = 'all', threshold = 55) {
  return roster.map(student => {
    const marks = student.grades.filter(mark => period === 'all' || periodOf(mark.assessment) === period)
      .filter(mark => mark.score !== null && mark.maximum !== null &&
        Number.isFinite(Number(mark.score)) && Number.isFinite(Number(mark.maximum)) &&
        Number(mark.maximum) > 0 && Number(mark.score) >= 0 && Number(mark.score) <= Number(mark.maximum));
    const percent = marks.length
      ? marks.reduce((sum, mark) => sum + Number(mark.score) / Number(mark.maximum) * 100, 0) / marks.length
      : null;
    return {...student, count: marks.length, percent,
      status: percent === null ? 'missing' : percent < threshold ? 'support' : 'within'};
  });
}
