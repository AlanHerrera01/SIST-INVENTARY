-- Fase 02 — Migración V2.
-- Área: usuarios locales, roles, permisos, alcances y custodios (Fase 00 §3 y §4).
--
-- El usuario local refleja al sujeto del proveedor de identidad; no almacena
-- credenciales. Las columnas de identidad usan utf8mb4_bin porque `sub` y el
-- emisor OIDC son sensibles a mayúsculas y no deben colisionar en claves
-- únicas bajo una intercalación insensible.

-- Usuario local de aplicación. El correo no es clave de identidad (§3).
CREATE TABLE app_user (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  organization_id BINARY(16) NOT NULL,
  identity_issuer VARCHAR(255) NOT NULL COLLATE utf8mb4_bin,
  identity_subject VARCHAR(255) NOT NULL COLLATE utf8mb4_bin,
  display_name VARCHAR(200) NOT NULL,
  email_address VARCHAR(320) NULL,
  user_state VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_app_user PRIMARY KEY (id),
  CONSTRAINT uq_app_user_identity UNIQUE (organization_id, identity_issuer, identity_subject),
  CONSTRAINT fk_app_user_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
  CONSTRAINT ck_app_user_state CHECK (user_state IN ('ACTIVO', 'INACTIVO'))
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Rol por organización. Los códigos no se restringen en SQL: la administración
-- de roles es operación de aplicación auditable (Fase 00 §9 nota final).
CREATE TABLE app_role (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  organization_id BINARY(16) NOT NULL,
  code VARCHAR(50) NOT NULL,
  name VARCHAR(200) NOT NULL,
  description VARCHAR(500) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_app_role PRIMARY KEY (id),
  CONSTRAINT uq_app_role_organization_code UNIQUE (organization_id, code),
  CONSTRAINT fk_app_role_organization FOREIGN KEY (organization_id) REFERENCES organization (id)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Catálogo global de permisos: capacidades del sistema que el backend debe
-- verificar en cada mutación (INV-06). Los códigos los define el código de
-- aplicación; la tabla permite persistir asignaciones y consultas.
CREATE TABLE permission (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  code VARCHAR(80) NOT NULL,
  name VARCHAR(200) NOT NULL,
  description VARCHAR(500) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_permission PRIMARY KEY (id),
  CONSTRAINT uq_permission_code UNIQUE (code)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Unión rol-permiso. Tabla pura de unión: clave primaria compuesta, sin
-- identificador sustituto.
CREATE TABLE role_permission (
  role_id BINARY(16) NOT NULL,
  permission_id BINARY(16) NOT NULL,
  CONSTRAINT pk_role_permission PRIMARY KEY (role_id, permission_id),
  CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES app_role (id),
  CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES permission (id)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Asignación de rol con vigencia, autor del cambio y revocación (§3).
-- active_flag = 1 mientras vigente; NULL tras revocación. El patrón
-- UNIQUE (..., active_flag) impide dos asignaciones activas del mismo rol
-- porque los NULL no colisionan en índices únicos de MySQL.
CREATE TABLE user_role_assignment (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  user_id BINARY(16) NOT NULL,
  role_id BINARY(16) NOT NULL,
  scope_kind VARCHAR(20) NOT NULL,
  valid_from DATETIME(6) NOT NULL,
  valid_until DATETIME(6) NULL,
  active_flag TINYINT(1) NOT NULL DEFAULT 1,
  granted_by_user_id BINARY(16) NOT NULL,
  revoked_at DATETIME(6) NULL,
  revoke_reason VARCHAR(500) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_user_role_assignment PRIMARY KEY (id),
  CONSTRAINT uq_user_role_assignment_active UNIQUE (user_id, role_id, active_flag),
  CONSTRAINT fk_user_role_assignment_user FOREIGN KEY (user_id) REFERENCES app_user (id),
  CONSTRAINT fk_user_role_assignment_role FOREIGN KEY (role_id) REFERENCES app_role (id),
  CONSTRAINT fk_user_role_assignment_grantor FOREIGN KEY (granted_by_user_id) REFERENCES app_user (id),
  CONSTRAINT ck_user_role_assignment_scope CHECK (scope_kind IN ('GLOBAL', 'CENTROS')),
  CONSTRAINT ck_user_role_assignment_validity CHECK (valid_until IS NULL OR valid_until >= valid_from),
  CONSTRAINT ck_user_role_assignment_active_flag CHECK (active_flag IS NULL OR active_flag = 1),
  CONSTRAINT ck_user_role_assignment_revocation CHECK (
    (active_flag = 1 AND revoked_at IS NULL)
    OR (active_flag IS NULL AND revoked_at IS NOT NULL)
  )
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Centros incluidos en el alcance de una asignación de rol. Solo aplica con
-- scope_kind = 'CENTROS'; la coherencia entre bandera y filas es verificación
-- de aplicación.
CREATE TABLE user_role_assignment_center (
  role_assignment_id BINARY(16) NOT NULL,
  center_id BINARY(16) NOT NULL,
  CONSTRAINT pk_user_role_assignment_center PRIMARY KEY (role_assignment_id, center_id),
  CONSTRAINT fk_urac_assignment FOREIGN KEY (role_assignment_id) REFERENCES user_role_assignment (id) ON DELETE CASCADE,
  CONSTRAINT fk_urac_center FOREIGN KEY (center_id) REFERENCES center (id)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Persona/custodio: no todo custodio necesita cuenta (§3). La clave compuesta
-- garantiza que el centro de vínculo pertenece a la organización declarada.
CREATE TABLE custodian (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  organization_id BINARY(16) NOT NULL,
  center_id BINARY(16) NULL,
  display_name VARCHAR(200) NOT NULL,
  document_number VARCHAR(50) NULL,
  linked_user_id BINARY(16) NULL,
  custodian_state VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_custodian PRIMARY KEY (id),
  CONSTRAINT fk_custodian_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
  CONSTRAINT fk_custodian_center FOREIGN KEY (organization_id, center_id) REFERENCES center (organization_id, id),
  CONSTRAINT fk_custodian_linked_user FOREIGN KEY (linked_user_id) REFERENCES app_user (id),
  CONSTRAINT ck_custodian_state CHECK (custodian_state IN ('ACTIVO', 'INACTIVO'))
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;
