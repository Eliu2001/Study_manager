const MIN_PASSWORD_LENGTH = 8
const MAX_PASSWORD_BYTES = 72

/** El mínimo usa unidades UTF-16, igual que @Size sobre String en el backend. */
export function validatePassword(password: string): string | null {
  if (password.length < MIN_PASSWORD_LENGTH) {
    return 'La contraseña debe tener al menos 8 caracteres.'
  }

  if (new TextEncoder().encode(password).length > MAX_PASSWORD_BYTES) {
    return 'La contraseña no puede superar 72 bytes en UTF-8. Las letras como ñ ocupan 2 bytes y los emojis como 😀, 4 bytes.'
  }

  return null
}
