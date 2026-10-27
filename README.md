# study-manager

Gestor de estudios y entregas: periodos, asignaturas, evaluaciones con ponderaciones, notas,
promedio necesario para aprobar, fechas academicas y recordatorios.

Backend en **Java 21 + Spring Boot** con **PostgreSQL** (Docker) y migraciones con **Flyway**.
La interfaz sera un frontend en React que consume esta API.

## Requisitos

- Java 21 (por ejemplo Temurin; se instala facil con [SDKMAN](https://sdkman.io))
- Docker con Docker Compose (en Linux puede requerir `sudo`)

## Puesta en marcha

1. Crea tu archivo de configuracion local a partir del ejemplo:

   ```bash
   cp .env.example .env
   ```

   Edita `.env`: cambia `DB_PASSWORD` y genera un secreto para `JWT_SECRET`
   (minimo 32 caracteres), por ejemplo con `openssl rand -base64 48`.
   El archivo `.env` esta en `.gitignore` y no debe subirse al repositorio.

2. Levanta la base de datos:

   ```bash
   docker compose up -d
   docker compose ps        # espera a que diga "healthy"
   ```

3. Ejecuta la aplicacion (queda en http://localhost:8080):

   ```bash
   ./mvnw spring-boot:run
   ```

   Flyway crea las tablas la primera vez.

4. Comprueba que todo funciona con la prueba de humo (necesita la app corriendo):

   ```bash
   ./scripts/smoke-test.sh
   ```

Para apagar la base de datos conservando los datos: `docker compose down`.
`docker compose down -v` tambien borra los datos.

## Pruebas

```bash
./mvnw test -Dtest='GradeCalculator*Test,ReminderRulesTest'
```

Esas pruebas no necesitan la base de datos. `StudyManagerApplicationTests` arranca toda la
aplicacion, asi que si la ejecutas necesita la base de datos levantada y el archivo `.env`.

## API

Todas las rutas, salvo las de autenticacion y `/actuator/health`, exigen la cabecera
`Authorization: Bearer <token>`. Cada usuario solo ve y modifica sus propios datos: pedir un
recurso ajeno devuelve 404.

| Metodo | Ruta | Descripcion |
|---|---|---|
| POST | `/api/auth/register` | Crear cuenta |
| POST | `/api/auth/login` | Obtener el token (dura 60 minutos) |
| GET | `/api/users/me` | Datos del usuario actual |
| POST, GET | `/api/terms` | Crear y listar periodos |
| GET, PUT, DELETE | `/api/terms/{id}` | Ver, editar y borrar un periodo |
| POST, GET | `/api/terms/{termId}/courses` | Crear y listar asignaturas de un periodo |
| GET, PUT, DELETE | `/api/courses/{id}` | Ver, editar y borrar una asignatura |
| POST, GET | `/api/courses/{courseId}/assessments` | Crear y listar evaluaciones |
| GET, PUT, DELETE | `/api/assessments/{id}` | Ver, editar y borrar una evaluacion |
| POST, GET | `/api/assessments/{id}/reminders` | Crear y listar recordatorios de una evaluacion |
| DELETE | `/api/reminders/{id}` | Borrar un recordatorio |
| GET | `/api/reminders/upcoming` | Avisos vigentes hoy |
| POST | `/api/reminders/{id}/dismiss` | Marcar un aviso como visto |
| POST, GET | `/api/academic-dates` | Crear y listar fechas academicas (`?from=&to=` opcionales) |
| GET, PUT, DELETE | `/api/academic-dates/{id}` | Ver, editar y borrar una fecha |
| GET | `/api/courses/{id}/summary` | Nota actual, nota final y promedio necesario |
| GET | `/api/terms/{id}/summary` | Promedio del periodo y resumen de sus asignaturas |
| GET | `/api/summary` | Promedio acumulado de todos los periodos |

Los `PUT` reemplazan el recurso completo: los campos que no se envian quedan vacios.

## Reglas de notas

- Las notas van de 1,0 a 7,0. Se aceptan dos decimales, pero **solo cuenta el primero, sin redondear**
  (5,57 se guarda como 5,5 y una nota final de 3,97 queda en 3,9).
- La nota de aprobacion por defecto es 4,0 y se puede cambiar por asignatura.
- Las ponderaciones de una asignatura no pueden sumar mas de 100% y solo puede haber un examen.
- El promedio necesario es lo que falta para llegar a la nota de aprobacion, dividido por la
  ponderacion pendiente, redondeado hacia arriba.
- El promedio de periodos y el acumulado son ponderados por creditos solo si todas las asignaturas
  tienen creditos; si falta alguno, es un promedio simple.

## Seguridad

- Contrasenas cifradas con BCrypt (maximo 72 bytes).
- Autenticacion sin sesiones, con JWT firmado (HS256) cuyo secreto sale de `JWT_SECRET`.
- CORS abierto solo para el frontend (`CORS_ALLOWED_ORIGINS`, por defecto `http://localhost:5173`).
- Las respuestas de error no incluyen trazas ni detalles internos.
