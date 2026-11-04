import { apiFetch } from './client'

export type AssessmentType = 'SUMMATIVE' | 'EXAM'
export type AssessmentStatus = 'PENDING' | 'SUBMITTED' | 'GRADED'

export type Assessment = {
  id: number
  courseId: number
  name: string
  type: AssessmentType
  weight: number
  dueDate: string | null
  status: AssessmentStatus
  grade: number | null
  topics: string | null
  comment: string | null
}

export type AssessmentInput = {
  name: string
  type: AssessmentType
  weight: number
  dueDate: string | null
  status: AssessmentStatus
  grade: number | null
  topics: string | null
  comment: string | null
}

export function listAssessments(courseId: number): Promise<Assessment[]> {
  return apiFetch<Assessment[]>(`/api/courses/${courseId}/assessments`)
}

export function createAssessment(courseId: number, data: AssessmentInput): Promise<Assessment> {
  return apiFetch<Assessment>(`/api/courses/${courseId}/assessments`, { method: 'POST', body: data })
}

export function updateAssessment(id: number, data: AssessmentInput): Promise<Assessment> {
  return apiFetch<Assessment>(`/api/assessments/${id}`, { method: 'PUT', body: data })
}

export function deleteAssessment(id: number): Promise<void> {
  return apiFetch<void>(`/api/assessments/${id}`, { method: 'DELETE' })
}
