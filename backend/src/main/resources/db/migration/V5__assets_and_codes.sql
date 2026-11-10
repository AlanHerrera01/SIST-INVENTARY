-- Fase 02 — Migración V5.
-- Área: activos, códigos permanentes, QR, atributos, custodia, eventos y
-- reclasificaciones (Fase 00 §3, §5.1, §7; INV-01, INV-02).
--
-- Código permanente: la parte numérica (permanent_code_seq, doce dígitos) es
-- única por organización; los dos dígitos verificadores
-- (permanent_code_check = 98 - (seq * 100 mod 97)) son función de ella. El
-- prefijo visible vive en organization.asset_code_prefix (D-09 pendiente).
-- QR: identificador público opaco de 128 bits distinto del id interno (INV-01,
-- §7), rotable sin cambiar el código permanente.

-- Contadores organizacionales. Numeración transaccional sin disparadores:
-- dentro de la transacción de negocio se ejecuta
--   SELECT last_value FROM code_sequence
--     WHERE organization_id = ? AND sequence_name = ? FOR UPDATE;
--   UPDATE code_sequence SET last_value = last_value + 1 WHERE id = ?;
-- El bloqueo de fila serializa la asignación; se aceptan huecos y jamás se
-- decrece el contador (sin reutilización, §7).
CREATE TABLE code_sequence (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  organization_id BINARY(16) NOT NULL,
  sequence_name VARCHAR(50) NOT NULL,
  last_value BIGINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_code_sequence PRIMARY KEY (id),
  CONSTRAINT uq_code_sequence_organization_name UNIQUE (organization_id, sequence_name),
  CONSTRAINT fk_code_sequence_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
  -- Nombres conocidos; extender la lista exige migración explícita.
  -- ACT_NUMBER no se crea todavía: la numeración de actas depende de D-14.
  CONSTRAINT ck_code_sequence_name CHECK (sequence_name IN ('ASSET_PERMANENT_CODE', 'TRANSFER_NUMBER'))
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Activo individual (§3). Centro registral vigente estricto (INV-02): la
-- ubicación física es independiente y puede ser NULL durante el tránsito.
-- Las claves compuestas garantizan que centro y ubicación pertenecen a la
-- organización declarada y al mismo centro, respectivamente.
CREATE TABLE asset (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  organization_id BINARY(16) NOT NULL,
  registral_center_id BINARY(16) NOT NULL,
  current_location_id BINARY(16) NULL,
  category_id BINARY(16) NOT NULL,
  permanent_code_seq BIGINT UNSIGNED NOT NULL,
  permanent_code_check TINYINT UNSIGNED NOT NULL,
  brand VARCHAR(100) NULL,
  model VARCHAR(150) NULL,
  serial_number VARCHAR(100) NULL,
  description VARCHAR(1000) NULL,
  physical_condition VARCHAR(20) NOT NULL,
  operational_state VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE',
  version BIGINT UNSIGNED NOT NULL DEFAULT 1,
  retired_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_asset PRIMARY KEY (id),
  -- Unicidad del código permanente por organización (INV-01). No se reutiliza:
  -- la baja no libera el número porque la fila se conserva.
  CONSTRAINT uq_asset_permanent_code UNIQUE (organization_id, permanent_code_seq),
  CONSTRAINT fk_asset_center FOREIGN KEY (organization_id, registral_center_id) REFERENCES center (organization_id, id),
  CONSTRAINT fk_asset_location FOREIGN KEY (registral_center_id, current_location_id) REFERENCES location (center_id, id),
  CONSTRAINT fk_asset_category FOREIGN KEY (organization_id, category_id) REFERENCES asset_category (organization_id, id),
  -- Doce dígitos de capacidad conforme a §7; capacidad definitiva en D-09.
  CONSTRAINT ck_asset_code_seq CHECK (permanent_code_seq BETWEEN 1 AND 999999999999),
  CONSTRAINT ck_asset_code_check CHECK (permanent_code_check BETWEEN 0 AND 99),
  -- Escala de condición física propuesta; valores finales pendientes de
  -- validación funcional (D-02/D-04). No sustituye al estado operativo (§3).
  CONSTRAINT ck_asset_physical_condition CHECK (physical_condition IN ('NUEVO', 'BUENO', 'REGULAR', 'DANADO', 'OBSOLETO')),
  -- Estados de §5.1. BAJA es terminal.
  CONSTRAINT ck_asset_operational_state CHECK (operational_state IN ('DISPONIBLE', 'ASIGNADO', 'RESERVADO', 'EN_TRANSITO', 'EN_INCIDENCIA', 'BAJA')),
  CONSTRAINT ck_asset_version CHECK (version >= 1),
  CONSTRAINT ck_asset_retirement CHECK (operational_state <> 'BAJA' OR retired_at IS NOT NULL),
  KEY idx_asset_org_center (organization_id, registral_center_id),
  KEY idx_asset_center_state (registral_center_id, operational_state),
  KEY idx_asset_center_location (registral_center_id, current_location_id),
  KEY idx_asset_category (category_id),
  -- Advertencia de duplicados por fabricante y serie normalizada (§3, D-04):
  -- índice no único a propósito; la serie no es identificador permanente.
  KEY idx_asset_brand_serial (brand, serial_number),
  KEY idx_asset_serial (serial_number)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Identificador público QR por activo (§7). Uno activo por activo; la rotación
-- por compromiso desactiva el anterior y conserva el historial auditado.
CREATE TABLE asset_qr_identifier (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  asset_id BINARY(16) NOT NULL,
  public_id BINARY(16) NOT NULL,
  active_flag TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  revoked_at DATETIME(6) NULL,
  revoke_reason VARCHAR(500) NULL,
  CONSTRAINT pk_asset_qr_identifier PRIMARY KEY (id),
  CONSTRAINT uq_asset_qr_identifier_public UNIQUE (public_id),
  CONSTRAINT uq_asset_qr_identifier_active UNIQUE (asset_id, active_flag),
  CONSTRAINT fk_asset_qr_identifier_asset FOREIGN KEY (asset_id) REFERENCES asset (id),
  CONSTRAINT ck_asset_qr_identifier_active_flag CHECK (active_flag IS NULL OR active_flag = 1),
  CONSTRAINT ck_asset_qr_identifier_revocation CHECK (
    (active_flag = 1 AND revoked_at IS NULL)
    OR (active_flag IS NULL AND revoked_at IS NOT NULL)
  )
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Valor de atributo dinámico por activo. Exactamente una columna de valor
-- informada según el tipo definido en asset_category_attribute.
CREATE TABLE asset_attribute_value (
  asset_id BINARY(16) NOT NULL,
  category_attribute_id BINARY(16) NOT NULL,
  value_text VARCHAR(1000) NULL,
  value_number DECIMAL(18, 6) NULL,
  value_datetime DATETIME(6) NULL,
  value_boolean TINYINT(1) NULL,
  CONSTRAINT pk_asset_attribute_value PRIMARY KEY (asset_id, category_attribute_id),
  CONSTRAINT fk_asset_attribute_value_asset FOREIGN KEY (asset_id) REFERENCES asset (id),
  CONSTRAINT fk_asset_attribute_value_attribute FOREIGN KEY (category_attribute_id) REFERENCES asset_category_attribute (id),
  CONSTRAINT ck_asset_attribute_value_single CHECK (
    (value_text IS NOT NULL) + (value_number IS NOT NULL)
      + (value_datetime IS NOT NULL) + (value_boolean IS NOT NULL) = 1
  ),
  CONSTRAINT ck_asset_attribute_value_boolean CHECK (value_boolean IS NULL OR value_boolean IN (0, 1))
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Asignación de custodia (§3). Una abierta por activo mediante el patrón de
-- bandera con UNIQUE; el cierre escribe fecha y causa y anula open_flag.
-- El movimiento o resolución relacionado se referencia de forma polimórfica
-- (related_entity/related_id) porque la causa puede provenir de traslados,
-- recepciones o resoluciones; integridad verificada por la aplicación.
CREATE TABLE asset_custody_assignment (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  asset_id BINARY(16) NOT NULL,
  custodian_id BINARY(16) NOT NULL,
  custody_cause VARCHAR(20) NOT NULL,
  open_flag TINYINT(1) NOT NULL DEFAULT 1,
  started_at DATETIME(6) NOT NULL,
  ended_at DATETIME(6) NULL,
  end_cause VARCHAR(500) NULL,
  related_entity VARCHAR(30) NULL,
  related_id BINARY(16) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_asset_custody_assignment PRIMARY KEY (id),
  CONSTRAINT uq_asset_custody_assignment_open UNIQUE (asset_id, open_flag),
  CONSTRAINT fk_asset_custody_assignment_asset FOREIGN KEY (asset_id) REFERENCES asset (id),
  CONSTRAINT fk_asset_custody_assignment_custodian FOREIGN KEY (custodian_id) REFERENCES custodian (id),
  CONSTRAINT ck_asset_custody_assignment_cause CHECK (custody_cause IN ('ASIGNACION', 'TRANSPORTE', 'RECHAZO', 'DEVOLUCION', 'RESOLUCION', 'OTRO')),
  CONSTRAINT ck_asset_custody_assignment_open_flag CHECK (open_flag IS NULL OR open_flag = 1),
  CONSTRAINT ck_asset_custody_assignment_dates CHECK (ended_at IS NULL OR ended_at >= started_at),
  CONSTRAINT ck_asset_custody_assignment_closure CHECK (
    (open_flag = 1 AND ended_at IS NULL)
    OR (open_flag IS NULL AND ended_at IS NOT NULL)
  ),
  CONSTRAINT ck_asset_custody_assignment_related CHECK (
    (related_entity IS NULL AND related_id IS NULL)
    OR (related_entity IS NOT NULL AND related_id IS NOT NULL)
  ),
  KEY idx_asset_custody_assignment_custodian (custodian_id)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Evento de activo: historial inmutable de movimientos (§3). Sin updated_at
-- por diseño append-only; la protección contra UPDATE/DELETE por la cuenta de
-- aplicación se documenta como pendiente en Fase 02.
CREATE TABLE asset_event (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  asset_id BINARY(16) NOT NULL,
  event_type VARCHAR(40) NOT NULL,
  occurred_at DATETIME(6) NOT NULL,
  actor_user_id BINARY(16) NULL,
  from_center_id BINARY(16) NULL,
  to_center_id BINARY(16) NULL,
  from_location_id BINARY(16) NULL,
  to_location_id BINARY(16) NULL,
  motive VARCHAR(500) NULL,
  related_entity VARCHAR(30) NULL,
  related_id BINARY(16) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_asset_event PRIMARY KEY (id),
  CONSTRAINT fk_asset_event_asset FOREIGN KEY (asset_id) REFERENCES asset (id),
  CONSTRAINT fk_asset_event_actor FOREIGN KEY (actor_user_id) REFERENCES app_user (id),
  CONSTRAINT fk_asset_event_from_center FOREIGN KEY (from_center_id) REFERENCES center (id),
  CONSTRAINT fk_asset_event_to_center FOREIGN KEY (to_center_id) REFERENCES center (id),
  CONSTRAINT fk_asset_event_from_location FOREIGN KEY (from_location_id) REFERENCES location (id),
  CONSTRAINT fk_asset_event_to_location FOREIGN KEY (to_location_id) REFERENCES location (id),
  CONSTRAINT ck_asset_event_type CHECK (event_type IN (
    'REGISTRO', 'ASIGNACION_CUSTODIA', 'CIERRE_CUSTODIA', 'RESERVA', 'LIBERACION_RESERVA',
    'DESPACHO', 'ACEPTACION_DESTINO', 'RECHAZO_DESTINO', 'DECLARACION_FALTANTE', 'RESOLUCION',
    'DEVOLUCION', 'BAJA', 'REIMPRESION_ETIQUETA', 'ROTACION_QR', 'RECLASIFICACION',
    'CAMBIO_UBICACION', 'INICIO_MANTENIMIENTO', 'FIN_MANTENIMIENTO', 'OTRO')),
  CONSTRAINT ck_asset_event_related CHECK (
    (related_entity IS NULL AND related_id IS NULL)
    OR (related_entity IS NOT NULL AND related_id IS NOT NULL)
  ),
  KEY idx_asset_event_asset_time (asset_id, occurred_at),
  KEY idx_asset_event_occurred (occurred_at)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Historial de reclasificaciones (cambio de categoría/atributos de un activo).
-- Fase 00 no define esta entidad: se interpreta como historial auditable de
-- cambios de categoría con motivo y actor; decisión registrada en Fase 02.
CREATE TABLE asset_reclassification (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  asset_id BINARY(16) NOT NULL,
  from_category_id BINARY(16) NOT NULL,
  to_category_id BINARY(16) NOT NULL,
  attribute_changes JSON NULL,
  motive VARCHAR(500) NOT NULL,
  performed_by_user_id BINARY(16) NOT NULL,
  performed_at DATETIME(6) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_asset_reclassification PRIMARY KEY (id),
  CONSTRAINT fk_asset_reclassification_asset FOREIGN KEY (asset_id) REFERENCES asset (id),
  CONSTRAINT fk_asset_reclassification_from FOREIGN KEY (from_category_id) REFERENCES asset_category (id),
  CONSTRAINT fk_asset_reclassification_to FOREIGN KEY (to_category_id) REFERENCES asset_category (id),
  CONSTRAINT fk_asset_reclassification_actor FOREIGN KEY (performed_by_user_id) REFERENCES app_user (id),
  CONSTRAINT ck_asset_reclassification_distinct CHECK (from_category_id <> to_category_id),
  KEY idx_asset_reclassification_asset_time (asset_id, performed_at)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;
