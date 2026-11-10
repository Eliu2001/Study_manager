# 07 - IP de login y proxy confiable

`AuthController` conserva `HttpServletRequest.getRemoteAddr()`: no analiza cabeceras del cliente. El limitador recibe la dirección decidida por el contenedor, no el primer elemento arbitrario de XFF.

## Conexión directa (predeterminado)

Sin perfil `proxy`, mantener `server.forward-headers-strategy=none` en la configuración base. Así ni la autodetección de plataformas cloud ni un `X-Forwarded-For` (XFF), `Forwarded` o `X-Real-IP` falsificado deben cambiar la IP del login. No añadir `ForwardedHeaderFilter` ni usar la estrategia `framework`: no implementan esta lista de confianza del socket.

## Activación explícita

El perfil `proxy` activa `server.forward-headers-strategy=native` y configura `RemoteIpValve` de Tomcat. **Sin `APP_PROXY_INTERNAL_PROXIES`, la lista queda vacía y no se confía en ninguna IP**, ni siquiera loopback. También se vacía `trusted-proxies`. No se heredan los rangos privados amplios de Tomcat. Activar el perfil sin indicar las IPs no recupera la IP del cliente: todos los intentos quedan agrupados por la IP del proxy.

Ejemplo PowerShell, usando un JAR ya construido y las credenciales habituales del entorno:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'proxy'
$env:APP_PROXY_INTERNAL_PROXIES = '^10[.]20[.]0[.]10$'
java -jar target/study-manager-0.0.1-SNAPSHOT.jar
```

O por argumentos (no guardar secretos en la línea de comandos):

```powershell
java -jar target/study-manager-0.0.1-SNAPSHOT.jar --spring.profiles.active=proxy '--server.tomcat.remoteip.internal-proxies=^(10[.]20[.]0[.]10|10[.]20[.]0[.]11)$'
```

Sustituir por las **IPs reales del socket del proxy**, no las IPs declaradas en XFF. La propiedad es una expresión regular Java; `[.]` evita problemas de escape de barras en `.properties`. No usar `.*`, redes privadas enteras, ni listas suministradas por usuarios. No sobrescribir `server.tomcat.remoteip.trusted-proxies` con una expresión amplia. Para proxy en el mismo host, permitir exclusivamente `^127[.]0[.]0[.]1$` (o las representaciones IPv6 concretas observadas) y restringir quién puede abrir conexiones locales: cualquier proceso con esa IP adquiere la confianza del proxy.

## Proxy de entrada

Ejemplo nginx para un único proxy de borde, con TLS configurado aparte:

```nginx
location /api/ {
    proxy_pass http://10.20.0.20:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $remote_addr;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header Forwarded "";
    proxy_set_header X-Real-IP "";
}
```

**Sobrescribir**, no copiar `$http_x_forwarded_for` ni preservar una cadena aportada por el cliente. Restringir por firewall el puerto del backend al proxy; si es local, enlazar además `server.address=127.0.0.1`. En cadenas de varios proxies, cada salto necesita una política explícita: Tomcat recorre XFF de derecha a izquierda y se detiene en el primer salto no confiable. Este ejemplo no configura automáticamente un CDN previo a nginx. Un atacante que controle el proxy o comparta su IP de salida puede falsificar XFF: la confianza por IP no sustituye al aislamiento de red.

## Pruebas de spoofing y límites

- `LoginRemoteAddressTest`: login pasa la IP del contenedor aunque las tres cabeceras declaren otra.
- `TrustedProxyConfigurationTest`: usa el binder real de Boot y `RemoteIpValve` real; verifica ausencia de válvula en modo directo, lista vacía en perfil sin configurar, aceptación exclusiva del proxy declarado, rechazo de vecinos privados/públicos, cabecera ausente y corte de cadenas ante un salto no confiable. MockMvc por sí solo no ejecuta válvulas de Tomcat.
- Prueba manual en un entorno aislado: repetir logins fallidos desde el mismo equipo alternando `X-Forwarded-For: 198.51.100.77` y `X-Forwarded-For: 198.51.100.78`. En conexión directa, y pasando por nginx con sobrescritura, no debe eludirse el bloqueo por IP; la respuesta debe llegar a 429 conforme a los límites configurados. Usar cuentas de prueba y esperar el desbloqueo entre escenarios. Desde una IP no confiable hacia el backend (solo durante la prueba), variar XFF tampoco debe cambiar la identidad. No exponer un endpoint público que devuelva la IP para depurar.
- Usuarios detrás de NAT/CGNAT comparten IP pública y, por tanto, cuota. No existe una IP individual recuperable a partir de XFF si el borde solo ve la NAT. Confiar en la IP del proxy sin activar correctamente el perfil agrupa todavía más usuarios. La IP tampoco es identidad autenticada ni evita ataques distribuidos.

Verificación de compatibilidad local: `spring-boot-tomcat-4.1.1.jar`, `TomcatServerProperties.Remoteip` y `TomcatWebServerFactoryCustomizer` enlazan `server.tomcat.remoteip.*` a la válvula; en `tomcat-embed-core-11.0.24.jar`, `setInternalProxies("")` elimina tanto regex como CIDR y `isInternalProxy` no acepta ninguna dirección. Revalidar estas pruebas al actualizar Tomcat/Boot. No se han instalado dependencias ni ejecutado Maven para este cambio.

Validación realizada: ambos tests compilados con `javac -proc:none` y el classpath de dependencias local ya existente; sus seis métodos `@Test` ejecutados mediante un lanzador temporal por reflexión: **6/6 correctos**. No equivale a ejecutar la suite completa con el motor JUnit/Maven.
