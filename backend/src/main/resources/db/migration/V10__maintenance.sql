-- Fase 02 — Migración V10.
-- Área: mantenimiento de activos (Fase 00 §3 difiere el módulo completo
-- conforme a D-02; la tabla mínima se modela ahora por decisión de Fase 02,
-- ver docs/DECISIONES-FASE-02.md).
--
-- Registro de trabajos preventivos o correctivos sobre un activo. No se
-- impide tener varias obras abiertas por activo: la coordinación real de
-- mantenimientos concurrentes es de la aplicación (decisión documentada).
-- La vinculación con el estado operativo del activo y los eventos
-- INICIO_MANTENIMIENTO / FIN_MANTENIMIENTO ya definidos en asset_event
-- (V5) queda a cargo del dominio en fases posteriores.

CREATE TABLE maintenance_work (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  asset_id BINARY(16) NOT NULL,
  maintenance_kind VARCHAR(20) NOT NULL,
  maintenance_state VARCHAR(20) NOT NULL DEFAULT 'PLANIFICADO',
  description VARCHAR(1000) NOT NULL,
  scheduled_start_at DATETIME(6) NULL,
  scheduled_end_at DATETIME(6) NULL,
  performed_start_at DATETIME(6) NULL,
  performed_end_at DATETIME(6) NULL,
  responsible_custodian_id BINARY(16) NULL,
  performed_by_user_id BINARY(16) NULL,
  result_notes VARCHAR(1000) NULL,
  created_by_user_id BINARY(16) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_maintenance_work PRIMARY KEY (id),
  CONSTRAINT fk_maintenance_work_asset FOREIGN KEY (asset_id) REFERENCES asset (id),
  CONSTRAINT fk_maintenance_work_responsible FOREIGN KEY (responsible_custodian_id) REFERENCES custodian (id),
  CONSTRAINT fk_maintenance_work_performed_by FOREIGN KEY (performed_by_user_id) REFERENCES app_user (id),
  CONSTRAINT fk_maintenance_work_creator FOREIGN KEY (created_by_user_id) REFERENCES app_user (id),
  CONSTRAINT ck_maintenance_work_kind CHECK (maintenance_kind IN ('PREVENTIVO', 'CORRECTIVO')),
  CONSTRAINT ck_maintenance_work_state CHECK (maintenance_state IN ('PLANIFICADO', 'EN_PROCESO', 'COMPLETADO', 'CANCELADO')),
  CONSTRAINT ck_maintenance_work_scheduled_dates CHECK (
    scheduled_start_at IS NULL OR scheduled_end_at IS NULL OR scheduled_end_at >= scheduled_start_at
  ),
  CONSTRAINT ck_maintenance_work_performed_dates CHECK (
    performed_start_at IS NULL OR performed_end_at IS NULL OR performed_end_at >= performed_start_at
  ),
  -- PLANIFICADO exige que no haya ejecución; EN_PROCESO exige inicio; solo
  -- COMPLETADO registra fin; CANCELADO nunca lo registra.
  CONSTRAINT ck_maintenance_work_state_dates CHECK (
    (maintenance_state = 'PLANIFICADO' AND performed_start_at IS NULL AND performed_end_at IS NULL)
    OR (maintenance_state = 'EN_PROCESO' AND performed_start_at IS NOT NULL AND performed_end_at IS NULL)
    OR (maintenance_state = 'COMPLETADO' AND performed_start_at IS NOT NULL AND performed_end_at IS NOT NULL)
    OR (maintenance_state = 'CANCELADO' AND performed_end_at IS NULL)
  ),
  KEY idx_maintenance_work_asset_state (asset_id, maintenance_state),
  KEY idx_maintenance_work_responsible (responsible_custodian_id)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;
