-- Fase 02 — Migración V11.
-- Área: auditoría, outbox, idempotencia, notificaciones y trabajos (Fase 00
-- §1.2, §2, §3, §9.1, §10.2; INV-09).
--
-- Tablas de infraestructura transversal. La auditoría es append-only a nivel
-- de aplicación: sin disparadores ni procedimientos (el usuario de
-- aplicación no posee SUPER y el registro binario está activo en la imagen
-- oficial mysql:8.4), la protección contra UPDATE/DELETE por la cuenta de
-- aplicación se endurecerá con cuentas separadas para migrar/aplicar o un
-- guion de DBA fuera de Flyway (decisión documentada). Las referencias a
-- entidades de negocio desde auditoría, outbox, notificaciones y trabajos
-- son polimórficas (entity/aggregate/related): la integridad se verifica en
-- la aplicación y se documentan como excepciones deliberadas.

-- Evento de auditoría (§3, §10.2). Sin updated_at por diseño append-only.
-- Actor efectivo y original para reflejar delegaciones; entity_id es
-- polimórfico. Los eventos denegados se registran sin confirmar transacción
-- rechazada (§10.2).
CREATE TABLE audit_event (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  organization_id BINARY(16) NOT NULL,
  center_id BINARY(16) NULL,
  effective_actor_user_id BINARY(16) NULL,
  original_actor_user_id BINARY(16) NULL,
  action VARCHAR(80) NOT NULL,
  entity_type VARCHAR(50) NOT NULL,
  entity_id BINARY(16) NULL,
  outcome VARCHAR(20) NOT NULL,
  occurred_at DATETIME(6) NOT NULL,
  correlation_id VARCHAR(64) NULL,
  motive VARCHAR(500) NULL,
  changed_fields JSON NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_audit_event PRIMARY KEY (id),
  CONSTRAINT fk_audit_event_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
  CONSTRAINT fk_audit_event_center FOREIGN KEY (organization_id, center_id) REFERENCES center (organization_id, id),
  CONSTRAINT fk_audit_event_effective_actor FOREIGN KEY (effective_actor_user_id) REFERENCES app_user (id),
  CONSTRAINT fk_audit_event_original_actor FOREIGN KEY (original_actor_user_id) REFERENCES app_user (id),
  -- Resultado de la acción auditada (§3): EXITO, DENEGADO o ERROR.
  CONSTRAINT ck_audit_event_outcome CHECK (outcome IN ('EXITO', 'DENEGADO', 'ERROR')),
  KEY idx_audit_event_scope (organization_id, center_id, occurred_at),
  KEY idx_audit_event_entity (organization_id, entity_type, entity_id),
  KEY idx_audit_event_effective_actor (effective_actor_user_id, occurred_at),
  KEY idx_audit_event_correlation (correlation_id),
  KEY idx_audit_event_occurred (occurred_at)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Bandeja transaccional de eventos (§1.2): la transacción de negocio inserta
-- el evento y el procesador lo entrega al menos una vez; los consumidores
-- deduplican por id de evento o event_key. aggregate_id es polimórfico.
CREATE TABLE outbox_event (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  organization_id BINARY(16) NOT NULL,
  event_type VARCHAR(100) NOT NULL,
  aggregate_entity VARCHAR(50) NOT NULL,
  aggregate_id BINARY(16) NOT NULL,
  event_key VARCHAR(200) NULL,
  payload JSON NOT NULL,
  delivery_state VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  attempts SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  next_attempt_at DATETIME(6) NULL,
  delivered_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_outbox_event PRIMARY KEY (id),
  CONSTRAINT fk_outbox_event_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
  -- FALLIDO es la cola de fallos visibles (§1.2): no reintenta solo.
  CONSTRAINT ck_outbox_event_delivery_state CHECK (delivery_state IN ('PENDIENTE', 'ENTREGADO', 'FALLIDO')),
  CONSTRAINT ck_outbox_event_delivery CHECK (
    (delivery_state IN ('PENDIENTE', 'FALLIDO') AND delivered_at IS NULL)
    OR (delivery_state = 'ENTREGADO' AND delivered_at IS NOT NULL)
  ),
  -- Un evento entregado no programa nuevos reintentos.
  CONSTRAINT ck_outbox_event_no_retry_after_delivery CHECK (delivered_at IS NULL OR next_attempt_at IS NULL),
  KEY idx_outbox_event_pending (delivery_state, next_attempt_at),
  KEY idx_outbox_event_aggregate (aggregate_entity, aggregate_id)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Clave de idempotencia por actor, organización y operación (§9.1). Misma
-- clave y misma huella de cuerpo devuelve el mismo resultado; huella
-- distinta es conflicto de aplicación. La expiración de la clave no libera
-- las restricciones de dominio contra duplicados (§9.1).
CREATE TABLE idempotency_key (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  organization_id BINARY(16) NOT NULL,
  actor_user_id BINARY(16) NOT NULL,
  idem_key VARCHAR(100) NOT NULL,
  operation VARCHAR(100) NOT NULL,
  request_fingerprint CHAR(64) NULL,
  response_snapshot JSON NULL,
  expires_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_idempotency_key PRIMARY KEY (id),
  CONSTRAINT uq_idempotency_key_scope UNIQUE (organization_id, actor_user_id, idem_key),
  CONSTRAINT fk_idempotency_key_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
  CONSTRAINT fk_idempotency_key_actor FOREIGN KEY (actor_user_id) REFERENCES app_user (id),
  CONSTRAINT ck_idempotency_key_fingerprint CHECK (request_fingerprint IS NULL OR request_fingerprint REGEXP '^[0-9a-f]{64}$'),
  -- Retención inicial propuesta de siete días (§9.1); la purga es trabajo
  -- de la aplicación.
  CONSTRAINT ck_idempotency_key_expiry CHECK (expires_at IS NULL OR expires_at > created_at),
  KEY idx_idempotency_key_expiry (expires_at)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Notificación pendiente (§1.2, §2): una notificación nunca modifica
-- estados de negocio. Destinatario usuario o centro completo (al menos uno).
CREATE TABLE notification_event (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  organization_id BINARY(16) NOT NULL,
  center_id BINARY(16) NULL,
  recipient_user_id BINARY(16) NULL,
  target_center_id BINARY(16) NULL,
  notification_kind VARCHAR(50) NOT NULL,
  payload JSON NOT NULL,
  delivery_state VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  delivered_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_notification_event PRIMARY KEY (id),
  CONSTRAINT fk_notification_event_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
  CONSTRAINT fk_notification_event_center FOREIGN KEY (organization_id, center_id) REFERENCES center (organization_id, id),
  CONSTRAINT fk_notification_event_recipient FOREIGN KEY (recipient_user_id) REFERENCES app_user (id),
  CONSTRAINT fk_notification_event_target_center FOREIGN KEY (organization_id, target_center_id) REFERENCES center (organization_id, id),
  CONSTRAINT ck_notification_event_delivery_state CHECK (delivery_state IN ('PENDIENTE', 'ENVIADO', 'FALLIDO')),
  CONSTRAINT ck_notification_event_recipient CHECK (recipient_user_id IS NOT NULL OR target_center_id IS NOT NULL),
  CONSTRAINT ck_notification_event_delivery CHECK (
    (delivery_state IN ('PENDIENTE', 'FALLIDO') AND delivered_at IS NULL)
    OR (delivery_state = 'ENVIADO' AND delivered_at IS NOT NULL)
  ),
  KEY idx_notification_event_pending (delivery_state),
  KEY idx_notification_event_recipient (recipient_user_id),
  KEY idx_notification_event_target_center (target_center_id)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Trabajo asíncrono (§1.2, §9): etiquetas, PDF, exportaciones, análisis.
-- El escalado de reintentos y la cola de fallos la gestiona el procesador;
-- la referencia relacionada es polimórfica.
CREATE TABLE job (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  organization_id BINARY(16) NOT NULL,
  job_type VARCHAR(80) NOT NULL,
  job_state VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  priority TINYINT UNSIGNED NOT NULL DEFAULT 5,
  payload JSON NULL,
  related_entity VARCHAR(30) NULL,
  related_id BINARY(16) NULL,
  attempts SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  next_attempt_at DATETIME(6) NULL,
  completed_at DATETIME(6) NULL,
  result_snapshot JSON NULL,
  last_error VARCHAR(1000) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_job PRIMARY KEY (id),
  CONSTRAINT fk_job_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
  -- FALLIDO permanece visible en la cola de fallos (§1.2); no es terminal a
  -- nivel de esquema porque puede reintentarse manualmente.
  CONSTRAINT ck_job_state CHECK (job_state IN ('PENDIENTE', 'EN_PROCESO', 'COMPLETADO', 'FALLIDO')),
  CONSTRAINT ck_job_priority CHECK (priority BETWEEN 1 AND 9),
  CONSTRAINT ck_job_related CHECK (
    (related_entity IS NULL AND related_id IS NULL)
    OR (related_entity IS NOT NULL AND related_id IS NOT NULL)
  ),
  -- Solo COMPLETADO registra fin; el error del último intento se conserva
  -- sin sobrescribir el resultado.
  CONSTRAINT ck_job_completion CHECK (
    (job_state = 'COMPLETADO' AND completed_at IS NOT NULL)
    OR (job_state <> 'COMPLETADO' AND completed_at IS NULL)
  ),
  KEY idx_job_pending (job_state, priority, next_attempt_at),
  KEY idx_job_related (related_entity, related_id)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;
