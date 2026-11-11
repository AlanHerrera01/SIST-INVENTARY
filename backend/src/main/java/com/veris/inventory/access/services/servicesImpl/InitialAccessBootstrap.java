package com.veris.inventory.access.services.servicesImpl;

import java.util.LinkedHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import com.veris.inventory.config.IdentityProperties;
import com.veris.inventory.config.SecurityProperties;
import com.veris.inventory.access.services.IdentityAdminGateway;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Aprovisionamiento inicial idempotente del módulo de acceso (prompt Fase 04
 * §3 y §5): catálogo global de permisos y roles iniciales por organización.
 *
 * <p>Solo crea lo que falta: nunca elimina permisos ni recorta conjuntos de
 * permisos ya personalizados por un administrador. Si la organización
 * configurada aún no existe, los roles se crearán en el siguiente arranque
 * posterior a su alta.</p>
 */
@Component
public class InitialAccessBootstrap implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(InitialAccessBootstrap.class);

    private final JdbcTemplate jdbc;
    private final PermissionCatalog catalog;
    private final IdentityAdminGateway identity;
    private final String organizationId;
    private final String issuerUri;

    public InitialAccessBootstrap(JdbcTemplate jdbc, PermissionCatalog catalog,
                                  IdentityAdminGateway identity,
                                  SecurityProperties security, IdentityProperties identityProperties) {
        this.jdbc = jdbc;
        this.catalog = catalog;
        this.identity = identity;
        this.organizationId = java.util.UUID.fromString(security.organizationId()).toString();
        this.issuerUri = identityProperties.issuerUri().toString();
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            catalog.ensureCatalog();
            ensureInitialRoles();
            ensureDevAdministrator();
        } catch (DataAccessException unavailable) {
            // Contextos sin el esquema de Fase 02+ (p. ej. pruebas H2 de arranque).
            LOG.warn("Aprovisionamiento de acceso omitido: esquema no disponible.");
        }
    }

    private void ensureInitialRoles() {
        var orgExists = jdbc.queryForObject("""
                SELECT COUNT(*) FROM organization WHERE id = UUID_TO_BIN(?, 1)
                """, Long.class, organizationId);
        if (orgExists == null || orgExists == 0) {
            LOG.info("Organización {} no existe; roles iniciales pendientes.", organizationId);
            return;
        }
        for (var role : PermissionCatalog.INITIAL_ROLES) {
            var roleId = jdbc.query("""
                    SELECT BIN_TO_UUID(id,1) FROM app_role
                    WHERE organization_id = UUID_TO_BIN(?, 1) AND code = ?
                    """, (rs, n) -> rs.getString(1), organizationId, role.code())
                    .stream().findFirst().orElse(null);
            if (roleId == null) {
                roleId = java.util.UUID.randomUUID().toString();
                jdbc.update("""
                        INSERT INTO app_role (id, organization_id, code, name, description)
                        VALUES (UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), ?, ?, ?)
                        """, roleId, organizationId, role.code(), role.name(), role.description());
                LOG.info("Rol inicial {} creado en la organización.", role.code());
            }
            var existing = new LinkedHashSet<>(jdbc.queryForList("""
                    SELECT p.code FROM role_permission rp
                    JOIN permission p ON p.id = rp.permission_id
                    WHERE rp.organization_id = UUID_TO_BIN(?, 1) AND rp.role_id = UUID_TO_BIN(?, 1)
                    """, String.class, organizationId, roleId));
            for (var permission : role.permissions()) {
                if (existing.add(permission)) {
                    jdbc.update("""
                            INSERT INTO role_permission (organization_id, role_id, permission_id)
                            SELECT UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), id FROM permission WHERE code = ?
                            """, organizationId, roleId, permission);
                }
            }
        }
    }

    /**
     * Usuario administrador de desarrollo: si el realm define el usuario
     * {@code admin} y aún no existe su perfil local, lo aprovisiona con el rol
     * ADMINISTRADOR de alcance GLOBAL. Idempotente; en producción el alta se
     * hace por la API de usuarios.
     */
    private void ensureDevAdministrator() {
        java.util.Optional<IdentityAdminGateway.ExternalUser> found;
        try {
            found = identity.findByUsername("admin");
        } catch (RuntimeException unavailable) {
            LOG.warn("Aprovisionamiento del administrador omitido: proveedor de identidad no disponible.");
            return;
        }
        var external = found;
        if (external.isEmpty()) {
            return;
        }
        var subject = external.get().id();
        var existing = jdbc.query("""
                SELECT BIN_TO_UUID(id,1) FROM app_user
                WHERE organization_id = UUID_TO_BIN(?, 1) AND username = 'admin'
                """, (rs, n) -> rs.getString(1), organizationId);
        String userId;
        if (existing.isEmpty()) {
            userId = java.util.UUID.randomUUID().toString();
            jdbc.update("""
                    INSERT INTO app_user (id, organization_id, username, identity_issuer,
                        identity_subject, display_name, email_address, user_state)
                    VALUES (UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), 'admin', ?, ?, ?, ?, 'ACTIVO')
                    """, userId, organizationId, issuerUri, subject,
                    external.get().firstName() == null ? "Administrador" : external.get().firstName(),
                    external.get().email());
            LOG.info("Perfil local del administrador de desarrollo {} creado.", subject);
        } else {
            userId = existing.getFirst();
            jdbc.update("""
                    UPDATE app_user SET identity_issuer = ?, identity_subject = ?, user_state = 'ACTIVO'
                    WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)
                    """, issuerUri, subject, organizationId, userId);
        }
        var roleId = jdbc.query("""
                SELECT BIN_TO_UUID(id,1) FROM app_role
                WHERE organization_id = UUID_TO_BIN(?, 1) AND code = 'ADMINISTRADOR'
                """, (rs, n) -> rs.getString(1), organizationId).stream().findFirst().orElse(null);
        if (roleId != null) {
            jdbc.update("""
                    INSERT INTO user_role_assignment (id, organization_id, user_id, role_id,
                        scope_kind, valid_from, granted_by_user_id)
                    SELECT UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1),
                        'GLOBAL', UTC_TIMESTAMP(6), UUID_TO_BIN(?, 1)
                    WHERE NOT EXISTS (
                        SELECT 1 FROM user_role_assignment a
                        WHERE a.organization_id = UUID_TO_BIN(?, 1) AND a.user_id = UUID_TO_BIN(?, 1)
                          AND a.role_id = UUID_TO_BIN(?, 1) AND a.revoked_at IS NULL)
                    """, java.util.UUID.randomUUID().toString(), organizationId, userId, roleId, userId,
                    organizationId, userId, roleId);
        }
    }
}
