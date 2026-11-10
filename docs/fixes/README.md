# Índice de correcciones

Los 11 hallazgos se integran de forma coordinada. La existencia de documentación
o configuración no certifica una ejecución satisfactoria de las pruebas; consultar
la validación indicada en cada documento y los resultados de CI.

La sesión principal verificó los cambios integrados después del trabajo de los subagentes:
consultar los [resultados y comprobaciones pendientes](VALIDACION.md). Las notas de cada
hallazgo sobre pruebas no ejecutadas describen la intervención individual, no esta validación final.

| Hallazgo | Tema |
| --- | --- |
| 01 | [Ponderaciones: suma máxima y concurrencia](01-ponderaciones-concurrentes.md) |
| 02 | [Examen único por asignatura](02-examen-unico.md) |
| 03 | [Registro confirmado, sin autologin](03-registro-confirmado.md) |
| 04 | [Contraseñas: límite en bytes UTF-8](04-password-bytes.md) |
| 05 | [Zona horaria académica](05-zona-horaria.md) |
| 06 | [Avisos de evaluaciones reprogramadas](06-avisos-reprogramados.md) |
| 07 | [Proxy de confianza y perfil `proxy`](07-proxy-confiable.md) |
| 08 | [Disponibilidad real de PostgreSQL y `TEST_DB_REQUIRED`](08-base-pruebas.md) |
| 09 | [PostgreSQL local](09-postgres-local.md) |
| 10 | [Créditos: entero positivo y máximo `Short` (32767)](10-creditos.md) |
| 11 | [Integración continua y Dependabot](11-integracion-continua.md) |

Volver al [README principal](../../README.md).

## Diagnósticos posteriores

- [Error de autenticación al arrancar: puerto de otra instancia PostgreSQL](12-puerto-postgresql.md).

## QA

- [Resultados de pruebas ejecutadas contra PostgreSQL, frontend y API](QA-EJECUCION.md).
