import type { AcademicDateType } from '../api/academicDates'
import type { AssessmentStatus, AssessmentType } from '../api/assessments'

export const assessmentTypeLabel: Record<AssessmentType, string> = {
  SUMMATIVE: 'Sumativa',
  EXAM: 'Examen',
}

export const assessmentStatusLabel: Record<AssessmentStatus, string> = {
  PENDING: 'Pendiente',
  SUBMITTED: 'Entregada',
  GRADED: 'Calificada',
}

export const assessmentStatusClass: Record<AssessmentStatus, string> = {
  PENDING: 'bg-slate-100 text-slate-700',
  SUBMITTED: 'bg-sky-100 text-sky-800',
  GRADED: 'bg-green-100 text-green-800',
}

export const academicDateTypeLabel: Record<AcademicDateType, string> = {
  HOLIDAY: 'Feriado',
  TERM_START: 'Inicio de semestre',
  TERM_END: 'Término de semestre',
  DEADLINE: 'Entrega',
  OTHER: 'Otro',
}
