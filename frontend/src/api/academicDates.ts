import { apiFetch } from './client'

export type AcademicDateType = 'HOLIDAY' | 'TERM_START' | 'TERM_END' | 'DEADLINE' | 'OTHER'

export type AcademicDate = {
  id: number
  courseId: number | null
  title: string
  eventDate: string
  type: AcademicDateType
}

export type AcademicDateInput = {
  title: string
  eventDate: string
  type: AcademicDateType
  courseId: number | null
}

/** Con `from` solo trae las fechas desde ese día (formato AAAA-MM-DD). */
export function listAcademicDates(from?: string): Promise<AcademicDate[]> {
  const query = from ? `?from=${from}` : ''
  return apiFetch<AcademicDate[]>(`/api/academic-dates${query}`)
}

export function createAcademicDate(data: AcademicDateInput): Promise<AcademicDate> {
  return apiFetch<AcademicDate>('/api/academic-dates', { method: 'POST', body: data })
}

export function updateAcademicDate(id: number, data: AcademicDateInput): Promise<AcademicDate> {
  return apiFetch<AcademicDate>(`/api/academic-dates/${id}`, { method: 'PUT', body: data })
}

export function deleteAcademicDate(id: number): Promise<void> {
  return apiFetch<void>(`/api/academic-dates/${id}`, { method: 'DELETE' })
}
