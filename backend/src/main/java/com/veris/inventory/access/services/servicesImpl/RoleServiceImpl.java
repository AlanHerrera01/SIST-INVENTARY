package com.veris.inventory.access.services.servicesImpl;

import com.veris.inventory.access.services.AccessGuard;
import com.veris.inventory.access.services.AccessGuard.AccessContext;
import com.veris.inventory.access.services.RoleService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.veris.inventory.common.error.ApiException;
import com.veris.inventory.platform.AuditRecorder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administración de roles por organización y de su conjunto de permisos.
 * El código del rol es inmutable una vez creado: las referencias históricas
 * (asignaciones, auditoría) deben seguir siendo interpretables.
 */
@Service
public class RoleServiceImpl implements RoleService {
    private final JdbcTemplate jdbc;
    private final AuditRecorder audit;
    private final AccessGuard guard;

    public RoleServiceImpl(JdbcTemplate jdbc, AuditRecorder audit, AccessGuard guard) {
        this.jdbc = jdbc;
        this.audit = audit;
        this.guard = guard;
    }

    public List<RoleView> list(String organizationId, AccessContext ctx) {
        guard.requireGlobal(ctx, PermissionCatalog.ROLES_READ);
        return jdbc.query("""
                SELECT BIN_TO_UUID(r.id,1), r.code, r.name, r.description,
                       CAST(r.created_at AS CHAR),
                       (SELECT COUNT(*) FROM user_role_assignment a
                        WHERE a.organization_id = r.organization_id AND a.role_id = r.id
                          AND a.revoked_at IS NULL)
                FROM app_role r
                WHERE r.organization_id = UUID_TO_BIN(?, 1)
                ORDER BY r.code
                """, (rs, n) -> new RoleView(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), permissionsOf(organizationId, rs.getString(1)),
                        rs.getLong(5), rs.getString(6)), organizationId);
    }

    public RoleView detail(String organizationId, AccessContext ctx, String roleId) {
        guard.requireGlobal(ctx, PermissionCatalog.ROLES_READ);
        return viewOf(organizationId, roleId);
    }

    private RoleView viewOf(String organizationId, String roleId) {
        return jdbc.query("""
                SELECT BIN_TO_UUID(r.id,1), r.code, r.name, r.description, CAST(r.created_at AS CHAR),
                       (SELECT COUNT(*) FROM user_role_assignment a
                        WHERE a.organization_id = r.organization_id AND a.role_id = r.id
                          AND a.revoked_at IS NULL)
                FROM app_role r
                WHERE r.organization_id = UUID_TO_BIN(?, 1) AND r.id = UUID_TO_BIN(?, 1)
                """, (rs, n) -> new RoleView(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), permissionsOf(organizationId, rs.getString(1)),
                        rs.getLong(5), rs.getString(6)), organizationId, roleId).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("ROLE_NOT_FOUND", "Rol no encontrado."));
    }

    @Transactional
    public RoleView create(String organizationId, AccessContext ctx, CreateRoleCommand command) {
        guard.requireGlobal(ctx, PermissionCatalog.ROLES_MANAGE);
        var code = requireCode(command.code());
        var name = requireName(command.name());
        var id = java.util.UUID.randomUUID().toString();
        try {
            jdbc.update("""
                    INSERT INTO app_role (id, organization_id, code, name, description)
                    VALUES (UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), ?, ?, ?)
                    """, id, organizationId, code, name,
                    command.description() == null || command.description().isBlank()
                            ? null : command.description().trim());
        } catch (DuplicateKeyException duplicate) {
            throw ApiException.conflict("ROLE_CODE_TAKEN", "Ya existe un rol con ese código.");
        }
        audit.record(organizationId, actor(ctx), "ROLE_CREATED", "APP_ROLE", id, null,
                Map.of("code", code, "name", name));
        return viewOf(organizationId, id);
    }

    @Transactional
    public RoleView update(String organizationId, AccessContext ctx, String roleId,
                           UpdateRoleCommand command) {
        guard.requireGlobal(ctx, PermissionCatalog.ROLES_MANAGE);
        viewOf(organizationId, roleId);
        var name = requireName(command.name());
        var description = command.description() == null || command.description().isBlank()
                ? null : command.description().trim();
        var changed = new LinkedHashMap<String, Object>();
        jdbc.query("SELECT name, description FROM app_role WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)",
                (rs, n) -> {
                    if (!name.equals(rs.getString(1))) changed.put("name", name);
                    var oldDescription = rs.getString(2);
                    if (!java.util.Objects.equals(description, oldDescription)) {
                        changed.put("description", description == null ? "" : description);
                    }
                    return null;
                }, organizationId, roleId);
        if (!changed.isEmpty()) {
            jdbc.update("UPDATE app_role SET name = ?, description = ? WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)",
                    name, description, organizationId, roleId);
            audit.record(organizationId, actor(ctx), "ROLE_UPDATED", "APP_ROLE", roleId, null, changed);
        }
        return viewOf(organizationId, roleId);
    }

    /**
     * Sustituye el conjunto de permisos del rol. La regla anti-escalación
     * exige que el actor posea con alcance GLOBAL cada permiso resultante.
     */
    @Transactional
    public RoleView replacePermissions(String organizationId, AccessContext ctx, String roleId,
                                       Set<String> permissionCodes) {
        guard.requireGlobal(ctx, PermissionCatalog.ROLES_MANAGE);
        viewOf(organizationId, roleId);
        var codes = new LinkedHashSet<String>();
        if (permissionCodes != null) {
            for (var code : permissionCodes) {
                if (code == null || !PermissionCatalog.ALL_CODES.contains(code)) {
                    throw ApiException.unprocessable("VALIDATION_FAILED",
                            "Permiso desconocido: " + code);
                }
                codes.add(code);
            }
        }
        guard.requireDelegable(ctx, codes);
        var before = permissionsOf(organizationId, roleId);
        jdbc.update("DELETE FROM role_permission WHERE organization_id = UUID_TO_BIN(?, 1) AND role_id = UUID_TO_BIN(?, 1)",
                organizationId, roleId);
        for (var code : codes) {
            jdbc.update("""
                    INSERT INTO role_permission (organization_id, role_id, permission_id)
                    SELECT UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), id FROM permission WHERE code = ?
                    """, organizationId, roleId, code);
        }
        audit.record(organizationId, actor(ctx), "ROLE_PERMISSIONS_REPLACED", "APP_ROLE", roleId, null,
                Map.of("before", before.toString(), "after", codes.toString()));
        return viewOf(organizationId, roleId);
    }

    // ---- internos ----

    private Set<String> permissionsOf(String organizationId, String roleId) {
        return Set.copyOf(jdbc.queryForList("""
                SELECT p.code FROM role_permission rp
                JOIN permission p ON p.id = rp.permission_id
                WHERE rp.organization_id = UUID_TO_BIN(?, 1) AND rp.role_id = UUID_TO_BIN(?, 1)
                ORDER BY p.code
                """, String.class, organizationId, roleId));
    }

    private static String requireCode(String code) {
        if (code == null || !code.matches("^[A-Z0-9_]{2,50}$")) {
            throw ApiException.unprocessable("VALIDATION_FAILED",
                    "code debe ser MAYÚSCULAS, dígitos o guion bajo (2-50).");
        }
        return code;
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank() || name.trim().length() > 200) {
            throw ApiException.unprocessable("VALIDATION_FAILED", "name es obligatorio (máx. 200).");
        }
        return name.trim();
    }

    private static String actor(AccessContext ctx) {
        return ctx.profile() == null ? null : ctx.profile().id();
    }
}
