import { describe, expect, it } from 'vitest'
import { validatePassword } from './password'

describe('validatePassword', () => {
  it.each([
    ['mínimo ASCII', 'a'.repeat(8)],
    ['ASCII de 72 bytes', 'a'.repeat(72)],
    ['36 eñes de 72 bytes', 'ñ'.repeat(36)],
    ['18 emojis de 72 bytes', '😀'.repeat(18)],
    ['mezcla de 72 bytes', 'a'.repeat(2) + 'ñ'.repeat(3) + '😀'.repeat(16)],
    ['mínimo UTF-16 igual que Java', '😀'.repeat(4)],
    ['espacios sin recortar', ' '.repeat(7) + 'a'],
  ])('acepta %s', (_description, password) => {
    expect(validatePassword(password)).toBeNull()
  })

  it.each([
    ['ASCII de 73 bytes', 'a'.repeat(73)],
    ['37 eñes de 74 bytes', 'ñ'.repeat(37)],
    ['19 emojis de 76 bytes', '😀'.repeat(19)],
    ['mezcla de 73 bytes', 'a'.repeat(3) + 'ñ'.repeat(3) + '😀'.repeat(16)],
    ['caracteres de tres bytes', '界'.repeat(25)],
    ['acentos descompuestos sin normalizar', 'e\u0301'.repeat(25)],
  ])('rechaza %s', (_description, password) => {
    const error = validatePassword(password)
    expect(error).toContain('72 bytes en UTF-8')
    expect(error).toContain('ñ ocupan 2 bytes')
    expect(error).toContain('4 bytes')
  })

  it.each(['', 'a'.repeat(7), 'ñ'.repeat(7), '😀'.repeat(3) + 'a'])(
    'rechaza menos de 8 unidades UTF-16: %s',
    (password) => {
      expect(validatePassword(password)).toBe('La contraseña debe tener al menos 8 caracteres.')
    },
  )
})
