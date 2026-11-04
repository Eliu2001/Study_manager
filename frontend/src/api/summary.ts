import { apiFetch } from './client'

export type CourseStatus = 'PASSED' | 'FAILED' | 'SECURED' | 'IN_PROGRESS' | 'IMPOSSIBLE'

export type CourseSummary = {
  courseId: number
  courseName: string
  credits: number | null
  passingGrade: number
  gradedWeight: number
  currentPoints: number
  currentAverage: number | null
  finalGrade: number | null
  requiredAverage: number | null
  status: CourseStatus
}

export type TermSummary = {
  termId: number
  termName: string
  average: number | null
  courses: CourseSummary[]
}

export type TermAverage = {
  termId: number
  termName: string
  average: number | null
}

export type OverallSummary = {
  cumulativeAverage: number | null
  terms: TermAverage[]
}

export function fetchOverallSummary(): Promise<OverallSummary> {
  return apiFetch<OverallSummary>('/api/summary')
}

export function fetchTermSummary(termId: number): Promise<TermSummary> {
  return apiFetch<TermSummary>(`/api/terms/${termId}/summary`)
}

export function fetchCourseSummary(courseId: number): Promise<CourseSummary> {
  return apiFetch<CourseSummary>(`/api/courses/${courseId}/summary`)
}
