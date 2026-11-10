const API_URL: string = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'
const TOKEN_KEY = 'study-manager.token'

export function getToken(): string | null {
  return sessionStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  sessionStorage.setItem(TOKEN_KEY, token)
}

export function clearToken(): void {
  sessionStorage.removeItem(TOKEN_KEY)
}

export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

let onUnauthorized: (() => void) | null = null

/** Permite que la sesión se cierre sola cuando el token venció o ya no es válido. */
export function setUnauthorizedHandler(handler: (() => void) | null): void {
  onUnauthorized = handler
}

function defaultMessage(status: number): string {
  switch (status) {
    case 0:
      return 'No se pudo conectar con el servidor. Revisa que esté en marcha e inténtalo de nuevo.'
    case 400:
      return 'Revisa los datos ingresados.'
    case 401:
      return 'Tu sesión no es válida o expiró. Inicia sesión de nuevo.'
    case 403:
      return 'No tienes permiso para hacer esto.'
    case 404:
      return 'No se encontró lo que buscas.'
    case 409:
      return 'Ya existe un registro con esos datos.'
    case 429:
      return 'Demasiados intentos. Espera unos minutos antes de volver a intentarlo.'
    default:
      return 'Ocurrió un error inesperado. Inténtalo de nuevo.'
  }
}

/** El servidor indica cuánto esperar en la cabecera Retry-After (en segundos). */
function tooManyRequestsMessage(response: Response): string {
  const seconds = Number(response.headers.get('Retry-After'))
  if (!Number.isFinite(seconds) || seconds <= 0) {
    return defaultMessage(429)
  }
  const minutes = Math.ceil(seconds / 60)
  return `Demasiados intentos fallidos. Vuelve a intentarlo en ${minutes} ${minutes === 1 ? 'minuto' : 'minutos'}.`
}

type RequestOptions = {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
  /** Si es false, no se envía el token (login y registro). */
  auth?: boolean
}

export async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, auth = true } = options

  const headers: Record<string, string> = {}
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  const token = getToken()
  if (auth && token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  let response: Response
  try {
    response = await fetch(`${API_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, defaultMessage(0))
  }

  if (response.status === 401 && auth && token) {
    onUnauthorized?.()
  }
  if (!response.ok) {
    const message =
      response.status === 429 ? tooManyRequestsMessage(response) : defaultMessage(response.status)
    throw new ApiError(response.status, message)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}
