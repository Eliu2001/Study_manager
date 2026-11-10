import { describe, expect, it } from 'vitest'
import { ApiError } from '../api/client'
import { errorMessage } from './errors'

describe('errorMessage', () => {
  it('devuelve null cuando no hay error', () => {
    expect(errorMessage(null, 'Ya existe')).toBeNull()
  })

  it('usa el texto específico para el código 409', () => {
    expect(errorMessage(new ApiError(409, 'repetido'), 'Ya existe')).toBe('Ya existe')
  })

  it('usa el mensaje del error para los demás códigos', () => {
    expect(errorMessage(new ApiError(400, 'Revisa los datos'), 'Ya existe')).toBe('Revisa los datos')
  })

  it('usa un mensaje genérico para errores desconocidos', () => {
    expect(errorMessage(new Error('boom'), 'Ya existe')).toContain('error inesperado')
  })
})
