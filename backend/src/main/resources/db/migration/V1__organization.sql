-- Fase 02 — Migración V1.
-- Área: organización, centros y ubicaciones (Fase 00 §3).
--
-- Convenciones del esquema:
--   * Identificadores snake_case; InnoDB; utf8mb4 / utf8mb4_0900_ai_ci.
--   * Identificador de fila: BINARY(16) con DEFAULT (UUID_TO_BIN(UUID(), 1)).
--     El intercambio de bytes temporal de UUID v1 mejora la localidad de
--     inserción en el índice agrupado de InnoDB.
--   * Fechas-hora: DATETIME(6) gestionado en UTC por la aplicación y el
--     conector; TIMESTAMP termina en 2038 y no es apto para retención.
--   * Sin disparadores ni procedimientos almacenados: la imagen oficial
--     mysql:8.4 mantiene el registro binario activo y el usuario de
--     aplicación no posee SUPER, por lo que no puede crearlos.
--
-- Esta migración crea estructura; no inserta datos de negocio.

-- Organización: raíz de aislamiento (Fase 00 §1.1). Se espera una fila por
-- despliegue; la creación de esa fila es una operación de administración de
-- fases posteriores, no de migración.
CREATE TABLE organization (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  name VARCHAR(200) NOT NULL,
  asset_code_prefix VARCHAR(10) NOT NULL DEFAULT 'ATI',
  presentation_timezone VARCHAR(64) NOT NULL DEFAULT 'America/Guayaquil',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_organization PRIMARY KEY (id),
  CONSTRAINT uq_organization_name UNIQUE (name),
  -- Prefijo y zona son propuestas de Fase 00 §7 y §9, pendientes de D-09 y D-12.
  CONSTRAINT ck_organization_prefix CHECK (asset_code_prefix <> ''),
  CONSTRAINT ck_organization_timezone CHECK (presentation_timezone <> '')
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Centro: código único por organización. No se elimina si tiene historial:
-- se inactiva mediante center_state (Fase 00 §3).
CREATE TABLE center (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  organization_id BINARY(16) NOT NULL,
  code VARCHAR(20) NOT NULL,
  name VARCHAR(200) NOT NULL,
  center_state VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_center PRIMARY KEY (id),
  CONSTRAINT uq_center_organization_code UNIQUE (organization_id, code),
  -- Destino de claves foráneas compuestas (organization_id, center_id) que
  -- usan asset, transfer, custodian, audit_event y notification_event para
  -- garantizar en la base que el centro pertenece a la organización declarada.
  CONSTRAINT uq_center_organization_id UNIQUE (organization_id, id),
  CONSTRAINT fk_center_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
  CONSTRAINT ck_center_state CHECK (center_state IN ('ACTIVO', 'INACTIVO'))
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Ubicación: pertenece a un centro; la ubicación superior opcional debe
-- pertenecer al mismo centro, forzado con la clave compuesta. La ausencia de
-- ciclos no es verificable en SQL y queda a cargo de la aplicación (§3).
CREATE TABLE location (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  center_id BINARY(16) NOT NULL,
  name VARCHAR(200) NOT NULL,
  location_type VARCHAR(50) NOT NULL,
  parent_location_id BINARY(16) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_location PRIMARY KEY (id),
  -- Destino de la clave compuesta de padre (mismo centro) para tablas que
  -- referencian ubicación junto con su centro (asset).
  CONSTRAINT uq_location_center_id UNIQUE (center_id, id),
  CONSTRAINT fk_location_center FOREIGN KEY (center_id) REFERENCES center (id),
  CONSTRAINT fk_location_parent FOREIGN KEY (center_id, parent_location_id) REFERENCES location (center_id, id),
  CONSTRAINT ck_location_type CHECK (location_type <> '')
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;
