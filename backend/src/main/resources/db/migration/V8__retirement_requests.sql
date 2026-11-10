-- Fase 02 — Migración V8.
-- Área: solicitudes de baja (Fase 00 §3, §5.1; INV-01, INV-02).
--
-- Baja documentada con aprobador independiente (§4): quien propone una baja
-- no la aprueba. Una solicitud viva (PROPUESTA o APROBADA) por activo; su
-- rechazo o ejecución anula open_flag y permite una nueva solicitud. La
-- ejecución escribe el estado terminal BAJA del activo (D-06) y conserva el
-- código permanente y el historial (INV-01). La baja por pérdida (PERDIDA)
-- se origina en una resolución BAJA_POR_PERDIDA ya ejecutada (§5.1); la
-- baja documentada (BAJA) no exige resolución. La coherencia entre el tipo
-- PERDIDA y el tipo de la resolución referenciada es verificación de
-- aplicación. El acta formal de la baja se referencia en V9 mediante
-- formal_act_id, agregado allí por la referencia circular.

CREATE TABLE retirement_request (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  asset_id BINARY(16) NOT NULL,
  retirement_kind VARCHAR(20) NOT NULL,
  request_state VARCHAR(20) NOT NULL DEFAULT 'PROPUESTA',
  open_flag TINYINT(1) NOT NULL DEFAULT 1,
  motive VARCHAR(500) NOT NULL,
  requested_by_user_id BINARY(16) NOT NULL,
  approver_user_id BINARY(16) NULL,
  decided_at DATETIME(6) NULL,
  executed_by_user_id BINARY(16) NULL,
  executed_at DATETIME(6) NULL,
  resolution_id BINARY(16) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_retirement_request PRIMARY KEY (id),
  CONSTRAINT uq_retirement_request_open UNIQUE (asset_id, open_flag),
  CONSTRAINT fk_retirement_request_asset FOREIGN KEY (asset_id) REFERENCES asset (id),
  CONSTRAINT fk_retirement_request_requester FOREIGN KEY (requested_by_user_id) REFERENCES app_user (id),
  CONSTRAINT fk_retirement_request_approver FOREIGN KEY (approver_user_id) REFERENCES app_user (id),
  CONSTRAINT fk_retirement_request_executor FOREIGN KEY (executed_by_user_id) REFERENCES app_user (id),
  CONSTRAINT fk_retirement_request_resolution FOREIGN KEY (resolution_id) REFERENCES resolution (id),
  CONSTRAINT ck_retirement_request_kind CHECK (retirement_kind IN ('BAJA', 'PERDIDA')),
  -- Estados de §5.1 con §4: RECHAZADA y EJECUTADA son terminales; la
  -- ejecución no admite reversión (D-06).
  CONSTRAINT ck_retirement_request_state CHECK (request_state IN ('PROPUESTA', 'APROBADA', 'RECHAZADA', 'EJECUTADA')),
  CONSTRAINT ck_retirement_request_open_flag CHECK (open_flag IS NULL OR open_flag = 1),
  -- Separación de funciones (§4): el solicitante de la baja no la aprueba.
  CONSTRAINT ck_retirement_request_approver_distinct CHECK (approver_user_id IS NULL OR approver_user_id <> requested_by_user_id),
  -- La baja por pérdida exige la resolución ejecutada que la origina (§5.1).
  CONSTRAINT ck_retirement_request_loss_resolution CHECK (retirement_kind <> 'PERDIDA' OR resolution_id IS NOT NULL),
  CONSTRAINT ck_retirement_request_state_flags CHECK (
    (request_state IN ('PROPUESTA', 'APROBADA') AND open_flag = 1)
    OR (request_state IN ('RECHAZADA', 'EJECUTADA') AND open_flag IS NULL)
  ),
  CONSTRAINT ck_retirement_request_decision CHECK (
    (request_state = 'PROPUESTA' AND approver_user_id IS NULL AND decided_at IS NULL)
    OR (request_state IN ('APROBADA', 'RECHAZADA', 'EJECUTADA') AND approver_user_id IS NOT NULL AND decided_at IS NOT NULL)
  ),
  CONSTRAINT ck_retirement_request_execution CHECK (
    (request_state = 'EJECUTADA' AND executed_by_user_id IS NOT NULL AND executed_at IS NOT NULL)
    OR (request_state <> 'EJECUTADA' AND executed_by_user_id IS NULL AND executed_at IS NULL)
  ),
  KEY idx_retirement_request_asset (asset_id)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;
