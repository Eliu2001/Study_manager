import { describe, expect, it } from 'vitest'
import { isValidCredits } from './credits'

describe('isValidCredits', () => {
  it.each([null, 1, 100, 101, 32767])('acepta créditos opcionales o válidos: %s', (credits) => {
    expect(isValidCredits(credits)).toBe(true)
  })

  it.each([0, -1, 1.5, 32767.5, 32768, NaN, Infinity, -Infinity])(
    'rechaza créditos fuera de rango o no enteros: %s',
    (credits) => {
      expect(isValidCredits(credits)).toBe(false)
    },
  )
})
