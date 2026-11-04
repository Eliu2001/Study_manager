import { apiFetch } from './client'

export type Course = {
  id: number
  termId: number
  name: string
  code: string | null
  credits: number | null
  passingGrade: number
}

export type CourseInput = {
  name: string
  code: string | null
  credits: number | null
  passingGrade: number
}

export function listCourses(termId: number): Promise<Course[]> {
  return apiFetch<Course[]>(`/api/terms/${termId}/courses`)
}

export function createCourse(termId: number, data: CourseInput): Promise<Course> {
  return apiFetch<Course>(`/api/terms/${termId}/courses`, { method: 'POST', body: data })
}

export function updateCourse(id: number, data: CourseInput): Promise<Course> {
  return apiFetch<Course>(`/api/courses/${id}`, { method: 'PUT', body: data })
}

export function deleteCourse(id: number): Promise<void> {
  return apiFetch<void>(`/api/courses/${id}`, { method: 'DELETE' })
}
