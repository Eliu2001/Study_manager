# Hallazgo 09: PostgreSQL accesible solo desde el equipo local

La publicación del puerto está limitada a
`127.0.0.1:${DB_PORT:-5433}:5432` en `docker-compose.yml`. Así, el puerto publicado
solo escucha en el loopback IPv4 del host, en lugar de todas sus interfaces.
Se mantienen el volumen `pgdata`, la imagen, las variables y el healthcheck.

## Validación de la configuración

Desde la raíz del proyecto, con Docker Compose disponible y las variables
`DB_NAME`, `DB_USER` y `DB_PASSWORD` definidas en el entorno, ejecutar:

```sh
docker compose config --quiet
```

Este comando valida la configuración sin iniciar ni detener contenedores.
Para revisar el binding se puede usar `docker compose config`: el puerto
publicado debe tener `host_ip: 127.0.0.1`, `published: "5433"` (o el `DB_PORT` configurado) y
`target: 5432`. Su salida puede incluir credenciales interpoladas; no
compartirla ni guardarla en el repositorio. Para una validación meramente
sintáctica, usar valores ficticios temporales en el entorno, sin escribir
un archivo `.env` ni utilizar secretos reales.

## Aplicación a un contenedor existente

Un simple reinicio (`docker compose restart db`) no cambia los puertos
publicados: es necesario recrear el contenedor. Cuando corresponda aplicar
el cambio, ejecutar manualmente desde el mismo proyecto y con las mismas
variables de configuración:

```sh
docker compose up -d --no-deps --force-recreate db
```

La recreación provoca una interrupción breve y reutiliza el volumen
existente `pgdata`; no borra sus datos. Mantener el mismo nombre de proyecto
Compose para reutilizar el volumen correcto. No ejecutar
`docker compose down -v`, `docker volume rm` ni otras operaciones de borrado
de volúmenes. Este procedimiento no se ejecuta como parte de esta corrección.

## Alcance local y producción

Las aplicaciones ejecutadas en el host deben conectarse mediante
`127.0.0.1:5433` (o el `DB_PORT` configurado); otros equipos ya no deben acceder mediante el puerto
publicado. Los servicios de la misma red Compose conservan el acceso a
`db:5432`: este cambio no aísla PostgreSQL de otros contenedores de esa red.

Esta configuración está orientada al desarrollo local. En producción,
evaluar no publicar el puerto y usar una red privada para los clientes,
además de autenticación, TLS y controles de red adecuados. El binding
local no sustituye esos controles; verificar también el comportamiento de
la versión y la plataforma de Docker utilizadas.
