# Hallazgo 03: registro confirmado independiente del inicio de sesión

## Problema

`RegisterPage.tsx` encadenaba registro e inicio de sesión en una sola mutación. Si el registro creaba la cuenta pero el login fallaba, se mostraba un fallo de registro y se invitaba implícitamente a repetir una operación ya completada.

## Solución

- La mutación de TanStack Query ejecuta únicamente `register`, sin login automático ni cambios de sesión.
- Tras el registro exitoso, el formulario se sustituye por «Cuenta creada», una confirmación explícita y un enlace a `/login`. La confirmación indica que no hace falta registrarse de nuevo.
- Se vacían ambas contraseñas al completar el registro, se bloquean envíos pendientes o ya exitosos y se desactivan explícitamente los reintentos automáticos de esta mutación.
- Los fallos reales de registro conservan el formulario y sus mensajes existentes.
- No se modifica la validación de longitud/bytes de contraseñas, reservada para otro hallazgo.

## Comprobación automatizada

Se añade `frontend/src/pages/RegisterPage.test.tsx` usando Vitest y el renderizado de servidor de React, ya disponibles. Comprueba que la mutación sólo registra aunque el login simulado falle, que no activa reintentos, que el estado exitoso muestra confirmación/enlace sin formulario ni campos de contraseña y que un conflicto de registro mantiene el error.

**No se han ejecutado pruebas ni build**, por instrucción de la tarea. Comando propuesto, desde `frontend`: `npm test -- src/pages/RegisterPage.test.tsx`.

Estas pruebas controlan los estados de TanStack y no simulan interacción de navegador ni transiciones reales de su observador; no sustituyen la comprobación manual siguiente.

## Comprobación manual pendiente

1. Abrir `/register` y crear una cuenta con un correo nuevo y datos válidos.
2. En la pestaña de red, verificar un único POST a `/api/auth/register` y ninguna llamada automática a `/api/auth/login`.
3. Confirmar que aparece «Cuenta creada», que no queda formulario para repetir el registro y que el enlace lleva a `/login`.
4. Hacer fallar el login (por ejemplo, bloquear su endpoint en las herramientas del navegador): el fallo debe mostrarse sólo en la pantalla de login, sin repetir el registro ni presentar la creación de la cuenta como fallida.
5. Rehabilitar el login y acceder con la cuenta creada.
6. Intentar registrar un correo existente y comprobar que permanece el formulario con el mensaje de conflicto.

Alcance: no se implementa persistencia de la confirmación tras recargar ni resolución de respuestas de registro perdidas por fallos de red.
