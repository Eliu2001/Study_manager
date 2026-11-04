import { apiFetch } from './client'

export type Term = {
  id: number
  name: string
  startDate: string
  endDate: string
}

export type TermInput = {
  name: string
  startDate: string
  endDate: string
}

export function listTerms(): Promise<Term[]> {
  return apiFetch<Term[]>('/api/terms')
}

export function fetchTerm(id: number): Promise<Term> {
  return apiFetch<Term>(`/api/terms/${id}`)
}

export function createTerm(data: TermInput): Promise<Term> {
  return apiFetch<Term>('/api/terms', { method: 'POST', body: data })
}

export function updateTerm(id: number, data: TermInput): Promise<Term> {
  return apiFetch<Term>(`/api/terms/${id}`, { method: 'PUT', body: data })
}

export function deleteTerm(id: number): Promise<void> {
  return apiFetch<void>(`/api/terms/${id}`, { method: 'DELETE' })
}
