# Hallazgo 08: disponibilidad real de la base de pruebas

`TestDatabase.isAvailable` carga la configuracion de Spring Boot con el perfil
`test`, incluido `optional:file:.env[.properties]`. Las variables de entorno y
propiedades del sistema mantienen la precedencia de Spring sobre `.env`.
No inicia el contexto, no crea bases, no ejecuta migraciones ni borra datos.

## Variables exactas (contrato con CI)

| Variable | Significado |
| --- | --- |
| `DB_NAME` | Nombre base, sin agregar `_test`; entre 1 y 58 caracteres ASCII alfanumericos o `_`. |
| `DB_PORT` | Puerto local de PostgreSQL (1 a 65535); por defecto 5432 para compatibilidad con CI. El entorno Docker local usa 5433. |
| `DB_USER` | Usuario PostgreSQL con acceso a la base de pruebas. |
| `DB_PASSWORD` | Clave no vacia del usuario PostgreSQL. |
| `JWT_SECRET` | Secreto JWT requerido al arrancar la aplicacion en las pruebas de integracion; usar un secreto exclusivo de pruebas que cumpla los requisitos de la aplicacion. |
| `TEST_DB_REQUIRED` | `true` en CI; `false` o ausente permite omitir localmente las pruebas dependientes de base. Otros valores provocan error. |

Estas propiedades se pueden proporcionar mediante el entorno o el archivo `.env`
de la raiz, con sintaxis Java properties (`NOMBRE=valor`), como ya configura Spring.
No se deben registrar ni versionar credenciales reales.

El destino permitido es exactamente
`jdbc:postgresql://localhost:${DB_PORT:5432}/${DB_NAME}_test`. Por ejemplo, `DB_NAME=study_manager`
selecciona `study_manager_test`, nunca `study_manager`. No hay fallback a la base
de desarrollo. Cualquier URL efectiva diferente (incluso un override mediante
`SPRING_DATASOURCE_URL`) se rechaza antes de abrir JDBC. Los nombres se validan
para impedir parametros inyectados o truncamiento de identificadores PostgreSQL.

Se abre una conexion JDBC con las credenciales efectivas `spring.datasource.username`
y `spring.datasource.password`, se valida y se cierra. Los tiempos de conexion,
lectura y validacion estan limitados a dos segundos cada uno. Un puerto abierto
sin una base accesible y autenticacion correcta ya no habilita las pruebas.

## Omision local y obligatoriedad en CI

Sin `TEST_DB_REQUIRED=true`, configuracion incompleta, credenciales incorrectas o
base ausente devuelven `false` y JUnit omite las clases que usan `@EnabledIf`.
En CI se deben definir **DB_NAME, DB_USER, DB_PASSWORD, JWT_SECRET y
TEST_DB_REQUIRED=true**, y provisionar previamente la base `${DB_NAME}_test` en
`localhost` en el puerto configurado (5432 en CI). Si no se puede validar, la condicion lanza un error explicito
en lugar de omitir las pruebas, evitando un resultado verde por falta de base.
El mensaje no incluye credenciales, URL real ni la excepcion JDBC original.

`TestDatabaseTest` cubre resolucion de placeholders, URL exclusiva, credenciales,
timeouts, cierre, rechazo previo de configuraciones inseguras y ambos modos sin
necesitar PostgreSQL. No se ejecuto Maven como parte de esta correccion.
