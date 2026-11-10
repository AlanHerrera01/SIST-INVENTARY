-- Fase 02 — Migración V7.
-- Área: recepciones, ítems de recepción, discrepancias y resoluciones
-- (Fase 00 §3, §5.3, §6.3; INV-05, INV-08).
--
-- Cada ítem de traslado solo puede estar reclamado por una recepción viva
-- (borrador o confirmada): claimed_flag = 1 mientras la recepción no se
-- descarte; al descartar se anula y el ítem vuelve a estar disponible para otra
-- recepción. La recepción confirmada es inmutable (§5.3).

-- Recepción por traslado; puede haber varias (§3). El receptor confirma el
-- conjunto de ítems en una única transacción atómica (§5.3).
CREATE TABLE reception (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  transfer_id BINARY(16) NOT NULL,
  receiver_user_id BINARY(16) NOT NULL,
  reception_state VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
  observed_at DATETIME(6) NULL,
  confirmed_at DATETIME(6) NULL,
  discarded_at DATETIME(6) NULL,
  discard_reason VARCHAR(500) NULL,
  version BIGINT UNSIGNED NOT NULL DEFAULT 1,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_reception PRIMARY KEY (id),
  CONSTRAINT fk_reception_transfer FOREIGN KEY (transfer_id) REFERENCES transfer (id),
  CONSTRAINT fk_reception_receiver FOREIGN KEY (receiver_user_id) REFERENCES app_user (id),
  -- Estados de §5.3: CONFIRMADA y DESCARTADA son terminales.
  CONSTRAINT ck_reception_state CHECK (reception_state IN ('BORRADOR', 'CONFIRMADA', 'DESCARTADA')),
  CONSTRAINT ck_reception_version CHECK (version >= 1),
  CONSTRAINT ck_reception_terminal_dates CHECK (
    (reception_state = 'BORRADOR' AND confirmed_at IS NULL AND discarded_at IS NULL)
    OR (reception_state = 'CONFIRMADA' AND confirmed_at IS NOT NULL AND discarded_at IS NULL)
    OR (reception_state = 'DESCARTADA' AND confirmed_at IS NULL AND discarded_at IS NOT NULL)
  ),
  KEY idx_reception_transfer (transfer_id)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Ítem de recepción: resultado por ítem de traslado (un resultado confirmado
-- por ítem, garantizado por UNIQUE (transfer_item_id, claimed_flag) mientras
-- la recepción no se descarte). Aceptado exige condición observada y
-- ubicación destino; rechazado exige motivo (§6.3). El custodio destino es
-- opcional a nivel de esquema porque su exigencia depende de la política D-08.
CREATE TABLE reception_item (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  reception_id BINARY(16) NOT NULL,
  transfer_item_id BINARY(16) NOT NULL,
  claimed_flag TINYINT(1) NOT NULL DEFAULT 1,
  outcome VARCHAR(20) NULL,
  observed_condition VARCHAR(20) NULL,
  destination_location_id BINARY(16) NULL,
  destination_custodian_id BINARY(16) NULL,
  rejection_reason VARCHAR(500) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_reception_item PRIMARY KEY (id),
  CONSTRAINT uq_reception_item_claimed UNIQUE (transfer_item_id, claimed_flag),
  CONSTRAINT fk_reception_item_reception FOREIGN KEY (reception_id) REFERENCES reception (id),
  CONSTRAINT fk_reception_item_transfer_item FOREIGN KEY (transfer_item_id) REFERENCES transfer_item (id),
  CONSTRAINT fk_reception_item_location FOREIGN KEY (destination_location_id) REFERENCES location (id),
  CONSTRAINT fk_reception_item_custodian FOREIGN KEY (destination_custodian_id) REFERENCES custodian (id),
  CONSTRAINT ck_reception_item_claimed_flag CHECK (claimed_flag IS NULL OR claimed_flag = 1),
  CONSTRAINT ck_reception_item_outcome CHECK (outcome IS NULL OR outcome IN ('ACEPTADO', 'RECHAZADO')),
  CONSTRAINT ck_reception_item_condition CHECK (
    observed_condition IS NULL
    OR observed_condition IN ('NUEVO', 'BUENO', 'REGULAR', 'DANADO', 'OBSOLETO')),
  CONSTRAINT ck_reception_item_accept_requires_data CHECK (
    outcome IS NULL OR outcome <> 'ACEPTADO'
    OR (destination_location_id IS NOT NULL AND observed_condition IS NOT NULL)),
  CONSTRAINT ck_reception_item_reject_requires_reason CHECK (
    outcome IS NULL OR outcome <> 'RECHAZADO' OR rejection_reason IS NOT NULL),
  CONSTRAINT ck_reception_item_rejection_only_for_rejected CHECK (
    outcome <> 'RECHAZADO' OR rejection_reason IS NOT NULL),
  KEY idx_reception_item_reception (reception_id)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Discrepancia: daño, rechazo, faltante u otro (§3). Una abierta por ítem de
-- traslado: resuelta la primera, puede registrarse otra; decisión de Fase 02
-- registrada en el documento de decisiones. El rechazo nunca provoca retorno
-- automático (INV-05): la salida exige resolución con evidencia.
CREATE TABLE discrepancy (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  transfer_item_id BINARY(16) NOT NULL,
  reception_item_id BINARY(16) NULL,
  discrepancy_type VARCHAR(20) NOT NULL,
  motive VARCHAR(500) NOT NULL,
  responsible_custodian_id BINARY(16) NULL,
  open_flag TINYINT(1) NOT NULL DEFAULT 1,
  created_by_user_id BINARY(16) NOT NULL,
  resolved_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_discrepancy PRIMARY KEY (id),
  CONSTRAINT uq_discrepancy_open UNIQUE (transfer_item_id, open_flag),
  CONSTRAINT fk_discrepancy_transfer_item FOREIGN KEY (transfer_item_id) REFERENCES transfer_item (id),
  CONSTRAINT fk_discrepancy_reception_item FOREIGN KEY (reception_item_id) REFERENCES reception_item (id),
  CONSTRAINT fk_discrepancy_responsible_custodian FOREIGN KEY (responsible_custodian_id) REFERENCES custodian (id),
  CONSTRAINT fk_discrepancy_creator FOREIGN KEY (created_by_user_id) REFERENCES app_user (id),
  CONSTRAINT ck_discrepancy_type CHECK (discrepancy_type IN ('DANO', 'RECHAZO', 'FALTANTE', 'OTRO')),
  CONSTRAINT ck_discrepancy_open_flag CHECK (open_flag IS NULL OR open_flag = 1),
  CONSTRAINT ck_discrepancy_resolution CHECK (
    (open_flag = 1 AND resolved_at IS NULL)
    OR (open_flag IS NULL AND resolved_at IS NOT NULL)
  ),
  KEY idx_discrepancy_reception_item (reception_item_id)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Resolución de discrepancia: propuesta, aprobación independiente y ejecución
-- con evidencia (§3, §6.3). No borra la recepción original. Los tipos se
-- alinean con los resultados terminales de ítem de §5.3.
CREATE TABLE resolution (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  discrepancy_id BINARY(16) NOT NULL,
  resolution_type VARCHAR(30) NOT NULL,
  resolution_state VARCHAR(20) NOT NULL DEFAULT 'PROPUESTA',
  pending_flag TINYINT(1) NOT NULL DEFAULT 1,
  executed_flag TINYINT(1) NULL,
  motive VARCHAR(500) NOT NULL,
  proposed_by_user_id BINARY(16) NOT NULL,
  approver_user_id BINARY(16) NULL,
  decided_at DATETIME(6) NULL,
  executed_by_user_id BINARY(16) NULL,
  executed_at DATETIME(6) NULL,
  destination_custodian_id BINARY(16) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_resolution PRIMARY KEY (id),
  -- Una resolución no terminal (PROPUESTA o APROBADA) por discrepancia y una
  -- sola ejecutada en su historia.
  CONSTRAINT uq_resolution_pending UNIQUE (discrepancy_id, pending_flag),
  CONSTRAINT uq_resolution_executed UNIQUE (discrepancy_id, executed_flag),
  CONSTRAINT fk_resolution_discrepancy FOREIGN KEY (discrepancy_id) REFERENCES discrepancy (id),
  CONSTRAINT fk_resolution_proposer FOREIGN KEY (proposed_by_user_id) REFERENCES app_user (id),
  CONSTRAINT fk_resolution_approver FOREIGN KEY (approver_user_id) REFERENCES app_user (id),
  CONSTRAINT fk_resolution_executor FOREIGN KEY (executed_by_user_id) REFERENCES app_user (id),
  CONSTRAINT fk_resolution_destination_custodian FOREIGN KEY (destination_custodian_id) REFERENCES custodian (id),
  CONSTRAINT ck_resolution_type CHECK (resolution_type IN ('ACEPTADO_POR_RESOLUCION', 'DEVUELTO_ORIGEN', 'BAJA_POR_PERDIDA')),
  CONSTRAINT ck_resolution_state CHECK (resolution_state IN ('PROPUESTA', 'APROBADA', 'EJECUTADA', 'RECHAZADA')),
  -- Separación de funciones: quien propone no aprueba (§4, aplicado también a
  -- discrepancias como política inicial).
  CONSTRAINT ck_resolution_approver_distinct CHECK (approver_user_id IS NULL OR approver_user_id <> proposed_by_user_id),
  CONSTRAINT ck_resolution_state_flags CHECK (
    (resolution_state IN ('PROPUESTA', 'APROBADA') AND pending_flag = 1 AND executed_flag IS NULL)
    OR (resolution_state = 'EJECUTADA' AND pending_flag IS NULL AND executed_flag = 1)
    OR (resolution_state = 'RECHAZADA' AND pending_flag IS NULL AND executed_flag IS NULL)
  ),
  CONSTRAINT ck_resolution_decision CHECK (
    (resolution_state = 'PROPUESTA' AND approver_user_id IS NULL AND decided_at IS NULL)
    OR (resolution_state IN ('APROBADA', 'RECHAZADA', 'EJECUTADA') AND approver_user_id IS NOT NULL AND decided_at IS NOT NULL)
  ),
  CONSTRAINT ck_resolution_execution CHECK (
    (resolution_state = 'EJECUTADA' AND executed_by_user_id IS NOT NULL AND executed_at IS NOT NULL)
    OR (resolution_state <> 'EJECUTADA' AND executed_by_user_id IS NULL AND executed_at IS NULL)
  ),
  KEY idx_resolution_discrepancy (discrepancy_id)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Referencia inversa: la resolución ejecutada que fijó el resultado terminal
-- del ítem de traslado (§3, "resolución" del ítem).
ALTER TABLE transfer_item
  ADD COLUMN resolution_id BINARY(16) NULL AFTER reception_outcome,
  ADD CONSTRAINT fk_transfer_item_resolution FOREIGN KEY (resolution_id) REFERENCES resolution (id),
  ADD KEY idx_transfer_item_resolution (resolution_id);
