import type { CourseStatus, CourseSummary } from '../api/summary'
import { formatGrade } from './format'

export const statusLabel: Record<CourseStatus, string> = {
  PASSED: 'Aprobada',
  FAILED: 'Reprobada',
  SECURED: 'Aprobación asegurada',
  IN_PROGRESS: 'En curso',
  IMPOSSIBLE: 'Imposible de aprobar',
}

export const statusClass: Record<CourseStatus, string> = {
  PASSED: 'bg-green-100 text-green-800',
  FAILED: 'bg-red-100 text-red-800',
  SECURED: 'bg-emerald-100 text-emerald-800',
  IN_PROGRESS: 'bg-amber-100 text-amber-800',
  IMPOSSIBLE: 'bg-red-100 text-red-800',
}

/** Frase corta que explica en qué situación está la asignatura. */
export function courseHint(summary: CourseSummary): string {
  switch (summary.status) {
    case 'PASSED':
      return `Aprobada con nota final ${formatGrade(summary.finalGrade)}.`
    case 'FAILED':
      return `Reprobada con nota final ${formatGrade(summary.finalGrade)}.`
    case 'SECURED':
      return 'Aprobación asegurada, aunque saques 1,0 en lo que falta.'
    case 'IMPOSSIBLE':
      return 'Ya no alcanza para aprobar: necesitarías más de 7,0 en lo que falta.'
    case 'IN_PROGRESS':
      return summary.currentAverage === null
        ? `Necesitas un promedio de ${formatGrade(summary.requiredAverage)} para aprobar.`
        : `Promedio actual ${formatGrade(summary.currentAverage)}. Necesitas ${formatGrade(summary.requiredAverage)} en lo que falta.`
  }
}
