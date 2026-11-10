-- Fase 02 — Migración V6.
-- Área: traslados, ítems y reservas (Fase 00 §3, §5.2; INV-03, INV-04).
--
-- Las referencias circulares con actas (acta de despacho / acta final) y con
-- resoluciones se agregan con ALTER TABLE en V7 y V9, una vez existan las
-- tablas dependientes.

-- Traslado: número legible por organización (secuencia TRANSFER_NUMBER),
-- origen y destino distintos y de la misma organización (claves compuestas),
-- solicitante distinto del autorizador (separación de funciones, §4).
CREATE TABLE transfer (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  organization_id BINARY(16) NOT NULL,
  transfer_number BIGINT UNSIGNED NOT NULL,
  source_center_id BINARY(16) NOT NULL,
  destination_center_id BINARY(16) NOT NULL,
  requester_user_id BINARY(16) NOT NULL,
  authorizer_user_id BINARY(16) NULL,
  transport_custodian_id BINARY(16) NULL,
  motive VARCHAR(500) NOT NULL,
  transfer_state VARCHAR(30) NOT NULL DEFAULT 'BORRADOR',
  version BIGINT UNSIGNED NOT NULL DEFAULT 1,
  authorized_at DATETIME(6) NULL,
  rejected_at DATETIME(6) NULL,
  dispatched_at DATETIME(6) NULL,
  closed_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_transfer PRIMARY KEY (id),
  CONSTRAINT uq_transfer_number UNIQUE (organization_id, transfer_number),
  CONSTRAINT fk_transfer_source_center FOREIGN KEY (organization_id, source_center_id) REFERENCES center (organization_id, id),
  CONSTRAINT fk_transfer_destination_center FOREIGN KEY (organization_id, destination_center_id) REFERENCES center (organization_id, id),
  CONSTRAINT fk_transfer_requester FOREIGN KEY (requester_user_id) REFERENCES app_user (id),
  CONSTRAINT fk_transfer_authorizer FOREIGN KEY (authorizer_user_id) REFERENCES app_user (id),
  CONSTRAINT fk_transfer_transport_custodian FOREIGN KEY (transport_custodian_id) REFERENCES custodian (id),
  CONSTRAINT ck_transfer_number CHECK (transfer_number >= 1),
  -- Origen y destino distintos (§3) y mismo solicitante/autorizador prohibido (§4).
  CONSTRAINT ck_transfer_centers_distinct CHECK (source_center_id <> destination_center_id),
  CONSTRAINT ck_transfer_authorizer_distinct CHECK (authorizer_user_id IS NULL OR authorizer_user_id <> requester_user_id),
  -- Estados de §5.2. CERRADO y CANCELADO/RECHAZADO son terminales.
  CONSTRAINT ck_transfer_state CHECK (transfer_state IN (
    'BORRADOR', 'PENDIENTE_AUTORIZACION', 'AUTORIZADO', 'EN_TRANSITO',
    'RECIBIDO_PARCIAL', 'RECIBIDO_TOTAL', 'EN_DISCREPANCIA', 'PENDIENTE_CIERRE',
    'CERRADO', 'RECHAZADO', 'CANCELADO')),
  CONSTRAINT ck_transfer_version CHECK (version >= 1),
  KEY idx_transfer_org_source (organization_id, source_center_id),
  KEY idx_transfer_org_destination (organization_id, destination_center_id),
  KEY idx_transfer_source_state (source_center_id, transfer_state),
  KEY idx_transfer_destination_state (destination_center_id, transfer_state)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Ítem de traslado: activo único dentro del traslado; instantánea al despacho
-- (dispatch_snapshot) y resultado de recepción que avanza conforme §5.2/§5.3.
CREATE TABLE transfer_item (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  transfer_id BINARY(16) NOT NULL,
  asset_id BINARY(16) NOT NULL,
  dispatch_snapshot JSON NULL,
  reception_outcome VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_transfer_item PRIMARY KEY (id),
  CONSTRAINT uq_transfer_item_asset UNIQUE (transfer_id, asset_id),
  CONSTRAINT fk_transfer_item_transfer FOREIGN KEY (transfer_id) REFERENCES transfer (id),
  CONSTRAINT fk_transfer_item_asset FOREIGN KEY (asset_id) REFERENCES asset (id),
  -- Resultados de ítem de §5.3: PENDIENTE mientras no hay recepción
  -- confirmada; los tres últimos solo mediante resolución ejecutada.
  CONSTRAINT ck_transfer_item_outcome CHECK (reception_outcome IN (
    'PENDIENTE', 'ACEPTADO', 'RECHAZADO', 'ACEPTADO_POR_RESOLUCION', 'DEVUELTO_ORIGEN', 'BAJA_POR_PERDIDA')),
  KEY idx_transfer_item_asset (asset_id)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Reserva exclusiva de activo para un traslado (INV-03). Una activa por activo
-- mediante UNIQUE (asset_id, active_flag); su liberación (rechazo, cancelación,
-- aceptación o resolución) anula la bandera y registra fecha y motivo. La fila
-- se conserva como historial; no hay segunda reserva concurrente por activo.
CREATE TABLE asset_reservation (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  asset_id BINARY(16) NOT NULL,
  transfer_id BINARY(16) NOT NULL,
  transfer_item_id BINARY(16) NOT NULL,
  active_flag TINYINT(1) NOT NULL DEFAULT 1,
  activated_at DATETIME(6) NOT NULL,
  released_at DATETIME(6) NULL,
  release_reason VARCHAR(500) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_asset_reservation PRIMARY KEY (id),
  CONSTRAINT uq_asset_reservation_active UNIQUE (asset_id, active_flag),
  CONSTRAINT uq_asset_reservation_item UNIQUE (transfer_item_id),
  CONSTRAINT fk_asset_reservation_asset FOREIGN KEY (asset_id) REFERENCES asset (id),
  CONSTRAINT fk_asset_reservation_transfer FOREIGN KEY (transfer_id) REFERENCES transfer (id),
  CONSTRAINT fk_asset_reservation_item FOREIGN KEY (transfer_item_id) REFERENCES transfer_item (id),
  CONSTRAINT ck_asset_reservation_active_flag CHECK (active_flag IS NULL OR active_flag = 1),
  CONSTRAINT ck_asset_reservation_dates CHECK (released_at IS NULL OR released_at >= activated_at),
  CONSTRAINT ck_asset_reservation_release CHECK (
    (active_flag = 1 AND released_at IS NULL)
    OR (active_flag IS NULL AND released_at IS NOT NULL)
  ),
  KEY idx_asset_reservation_transfer (transfer_id)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;
