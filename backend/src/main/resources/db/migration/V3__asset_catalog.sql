-- Fase 02 — Migración V3.
-- Área: catálogo dinámico de activos (Fase 00 §3, campos por categoría D-04).
--
-- El catálogo es dinámico por organización: categorías con jerarquía opcional
-- y definición de atributos tipados por categoría. Los valores por activo se
-- guardan en V5 (asset_attribute_value) porque requieren la tabla asset.

-- Categoría de activo. La jerarquía es opcional y no admite ciclos en SQL:
-- la verificación es responsabilidad de la aplicación (§3).
CREATE TABLE asset_category (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  organization_id BINARY(16) NOT NULL,
  code VARCHAR(50) NOT NULL,
  name VARCHAR(200) NOT NULL,
  parent_category_id BINARY(16) NULL,
  category_state VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_asset_category PRIMARY KEY (id),
  CONSTRAINT uq_asset_category_organization_code UNIQUE (organization_id, code),
  -- Destino de la clave compuesta (organization_id, category_id) de asset.
  CONSTRAINT uq_asset_category_organization_id UNIQUE (organization_id, id),
  CONSTRAINT fk_asset_category_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
  CONSTRAINT fk_asset_category_parent FOREIGN KEY (organization_id, parent_category_id) REFERENCES asset_category (organization_id, id),
  CONSTRAINT ck_asset_category_state CHECK (category_state IN ('ACTIVO', 'INACTIVO'))
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Atributo definido para una categoría. value_type controla la columna de
-- valor usada en asset_attribute_value; LISTA exige allowed_values.
CREATE TABLE asset_category_attribute (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  category_id BINARY(16) NOT NULL,
  attribute_code VARCHAR(50) NOT NULL,
  attribute_name VARCHAR(200) NOT NULL,
  value_type VARCHAR(20) NOT NULL,
  required_flag TINYINT(1) NOT NULL DEFAULT 0,
  allowed_values JSON NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_asset_category_attribute PRIMARY KEY (id),
  CONSTRAINT uq_asset_category_attribute UNIQUE (category_id, attribute_code),
  CONSTRAINT fk_asset_category_attribute_category FOREIGN KEY (category_id) REFERENCES asset_category (id),
  CONSTRAINT ck_asset_category_attribute_value_type CHECK (value_type IN ('TEXTO', 'NUMERO', 'FECHA', 'BOOLEANO', 'LISTA')),
  CONSTRAINT ck_asset_category_attribute_required CHECK (required_flag IN (0, 1)),
  CONSTRAINT ck_asset_category_attribute_allowed_values CHECK (
    (value_type = 'LISTA' AND allowed_values IS NOT NULL)
    OR (value_type <> 'LISTA' AND allowed_values IS NULL)
  )
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;
