-- Privilegios de migración: CREATE ROUTINE, ALTER ROUTINE, TRIGGER además de DDL/DML.
-- Con binlog activo, el DBA habilita log_bin_trust_function_creators para este entorno.
-- Este procedimiento no inicia ni confirma transacciones: el llamador debe usar una
-- transacción explícita y confirmar numeración + activo/traslado + auditoría + outbox.
-- Reconciliar el contador con identidades existentes antes de permitir nuevas asignaciones.
INSERT INTO code_sequence(organization_id,sequence_name,current_value)
SELECT source.organization_id,'ASSET_PERMANENT_CODE',source.high_watermark
FROM (SELECT organization_id,MAX(permanent_code_seq) AS high_watermark FROM asset GROUP BY organization_id) AS source
ON DUPLICATE KEY UPDATE current_value=GREATEST(code_sequence.current_value,source.high_watermark);
INSERT INTO code_sequence(organization_id,sequence_name,current_value)
SELECT source.organization_id,'TRANSFER_NUMBER',source.high_watermark
FROM (SELECT organization_id,MAX(transfer_number) AS high_watermark FROM transfer GROUP BY organization_id) AS source
ON DUPLICATE KEY UPDATE current_value=GREATEST(code_sequence.current_value,source.high_watermark);

DELIMITER $$
CREATE PROCEDURE allocate_code_number(
  IN p_organization_id BINARY(16), IN p_sequence_name VARCHAR(50), OUT p_number BIGINT UNSIGNED
)
SQL SECURITY INVOKER
MODIFIES SQL DATA
BEGIN
  DECLARE v_last BIGINT UNSIGNED;
  IF p_sequence_name IS NULL OR p_sequence_name NOT IN ('ASSET_PERMANENT_CODE','TRANSFER_NUMBER') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unsupported sequence';
  END IF;
  INSERT INTO code_sequence(organization_id,sequence_name,current_value)
    VALUES(p_organization_id,p_sequence_name,0)
    ON DUPLICATE KEY UPDATE current_value = code_sequence.current_value;
  SELECT current_value INTO v_last FROM code_sequence
    WHERE organization_id=p_organization_id AND sequence_name=p_sequence_name FOR UPDATE;
  IF v_last >= 999999999999 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Sequence exhausted';
  END IF;
  SET p_number = v_last + 1;
  UPDATE code_sequence SET current_value=p_number
    WHERE organization_id=p_organization_id AND sequence_name=p_sequence_name;
END$$

CREATE TRIGGER code_sequence_monotonic BEFORE UPDATE ON code_sequence FOR EACH ROW
BEGIN
  IF NEW.current_value < OLD.current_value OR NEW.organization_id <> OLD.organization_id
     OR NEW.sequence_name <> OLD.sequence_name THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Sequence identity and high-water mark are permanent';
  END IF;
END$$
CREATE TRIGGER code_sequence_no_delete BEFORE DELETE ON code_sequence FOR EACH ROW
BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Sequence cannot be deleted'; END$$

CREATE TRIGGER asset_permanent_identity BEFORE UPDATE ON asset FOR EACH ROW
BEGIN
  IF NEW.id <> OLD.id OR NEW.organization_id <> OLD.organization_id
     OR NEW.permanent_code_seq <> OLD.permanent_code_seq
     OR CAST(NEW.permanent_code_prefix AS BINARY) <> CAST(OLD.permanent_code_prefix AS BINARY)
     OR NEW.permanent_code_check <> OLD.permanent_code_check THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Permanent asset identity cannot change';
  END IF;
END$$
CREATE TRIGGER asset_no_delete BEFORE DELETE ON asset FOR EACH ROW
BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Retire assets instead of deleting their identity'; END$$

CREATE TRIGGER audit_event_no_update BEFORE UPDATE ON audit_event FOR EACH ROW
BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Audit history is append only'; END$$
CREATE TRIGGER audit_event_no_delete BEFORE DELETE ON audit_event FOR EACH ROW
BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Audit history is append only'; END$$
CREATE TRIGGER asset_event_no_update BEFORE UPDATE ON asset_event FOR EACH ROW
BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Asset history is append only'; END$$
CREATE TRIGGER asset_event_no_delete BEFORE DELETE ON asset_event FOR EACH ROW
BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Asset history is append only'; END$$
CREATE TRIGGER reclassification_no_update BEFORE UPDATE ON asset_reclassification FOR EACH ROW
BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Reclassification history is append only'; END$$
CREATE TRIGGER reclassification_no_delete BEFORE DELETE ON asset_reclassification FOR EACH ROW
BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Reclassification history is append only'; END$$

CREATE TRIGGER act_frozen_content BEFORE UPDATE ON act FOR EACH ROW
BEGIN
  IF OLD.act_state <> 'BORRADOR' AND (
    NOT (NEW.content_hash <=> OLD.content_hash) OR NOT (NEW.file_id <=> OLD.file_id)
    OR NOT (NEW.content_snapshot <=> OLD.content_snapshot) OR NEW.content_version <> OLD.content_version
    OR NEW.organization_id <> OLD.organization_id OR NEW.act_type <> OLD.act_type
    OR NOT (NEW.transfer_id <=> OLD.transfer_id) OR NOT (NEW.reception_id <=> OLD.reception_id)
    OR NOT (NEW.resolution_id <=> OLD.resolution_id) OR NOT (NEW.retirement_request_id <=> OLD.retirement_request_id)
    OR NOT (NEW.issued_at <=> OLD.issued_at) OR NEW.act_state = 'BORRADOR') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Issued act content is immutable; create a correction';
  END IF;
END$$
CREATE TRIGGER act_no_delete BEFORE DELETE ON act FOR EACH ROW
BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Act history must be retained'; END$$
CREATE TRIGGER signature_final_no_update BEFORE UPDATE ON signature FOR EACH ROW
BEGIN
  IF OLD.signature_state IN ('FIRMADA','RECHAZADA','VENCIDA','CANCELADA') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Final signature evidence is immutable';
  END IF;
END$$
CREATE TRIGGER signature_no_delete BEFORE DELETE ON signature FOR EACH ROW
BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Signature history must be retained'; END$$
DELIMITER ;

-- Atributos: la definición, tipo de valor y categoría deben coincidir.
ALTER TABLE asset ADD UNIQUE KEY uq_asset_category_identity (id,category_id);
ALTER TABLE asset_category_attribute ADD UNIQUE KEY uq_attribute_category_type (id,category_id,value_type);
ALTER TABLE asset_attribute_value ADD COLUMN category_id BINARY(16) NULL,
  ADD COLUMN value_type VARCHAR(20) NULL;
UPDATE asset_attribute_value v JOIN asset a ON a.id=v.asset_id
 JOIN asset_category_attribute d ON d.id=v.category_attribute_id
 SET v.category_id=a.category_id,v.value_type=d.value_type;
ALTER TABLE asset_attribute_value MODIFY category_id BINARY(16) NOT NULL,
  MODIFY value_type VARCHAR(20) NOT NULL,
  ADD CONSTRAINT fk_attribute_value_asset_category FOREIGN KEY(asset_id,category_id) REFERENCES asset(id,category_id),
  ADD CONSTRAINT fk_attribute_value_definition FOREIGN KEY(category_attribute_id,category_id,value_type) REFERENCES asset_category_attribute(id,category_id,value_type),
  ADD CONSTRAINT ck_attribute_value_type CHECK (
    (value_type IN ('TEXTO','LISTA') AND value_text IS NOT NULL)
    OR (value_type='NUMERO' AND value_number IS NOT NULL)
    OR (value_type='FECHA' AND value_datetime IS NOT NULL)
    OR (value_type='BOOLEANO' AND value_boolean IS NOT NULL));

ALTER TABLE outbox_event ADD CONSTRAINT ck_outbox_lease CHECK
  ((lease_token IS NULL AND leased_until IS NULL) OR (lease_token IS NOT NULL AND leased_until IS NOT NULL));
ALTER TABLE job ADD CONSTRAINT ck_job_lease CHECK
  ((lease_token IS NULL AND leased_until IS NULL) OR (lease_token IS NOT NULL AND leased_until IS NOT NULL));

ALTER TABLE act ADD UNIQUE KEY uq_act_content_hash (id,content_hash),
  ADD CONSTRAINT ck_act_issued_snapshot CHECK (act_state='BORRADOR' OR content_snapshot IS NOT NULL);
ALTER TABLE signature_request ADD CONSTRAINT fk_request_exact_act_hash
  FOREIGN KEY (act_id,content_hash) REFERENCES act(id,content_hash);
ALTER TABLE signature ADD COLUMN identity_evidence JSON NULL,
  ADD CONSTRAINT ck_signature_evidence CHECK (signature_state <> 'FIRMADA' OR evidence_file_id IS NOT NULL OR identity_evidence IS NOT NULL);

DELIMITER $$
CREATE TRIGGER signature_request_frozen BEFORE UPDATE ON signature_request FOR EACH ROW
BEGIN
  IF NEW.act_id <> OLD.act_id OR NEW.content_hash <> OLD.content_hash
     OR NEW.signature_policy <> OLD.signature_policy OR NEW.organization_id <> OLD.organization_id THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Signature request content and policy are immutable';
  END IF;
END$$
DELIMITER ;
