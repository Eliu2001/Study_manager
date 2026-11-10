# Hallazgo 02: un examen por asignatura

## Alcance

`V2__unique_exam_per_course.sql` incorpora el índice único parcial PostgreSQL
`uq_assessments_exam_per_course` sobre `assessments(course_id)` únicamente cuando
`type = 'EXAM'`. Garantiza **como máximo un examen** por asignatura, incluso ante
escrituras directas o concurrentes. También cubre actualizaciones del tipo o de
la asignatura. No exige que exista un examen y permite múltiples `SUMMATIVE`.

No modifica V1, `AssessmentService`, ni datos existentes. La presentación del
error al cliente queda fuera de este cambio; PostgreSQL rechaza la infracción
con SQLSTATE `23505`.

## Datos previos: diagnóstico no destructivo

Antes del despliegue, ejecutar estas consultas en la base de destino con una
cuenta de lectura. No modifican registros:

```sql
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;

SELECT course_id, count(*) AS cantidad_examenes,
       array_agg(id ORDER BY id) AS assessment_ids
FROM assessments
WHERE type = 'EXAM'
GROUP BY course_id
HAVING count(*) > 1
ORDER BY course_id;

SELECT a.id, a.course_id, a.name, a.type, a.weight,
       a.status, a.grade, a.due_date
FROM assessments a
JOIN (
    SELECT course_id
    FROM assessments
    WHERE type = 'EXAM'
    GROUP BY course_id
    HAVING count(*) > 1
) duplicados ON duplicados.course_id = a.course_id
WHERE a.type = 'EXAM'
ORDER BY a.course_id, a.id;

COMMIT;
```

Si aparecen filas, **detener el despliegue**. Conservar el resultado en un lugar
seguro y obtener una copia de respaldo; solicitar al responsable de los datos
una decisión explícita sobre cada caso. No borrar, fusionar, cambiar tipos ni
seleccionar automáticamente un examen como válido. Esta entrega no incluye
corrección de datos. Solo reintentar cuando un procedimiento separado y aprobado
haya resuelto los conflictos y la consulta ya no devuelva filas.

## Despliegue

1. Respaldar la base y probar la migración en una copia representativa.
2. Coordinar una ventana de mantenimiento y detener las escrituras de todas las
   instancias y clientes externos; repetir el diagnóstico antes de migrar.
3. Desplegar la aplicación: Flyway aplica V2 después de V1, sin alterar su checksum.
   El índice se crea de forma normal, no `CONCURRENTLY`: su creación bloquea
   escrituras durante el proceso. Dimensionar la ventana según el volumen.
4. Si existen duplicados, la creación falla y Flyway impide arrancar la nueva
   aplicación. En PostgreSQL esta migración es transaccional: no elimina datos
   ni deja aplicada parcialmente V2. Revisar el error y los datos antes de
   reintentar; no usar `clean`, modificar V1 ni marcar V2 como aplicada manualmente.
5. Verificar el índice y el registro exitoso de V2 antes de reabrir escrituras:

```sql
SELECT indexname, indexdef
FROM pg_indexes
WHERE schemaname = current_schema()
  AND tablename = 'assessments'
  AND indexname = 'uq_assessments_exam_per_course';

SELECT version, description, success
FROM flyway_schema_history
WHERE version = '2';
```

La definición debe mostrar `UNIQUE`, la clave `course_id` y el predicado
`type = 'EXAM'`; V2 debe figurar con `success = true`.

## Validación

`UniqueExamPerCourseIntegrationTest` usa el perfil `test`, el mismo mecanismo
`@EnabledIf(TestDatabase#isAvailable)` de las integraciones existentes y SQL
directo mediante `JdbcTemplate`. Cada prueba revierte sus propios datos mediante
`@Transactional`; no limpia datos ajenos ni depende del servicio.

Cobertura: rechazo de un segundo examen, conversión de sumativa a examen y
traslado a una asignatura con examen; aceptación de múltiples sumativas, un examen
en cada asignatura y edición del examen existente. Los rechazos verifican tanto
SQLSTATE `23505` como el nombre del índice, para no confundir otra restricción.

Para ejecutarlas posteriormente con PostgreSQL de pruebas disponible y la
configuración habitual: `./mvnw -Dtest=UniqueExamPerCourseIntegrationTest test`
(Windows: `./mvnw.cmd -Dtest=UniqueExamPerCourseIntegrationTest test`). Si la base
no está disponible se omiten según `TestDatabase`; una omisión no valida la
migración. Estas pruebas requieren PostgreSQL real, no H2.

No se ejecutó Maven como parte de este cambio; la ejecución de las pruebas y la
validación del despliegue quedan pendientes.
