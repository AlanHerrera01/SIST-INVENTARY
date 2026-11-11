# Migración, pruebas y reversibilidad operativa

## Aplicación

Existe un único `compose.yml`, para desarrollo. La prueba de esquema usa cualquier servidor **desechable** MySQL Community 8.4 proporcionado explícitamente. No se necesita un segundo Compose.

Spring Boot aplica Flyway al arrancar: `db/migration`, validación activada, `clean` deshabilitado, sin baseline automático ni versiones fuera de orden. `spring.sql.init.mode=never` deshabilita únicamente la inicialización genérica de Spring, no Flyway.

Desde raíz, preparar `.env` y secretos con `infra/scripts/init-dev.ps1`; ejecutar `docker compose config --quiet` y `docker compose up --build --wait --wait-timeout 600`. El primer arranque crea esquema vacío. El esquema `keycloak` y sus usuarios técnicos son independientes de `inventory`.

La cuenta migradora necesita DDL, DML, CREATE ROUTINE, ALTER ROUTINE, EXECUTE y TRIGGER en su esquema. Compose habilita `log_bin_trust_function_creators=1` y UTC para desarrollo. En un servidor administrado, el DBA debe definir los privilegios y la política de binlog antes de migrar; no se concede SUPER a la cuenta de aplicación ni se modifica automáticamente el servidor productivo.

No editar migraciones que ya hayan sido aplicadas en una instalación entregada. Si V12/V13 encuentra filas incompatibles, fallará: revisar las referencias detectadas, respaldar y corregir datos mediante un cambio aprobado; no deshabilitar claves foráneas para forzar una migración.

## Pruebas reales

Requisitos: Java 21, Maven Wrapper, MySQL 8.4 desechable y una cuenta técnica que pueda crear/eliminar bases de prueba y sus objetos. No usar credenciales de producción. La URL debe apuntar a ese servidor; la suite crea bases con sufijo UUID, nunca ejecuta `clean` ni vacía una base recibida.

PowerShell desde `backend/`:

```powershell
$env:TEST_MYSQL_URL = 'jdbc:mysql://127.0.0.1:3307/mysql'
$env:TEST_MYSQL_USER = 'root'
# TEST_MYSQL_PASSWORD debe provenir del secreto local del servidor de pruebas.
# Omitirla solo para una instancia temporal local preparada sin contraseña.
.\mvnw.cmd -Pmysql-it verify
```

La suite falla si falta la URL, si no puede conectarse o si el motor no es MySQL 8.4; no omite silenciosamente pruebas. `mvnw verify` sin el perfil ejecuta la prueba de arranque con H2 y no acredita migraciones MySQL.

Para conservar únicamente las bases creadas por la suite para probar respaldos:

```powershell
$env:KEEP_TEST_DATABASES = 'true'
.\mvnw.cmd -Pmysql-it verify
```

`target/mysql-test-databases.txt` registra los nombres generados. Para generar nuevamente el diccionario, copiar `target/DICCIONARIO-DATOS-FASE-02.md` a `docs/` solo después de una ejecución correcta. Informes: `target/failsafe-reports/` y `target/surefire-reports/`.

La prueba de numeración ejecuta 32 transacciones mediante 8 conexiones trabajadoras, verifica unicidad, rollback y agotamiento. La prueba de leases comprueba SKIP LOCKED y rechazo de finalización con token obsoleto. La prueba de actualización parte de V11 con un activo sintético, aplica V12–V14, compara columnas con una instalación nueva y verifica que el contador avance por encima del código existente.

## Ensayo de respaldo/restauración

Desde raíz, después de conservar las bases de la suite, ejecutar con las herramientas cliente de MySQL:

```powershell
$testDatabase = (Get-Content backend/target/mysql-test-databases.txt)[0]
.\infra\scripts\test-schema-recovery.ps1 -MySqlBin 'RUTA-AL-CLIENTE-MYSQL\bin' -SourceDatabase $testDatabase -Port 3307
```

El script solo acepta nombres con prefijo de prueba, respalda con `mysqldump --single-transaction --routines --triggers --hex-blob`, restaura en una base nueva de nombre aleatorio y compara conteos y checksums de todas las tablas, incluidos datos e historial Flyway. Comprueba también cantidades de rutinas y triggers. No modifica el origen. El destino del ensayo se elimina al terminar. Artefactos locales en `backend/target/recovery-test/`, ignorados por Git; no copiar volcados con datos reales al repositorio.

## Recuperación ante una migración fallida

MySQL hace commits implícitos con DDL. Una migración con varios CREATE/ALTER **no es reversible mediante un ROLLBACK SQL**, aunque cada DDL individual pueda ser atómico. Flyway Community no ofrece aquí un undo de negocio automático. [Transacciones de migración de Flyway](https://documentation.red-gate.com/fd/migration-transaction-handling-273973399.html).

Procedimiento operativo:

1. Pausar escrituras e integraciones y registrar versión de aplicación, migraciones y punto de respaldo. Obtener respaldo consistente y verificar restauración en instancia aislada antes del cambio.
2. Aplicar y validar migración. Reanudar solo después de comprobar objetos, referencias, conteos, códigos y probes.
3. Si falla, mantener aplicación detenida. Preservar logs, esquema parcial y copia para diagnóstico. Preferir una corrección hacia adelante validada.
4. Si se revierte, restaurar el respaldo previo en otra instancia/base, validar checksums y versión Flyway, cambiar la conexión y desplegar la versión compatible. No borrar tablas de la instancia fallida como primera medida.
5. Si hubo escrituras posteriores al respaldo, reconciliarlas antes de conmutar. No bajar contadores de códigos ya publicados, no reenviar outbox a ciegas y no asumir que un proveedor externo deshizo una firma.
6. `repair` solo después de evaluar los DDL ya aplicados y documentar la corrección; no revierte objetos ni hace válida una migración alterada.

V12/V13 incorporan columnas obligatorias y generadas: un cliente que escribía las banderas antiguas no es compatible. La reversión del binario por sí sola no es suficiente; requiere versión de esquema compatible y ventana de mantenimiento. La prueba de restore acredita recuperación de base y metadatos, no restauración de binarios de archivos, configuración de Keycloak o efectos externos.

## Separación de privilegios para producción

La cuenta de desarrollo tiene privilegios para Flyway. Antes de producción, ejecutar migraciones con una identidad separada, deshabilitar Flyway en el proceso operativo y conceder a este únicamente DML necesario y EXECUTE del asignador, sin DROP/ALTER/TRIGGER. Proteger auditoría con política de retención y respaldo externo. No se crean contraseñas o cuentas empresariales en las migraciones de esta fase.
