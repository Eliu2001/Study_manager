# Hallazgo 11: integración continua y actualizaciones reales

El README mencionaba CI y Dependabot, pero faltaban sus archivos. Se añaden
`.github/workflows/ci.yml` y `.github/dependabot.yml`.

## Backend

- Cada push y pull request ejecuta Maven Wrapper `verify` con Temurin Java 21.
- Un servicio PostgreSQL 16 crea `study_manager_test` mediante `POSTGRES_DB`.
- `DB_NAME=study_manager` mantiene el contrato de las pruebas: agregar `_test`.
- `DB_USER` y `DB_PASSWORD` coinciden con el servicio; `JWT_SECRET` supera los
  32 caracteres. Son valores públicos de pruebas, nunca secretos de producción.
- `TEST_DB_REQUIRED=true` exige la conexión JDBC real implementada en el
  hallazgo 08: si no está disponible la base, la ejecución debe fallar, no omitir
  silenciosamente la integración. El healthcheck no sustituye esta comprobación.
- `APP_TIME_ZONE=America/Santiago` fija explícitamente la zona académica.

## Frontend

Node 24 y pnpm 11 se seleccionan explícitamente. Dentro de `frontend` se ejecutan,
en orden, `pnpm install --frozen-lockfile`, `pnpm lint`, `pnpm test` y `pnpm build`.
No se modifica ni regenera `pnpm-lock.yaml`, ni se desactiva
`minimumReleaseAge: 4320` de `pnpm-workspace.yaml` (3 días). Las actualizaciones
deben entregar un lockfile coherente; CI debe rechazarlas si no lo es.

## Permisos y Dependabot

El workflow solo concede `contents: read`, no conserva credenciales de checkout,
no usa `pull_request_target`, no despliega y no referencia secretos del repositorio.
Los jobs tienen límites de tiempo y se ejecutan independientemente.

Dependabot revisa semanalmente Maven (`/`), npm/pnpm (`/frontend`) y GitHub
Actions (`/`). Se configura `cooldown.default-days: 3` para los tres ecosistemas:
la [referencia oficial de GitHub](https://docs.github.com/en/code-security/dependabot/working-with-dependabot/dependabot-options-reference#cooldown)
incluye soporte de `default-days` para GitHub Actions, pero no para sus opciones
SemVer específicas; por eso estas últimas no se usan. El cooldown se aplica a
actualizaciones de versión, no a las de seguridad. No se configura automerge.

## Validación pendiente

Esta corrección añade configuración y documentación; no se han ejecutado builds
ni suites de pruebas como parte de ella. Otros hallazgos se integran en paralelo,
en particular el modo JDBC obligatorio del hallazgo 08. Tras integrar todos los
cambios, revisar ambos jobs en Actions y confirmar que la integración no se omite;
comprobar además que una base inaccesible con `TEST_DB_REQUIRED=true` produce fallo.
Dependabot deberá validar su configuración al estar disponible en la rama por defecto.

Volver al [índice](README.md).
