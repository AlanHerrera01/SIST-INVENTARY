# Convenciones de estructura y configuración

## Frontend

`src/app/` contiene composición de la aplicación; `src/components/ui/` contiene componentes shadcn/ui mantenidos como código fuente; `src/lib/` contiene utilidades compartidas; `src/styles/` contiene tokens y estilos globales. No se crean módulos funcionales vacíos para simular implementación. Los módulos futuros irán en `src/features/<nombre>/` con dependencias hacia componentes compartidos, no al revés.

Componentes React en PascalCase, funciones y hooks en camelCase, archivos UI compartidos en kebab-case y alias `@/` hacia `src/`. TypeScript estricto; evitar `any`. Formato UTF-8, dos espacios y fin de línea LF. `components.json`, alias y tokens CSS permiten agregar componentes desde el registro oficial de shadcn/ui. El botón inicial se incorpora manualmente conforme al patrón shadcn con Radix Slot y class-variance-authority; no se necesita un servidor shadcn ni un paquete de componentes cerrado.

La pantalla inicial únicamente comprueba salud. No crea sesiones, tokens ni datos de inventario. El navegador nunca determina roles o autorización efectiva.

## Backend

Paquete raíz `com.veris.inventory`; `platform/config` y `platform/health` son infraestructura técnica. Los futuros módulos de dominio podrán organizarse por capacidad bajo ese paquete, con capas internas propias. No hay entidades, repositorios, controladores de negocio ni tablas de inventario en Fase 01.

Clases en PascalCase, métodos en camelCase y constantes en UPPER_SNAKE_CASE; paquetes en minúsculas. Java 21, cuatro espacios en archivos nuevos, inyección por constructor. SQL futuro en snake_case. API futura en `/api/v1`, rutas de recursos en kebab-case, JSON de dominio en snake_case conforme al contrato detallado que se apruebe; esta convención no cambia los formatos estándar de Actuator/OIDC.

Configuración común en `application.yml`, sobreescrita por variables de entorno o configtree. No crear perfiles con contraseñas predeterminadas, `ddl-auto=create`, generación automática de tablas o cuentas de desarrollo dentro del código. No agregar migraciones de dominio en esta fase; Spring tiene inicialización SQL deshabilitada.

## Errores y logs

Activado soporte estándar Problem Details de Spring MVC para errores compatibles. Backend no expone mensajes internos ni trazas mediante `server.error`; un 404 de infraestructura puede usar el formato nativo de Boot. En módulos posteriores, los errores REST tendrán `type`, `title`, `status`, `detail` seguro, `instance` y extensiones `code`, `correlation_id`, `errors`, con estados 400/401/403/404/409/412/422/428 según Fase 00. No se implementa un controlador genérico que invente errores de negocio.

Backend emite JSON Logstash a stdout; Keycloak emite JSON; MySQL y Vite conservan sus formatos propios. Docker rota logs a tres archivos de 10 MB por servicio. No se afirma tener auditoría de negocio, trazas distribuidas ni correlación por petición: quedan para fases posteriores. No registrar contraseñas, cookies, cabeceras de autorización ni contenidos de documentos.

## Salud

- Backend liveness: solo proceso/ciclo de vida. Una caída de Keycloak no debe reiniciar el backend.
- Backend readiness: estado de aplicación + consulta MySQL + discovery OIDC con issuer esperado. Cada consulta HTTP a Keycloak tiene límite de conexión y lectura de dos segundos. No realiza login ni pide tokens.
- Keycloak readiness: endpoint de management 9000; salud y métricas habilitadas para incluir comprobaciones de base.
- MySQL: consulta autenticada a la base `inventory`, no solo un ping al puerto.
- Frontend: HTTP 200 de Vite. El smoke test además valida el proxy al backend.

Readiness fallida excluye disponibilidad; los healthchecks de Compose no implementan recuperación o reinicio automático ante una dependencia que cae después de arrancar.

## Infraestructura

`compose.yml` es exclusivamente desarrollo. Scripts shell con LF, secretos sin BOM, imágenes con etiqueta explícita y dependencias frontend fijadas por `package-lock.json`. No cambiar un lock manualmente. El wrapper fija Maven; Spring Boot gestiona versiones transitivas. Etiquetas de imagen y sistema operativo base pueden recibir cambios upstream: para releases reproducibles a nivel binario se fijarán digests tras verificar las imágenes en el motor objetivo.

Verificar antes de una revisión: frontend lint/build, backend verify, Compose config y smoke sobre servicios reales. Registrar verificaciones bloqueadas con causa; no sustituir una prueba de MySQL por H2 sin declararlo.
