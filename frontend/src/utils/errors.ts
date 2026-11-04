import { ApiError } from '../api/client'

/**
 * Convierte el error de una mutación en un mensaje para mostrar.
 * `conflictMessage` es el texto específico para el código 409 (dato repetido).
 */
export function errorMessage(error: unknown, conflictMessage: string): string | null {
  if (!error) {
    return null
  }
  if (error instanceof ApiError) {
    return error.status === 409 ? conflictMessage : error.message
  }
  return 'Ocurrió un error inesperado. Inténtalo de nuevo.'
}
