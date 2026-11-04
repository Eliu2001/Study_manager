import { apiFetch } from './client'

export type User = {
  id: number
  name: string
  email: string
}

export type LoginResponse = {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
}

export type RegisterData = {
  name: string
  email: string
  password: string
}

export type LoginData = {
  email: string
  password: string
}

export function register(data: RegisterData): Promise<User> {
  return apiFetch<User>('/api/auth/register', { method: 'POST', body: data, auth: false })
}

export function login(data: LoginData): Promise<LoginResponse> {
  return apiFetch<LoginResponse>('/api/auth/login', { method: 'POST', body: data, auth: false })
}

export function fetchMe(): Promise<User> {
  return apiFetch<User>('/api/users/me')
}
