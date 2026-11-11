-- Correcciones aditivas de V1-V11. V5 se corrigió antes de su primera aplicación exitosa.
-- Banderas derivadas: 1 activa, NULL histórica. Nunca dependen de input del cliente.
ALTER TABLE user_role_assignment
  DROP CHECK ck_user_role_assignment_active_flag,
  DROP CHECK ck_user_role_assignment_revocation,
  DROP INDEX uq_user_role_assignment_active,
  DROP COLUMN active_flag,
  ADD COLUMN active_flag TINYINT GENERATED ALWAYS AS (IF(revoked_at IS NULL,1,NULL)) STORED,
  ADD UNIQUE KEY uq_user_role_assignment_active (user_id, role_id, active_flag);

ALTER TABLE asset_qr_identifier
  DROP CHECK ck_asset_qr_identifier_active_flag,
  DROP CHECK ck_asset_qr_identifier_revocation,
  DROP INDEX uq_asset_qr_identifier_active,
  DROP COLUMN active_flag,
  ADD COLUMN active_flag TINYINT GENERATED ALWAYS AS (IF(revoked_at IS NULL,1,NULL)) STORED,
  ADD UNIQUE KEY uq_asset_qr_identifier_active (asset_id, active_flag);

ALTER TABLE asset_custody_assignment
  DROP CHECK ck_asset_custody_assignment_open_flag,
  DROP CHECK ck_asset_custody_assignment_closure,
  DROP INDEX uq_asset_custody_assignment_open,
  DROP COLUMN open_flag,
  ADD COLUMN open_flag TINYINT GENERATED ALWAYS AS (IF(ended_at IS NULL,1,NULL)) STORED,
  ADD UNIQUE KEY uq_asset_custody_assignment_open (asset_id, open_flag);

ALTER TABLE asset_reservation
  DROP CHECK ck_asset_reservation_active_flag,
  DROP CHECK ck_asset_reservation_release,
  DROP INDEX uq_asset_reservation_active,
  DROP COLUMN active_flag,
  ADD COLUMN active_flag TINYINT GENERATED ALWAYS AS (IF(released_at IS NULL,1,NULL)) STORED,
  ADD UNIQUE KEY uq_asset_reservation_active (asset_id, active_flag);

ALTER TABLE discrepancy
  DROP CHECK ck_discrepancy_open_flag,
  DROP CHECK ck_discrepancy_resolution,
  DROP INDEX uq_discrepancy_open,
  DROP COLUMN open_flag,
  ADD COLUMN open_flag TINYINT GENERATED ALWAYS AS (IF(resolved_at IS NULL,1,NULL)) STORED,
  ADD UNIQUE KEY uq_discrepancy_open (transfer_item_id, open_flag);

ALTER TABLE retirement_request
  DROP CHECK ck_retirement_request_open_flag,
  DROP CHECK ck_retirement_request_state_flags,
  DROP INDEX uq_retirement_request_open,
  DROP COLUMN open_flag,
  ADD COLUMN open_flag TINYINT GENERATED ALWAYS AS (IF(request_state IN ('PROPUESTA','APROBADA'),1,NULL)) STORED,
  ADD UNIQUE KEY uq_retirement_request_open (asset_id, open_flag);

ALTER TABLE signature_request
  DROP CHECK ck_signature_request_active_flag,
  DROP CHECK ck_signature_request_state_flag,
  DROP INDEX uq_signature_request_active,
  DROP COLUMN active_flag,
  ADD COLUMN active_flag TINYINT GENERATED ALWAYS AS (IF(request_state IN ('CREADA','PENDIENTE'),1,NULL)) STORED,
  ADD UNIQUE KEY uq_signature_request_active (act_id, active_flag);

ALTER TABLE resolution
  DROP CHECK ck_resolution_state_flags,
  DROP INDEX uq_resolution_pending, DROP INDEX uq_resolution_executed,
  DROP COLUMN pending_flag, DROP COLUMN executed_flag,
  ADD COLUMN pending_flag TINYINT GENERATED ALWAYS AS (IF(resolution_state IN ('PROPUESTA','APROBADA'),1,NULL)) STORED,
  ADD COLUMN executed_flag TINYINT GENERATED ALWAYS AS (IF(resolution_state = 'EJECUTADA',1,NULL)) STORED,
  ADD UNIQUE KEY uq_resolution_pending (discrepancy_id,pending_flag),
  ADD UNIQUE KEY uq_resolution_executed (discrepancy_id,executed_flag);

ALTER TABLE reception_item
  DROP CHECK ck_reception_item_claimed_flag,
  DROP INDEX uq_reception_item_claimed,
  DROP COLUMN claimed_flag,
  ADD COLUMN voided_at DATETIME(6) NULL,
  ADD COLUMN claimed_flag TINYINT GENERATED ALWAYS AS (IF(voided_at IS NULL,1,NULL)) STORED,
  ADD UNIQUE KEY uq_reception_item_claimed (transfer_item_id,claimed_flag),
  ADD UNIQUE KEY uq_reception_item_once (reception_id,transfer_item_id);

ALTER TABLE asset ADD COLUMN physical_center_id BINARY(16) NULL,
  ADD COLUMN permanent_code_prefix VARCHAR(10) COLLATE utf8mb4_bin NULL;
UPDATE asset a JOIN organization o ON o.id = a.organization_id
 SET a.physical_center_id = IF(a.current_location_id IS NULL,NULL,a.registral_center_id),
     a.permanent_code_prefix = o.asset_code_prefix;
ALTER TABLE asset
  DROP FOREIGN KEY fk_asset_location,
  MODIFY permanent_code_prefix VARCHAR(10) COLLATE utf8mb4_bin NOT NULL,
  ADD COLUMN permanent_code VARCHAR(26) COLLATE utf8mb4_bin GENERATED ALWAYS AS
    (CONCAT(permanent_code_prefix,'-',LPAD(permanent_code_seq,12,'0'),'-',LPAD(permanent_code_check,2,'0'))) STORED,
  ADD UNIQUE KEY uq_asset_code_display (organization_id,permanent_code),
  ADD CONSTRAINT fk_asset_physical_center FOREIGN KEY (organization_id,physical_center_id) REFERENCES center(organization_id,id),
  ADD CONSTRAINT fk_asset_physical_location FOREIGN KEY (physical_center_id,current_location_id) REFERENCES location(center_id,id),
  ADD CONSTRAINT ck_asset_physical_location CHECK (current_location_id IS NULL OR physical_center_id IS NOT NULL),
  ADD CONSTRAINT ck_asset_code_checksum CHECK (permanent_code_check = 98 - MOD(permanent_code_seq * 100,97)),
  ADD CONSTRAINT ck_asset_code_prefix CHECK (permanent_code_prefix REGEXP '^[A-Z0-9]{1,10}$');

ALTER TABLE location ADD CONSTRAINT ck_location_not_self CHECK (parent_location_id IS NULL OR parent_location_id <> id);
ALTER TABLE asset_category ADD CONSTRAINT ck_category_not_self CHECK (parent_category_id IS NULL OR parent_category_id <> id);
ALTER TABLE code_sequence ADD CONSTRAINT ck_sequence_capacity CHECK (current_value <= 999999999999);
ALTER TABLE idempotency_key DROP INDEX uq_idempotency_key_scope,
  ADD UNIQUE KEY uq_idempotency_key_scope (organization_id,actor_user_id,operation,idem_key);
ALTER TABLE stored_file ADD COLUMN legal_hold BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN object_version VARCHAR(255) NULL,
  ADD CONSTRAINT ck_file_clean_metadata CHECK (file_state <> 'LIMPIO' OR (sha256_hash IS NOT NULL AND analyzed_at IS NOT NULL AND content_type IS NOT NULL));
ALTER TABLE outbox_event
  ADD UNIQUE KEY uq_outbox_event_key (organization_id,event_key),
  ADD COLUMN lease_token BINARY(16) NULL, ADD COLUMN leased_until DATETIME(6) NULL;
ALTER TABLE job
  ADD COLUMN lease_token BINARY(16) NULL, ADD COLUMN leased_until DATETIME(6) NULL,
  ADD COLUMN deduplication_key VARCHAR(200) COLLATE utf8mb4_bin NULL,
  ADD UNIQUE KEY uq_job_deduplication (organization_id,deduplication_key),
  ADD KEY idx_job_lease (job_state,leased_until);
ALTER TABLE notification_event
  ADD COLUMN source_event_id BINARY(16) NULL,
  ADD COLUMN channel VARCHAR(20) NOT NULL DEFAULT 'IN_APP',
  ADD COLUMN attempts INT UNSIGNED NOT NULL DEFAULT 0,
  ADD COLUMN next_attempt_at DATETIME(6) NULL,
  ADD COLUMN last_error_code VARCHAR(100) NULL,
  ADD KEY idx_notification_retry (delivery_state,next_attempt_at),
  ADD CONSTRAINT fk_notification_source FOREIGN KEY (source_event_id) REFERENCES outbox_event(id);
