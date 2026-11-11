-- Fase 06 — Estructura organizacional multicentro.
-- Ciudades, sedes, matrices receptoras, tipos de centro, bodegas/ubicaciones
-- y responsables con historial. Todas las tablas llevan organization_id para
-- aislamiento de tenant con claves foráneas compuestas.

-- Ciudad: divisiones geográficas de la operación (Quito, Guayaquil, Cuenca…).
CREATE TABLE city (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  organization_id BINARY(16) NOT NULL,
  code VARCHAR(20) NOT NULL,
  name VARCHAR(120) NOT NULL,
  city_state VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_city PRIMARY KEY (id),
  CONSTRAINT uq_city_organization_code UNIQUE (organization_id, code),
  CONSTRAINT uq_city_organization_id UNIQUE (organization_id, id),
  CONSTRAINT fk_city_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
  CONSTRAINT ck_city_state CHECK (city_state IN ('ACTIVO', 'INACTIVO'))
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Sede: establecimiento físico dentro de una ciudad. site_kind distingue la
-- matriz (sede principal) de sucursales y sedes administrativas.
CREATE TABLE site (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  organization_id BINARY(16) NOT NULL,
  city_id BINARY(16) NOT NULL,
  code VARCHAR(20) NOT NULL,
  name VARCHAR(200) NOT NULL,
  site_kind VARCHAR(20) NOT NULL DEFAULT 'SEDE',
  site_state VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_site PRIMARY KEY (id),
  CONSTRAINT uq_site_organization_code UNIQUE (organization_id, code),
  CONSTRAINT uq_site_organization_id UNIQUE (organization_id, id),
  CONSTRAINT fk_site_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
  CONSTRAINT fk_site_city FOREIGN KEY (organization_id, city_id) REFERENCES city (organization_id, id),
  CONSTRAINT ck_site_kind CHECK (site_kind IN ('MATRIZ', 'SUCURSAL', 'SEDE')),
  CONSTRAINT ck_site_state CHECK (site_state IN ('ACTIVO', 'INACTIVO'))
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Matriz receptora: punto de recepción/redistribución de inventario. Es
-- OPCIONAL para un centro (un centro puede operar sin matriz receptora).
CREATE TABLE receiving_matrix (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  organization_id BINARY(16) NOT NULL,
  city_id BINARY(16) NULL,
  site_id BINARY(16) NULL,
  code VARCHAR(20) NOT NULL,
  name VARCHAR(200) NOT NULL,
  matrix_state VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
  description VARCHAR(500) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_receiving_matrix PRIMARY KEY (id),
  CONSTRAINT uq_receiving_matrix_organization_code UNIQUE (organization_id, code),
  CONSTRAINT uq_receiving_matrix_organization_id UNIQUE (organization_id, id),
  CONSTRAINT fk_receiving_matrix_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
  CONSTRAINT fk_receiving_matrix_city FOREIGN KEY (organization_id, city_id) REFERENCES city (organization_id, id),
  CONSTRAINT fk_receiving_matrix_site FOREIGN KEY (organization_id, site_id) REFERENCES site (organization_id, id),
  CONSTRAINT ck_receiving_matrix_state CHECK (matrix_state IN ('ACTIVO', 'INACTIVO'))
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Vínculo matriz receptora ↔ centro (relación muchos a muchos opcional).
CREATE TABLE receiving_matrix_center (
  organization_id BINARY(16) NOT NULL,
  matrix_id BINARY(16) NOT NULL,
  center_id BINARY(16) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_receiving_matrix_center PRIMARY KEY (matrix_id, center_id),
  CONSTRAINT fk_rmc_matrix FOREIGN KEY (organization_id, matrix_id) REFERENCES receiving_matrix (organization_id, id),
  CONSTRAINT fk_rmc_center FOREIGN KEY (organization_id, center_id) REFERENCES center (organization_id, id)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Centro: se enriquece con ciudad, sede y tipo (centro médico, bodega TI…).
ALTER TABLE center
  ADD COLUMN city_id BINARY(16) NULL AFTER organization_id,
  ADD COLUMN site_id BINARY(16) NULL,
  ADD COLUMN center_kind VARCHAR(30) NOT NULL DEFAULT 'OTRO',
  ADD CONSTRAINT fk_center_city FOREIGN KEY (organization_id, city_id) REFERENCES city (organization_id, id),
  ADD CONSTRAINT fk_center_site FOREIGN KEY (organization_id, site_id) REFERENCES site (organization_id, id),
  ADD CONSTRAINT ck_center_kind CHECK (center_kind IN ('MEDICO', 'BODEGA_TI', 'ADMINISTRATIVO', 'OTRO')),
  ADD KEY idx_center_city (organization_id, city_id);

-- Responsable de centro: jefatura, coordinación o responsable TI asociado a
-- un usuario local y a un correo institucional. user_id admite NULL para
-- registrar responsables aún no provisionados como usuarios.
CREATE TABLE center_responsible (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  organization_id BINARY(16) NOT NULL,
  center_id BINARY(16) NOT NULL,
  user_id BINARY(16) NULL,
  responsibility_role VARCHAR(30) NOT NULL,
  institutional_email VARCHAR(200) NOT NULL,
  valid_from DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  valid_until DATETIME(6) NULL,
  responsible_state VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
  assigned_by BINARY(16) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_center_responsible PRIMARY KEY (id),
  CONSTRAINT uq_center_responsible_organization_id UNIQUE (organization_id, id),
  CONSTRAINT fk_responsible_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
  CONSTRAINT fk_responsible_center FOREIGN KEY (organization_id, center_id) REFERENCES center (organization_id, id),
  CONSTRAINT fk_responsible_user FOREIGN KEY (user_id) REFERENCES app_user (id),
  CONSTRAINT fk_responsible_assigned_by FOREIGN KEY (assigned_by) REFERENCES app_user (id),
  CONSTRAINT ck_responsibility_role CHECK (responsibility_role IN ('JEFE_CENTRO', 'COORDINADOR', 'RESPONSABLE_TI')),
  CONSTRAINT ck_responsible_state CHECK (responsible_state IN ('ACTIVO', 'INACTIVO')),
  KEY idx_responsible_center (organization_id, center_id, responsible_state)
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;

-- Historial de responsables: cada alta, cambio o cierre deja una instantánea
-- completa del registro anterior, de modo que la trazabilidad no depende solo
-- de audit_event.
CREATE TABLE center_responsible_history (
  id BINARY(16) NOT NULL DEFAULT (UUID_TO_BIN(UUID(), 1)),
  organization_id BINARY(16) NOT NULL,
  responsible_id BINARY(16) NOT NULL,
  action VARCHAR(30) NOT NULL,
  center_id BINARY(16) NULL,
  user_id BINARY(16) NULL,
  responsibility_role VARCHAR(30) NULL,
  institutional_email VARCHAR(200) NULL,
  valid_from DATETIME(6) NULL,
  valid_until DATETIME(6) NULL,
  changed_by BINARY(16) NULL,
  changed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  reason VARCHAR(300) NULL,
  CONSTRAINT pk_center_responsible_history PRIMARY KEY (id),
  CONSTRAINT fk_crh_responsible FOREIGN KEY (organization_id, responsible_id)
    REFERENCES center_responsible (organization_id, id),
  CONSTRAINT fk_crh_changed_by FOREIGN KEY (changed_by) REFERENCES app_user (id),
  CONSTRAINT ck_crh_action CHECK (action IN ('CREATED', 'UPDATED', 'ENDED'))
) ENGINE = InnoDB, DEFAULT CHARSET = utf8mb4, COLLATE = utf8mb4_0900_ai_ci;
