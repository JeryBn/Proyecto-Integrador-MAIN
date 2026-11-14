const dayIndex = value => value === 0 ? 7 : value;

export function progressPercent(grades = []) {
  const entries = grades.flatMap(course => course.grades || []);
  if (!entries.length) return null;
  const ratios = entries
    .map(item => Number(item.maximum) > 0 ? Number(item.score) / Number(item.maximum) : null)
    .filter(value => Number.isFinite(value));
  if (!ratios.length) return null;
  return Math.round((ratios.reduce((sum, value) => sum + value, 0) / ratios.length) * 100);
}

export function nextClass(timetable = [], now = new Date()) {
  if (!timetable.length) return null;
  const currentDay = dayIndex(now.getDay());
  const currentMinutes = now.getHours() * 60 + now.getMinutes();
  const ranked = timetable.map(item => {
    const [hours = 0, minutes = 0] = String(item.startsAt || '00:00').split(':').map(Number);
    let days = Number(item.dayOfWeek) - currentDay;
    if (days < 0 || (days === 0 && hours * 60 + minutes < currentMinutes)) days += 7;
    return {...item, wait: days * 1440 + hours * 60 + minutes - currentMinutes};
  });
  return ranked.sort((a, b) => a.wait - b.wait)[0] || null;
}

export function recentNotices(notices = [], limit = 3) {
  return [...notices]
    .sort((a, b) => Date.parse(b.createdAt || 0) - Date.parse(a.createdAt || 0))
    .slice(0, limit);
}

