# 01 — Ponderaciones concurrentes

## Problema

`AssessmentService` comprobaba la suma antes de escribir, pero dos transacciones podían leer
el mismo total y aprobar cambios cuya suma final superara el 100 %. La misma carrera permitía
crear dos exámenes. `@Transactional` por sí sola no serializa esas comprobaciones.

## Solución

- `CourseRepository.findByIdAndTermUserIdForUpdate` adquiere `PESSIMISTIC_WRITE` sobre la
  asignatura y filtra por propietario. El bloqueo dura hasta el commit o rollback.
- Crear, actualizar y borrar evaluaciones usan esa misma fila como punto de serialización,
  antes de validar sumas, comprobar exámenes o modificar evaluaciones. Lecturas no cambian.
- Actualizar y borrar primero buscan la evaluación con autorización para conocer la asignatura.
  Después del bloqueo se ejecuta `EntityManager.refresh`: volver a consultar no sirve cuando
  JPA ya mantiene una instancia antigua en su caché de primer nivel. Esto también evita que
  Hibernate omita un cambio que coincida con el valor antiguo pero no con el confirmado.
- Si otra transacción borró la evaluación durante la espera, el refresco se traduce a
  `ResourceNotFoundException`, sin recrearla. Se mantienen los filtros por usuario y los 404.
- No se modifican migraciones ni se añade reactivación de avisos.

## Archivos

- `src/main/java/cl/germain/studymanager/assessment/AssessmentService.java`
- `src/main/java/cl/germain/studymanager/course/CourseRepository.java`
- `src/test/java/cl/germain/studymanager/assessment/AssessmentServiceLockingTest.java`
- `src/test/java/cl/germain/studymanager/assessment/AssessmentConcurrencyIntegrationTest.java`
- Este documento.

## Pruebas y limitaciones

Las pruebas unitarias con Mockito comprueban orden de bloqueo/refresco/validación, borrado,
eliminación concurrente y autorización. No requieren base de datos.

La integración usa transacciones e hilos reales: dos altas de 60 %, dos altas de examen,
dos actualizaciones de 20 % a 70 % en evaluaciones distintas,
actualización de una entidad precargada después de un commit ajeno y borrado después de
precargarla. Las altas arrancan mediante una barrera; su planificación exacta depende del SO.
Las pruebas de entidad obsoleta fuerzan explícitamente la secuencia de commits.

Sigue el patrón existente: perfil `test`, PostgreSQL `${DB_NAME}_test` y condición
`TestDatabase.isAvailable`. No usa la base de producción; crea un usuario exclusivo por prueba
y elimina únicamente sus datos. Si PostgreSQL local no está disponible, la integración se omite.
El detector JDBC del hallazgo 08 comprueba la base de pruebas y sus credenciales; en CI
`TEST_DB_REQUIRED=true` impide omitir estas pruebas si falta la conexión. No se añadieron dependencias ni una base embebida;
es importante comprobar el comportamiento real de bloqueo PostgreSQL, no simular su dialecto.

No se ejecutó Maven por coordinación con el agente principal. Ejecución sugerida para este:

```powershell
.\mvnw.cmd '-Dtest=AssessmentServiceLockingTest,AssessmentConcurrencyIntegrationTest' test
```

La garantía cubre escrituras que pasan por este servicio con aislamiento habitual READ COMMITTED;
SQL directo u otros escritores deben respetar el mismo protocolo de bloqueo de asignatura.
