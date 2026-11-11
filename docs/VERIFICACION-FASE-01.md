# Verificación — Fase 01

Fecha de ejecución: 11 de noviembre de 2026 (America/Guayaquil).
Alcance: estructura técnica de Fase 01. No incluye inventario, autenticación funcional ni reglas de negocio.

## Entorno observado

| Componente | Versión / condición |
|---|---|
| Docker Engine | 29.9.3, motor Linux |
| Docker Compose | v5.7.0 |
| Node.js (host) | 24.21.0 |
| npm (host) | 11.19.0 |
| JDK en host | No instalado; se usó un contenedor Maven con Java 21 |
| Docker CLI en `PATH` | No; se usó `%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin` |

Versiones dentro de los contenedores: Java 21.0.13, Spring Boot 4.1.2, MySQL Community 8.4.13, Keycloak 26.8.2, Node 24.21.0, Vite 8.3.7, React 19.3.1.

## Resultados

| Comprobación | Comando | Resultado observado |
|---|---|---|
| Configuración Compose | `docker compose config --quiet` | Válida, sin errores. |
| Build de imágenes | `docker compose up --build --wait --wait-timeout 900` | Backend y frontend construidos; MySQL, Keycloak, backend y frontend `healthy`. |
| Arranque limpio | `docker compose down` + `docker compose up -d --wait` | Cuatro servicios `healthy`; salida 0. |
| Smoke de extremo a extremo | `powershell -ExecutionPolicy Bypass -File .\infra\scripts\smoke.ps1` | 6/6: frontend, liveness, readiness (MySQL + OIDC), Keycloak readiness, discovery y proxy Vite → backend. |
| Frontend lint | `npm run lint` | Aprobado, sin advertencias (`--max-warnings 0`). |
| Frontend tests | `npm run test` | 4/4 aprobados (estado inicial, UP, DOWN y fallo de red). |
| Frontend build | `npm run build` | Aprobado: TypeScript y Vite 8.3.7 generan `dist/`. |
| Backend verify | `mvn -B verify` en contenedor `maven:3.10.0-eclipse-temurin-21` | `Tests run: 1, Failures: 0, Errors: 0`; `BUILD SUCCESS`. |
| Dependencias de producción | `npm audit --omit=dev` | 0 vulnerabilidades. |
| Dependencias de desarrollo | `npm audit` | 7 moderadas transitivas en la cadena `@typescript-eslint`; sin exposición en runtime. |
| Preparación de secretos en Linux/macOS | `init-dev.sh` ejecutado en Alpine 3.22 | 5 secretos de 64 bytes, `.env` creado desde `.env.example`. |

La prueba del backend usa H2 y un servidor OIDC simulado: acredita configuración técnica, no autenticación funcional ni integración real con Keycloak. La integración real se comprobó con el smoke anterior.

## Evidencia del smoke

```text
OK: Frontend
OK: Backend liveness
OK: Backend readiness (MySQL + OIDC)
OK: Keycloak readiness
OK: OIDC discovery
OK: Proxy frontend -> backend
```

## Incidencias encontradas y correcciones

1. **Descarga Maven truncada durante el build.** `dependency:go-offline` falló por `Premature end of Content-Length delimited message body`. Se sustituyó por un único `mvn package -DskipTests` con caché BuildKit de `/root/.m2` y reintentos HTTP. Los tests quedan en `mvnw verify` y en CI, no en el build de imagen.
2. **`npm ci` con `ECONNRESET`.** Se añadió caché de `/root/.npm` y reintentos de fetch al Dockerfile del frontend.
3. **Smoke test falso negativo en Windows PowerShell 5.1.** Actuator responde con `application/vnd.spring-boot.actuator.v3+json` y `Invoke-WebRequest` devuelve `byte[]`; `ConvertFrom-Json` no leía `status`. Se añadió decodificación UTF-8 explícita en `infra/scripts/smoke.ps1`.
4. **Primer `up --wait` tras el build devolvió 1 con todos los servicios ya `healthy`.** No se reprodujo: una segunda ejecución y un arranque limpio posterior devolvieron 0. Se registra como comportamiento transitorio a vigilar, no como fallo de configuración.
5. **`init-dev.sh` no generaba `oidc_client_secret` y añadía salto de línea a los secretos.** El primero impedía arrancar Compose en Linux/macOS; el segundo producía archivos de 65 bytes frente a los 64 del script de PowerShell, con riesgo de contraseña distinta según el consumidor. Se añadió el quinto secreto y se normalizó a 64 bytes sin salto; validado en Alpine 3.22.

## Alcance real del árbol de trabajo

La estructura y la configuración descritas pertenecen a Fase 01, pero **el árbol de trabajo contiene código sin commitear de fases posteriores**. En concreto, `backend/src/main/java/com/veris/inventory/identity/` implementa autenticación funcional y el `Dockerfile` copia todo `src`, por lo que la imagen verificada lo incluye.

Consecuencia medida sobre el backend en ejecución:

| Ruta | Respuesta observada |
|---|---|
| `/api/auth/csrf` | 200 |
| `/api/auth/me` | 401 sin sesión |
| `/api/v1/me` | 401 sin sesión |
| `/oauth2/authorization/inventory` | 302 hacia Keycloak |
| `/login/oauth2/code/inventory` | 302 |
| `/actuator/health/readiness` | 200 `UP` |
| `/actuator`, `/actuator/info`, `/actuator/metrics`, `/actuator/env` | 401 |

Esto **no forma parte de Fase 01** y contradice el alcance solicitado. Se preserva por ser trabajo previo del usuario, pero debe aislarse (rama o commit aparte) para que Fase 01 quede cerrada de forma exclusiva. La verificación de arranque, salud y smoke sí es válida: no depende de esos endpoints.

## Límites comprobados

- Actuator expone públicamente solo `health` (liveness y readiness); el resto de endpoints responde 401 con el código de seguridad de fase posterior. `show-details` es `never`.
- Todos los puertos publicados se ligan a `127.0.0.1`; no hay TLS en MySQL ni en Keycloak (`start-dev`).
- Los healthchecks no reinician dependencias que caen después del arranque.
- Las imágenes están fijadas por etiqueta, no por digest.
- `mysql_data` conserva las bases y el realm entre reinicios.

## Reproducción

```sh
sh infra/scripts/init-dev.sh          # o infra/scripts/init-dev.ps1 en Windows
docker compose config --quiet
docker compose up --build --wait --wait-timeout 900
powershell -ExecutionPolicy Bypass -File ./infra/scripts/smoke.ps1
```

Con el stack arriba, los gates de código son:

```sh
cd frontend && npm ci && npm run lint && npm run test && npm run build
cd backend && ./mvnw verify
```

El workflow `.github/workflows/ci.yml` ejecuta esos mismos gates y valida Compose en cada push a `main` y en cada pull request.
