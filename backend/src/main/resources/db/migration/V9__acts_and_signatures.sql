-- Fase 02 — Migración V9.
-- Área: actas, solicitudes de firma y firmas (Fase 00 §3, §5.4, §5.5, §6.4;
-- INV-07, INV-08).
--
-- Acta: documento generado a partir de hechos ya confirmados o de un
-- compromiso de despacho autorizado (§5.5). EMITIDA congela instantánea,
-- hash y archivo limpio; BORRADOR se descarta sin haber emitido contenido,
-- por lo que su fila puede eliminarse a nivel de aplicación (carece de hash
-- y archivo); ANULADA procede de un acta emitida o en firma, nunca de un
-- borrador. SUSTITUIDA solo ocurre al formalizar la versión correctiva que
-- la referencia: cada acta puede ser sustituida una sola vez (UNIQUE de
-- replaced_act_id). La coherencia de estados del acta con el movimiento
-- (despacho/cierre de traslado, baja) y la limpieza del archivo adjunto son
-- verificación de aplicación.
--
-- Numeración de actas: pendiente de D-14; el identificador actual es el id
-- interno y code_sequence todavía no define ACT_NUMBER.
--
-- Solicitud de firma: una activa por acta (patrón de bandera); el hash, la
-- política y el orden de firmantes quedan congelados al crearla. La firma se
-- registra sin sobrescribir (INV-07): cada firmante de la solicitud produce
-- exactamente una fila de firma con su resultado.

CREATE TABLE act (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  act_type VARCHAR(20) NOT NULL,
  act_state VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
  content_version INT UNSIGNED NOT NULL DEFAULT 1,
  transfer_id BINARY(16) NULL,
  reception_id BINARY(16) NULL,
  resolution_id BINARY(16) NULL,
  retirement_request_id BINARY(16) NULL,
  replaced_act_id BINARY(16) NULL,
  content_snapshot JSON NULL,
  content_hash CHAR(64) NULL,
  file_id BINARY(16) NULL,
  issued_at DATETIME(6) NULL,
  formalized_at DATETIME(6) NULL,
  annulled_at DATETIME(6) NULL,
  annul_reason VARCHAR(500) NULL,
  superseded_at DATETIME(6) NULL,
  created_by_user_id BINARY(16) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_act PRIMARY KEY (id),
  -- Una sola versión correctiva por acta sustituida (§5.5).
  CONSTRAINT uq_act_replaced UNIQUE (replaced_act_id),
  CONSTRAINT fk_act_transfer FOREIGN KEY (transfer_id) REFERENCES transfer (id),
  CONSTRAINT fk_act_reception FOREIGN KEY (reception_id) REFERENCES reception (id),
  CONSTRAINT fk_act_resolution FOREIGN KEY (resolution_id) REFERENCES resolution (id),
  CONSTRAINT fk_act_retirement_request FOREIGN KEY (retirement_request_id) REFERENCES retirement_request (id),
  CONSTRAINT fk_act_replaced FOREIGN KEY (replaced_act_id) REFERENCES act (id),
  CONSTRAINT fk_act_file FOREIGN KEY (file_id) REFERENCES stored_file (id),
  CONSTRAINT fk_act_creator FOREIGN KEY (created_by_user_id) REFERENCES app_user (id),
  CONSTRAINT ck_act_type CHECK (act_type IN ('DESPACHO', 'RECEPCION', 'FINAL', 'BAJA')),
  -- Estados de §5.5. ANULADA, FORMALIZADA y SUSTITUIDA son terminales.
  CONSTRAINT ck_act_state CHECK (act_state IN ('BORRADOR', 'EMITIDA', 'EN_FIRMA', 'FORMALIZADA', 'ANULADA', 'SUSTITUIDA')),
  -- Exactamente un origen por acta.
  CONSTRAINT ck_act_origin_single CHECK (
    ((transfer_id IS NOT NULL) + (reception_id IS NOT NULL)
      + (resolution_id IS NOT NULL) + (retirement_request_id IS NOT NULL)) = 1
  ),
  -- Coherencia tipo-origen: despacho y final nacen del traslado; la de
  -- recepción, de la recepción; la de baja, de la solicitud de baja o de la
  -- resolución de pérdida (§5.1, §6.4).
  CONSTRAINT ck_act_type_origin CHECK (
    (act_type IN ('DESPACHO', 'FINAL') AND transfer_id IS NOT NULL
      AND reception_id IS NULL AND resolution_id IS NULL AND retirement_request_id IS NULL)
    OR (act_type = 'RECEPCION' AND reception_id IS NOT NULL
      AND transfer_id IS NULL AND resolution_id IS NULL AND retirement_request_id IS NULL)
    OR (act_type = 'BAJA' AND (retirement_request_id IS NOT NULL OR resolution_id IS NOT NULL)
      AND transfer_id IS NULL AND reception_id IS NULL)
  ),
  -- Congelación de contenido (§5.5): fuera de BORRADOR, hash, archivo y
  -- fecha de emisión son obligatorios; en BORRADOR no se han fijado.
  CONSTRAINT ck_act_content_frozen CHECK (
    (act_state = 'BORRADOR' AND content_hash IS NULL AND file_id IS NULL AND issued_at IS NULL)
    OR (act_state <> 'BORRADOR' AND content_hash IS NOT NULL AND file_id IS NOT NULL AND issued_at IS NOT NULL)
  ),
  -- Fechas por estado; una acta anulada nunca se formalizó ni se sustituyó,
  -- y una sustituida se formalizó antes de ser reemplazada (§5.5).
  CONSTRAINT ck_act_state_dates CHECK (
    (act_state IN ('BORRADOR', 'EMITIDA', 'EN_FIRMA') AND formalized_at IS NULL AND annulled_at IS NULL AND superseded_at IS NULL)
    OR (act_state = 'FORMALIZADA' AND formalized_at IS NOT NULL AND annulled_at IS NULL AND superseded_at IS NULL)
    OR (act_state = 'ANULADA' AND annulled_at IS NOT NULL AND annul_reason IS NOT NULL AND formalized_at IS NULL AND superseded_at IS NULL)
    OR (act_state = 'SUSTITUIDA' AND formalized_at IS NOT NULL AND superseded_at IS NOT NULL AND annulled_at IS NULL)
  ),
  CONSTRAINT ck_act_hash CHECK (content_hash IS NULL OR content_hash REGEXP '^[0-9a-f]{64}$'),
  CONSTRAINT ck_act_annul_reason CHECK (
    (annulled_at IS NULL AND annul_reason IS NULL)
    OR (annulled_at IS NOT NULL AND annul_reason IS NOT NULL)
  ),
  CONSTRAINT ck_act_replacement_self CHECK (replaced_act_id IS NULL OR replaced_act_id <> id),
  CONSTRAINT ck_act_content_version CHECK (content_version >= 1),
  KEY idx_act_transfer (transfer_id),
  KEY idx_act_reception (reception_id),
  KEY idx_act_resolution (resolution_id),
  KEY idx_act_retirement_request (retirement_request_id),
  KEY idx_act_type_state (act_type, act_state)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Solicitud de firma sobre un acta (§5.4). content_hash repite el hash del
-- acta y queda congelado al crear la solicitud. Una solicitud activa por
-- acta; al terminar (COMPLETA, NO_COMPLETADA, CANCELADA) se anula active_flag
-- y puede emitirse otra sobre el mismo contenido vigente. El vencimiento lo
-- aplica la aplicación según expires_at.
CREATE TABLE signature_request (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  act_id BINARY(16) NOT NULL,
  content_hash CHAR(64) NOT NULL,
  signature_policy VARCHAR(50) NOT NULL,
  provider VARCHAR(100) NULL,
  request_state VARCHAR(20) NOT NULL DEFAULT 'CREADA',
  active_flag TINYINT(1) NOT NULL DEFAULT 1,
  expires_at DATETIME(6) NULL,
  created_by_user_id BINARY(16) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_signature_request PRIMARY KEY (id),
  CONSTRAINT uq_signature_request_active UNIQUE (act_id, active_flag),
  CONSTRAINT fk_signature_request_act FOREIGN KEY (act_id) REFERENCES act (id),
  CONSTRAINT fk_signature_request_creator FOREIGN KEY (created_by_user_id) REFERENCES app_user (id),
  -- Estados de §5.4: los terminales no inhabilitan una nueva solicitud
  -- sobre el mismo contenido si sigue vigente.
  CONSTRAINT ck_signature_request_state CHECK (request_state IN ('CREADA', 'PENDIENTE', 'COMPLETA', 'NO_COMPLETADA', 'CANCELADA')),
  CONSTRAINT ck_signature_request_active_flag CHECK (active_flag IS NULL OR active_flag = 1),
  CONSTRAINT ck_signature_request_state_flag CHECK (
    (request_state IN ('CREADA', 'PENDIENTE') AND active_flag = 1)
    OR (request_state IN ('COMPLETA', 'NO_COMPLETADA', 'CANCELADA') AND active_flag IS NULL)
  ),
  CONSTRAINT ck_signature_request_hash CHECK (content_hash REGEXP '^[0-9a-f]{64}$'),
  CONSTRAINT ck_signature_request_expiry CHECK (expires_at IS NULL OR expires_at > created_at),
  KEY idx_signature_request_act (act_id)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Firmante esperado y posición en el orden de la solicitud (§3, §4, §5.4).
-- Solo el firmante asignado puede firmar; la delegación exige reemplazo
-- explícito previo con registro de motivo. El nombre visible se congela para
-- no depender de cambios posteriores en la cuenta (D-07 pendiente).
CREATE TABLE signature_request_signer (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  request_id BINARY(16) NOT NULL,
  signer_position SMALLINT UNSIGNED NOT NULL,
  expected_user_id BINARY(16) NOT NULL,
  display_name VARCHAR(200) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_signature_request_signer PRIMARY KEY (id),
  CONSTRAINT uq_signature_request_signer_position UNIQUE (request_id, signer_position),
  -- Destino de la clave compuesta de signature: garantiza que el firmante
  -- referido pertenece a la misma solicitud que la firma.
  CONSTRAINT uq_signature_request_signer_request UNIQUE (request_id, id),
  CONSTRAINT fk_signature_request_signer_request FOREIGN KEY (request_id) REFERENCES signature_request (id),
  CONSTRAINT fk_signature_request_signer_user FOREIGN KEY (expected_user_id) REFERENCES app_user (id),
  CONSTRAINT ck_signature_request_signer_position CHECK (signer_position >= 1),
  KEY idx_signature_request_signer_user (expected_user_id)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Firma individual (§5.4, §6.4). Una fila por firmante de la solicitud; el
-- resultado se registra sin sobrescribir (INV-07). La coincidencia entre el
-- firmante real y el esperado, y entre el hash firmado y el de la solicitud,
-- son verificaciones de la aplicación al momento de firmar.
CREATE TABLE signature (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  request_id BINARY(16) NOT NULL,
  request_signer_id BINARY(16) NOT NULL,
  signature_state VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  signature_method VARCHAR(20) NULL,
  actual_user_id BINARY(16) NULL,
  signed_at DATETIME(6) NULL,
  signed_content_hash CHAR(64) NULL,
  rejection_reason VARCHAR(500) NULL,
  evidence_file_id BINARY(16) NULL,
  external_reference VARCHAR(200) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_signature PRIMARY KEY (id),
  CONSTRAINT uq_signature_request_signer UNIQUE (request_id, request_signer_id),
  CONSTRAINT fk_signature_request FOREIGN KEY (request_id) REFERENCES signature_request (id),
  CONSTRAINT fk_signature_signer FOREIGN KEY (request_id, request_signer_id) REFERENCES signature_request_signer (request_id, id),
  CONSTRAINT fk_signature_actual_user FOREIGN KEY (actual_user_id) REFERENCES app_user (id),
  CONSTRAINT fk_signature_evidence_file FOREIGN KEY (evidence_file_id) REFERENCES stored_file (id),
  -- Estados de §5.4; los terminales no se reabren.
  CONSTRAINT ck_signature_state CHECK (signature_state IN ('PENDIENTE', 'FIRMADA', 'RECHAZADA', 'VENCIDA', 'CANCELADA')),
  -- El método interno y el proveedor externo son alternativas pendientes de
  -- D-07; no se equiparan en efectos (§5.4).
  CONSTRAINT ck_signature_method CHECK (signature_method IS NULL OR signature_method IN ('INTERNA', 'PROVEEDOR_EXTERNO')),
  CONSTRAINT ck_signature_hash CHECK (signed_content_hash IS NULL OR signed_content_hash REGEXP '^[0-9a-f]{64}$'),
  -- FIRMADA exige momento, hash, usuario real y método (§5.4); los demás
  -- estados no registran firma materializada.
  CONSTRAINT ck_signature_signed CHECK (
    (signature_state = 'FIRMADA' AND signed_at IS NOT NULL AND signed_content_hash IS NOT NULL
      AND actual_user_id IS NOT NULL AND signature_method IS NOT NULL)
    OR (signature_state <> 'FIRMADA' AND signed_at IS NULL AND signed_content_hash IS NULL)
  ),
  CONSTRAINT ck_signature_rejection CHECK (
    (signature_state = 'RECHAZADA' AND rejection_reason IS NOT NULL)
    OR (signature_state <> 'RECHAZADA' AND rejection_reason IS NULL)
  ),
  KEY idx_signature_actual_user (actual_user_id)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Referencias inversas desde el movimiento hacia sus actas (§5.5, INV-08).
-- transfer: acta de despacho (exigida para pasar a EN_TRANSITO) y acta final
-- (exigida para CERRADO); retirement_request: acta formal de la baja
-- ejecutada. La coherencia entre estado del movimiento y acta referida se
-- valida en la aplicación.
ALTER TABLE transfer
  ADD COLUMN dispatch_act_id BINARY(16) NULL AFTER transport_custodian_id,
  ADD COLUMN final_act_id BINARY(16) NULL AFTER dispatch_act_id,
  ADD CONSTRAINT fk_transfer_dispatch_act FOREIGN KEY (dispatch_act_id) REFERENCES act (id),
  ADD CONSTRAINT fk_transfer_final_act FOREIGN KEY (final_act_id) REFERENCES act (id),
  ADD KEY idx_transfer_dispatch_act (dispatch_act_id),
  ADD KEY idx_transfer_final_act (final_act_id);

ALTER TABLE retirement_request
  ADD COLUMN formal_act_id BINARY(16) NULL AFTER resolution_id,
  ADD CONSTRAINT fk_retirement_request_formal_act FOREIGN KEY (formal_act_id) REFERENCES act (id),
  ADD KEY idx_retirement_request_formal_act (formal_act_id);
