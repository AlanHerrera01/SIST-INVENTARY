-- Fase 04 — Migración V15.
-- Área: matrices organizacionales de firmas y aprobaciones (prompt Fase 04 §4
-- y §6; Fase 00 §3 «Solicitud de firma» y §4 «alcances»).
--
-- Una matriz define qué usuarios están autorizados a firmar o aprobar actas
-- de un tipo dado, en un centro o en toda la organización (center_id NULL).
-- La membresía es la asignación del usuario a la matriz: un usuario puede
-- pertenecer a varias matrices y su alcance efectivo combina matrices y
-- centros de sus asignaciones de rol. La verificación de «firma autorizada»
-- en los endpoints de firma (fases posteriores) consulta estas tablas; la
-- clave compuesta de inquilino impide membresías cruzadas.
--
-- Esta migración crea estructura; no inserta datos de negocio. El catálogo
-- de permisos y los roles iniciales los aprovisiona la aplicación de forma
-- idempotente (com.veris.inventory.access), no Flyway.

-- Nombre de usuario del proveedor de identidad. La identidad lógica sigue
-- siendo (identity_issuer, identity_subject); username es el identificador
-- de acceso en Keycloak y metadato de despliegue local. UNIQUE permite
-- múltiples NULL en filas históricas sin username.
ALTER TABLE app_user
  ADD COLUMN username VARCHAR(100) NULL,
  ADD UNIQUE KEY uq_app_user_username (organization_id, username);

-- Matriz organizacional de firmas/aprobaciones.
CREATE TABLE signature_matrix (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  organization_id BINARY(16) NOT NULL,
  center_id BINARY(16) NULL,
  code VARCHAR(50) NOT NULL,
  name VARCHAR(200) NOT NULL,
  act_type VARCHAR(20) NULL,
  description VARCHAR(500) NULL,
  matrix_state VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_signature_matrix PRIMARY KEY (id),
  CONSTRAINT uq_signature_matrix_code UNIQUE (organization_id, code),
  CONSTRAINT uq_signature_matrix_tenant_id UNIQUE (organization_id, id),
  CONSTRAINT fk_signature_matrix_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
  CONSTRAINT fk_signature_matrix_center FOREIGN KEY (organization_id, center_id) REFERENCES center (organization_id, id),
  -- NULL = la matriz aplica a todos los tipos de acta (ck usa IS NULL OR).
  CONSTRAINT ck_signature_matrix_act_type CHECK (act_type IS NULL OR act_type IN ('DESPACHO', 'RECEPCION', 'FINAL', 'BAJA')),
  CONSTRAINT ck_signature_matrix_state CHECK (matrix_state IN ('ACTIVO', 'INACTIVO'))
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Miembro de una matriz: usuario autorizado con un rol dentro de ella
-- (FIRMANTE firma el contenido; APROBADOR aprueba solicitudes de firma).
-- signer_position fija el orden cuando la política de la matriz lo exige;
-- NULL significa sin orden.
CREATE TABLE signature_matrix_member (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  organization_id BINARY(16) NOT NULL,
  matrix_id BINARY(16) NOT NULL,
  user_id BINARY(16) NOT NULL,
  member_role VARCHAR(20) NOT NULL,
  signer_position SMALLINT UNSIGNED NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_signature_matrix_member PRIMARY KEY (id),
  CONSTRAINT uq_signature_matrix_member UNIQUE (matrix_id, user_id, member_role),
  CONSTRAINT uq_signature_matrix_member_tenant_id UNIQUE (organization_id, id),
  CONSTRAINT fk_signature_matrix_member_matrix FOREIGN KEY (organization_id, matrix_id) REFERENCES signature_matrix (organization_id, id),
  CONSTRAINT fk_signature_matrix_member_user FOREIGN KEY (organization_id, user_id) REFERENCES app_user (organization_id, id),
  CONSTRAINT ck_signature_matrix_member_role CHECK (member_role IN ('FIRMANTE', 'APROBADOR')),
  CONSTRAINT ck_signature_matrix_member_position CHECK (signer_position IS NULL OR signer_position >= 1)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;
