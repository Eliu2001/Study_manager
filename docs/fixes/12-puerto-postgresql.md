# Arranque: autenticación contra una instancia PostgreSQL distinta

## Diagnóstico

El error SQLSTATE `28P01` no era de compilación ni de la migración V2: el backend
intentaba autenticar `study_manager` en `localhost:5432`. Ese puerto estaba ocupado
por un proceso PostgreSQL de Windows, mientras que `study-manager-db` publicaba
`127.0.0.1:5433` hacia el puerto 5432 del contenedor. La autenticación dentro del
contenedor con sus credenciales existentes funcionaba.

## Solución

- Se conserva el puerto local 5433 elegido para Docker, sin detener ni modificar
  el PostgreSQL de Windows.
- Se añade `DB_PORT=5433` al `.env` local y al ejemplo; no se cambian contraseñas.
- Compose usa ese puerto publicado; el backend y el perfil de pruebas lo leen
  mediante `${DB_PORT:5432}`. El valor por defecto 5432 mantiene la CI existente.
- El detector de base de pruebas valida el puerto y sigue admitiendo únicamente
  la base con sufijo `_test`, nunca la base de desarrollo.

No hace falta borrar ni recrear volúmenes. Si se cambia el valor de `DB_PORT` en
otro momento, actualizar el contenedor con `docker compose up -d db` y reiniciar
el backend para que ambos usen el mismo puerto.

## Comprobación

`docker compose ps` debe mostrar `127.0.0.1:5433->5432/tcp`. Reiniciar con:

```powershell
.\mvnw.cmd spring-boot:run
```

Las pruebas `TestDatabaseTest` cubren el puerto alternativo, el rechazo de una
URL que apunte a otro puerto y los puertos inválidos. La conexión JDBC se puede
comprobar sin arrancar Spring ni aplicar migraciones; el arranque completo sí
ejecuta Flyway y debe respetar las precauciones del hallazgo 02.

Los avisos HTTP 401 de repositorios Maven son independientes de este error.
`BUILD SUCCESS` con DevTools no garantiza que Spring haya arrancado: comprobar
`Started StudyManagerApplication` y la respuesta de `/actuator/health`.

Validación realizada: autenticación JDBC correcta desde el host a `localhost:5433`
con las credenciales del proyecto, sin SQL de escritura ni migraciones; 9 pruebas de
`TestDatabaseTest` correctas y `docker compose config --quiet` satisfactorio.
No se inició Spring desde la sesión de diagnóstico: el usuario debe reiniciarlo
para aplicar su configuración y ejecutar las migraciones pendientes.
