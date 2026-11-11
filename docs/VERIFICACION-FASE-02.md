# Resultados de verificación — Fase 02

Fecha: 11 de noviembre de 2026, America/Guayaquil.

## Entorno y alcance

MySQL Community 8.4.13 real, ejecutado temporalmente en Windows sobre `127.0.0.1:3307`; JDK Temurin 21.0.13; Maven Wrapper 3.10.0; Spring Boot 4.1.2 y Flyway 12.4.0. Sin servicios de negocio ni datos de producción. Se descargó el paquete oficial de MySQL y se comprobó su checksum publicado antes de utilizarlo.

El esquema contiene **38 tablas de aplicación y 14 migraciones**; la tabla técnica de Flyway eleva el total físico a 39. La suite comprueba que no se precargue información empresarial antes de insertar sus fixtures sintéticos.

## Pruebas de esquema ejecutadas

Suite `MySqlSchemaIT`: **12 pruebas, 0 fallos, 0 errores, 0 omitidas**. Última ejecución tras reconciliar contadores durante migración: 83,62 segundos; Maven `BUILD SUCCESS`.

| Verificación | Resultado |
|---|---|
| V1–V14 desde base vacía, validate y nueva ejecución sin cambios | Aprobada. |
| InnoDB y ausencia de datos precargados | Aprobada. |
| `clean` deshabilitado | Aprobada: la operación se rechaza. |
| Actualización V11 → V14 con activo existente | Aprobada: preserva código/datos y coincide la estructura de columnas. |
| Contador posterior a datos migrados | Aprobada: un código existente 1 hace que el siguiente asignado sea 2. |
| Referencias huérfanas o entre organizaciones | Rechazadas mediante FKs. |
| Reserva exclusiva, liberación e historial | Aprobada: una activa y múltiples históricas. |
| Ubicación física en destino con centro registral en origen | Aprobada; ubicación de otro centro físico rechazada. |
| Código duplicado, verificador inválido, cambio/eliminación de identidad | Rechazados. La baja no libera el código. |
| Reescritura/eliminación de auditoría | Rechazadas. |
| Numeración concurrente | 32 asignaciones confirmadas por 8 trabajadores, sin duplicados. |
| Rollback de número no publicado y agotamiento del contador | Aprobados. |
| Rollback conjunto de activo, auditoría y outbox | Aprobado. |
| Recepción de ítem perteneciente a otro traslado | Rechazada. |
| Atributo con tipo incompatible | Rechazado. |
| Firma con hash incorrecto y modificación de evidencia final/acta emitida | Rechazadas. |
| Reclamo SKIP LOCKED y finalización con token obsoleto | Trabajadores independientes; token obsoleto no modifica filas. |

Las filas describen aserciones agrupadas en doce métodos, no doce aserciones individuales. Los datos sintéticos y resultados locales quedan fuera de Git.

## Reversibilidad operativa

`infra/scripts/test-schema-recovery.ps1` ejecutado con una base de la suite: **PASS**. Dump y restauración en base nueva; coincidencia de conteos/checksums para las 39 tablas y coincidencia de cantidades de rutinas/triggers. Fuente intacta y destino temporal eliminado. Reporte local: `backend/target/recovery-test/result.txt`.

Este ensayo valida recuperación de base y metadatos; no implica rollback transaccional del DDL de MySQL ni restauración de archivos externos. La estrategia y sus limitaciones están en [operación del esquema](OPERACION-ESQUEMA-FASE-02.md).

## Docker

Docker Desktop localizado en instalación por usuario (`AppData/Local/Programs/DockerDesktop`); motor Linux 29.9.3 y Compose 5.7.0 conectados. `docker compose config --quiet` aprobado. Se conserva un único `compose.yml`, según indicación del usuario.

Verificación de arranque completo en curso al redactar esta sección; se actualizará con el resultado observado. No confundir las pruebas MySQL aprobadas con el estado de los cuatro contenedores.

## Incidencias corregidas

- V5 utilizaba `last_value`, reservado por MySQL; corregido a `current_value` antes de poder aplicar esa migración.
- Las banderas `NOT NULL` impedían liberar/cerrar registros; V12 las convierte en columnas derivadas de fechas/estados, conservando historial.
- La ubicación física estaba forzada al centro registral; V12 separa ambos conceptos manteniendo integridad organizacional.
- V13 añade correspondencia por organización y por traslado/ítem/firmante para evitar enlaces a registros válidos pero incorrectos.
- V14 conserva códigos emitidos, protege historial y reconcilia los contadores con datos existentes al actualizar.
- La conexión local MySQL utiliza recuperación de clave pública para el handshake sin TLS del entorno de desarrollo. Producción debe usar TLS validado.

No se implementó ningún servicio de negocio, flujo de autenticación, envío de notificaciones ni firma electrónica funcional.
