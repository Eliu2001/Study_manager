# Validación de las 11 correcciones

## Alcance

Se asignó un subagente a cada hallazgo, conservando las capas y dependencias del proyecto.
Los cambios sobre `RegisterPage` y `AssessmentService` se hicieron por turnos para no
sobrescribir soluciones. La sesión principal revisó su integración y realizó estas comprobaciones.
No se crearon commits, no se instalaron dependencias ni se modificaron bases existentes.

## Resultados locales

| Comprobación | Resultado |
| --- | --- |
| Maven Wrapper `verify`, Java 21 | Correcto; compila y genera el JAR ejecutable. |
| Backend JUnit | 88 pruebas detectadas: 68 ejecutadas correctamente, 20 omitidas; ningún fallo ni error. |
| Frontend Vitest | 7 archivos y 57 pruebas correctas. |
| Frontend ESLint | Correcto. |
| Frontend TypeScript `tsc -b` | Correcto. |
| Frontend Vite `build` | Correcto. |
| Docker Compose `config --quiet` | Correcto con variables ficticias temporales; no inicia contenedores. |
| Revisión de espacios en Git | Sin errores, respetando los finales de línea de Windows. |

El frontend se comprobó con los ejecutables ya instalados en `frontend/node_modules/.bin`
(`vitest.cmd run`, `eslint.cmd .`, `tsc.cmd -b`, `vite.cmd build`), equivalentes a los scripts
del proyecto. pnpm no estaba disponible en PATH; no se probó una instalación nueva ni se
regeneró el lockfile. La CI sí exige `pnpm install --frozen-lockfile`.

Maven mostró avisos no bloqueantes de metadatos HTTP 401 de repositorios adicionales y
de instrumentación dinámica de Mockito. La advertencia por `Invalid/Zone` corresponde a
una prueba deliberada de rechazo de zona inválida; no es un fallo del arranque normal.

## Cobertura por hallazgo

1. **Ponderaciones:** 6 pruebas unitarias de bloqueo, orden y autorización correctas;
   5 pruebas de concurrencia/estado obsoleto compiladas, pero omitidas sin PostgreSQL.
2. **Examen único:** migración V2 incorporada sin alterar V1; las 4 pruebas SQL directas
   están compiladas, pero su ejecución y la aplicación de la migración siguen pendientes.
3. **Registro:** 3 pruebas de renderizado y configuración de la mutación correctas;
   no sustituyen una interacción real de navegador.
4. **Contraseñas:** pruebas UTF-8 correctas para ASCII, eñes, emojis y valores límite.
5. **Zona horaria:** 8 pruebas correctas de reloj, medianoche, verano y configuración;
   la integración HTTP existente usa también el reloj académico al preparar su fecha.
6. **Reprogramación:** 6 pruebas unitarias correctas; 2 pruebas de persistencia/rollback
   compiladas, pero omitidas sin PostgreSQL.
7. **Proxy:** 7 pruebas correctas del controlador y Tomcat. Se incluye el rechazo de la
   autodetección cloud sin perfil `proxy`; la configuración base fuerza `none`.
8. **Base de pruebas:** 7 pruebas unitarias correctas de JDBC, credenciales, configuración,
   destino exclusivo, cierre de conexión y modo obligatorio ante indisponibilidad.
9. **PostgreSQL local:** configuración Compose validada; no se recreó ningún contenedor.
10. **Créditos:** pruebas correctas del rango compatible con `Short`, incluido 101 y 32767.
11. **Automatización:** CI y Dependabot incorporados y revisados; no se publicaron cambios
    ni se ejecutó GitHub Actions desde esta sesión.

## Qué falta comprobar

Docker Desktop no tiene el motor activo y no hay una base de pruebas configurada disponible.
Las 20 omisiones incluyen 8 pruebas HTTP existentes, 1 de contexto, 5 de concurrencia,
4 de la restricción SQL y 2 de reprogramación. **La compilación y las pruebas unitarias
no validan los bloqueos ni la migración contra PostgreSQL real.**

Con el motor y la base separada preparados según el README, ejecutar en PowerShell:

```powershell
$env:TEST_DB_REQUIRED = 'true'
.\mvnw.cmd --batch-mode --no-transfer-progress verify
```

Proporcionar las credenciales de pruebas y `JWT_SECRET` mediante la configuración habitual.
Este modo debe fallar si no puede conectar, en lugar de presentar una suite verde con omisiones.
La CI provisiona su propia base para realizar esa comprobación.

También quedan pendientes las comprobaciones manuales de registro/contraseña en navegador,
del proxy real y firewall, y la validación de Actions y Dependabot en GitHub.

## Precauciones de despliegue

- Antes de aplicar V2, revisar los exámenes duplicados con las consultas de
  [02-examen-unico.md](02-examen-unico.md). La migración no borra ni elige registros:
  si hay duplicados, se detiene para resolverlos con autorización del responsable de datos.
- Recrear el contenedor de PostgreSQL para que tome el binding local, sin borrar el volumen;
  seguir [09-postgres-local.md](09-postgres-local.md).
- Tras crear una cuenta se inicia sesión manualmente. Los créditos conservan el contrato
  existente de la API (1 a 32767), en lugar de invalidar datos previos mayores de 100.
- Para un proxy, activar el perfil y declarar IPs exactas según
  [07-proxy-confiable.md](07-proxy-confiable.md); sin lista explícita no se confía en nadie.

Volver al [índice](README.md).
