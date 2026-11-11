package com.veris.inventory.organization.services.servicesImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.veris.inventory.config.SecurityProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Aprovisionamiento inicial idempotente de la estructura organizacional
 * (Fase 06 §2-5): garantiza la fila de organización configurada y siembra las
 * ciudades Quito, Guayaquil y Cuenca con sus sedes, centros médicos, bodegas
 * TI y las matrices receptoras de Quito y Guayaquil. Cuenca opera con centro
 * médico sin matriz receptora ni sede TI obligatoria.
 *
 * <p>Solo inserta lo que falta (por código único); nunca modifica ni elimina
 * datos existentes.</p>
 */
@Component
public class InitialOrganizationBootstrap implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(InitialOrganizationBootstrap.class);

    private final JdbcTemplate jdbc;
    private final String organizationId;

    public InitialOrganizationBootstrap(JdbcTemplate jdbc, SecurityProperties security) {
        this.jdbc = jdbc;
        this.organizationId = java.util.UUID.fromString(security.organizationId()).toString();
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureOrganization();
            seedStructure();
        } catch (DataAccessException unavailable) {
            LOG.warn("Aprovisionamiento organizacional omitido: esquema no disponible.");
        }
    }

    private void ensureOrganization() {
        var exists = jdbc.queryForObject("""
                SELECT COUNT(*) FROM organization WHERE id = UUID_TO_BIN(?, 1)
                """, Long.class, organizationId);
        if (exists == null || exists == 0) {
            jdbc.update("""
                    INSERT INTO organization (id, name) VALUES (UUID_TO_BIN(?, 1), 'Organización principal')
                    """, organizationId);
            LOG.info("Organización {} creada.", organizationId);
        }
    }

    private void seedStructure() {
        var quito = ensureCity("UIO", "Quito");
        var guayaquil = ensureCity("GYE", "Guayaquil");
        var cuenca = ensureCity("CUE", "Cuenca");

        var matrizQuito = ensureSite(quito, "SD-QTO-M", "Matriz Quito", "MATRIZ");
        var matrizGuayaquil = ensureSite(guayaquil, "SD-GYE-M", "Matriz Guayaquil", "MATRIZ");

        var cmQuito = ensureCenter("CM-QTO-01", "Centro Médico Quito", "MEDICO", quito, matrizQuito);
        var bodQuito = ensureCenter("BOD-QTO-TI", "Bodega TI Quito", "BODEGA_TI", quito, matrizQuito);
        var cmGuayaquil = ensureCenter("CM-GYE-01", "Centro Médico Guayaquil", "MEDICO", guayaquil, matrizGuayaquil);
        var bodGuayaquil = ensureCenter("BOD-GYE-TI", "Bodega TI Guayaquil", "BODEGA_TI", guayaquil, matrizGuayaquil);
        // Cuenca: centro médico sin sede ni matriz receptora obligatoria.
        ensureCenter("CM-CUE-01", "Centro Médico Cuenca", "MEDICO", cuenca, null);

        ensureMatrix("MQR-QTO", "Matriz receptora Quito", quito, matrizQuito, cmQuito);
        ensureMatrix("MQR-GYE", "Matriz receptora Guayaquil", guayaquil, matrizGuayaquil, cmGuayaquil);
        ensureLocation(bodQuito, "Patio de recepción", "AREA");
        ensureLocation(bodGuayaquil, "Patio de recepción", "AREA");
    }

    private String ensureCity(String code, String name) {
        var existing = findId("""
                SELECT BIN_TO_UUID(id,1) FROM city WHERE organization_id = UUID_TO_BIN(?, 1) AND code = ?
                """, code);
        if (existing != null) return existing;
        var id = java.util.UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO city (id, organization_id, code, name) VALUES (UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), ?, ?)
                """, id, organizationId, code, name);
        LOG.info("Ciudad {} creada.", code);
        return id;
    }

    private String ensureSite(String cityId, String code, String name, String kind) {
        var existing = findId("""
                SELECT BIN_TO_UUID(id,1) FROM site
                WHERE organization_id = UUID_TO_BIN(?, 1) AND code = ?
                """, code);
        if (existing != null) return existing;
        var id = java.util.UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO site (id, organization_id, city_id, code, name, site_kind)
                VALUES (UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), ?, ?, ?)
                """, id, organizationId, cityId, code, name, kind);
        return id;
    }

    private String ensureCenter(String code, String name, String kind, String cityId, String siteId) {
        var existing = findId("""
                SELECT BIN_TO_UUID(id,1) FROM center WHERE organization_id = UUID_TO_BIN(?, 1) AND code = ?
                """, code);
        if (existing != null) return existing;
        var id = java.util.UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO center (id, organization_id, code, name, city_id, site_id, center_kind)
                VALUES (UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), ?, ?, UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), ?)
                """, id, organizationId, code, name, cityId, siteId, kind);
        return id;
    }

    private void ensureMatrix(String code, String name, String cityId, String siteId, String centerId) {
        var matrixId = findId("""
                SELECT BIN_TO_UUID(id,1) FROM receiving_matrix
                WHERE organization_id = UUID_TO_BIN(?, 1) AND code = ?
                """, code);
        if (matrixId == null) {
            matrixId = java.util.UUID.randomUUID().toString();
            jdbc.update("""
                    INSERT INTO receiving_matrix (id, organization_id, city_id, site_id, code, name, description)
                    VALUES (UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), ?, ?, ?)
                    """, matrixId, organizationId, cityId, siteId, code, name,
                    "Matriz receptora inicial");
        }
        var linked = jdbc.queryForObject("""
                SELECT COUNT(*) FROM receiving_matrix_center
                WHERE organization_id = UUID_TO_BIN(?, 1) AND matrix_id = UUID_TO_BIN(?, 1)
                  AND center_id = UUID_TO_BIN(?, 1)
                """, Long.class, organizationId, matrixId, centerId);
        if (linked == null || linked == 0) {
            jdbc.update("""
                    INSERT INTO receiving_matrix_center (organization_id, matrix_id, center_id)
                    VALUES (UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1))
                    """, organizationId, matrixId, centerId);
        }
    }

    private void ensureLocation(String centerId, String name, String locationType) {
        var exists = jdbc.queryForObject("""
                SELECT COUNT(*) FROM location
                WHERE organization_id = UUID_TO_BIN(?, 1) AND center_id = UUID_TO_BIN(?, 1) AND name = ?
                """, Long.class, organizationId, centerId, name);
        if (exists == null || exists == 0) {
            jdbc.update("""
                    INSERT INTO location (id, organization_id, center_id, name, location_type)
                    VALUES (UUID_TO_BIN(UUID(), 1), UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), ?, ?)
                    """, organizationId, centerId, name, locationType);
        }
    }

    private String findId(String sql, String code) {
        return jdbc.query(sql, (rs, n) -> rs.getString(1), organizationId, code)
                .stream().findFirst().orElse(null);
    }
}
