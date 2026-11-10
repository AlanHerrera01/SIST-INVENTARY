# Decisiones técnicas — Fase 01

La solicitud de Fase 01 autoriza iniciar únicamente la estructura técnica y establece el stack. Esta autorización posterior sustituye la prohibición temporal de iniciar Fase 01 del documento original. Se preserva Fase 00 como registro histórico.

| Decisión | Estado y fundamento |
|---|---|
| React + TypeScript + Vite + Tailwind + shadcn/ui | Aprobado por solicitud Fase 01; se usa integración Tailwind vía plugin de Vite, CSS con variables y componente Button base. |
| Spring Boot + Java LTS | Aprobado; Java 21 LTS como elección técnica compatible. Maven Wrapper para no depender de Maven global. |
| MySQL Community | Aprobado por solicitud Fase 01; sustituye la propuesta PostgreSQL de §1.1 de Fase 00. No se asume compatibilidad automática de futuras migraciones/restricciones entre motores. |
| Keycloak | Aprobada preparación de infraestructura; realm vacío, persistencia en esquema y usuario separados. No se habilitan cliente OIDC, usuarios de aplicación o autenticación funcional. |
| Monolito modular + frontend separado | Se mantiene el límite arquitectónico de Fase 00; sin microservicios de dominio. |
| Identidad futura tipo BFF | Continúa como propuesta de Fase 00. No se sustituye por tokens en navegador ni se implementa en esta fase. |
| Secretos locales por archivo | Elección para desarrollo; no credenciales fijas ni secretos versionados. Configtree en backend y archivos Compose. |
| Realm y descubrimiento | `inventory-dev` permite comprobar conectividad y metadatos. El administrador bootstrap pertenece al plano de administración de Keycloak. |
| Almacenamiento documental y procesador asíncrono | Diferidos: no hacen falta para iniciar estructura sin módulos funcionales. |

Spring Boot se genera desde Spring Initializr. Las versiones exactas se registran en el POM y el lock de npm. Se consultaron fuentes primarias:

- [Spring Boot: requisitos del sistema](https://docs.spring.io/spring-boot/system-requirements.html).
- [Vite: requisitos y arranque](https://vite.dev/guide/).
- [Tailwind CSS con Vite](https://tailwindcss.com/docs/installation/using-vite).
- [shadcn/ui con Vite](https://ui.shadcn.com/docs/installation/vite).
- [Keycloak: contenedores](https://www.keycloak.org/server/containers).
- [Keycloak: bases compatibles, MySQL 8.4](https://www.keycloak.org/server/db).
- [Keycloak: comprobaciones de salud y ausencia de curl en imagen](https://www.keycloak.org/observability/health).

Continúan pendientes las decisiones de Fase 00 no resueltas expresamente por esta solicitud, especialmente roles, reglas de negocio, efectos legales de firma, retención y despliegue productivo. No se recibió un archivo adicional de instrucciones globales del proyecto.
