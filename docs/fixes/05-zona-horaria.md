# Hallazgo 05: zona horaria de los avisos

## Problema

`ReminderService.upcoming` usaba `LocalDate.now()` sin zona explícita. Cerca de
medianoche UTC, un servidor en UTC podía adelantar el día respecto de Chile,
activar avisos antes de tiempo o retirar los del día todavía vigente.

## Política y solución

- La fecha de negocio para los avisos es la de `APP_TIME_ZONE`, por defecto
  `America/Santiago`, mediante `app.time-zone` en `application.properties`.
- Se utiliza una zona IANA, no un desfase fijo: Java aplica las reglas de horario
  de verano. Una zona inválida impide arrancar para evitar un fallback silencioso.
- `ReminderTimeConfig` proporciona `reminderClock`, inyectado explícitamente por
  qualifier en `ReminderService`. Se excluye como candidato genérico; el reloj
  `Clock.systemUTC()` del login no cambia ni recibe esta zona.
- `upcoming` calcula una sola fecha con ese reloj para las ventanas y `daysLeft`.
  `dismiss` usa el mismo reloj para facilitar pruebas, pero conserva `sentAt` como
  `Instant`: es un instante absoluto, no una fecha/hora local.
- Las fechas académicas siguen siendo `LocalDate`, sin conversión de zona ni
  cambios de persistencia. El formato de fechas del frontend no determina los
  avisos: la API calcula la selección y los días restantes con la zona de negocio.
  No se modifica el frontend ni `AssessmentService`.
- Es una política única del despliegue, no una zona por usuario o navegador.

## Pruebas

`ReminderServiceTimeTest` usa `Clock.fixed` y comprueba el desfase entre
medianoche UTC y Chile, que el aviso del día chileno siga visible, la apertura a
medianoche chilena, los días restantes en verano y el instante estable al descartar.
`ReminderTimeConfigTest` cubre la zona predeterminada, una alternativa, el rechazo
de zonas inválidas y la convivencia con la configuración UTC del login.

No se ejecutó Maven en esta intervención; la sesión principal realizará la
validación de compilación y pruebas.
