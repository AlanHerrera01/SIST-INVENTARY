# Sistema de Inventario Tecnológico Multicentro

Estructura de desarrollo de Fase 01 y esquema de persistencia de **Fase 02**. Incluye React, TypeScript, Vite, Tailwind CSS, shadcn/ui; Spring Boot con Java 21; MySQL Community con Flyway; y Keycloak con un realm vacío de desarrollo.

Incluye 14 migraciones y 38 tablas, sin datos empresariales precargados. No hay servicios de inventario, cliente OIDC, login ni autorización funcional. El botón de la pantalla inicial consulta únicamente la disponibilidad técnica del backend.

> Nota de alcance: el árbol de trabajo actual contiene código de autenticación de una fase posterior aún sin commitear (`backend/src/main/java/com/veris/inventory/identity/`). No forma parte de Fase 01 y debe aislarse antes de cerrarla. Detalle en [verificación de Fase 01](docs/VERIFICACION-FASE-01.md).

La Fase 02 se describe en [esquema y relaciones](docs/ESQUEMA-FASE-02.md), [diccionario de datos](docs/DICCIONARIO-DATOS-FASE-02.md), [migración y recuperación](docs/OPERACION-ESQUEMA-FASE-02.md) y [verificación](docs/VERIFICACION-FASE-02.md). Se utiliza un solo `compose.yml` de desarrollo.

## Organización

| Carpeta | Contenido |
|---|---|
| `frontend/` | Aplicación web y componentes UI compartidos. |
| `backend/` | Aplicación Spring Boot, configuración y salud de dependencias. |
| `infra/` | Inicialización de MySQL, realm Keycloak y scripts locales. |
| `docs/` | Convenciones, decisiones técnicas y resultados de verificación. |
| `ESPECIFICACION-FASE-00.md` | Línea base de planificación original, conservada sin reescribir. |

## Arranque con Docker (recomendado)

Requisitos: Git, Docker Engine con contenedores Linux y Docker Compose v2.20 o superior. En Windows, Docker Desktop necesita un backend Linux operativo (por ejemplo WSL2); abrir Docker Desktop y esperar a que el motor esté listo. No es necesario instalar Node, Maven o Java para esta opción. Los puertos 5173, 8080, 8081, 9000 y 3306 deben estar disponibles.

Desde la raíz, preparar la configuración y cuatro secretos aleatorios. El script no reemplaza secretos existentes ni imprime sus valores.

PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File .\infra\scripts\init-dev.ps1
docker version
docker compose version
docker compose config --quiet
docker compose up --build --wait --wait-timeout 600
powershell -ExecutionPolicy Bypass -File .\infra\scripts\smoke.ps1
```

Linux/macOS (requiere `openssl`):

```sh
sh infra/scripts/init-dev.sh
docker version
docker compose version
docker compose config --quiet
docker compose up --build --wait --wait-timeout 600
curl --fail http://localhost:5173/
curl --fail http://localhost:8080/actuator/health/readiness
curl --fail http://localhost:9000/health/ready
curl --fail http://localhost:8081/realms/inventory-dev/.well-known/openid-configuration
curl --fail http://localhost:5173/actuator/health/readiness
```

La primera compilación necesita acceso a npm, Maven Central, Docker Hub y Quay. Compose inicia MySQL, espera salud autenticada, inicia Keycloak, espera disponibilidad y luego inicia backend y frontend. `up --wait` falla si una dependencia no llega a saludable; no interpretar un puerto abierto como arranque exitoso.

| Servicio | Dirección por defecto | Verificación |
|---|---|---|
| Frontend | http://localhost:5173 | Página inicial y botón «Comprobar servicios». |
| Backend | http://localhost:8080 | `/actuator/health/liveness` y `/actuator/health/readiness`. |
| Keycloak | http://localhost:8081 | `/realms/inventory-dev/.well-known/openid-configuration`. |
| Salud de Keycloak | http://localhost:9000 | `/health/ready`. |
| MySQL Community | localhost:3306 | Compose ejecuta `SELECT 1` con usuario `inventory`. |

Consultar estado y logs; detener sin borrar datos:

```sh
docker compose ps
docker compose logs --tail=100 backend keycloak mysql frontend
docker compose down
```

`mysql_data` conserva ambas bases y el realm. **No ejecutar `down -v` salvo que se quiera borrar deliberadamente todos los datos locales**. La inicialización de usuarios y bases solo se aplica a volúmenes nuevos. Cambiar un archivo de contraseña no cambia la contraseña existente en MySQL o Keycloak: la rotación requiere actualizar el servicio correspondiente de manera coordinada.

## Desarrollo con frontend y backend en el host

Requisitos adicionales: Node.js 24 (como el contenedor), JDK 21 LTS y `JAVA_HOME` apuntando a ese JDK. El Maven Wrapper descarga la versión fijada; no hace falta Maven global. Java 8 no sirve.

1. Ejecutar el script de preparación anterior y arrancar dependencias:

```sh
docker compose up -d --wait --wait-timeout 300 mysql keycloak
```

2. Backend en una terminal PowerShell:

```powershell
$env:DB_PASSWORD = [System.IO.File]::ReadAllText((Join-Path $PWD '.secrets/app_db_password')).Trim()
Set-Location backend
.\mvnw.cmd verify
.\mvnw.cmd spring-boot:run
```

En Linux/macOS:

```sh
export DB_PASSWORD="$(cat .secrets/app_db_password)"
cd backend
./mvnw verify
./mvnw spring-boot:run
```

Si el wrapper no tiene permiso de ejecución, usar `sh mvnw` o asignar permiso al archivo. El backend local utiliza MySQL en `localhost:3306` y el issuer local `http://localhost:8081/realms/inventory-dev`. No carga automáticamente `.env` de Compose; exportar `DB_URL`/`OIDC_ISSUER_URI` si se cambian puertos. Eliminar `DB_PASSWORD` del entorno al terminar la terminal compartida.

3. Frontend en otra terminal:

```sh
cd frontend
npm ci
npm run lint
npm run test
npm run build
npm run dev
```

En PowerShell puede utilizarse `npm.cmd` si la política de ejecución bloquea `npm.ps1`. El proxy de Vite envía `/actuator` y `/api` al backend, evitando configurar CORS permisivo. Si el backend usa otro puerto, establecer `API_PROXY_TARGET` antes de arrancar Vite. `npm run preview` sirve el build, pero no sustituye al proxy del servidor de desarrollo ni configura despliegue productivo.

## Configuración y secretos

Copiar automáticamente `.env.example` mediante el script inicial. `.env` contiene solo configuración no sensible, está ignorado por Git y Compose lo carga desde la raíz. No introducir secretos en variables `VITE_*`: pasan al navegador.

Las cuatro credenciales se generan con 256 bits aleatorios y se guardan sin BOM en `.secrets/`, fuera de Git y de los contextos de build. En Windows se restringen ACL al usuario actual y SYSTEM. En Unix el directorio es 0700; archivos 0644 para lectura de montajes por contenedores no privilegiados, protegidos en el host por el directorio privado. Verificar los permisos según el entorno. Compose secrets monta archivos: **no es una bóveda ni cifra los archivos del host**.

Spring lee `app_db_password` con configtree; Keycloak recibe sus secretos desde un script que los exporta al proceso; MySQL usa las opciones `_FILE`. Nunca se pasan secretos como argumentos de build. No copiar `.secrets` a imágenes. En producción se requiere gestor de secretos, TLS, política de acceso y rotación, fuera de esta fase.

La consola de administración de Keycloak usa `KEYCLOAK_ADMIN_USER` y el archivo privado `keycloak_admin_password`. Solo consultar el valor localmente cuando sea necesario; no pegarlo en tickets o logs. El administrador bootstrap es una cuenta técnica de desarrollo del realm maestro, no un usuario funcional de la aplicación.

Si se cambian los puertos, ajustar `.env` y los parámetros del smoke test. Cambiar `KEYCLOAK_PORT` requiere actualizar `KEYCLOAK_PUBLIC_URL` y `OIDC_ISSUER_URI`. La URL pública identifica al emisor, mientras el backend Docker consulta discovery por la red interna; compara el issuer recibido con el público. No se validan tokens en esta fase.

## Límites del entorno

Todos los puertos publicados se ligan a `127.0.0.1`. MySQL no usa TLS en la red Docker local y Keycloak se ejecuta con `start-dev`, realm sin exigencia de TLS para uso local. No exponer este Compose a redes externas ni usarlo como despliegue de producción. Los endpoints de salud no revelan detalles ni secretos; solo se exponen health e info de Actuator.

El backend no aplica autenticación funcional: no existen endpoints de negocio. Antes de incorporarlos debe implementarse la política de identidad y autorización definida en las fases correspondientes.

Consulte [arquitectura](docs/ARQUITECTURA-FASE-01.md), [convenciones](docs/CONVENCIONES.md), [decisiones de Fase 01](docs/DECISIONES-FASE-01.md) y [resultados de verificación](docs/VERIFICACION-FASE-01.md). La disponibilidad real de Docker y los servicios se registra allí; esta guía de arranque no constituye evidencia de que se hayan iniciado.
