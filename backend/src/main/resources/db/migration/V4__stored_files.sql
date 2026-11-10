-- Fase 02 — Migración V4.
-- Área: archivos (Fase 00 §8).
--
-- Registro de archivos: el binario vive en almacenamiento de objetos, no en
-- la base. object_key es aleatorio de 128 bits y nunca se deriva del nombre
-- original; este queda saneado como metadato. El propietario lógico es
-- polimórfico (owner_entity/owner_id): la integridad referencial con el
-- propietario se verifica en la aplicación y se documenta como excepción
-- deliberada en el diccionario de datos.

CREATE TABLE stored_file (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  organization_id BINARY(16) NOT NULL,
  object_key BINARY(16) NOT NULL,
  owner_entity VARCHAR(30) NULL,
  owner_id BINARY(16) NULL,
  original_filename VARCHAR(255) NULL,
  content_type VARCHAR(100) NULL,
  size_bytes BIGINT UNSIGNED NOT NULL,
  sha256_hash CHAR(64) NULL,
  file_state VARCHAR(20) NOT NULL DEFAULT 'SUBIDA_PENDIENTE',
  analysis_result VARCHAR(200) NULL,
  analyzed_at DATETIME(6) NULL,
  classification VARCHAR(20) NOT NULL,
  retention_class VARCHAR(50) NULL,
  retention_until DATETIME(6) NULL,
  created_by_user_id BINARY(16) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_stored_file PRIMARY KEY (id),
  CONSTRAINT uq_stored_file_object_key UNIQUE (object_key),
  CONSTRAINT fk_stored_file_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
  CONSTRAINT fk_stored_file_creator FOREIGN KEY (created_by_user_id) REFERENCES app_user (id),
  -- Límite inicial propuesto de 20 MiB por archivo (§8.2), pendiente de política.
  CONSTRAINT ck_stored_file_size CHECK (size_bytes > 0 AND size_bytes <= 20971520),
  -- Estados de §8.4: errores de análisis permanecen BLOQUEADO.
  CONSTRAINT ck_stored_file_state CHECK (file_state IN ('SUBIDA_PENDIENTE', 'CUARENTENA', 'LIMPIO', 'RECHAZADO', 'BLOQUEADO')),
  CONSTRAINT ck_stored_file_owner CHECK (
    (owner_entity IS NULL AND owner_id IS NULL)
    OR (owner_entity IS NOT NULL AND owner_id IS NOT NULL)
  ),
  CONSTRAINT ck_stored_file_hash CHECK (sha256_hash IS NULL OR sha256_hash REGEXP '^[0-9a-f]{64}$'),
  CONSTRAINT ck_stored_file_classification CHECK (classification IN ('EVIDENCIA', 'ACTA', 'ETIQUETA', 'OTRO'))
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;
