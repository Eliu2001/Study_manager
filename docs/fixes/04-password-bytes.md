# Hallazgo 04: límite de contraseña en bytes UTF-8

## Problema

El formulario anunciaba un máximo de 72 caracteres, pero BCrypt admite un máximo de 72 bytes. Contraseñas con caracteres multibyte podían superar el límite y llegar al backend como si fueran válidas.

## Solución

- `frontend/src/utils/password.ts` expone la función pura `validatePassword`: mínimo de 8 unidades UTF-16 (`String.length`, equivalente a `@Size` de Java) y máximo de 72 bytes medidos con `TextEncoder` en UTF-8. No recorta ni normaliza la contraseña.
- `RegisterPage.handleSubmit` conserva la comprobación de confirmación y valida la contraseña antes de llamar a `mutate`. Conserva el registro sin autologin, su estado de éxito y el bloqueo de reenvíos.
- La ayuda accesible y los errores explican el máximo en bytes, con ejemplos de ñ (2 bytes) y 😀 (4 bytes), en lugar de prometer 72 caracteres. Se elimina el máximo HTML en caracteres; el mínimo HTML se mantiene.
- La validación del backend permanece intacta y sigue siendo la autoridad para solicitudes directas. Tampoco se modifican las pruebas del hallazgo 03.

## Comprobaciones pendientes

No se han ejecutado pruebas ni build, según las instrucciones. El coordinador puede ejecutar desde `frontend`: `npm test -- src/utils/password.test.ts src/pages/RegisterPage.test.tsx`.

Las pruebas de la utilidad cubren ASCII 72/73 bytes, ñ 36/37 repeticiones, emoji 18/19 repeticiones, mezclas de 72/73 bytes, caracteres de tres bytes, acentos descompuestos, mínimo UTF-16 y ausencia de recorte.

Comprobación manual pendiente: enviar contraseñas coincidentes de 37 eñes o 19 emojis y verificar el error local sin POST; con 36 eñes o 18 emojis y datos de registro válidos debe enviarse el registro. Verificar también discrepancia de confirmación y éxito sin inicio de sesión automático.
