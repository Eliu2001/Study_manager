# Ejecución QA del proyecto

**Fecha:** 10 de noviembre de 2026. **Entorno:** Windows, Java 21, PostgreSQL 16 en Docker.

## Resumen

| Grupo | Resultado |
| --- | --- |
| PostgreSQL Docker | OK: `study-manager-db` healthy, host `127.0.0.1:5433` al puerto de contenedor 5432. |
| Base de pruebas | OK: existe `study_manager_test`; JDBC real y credenciales aceptados. |
| Backend `verify` obligatorio | OK: `TEST_DB_REQUIRED=true`, 90 pruebas ejecutadas, 0 fallidas, 0 errores, 0 omitidas. |
| Flyway en base de pruebas | OK: V1 y V2 aplicadas; schema versión 2. |
| Flyway/índice en base principal | OK: V1/V2 exitosas e índice único parcial por curso para `EXAM`. |
| Frontend Vitest | OK: 7 archivos, 57 pruebas aprobadas. |
| ESLint | OK: sin errores. |
| TypeScript | OK: `tsc -b` sin errores. |
| Build frontend | OK: Vite transformó 176 módulos y finalizó el build. |
| API smoke test | OK: 86 correctas, 0 fallidas. |

## Detalle de cobertura ejecutada

### Backend automatizado y base real

Comando: `TEST_DB_REQUIRED=true .\mvnw.cmd --batch-mode --no-transfer-progress verify`.
La suite se ejecutó contra `study_manager_test` por el puerto 5433; Flyway creó la base
vacía en versiones 1 y 2. Resultados por clases:

- API HTTP: 8 pruebas.
- Concurrencia de ponderaciones/exámenes y entidad obsoleta: 5 pruebas.
- Reprogramación y rollback transaccional de avisos: 2 pruebas.
- Restricción PostgreSQL de examen único (insert/update): 4 pruebas.
- Pruebas unitarias y de configuración, incluidas zona horaria, proxy, bloqueo de login,
  cálculo de notas y detección segura de la base: restantes hasta 90.

Todas finalizaron sin fallos ni omisiones. Esto sí ejercitó PostgreSQL, concurrencia,
migración y persistencia/rollback, no solo mocks.

### Inspección de la base principal

- La consulta de duplicados de tipo `EXAM` devolvió cero filas antes de V2.
- Flyway registra V1 y V2 con `success=true`.
- `uq_assessments_exam_per_course` existe como `UNIQUE` sobre `course_id` con predicado
  `type = 'EXAM'`.

### Smoke test HTTP

Se verificaron health, autorización anónima, registro/login, duplicados, datos inválidos,
aislamiento entre dos usuarios, periodos/asignaturas, validación de notas/pesos,
examen único, recordatorios y descarte, fechas académicas, promedios parciales/finales
y borrado en cascada. Resultado del script: **86 correctas, 0 fallidas**.

El smoke test dejó **dos cuentas `smoke-*`** en la base principal (ambas sin periodos,
porque el script verifica y borra en cascada su periodo de prueba). No se limpiaron cuentas
automáticamente ni se borraron datos del usuario.

## Casos del plan QA

| Caso | Evidencia | Estado |
| --- | --- | --- |
| Arranque/respuesta del servicio | `/actuator/health` devolvió UP antes del smoke test. | OK |
| Sin token | API smoke: `GET /api/terms` devuelve 401. | OK |
| Registro, duplicado y validación | Smoke: 201, 409 y 400. | OK |
| Login correcto/incorrecto y perfil | Smoke: 200, 401 y usuario actual correcto. | OK |
| Registro frontend sin autologin | Vitest `RegisterPage.test.tsx` verifica mutación y estado. | OK automatizado; navegador manual pendiente |
| Periodos: crear/listar/editar/duplicado/fechas invertidas | Smoke: 201/200/200, conflicto 409 y fecha inválida 400. | OK |
| Asignaturas, validación y privacidad | Smoke: alta/listado/edición, duplicado/datos inválidos y 404 para otro usuario. | OK |
| Ponderación total y concurrencia | Smoke rechaza exceso; integración compite transacciones reales en PostgreSQL. | OK |
| Solo un examen, también ante SQL directo | V2 instalada y cuatro pruebas PostgreSQL de insert/updates aprobadas. | OK |
| Nota 5.57 truncada, promedio necesario y final | Smoke confirma 5.5, 3.2 y final 4.8/aprobada. | OK |
| Nota fuera de rango y GRADED sin nota | Smoke devuelve 400 en ambos casos. | OK |
| Créditos 1..32767 y entero | Suite frontend cubre límites, 101 y 32767; lint y build limpios. | OK automatizado; UI manual pendiente |
| Contraseña UTF-8 72 bytes | Suite frontend cubre ASCII/eñe/emoji y bytes límite. | OK automatizado; UI manual pendiente |
| Segundo aviso igual, sin fecha, anticipación negativa | Smoke: 409 y dos casos 400. | OK |
| Descartar aviso/propiedad | Smoke: 204, deja de aparecer; usuario ajeno recibe 404. | OK |
| Reprogramar aviso descartado | Prueba PostgreSQL persistente, cambio de fecha y rollback. | OK |
| Zona horaria en medianoche/verano | Pruebas de reloj fijo y configuración; integración HTTP usa el reloj del negocio. | OK automatizado |
| Fechas académicas, filtros, tipo inválido | Smoke verifica CRUD/rango, inclusión/exclusión y 400. | OK |
| Privacidad de resumen y cascada | Smoke: ajeno recibe 404, periodo borrado elimina sus dependencias. | OK |
| IP falsificada / perfil proxy | Pruebas JUnit con RemoteIpValve real; no se configuró un proxy externo. | OK automatizado; proxy real/firewall pendiente |
| Limitación de intentos login | Prueba de integración incluida en las 8 pruebas HTTP. | OK |

## Pendiente / límites de la sesión

- No se usó navegador gráfico; las rutas frontend se validaron por pruebas, lint, tipos y build.
- No hay proxy inverso real en este entorno; la parte de red/firewall queda pendiente.
- El smoke test usa la instancia de API que ya estaba escuchando en 8080; no se detuvo ni
  sustituyó el proceso iniciado por el usuario.
- No se eliminó la base de pruebas ni los registros de las dos cuentas del smoke test.
- Las pruebas de acceso/proxy reales deben repetirse en el despliegue objetivo.

Volver al [índice QA y correcciones](README.md).
