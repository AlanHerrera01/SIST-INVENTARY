package com.veris.inventory.access.services.servicesImpl;

import com.veris.inventory.access.services.AccessGuard;
import com.veris.inventory.access.services.AccessGuard.AccessContext;
import com.veris.inventory.access.services.UserService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.veris.inventory.access.services.IdentityAdminGateway;
import com.veris.inventory.access.services.IdentityAdminGateway.NewUser;
import com.veris.inventory.access.domain.ScopeKind;
import com.veris.inventory.common.error.ApiException;
import com.veris.inventory.platform.AuditRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.veris.inventory.config.IdentityProperties;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Administración de usuarios locales y su sincronización con el proveedor de
 * identidad. La fila local usa (issuer, subject) emitidos por Keycloak; la
 * suspensión se aplica primero en local (corta el acceso en la siguiente
 * petición) y después en el proveedor, que queda autocorregido por
 * {@link #sync}.
 */
@Service
public class UserServiceImpl implements UserService {
    private static final Logger LOG = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String USERNAME_PATTERN = "^[A-Za-z0-9][A-Za-z0-9._@-]{2,99}$";

    private final JdbcTemplate jdbc;
    private final IdentityAdminGateway identity;
    private final AuditRecorder audit;
    private final AccessGuard guard;
    private final TransactionTemplate tx;
    private final String issuerUri;

    public UserServiceImpl(JdbcTemplate jdbc, IdentityAdminGateway identity,
                           AuditRecorder audit, AccessGuard guard,
                           TransactionTemplate tx, IdentityProperties identityProperties) {
        this.jdbc = jdbc;
        this.identity = identity;
        this.audit = audit;
        this.guard = guard;
        this.tx = tx;
        this.issuerUri = identityProperties.issuerUri().toString();
    }

    public List<UserSummary> list(String organizationId, AccessContext ctx, String state,
                                  String query, int limit, int offset) {
        guard.requireGlobal(ctx, PermissionCatalog.USERS_READ);
        var sql = new StringBuilder("""
                SELECT BIN_TO_UUID(id,1), username, display_name, email_address, user_state,
                       CAST(created_at AS CHAR)
                FROM app_user WHERE organization_id = UUID_TO_BIN(?, 1)
                """);
        var args = new ArrayList<Object>();
        args.add(organizationId);
        if (state != null && !state.isBlank()) {
            sql.append(" AND user_state = ?");
            args.add(state.trim().toUpperCase());
        }
        if (query != null && !query.isBlank()) {
            sql.append(" AND (username LIKE ? OR display_name LIKE ? OR email_address LIKE ?)");
            var like = "%" + query.trim() + "%";
            args.add(like); args.add(like); args.add(like);
        }
        sql.append(" ORDER BY display_name, id LIMIT ? OFFSET ?");
        args.add(limit); args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> new UserSummary(
                rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                rs.getString(5), rs.getString(6)), args.toArray());
    }

    public UserDetail detail(String organizationId, AccessContext ctx, String userId) {
        guard.requireGlobal(ctx, PermissionCatalog.USERS_READ);
        var summary = jdbc.query("""
                SELECT BIN_TO_UUID(id,1), username, display_name, email_address, user_state,
                       CAST(created_at AS CHAR)
                FROM app_user WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)
                """, (rs, n) -> new UserSummary(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6)),
                organizationId, userId).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "Usuario no encontrado."));
        var assignments = assignmentsOf(organizationId, userId);
        var matrices = matricesOf(organizationId, userId);
        return new UserDetail(summary, assignments, matrices);
    }

    /**
     * Crea el usuario en el proveedor y luego la fila local en una sola
     * transacción con su auditoría. Si la persistencia local falla se intenta
     * compensar eliminando el usuario remoto.
     */
    public UserSummary create(String organizationId, AccessContext ctx, CreateUserCommand command) {
        guard.requireGlobal(ctx, PermissionCatalog.USERS_MANAGE);
        var username = requireText(command.username(), "username", USERNAME_PATTERN);
        var displayName = requireText(command.displayName(), "display_name", null);
        var email = optionalText(command.email(), 320);
        if (email != null && !email.contains("@")) {
            throw ApiException.unprocessable("VALIDATION_FAILED", "email_address no es un correo válido.");
        }
        if (command.temporaryPassword() != null && command.temporaryPassword().length() < 8) {
            throw ApiException.unprocessable("VALIDATION_FAILED", "La contraseña temporal requiere al menos 8 caracteres.");
        }
        requireNoUsername(organizationId, username);
        var existing = identity.findByUsername(username);
        if (existing.isPresent()) {
            throw ApiException.conflict("IDENTITY_USERNAME_TAKEN",
                    "El nombre de usuario ya existe en el proveedor de identidad.");
        }
        var external = identity.create(new NewUser(username, email, displayName, "", true,
                command.temporaryPassword()));
        try {
            return tx.execute(status -> {
                var id = java.util.UUID.randomUUID().toString();
                try {
                    jdbc.update("""
                            INSERT INTO app_user (id, organization_id, username, identity_issuer,
                                identity_subject, display_name, email_address, user_state)
                            VALUES (UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), ?, ?, ?, ?, ?, 'ACTIVO')
                            """, id, organizationId, username, issuerUri, external.id(), displayName, email);
                } catch (DuplicateKeyException duplicate) {
                    throw ApiException.conflict("USERNAME_TAKEN", "El nombre de usuario ya está en uso.");
                }
                audit.record(organizationId, actorId(ctx), "USER_CREATED", "APP_USER", id, null,
                        Map.of("username", username, "display_name", displayName,
                                "email", email == null ? "" : email));
                return new UserSummary(id, username, displayName, email, "ACTIVO",
                        java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).toString());
            });
        } catch (RuntimeException failure) {
            try {
                identity.delete(external.id());
            } catch (RuntimeException cleanup) {
                LOG.warn("No se pudo compensar el usuario {} en el proveedor", external.id());
            }
            throw failure;
        }
    }

    /** Actualiza nombre visible y correo; el nombre de usuario es inmutable. */
    @Transactional
    public UserSummary update(String organizationId, AccessContext ctx, String userId, UpdateUserCommand command) {
        guard.requireGlobal(ctx, PermissionCatalog.USERS_MANAGE);
        var current = requireUser(organizationId, userId);
        var displayName = requireText(command.displayName(), "display_name", null);
        var email = optionalText(command.email(), 320);
        if (email != null && !email.contains("@")) {
            throw ApiException.unprocessable("VALIDATION_FAILED", "email_address no es un correo válido.");
        }
        var changed = new LinkedHashMap<String, Object>();
        if (!displayName.equals(current.displayName())) changed.put("display_name", displayName);
        if (!java.util.Objects.equals(email, current.email())) changed.put("email_address", email);
        if (!changed.isEmpty()) {
            jdbc.update("UPDATE app_user SET display_name = ?, email_address = ? WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)",
                    displayName, email, organizationId, userId);
            syncIdentityBestEffort(organizationId, userId, "profile", () ->
                    identity.updateProfile(current.subject(), displayName, email));
            audit.record(organizationId, actorId(ctx), "USER_UPDATED", "APP_USER", userId, null, changed);
        }
        return summaryOf(organizationId, userId);
    }

    /** Suspende: corta el acceso local de inmediato y deshabilita el proveedor. */
    @Transactional
    public UserSummary suspend(String organizationId, AccessContext ctx, String userId, String reason) {
        guard.requireGlobal(ctx, PermissionCatalog.USERS_MANAGE);
        var current = requireUser(organizationId, userId);
        if ("INACTIVO".equals(current.state())) {
            throw ApiException.conflict("INVALID_TRANSITION", "El usuario ya está suspendido.");
        }
        jdbc.update("UPDATE app_user SET user_state = 'INACTIVO' WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)",
                organizationId, userId);
        audit.record(organizationId, actorId(ctx), "USER_SUSPENDED", "APP_USER", userId, null,
                Map.of("reason", reason == null ? "" : reason));
        syncIdentityBestEffort(organizationId, userId, "suspension", () -> {
            identity.setEnabled(current.subject(), false);
            identity.logout(current.subject());
        });
        return summaryOf(organizationId, userId);
    }

    /** Reactiva el usuario local y realinea el estado del proveedor. */
    @Transactional
    public UserSummary activate(String organizationId, AccessContext ctx, String userId) {
        guard.requireGlobal(ctx, PermissionCatalog.USERS_MANAGE);
        var current = requireUser(organizationId, userId);
        if ("ACTIVO".equals(current.state())) {
            throw ApiException.conflict("INVALID_TRANSITION", "El usuario ya está activo.");
        }
        jdbc.update("UPDATE app_user SET user_state = 'ACTIVO' WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)",
                organizationId, userId);
        audit.record(organizationId, actorId(ctx), "USER_ACTIVATED", "APP_USER", userId, null, Map.of());
        syncIdentityBestEffort(organizationId, userId, "activation", () ->
                identity.setEnabled(current.subject(), true));
        return summaryOf(organizationId, userId);
    }

    /**
     * Resincroniza el perfil local desde el proveedor y autocorrige el estado
     * habilitado del proveedor para que coincida con el estado local, que es
     * el autoritativo para la aplicación.
     */
    @Transactional
    public UserDetail sync(String organizationId, AccessContext ctx, String userId) {
        guard.requireGlobal(ctx, PermissionCatalog.USERS_MANAGE);
        var current = requireUser(organizationId, userId);
        var remote = identity.findBySubject(current.subject())
                .orElseThrow(() -> ApiException.conflict("IDENTITY_USER_MISSING",
                        "El usuario ya no existe en el proveedor de identidad."));
        var changed = new LinkedHashMap<String, Object>();
        var displayName = remote.firstName() == null || remote.firstName().isBlank()
                ? current.displayName() : remote.firstName();
        var email = remote.email();
        if (!displayName.equals(current.displayName())) changed.put("display_name", displayName);
        if (!java.util.Objects.equals(email, current.email())) changed.put("email_address", email);
        if (!changed.isEmpty()) {
            jdbc.update("UPDATE app_user SET display_name = ?, email_address = ? WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)",
                    displayName, email, organizationId, userId);
        }
        boolean shouldBeEnabled = "ACTIVO".equals(current.state());
        if (remote.enabled() != shouldBeEnabled) {
            try {
                identity.setEnabled(current.subject(), shouldBeEnabled);
                changed.put("identity_enabled", shouldBeEnabled);
            } catch (RuntimeException failure) {
                LOG.warn("No se pudo realinear el estado del usuario {} en el proveedor", userId);
            }
        }
        if (!changed.isEmpty()) {
            audit.record(organizationId, actorId(ctx), "USER_SYNCED", "APP_USER", userId, null, changed);
        }
        return detail(organizationId, ctx, userId);
    }

    /**
     * Asigna un rol con alcance GLOBAL o CENTROS. La regla anti-escalación
     * exige que el actor posea con alcance GLOBAL cada permiso del rol.
     */
    @Transactional
    public AssignmentView assignRole(String organizationId, AccessContext ctx, String userId,
                                     AssignRoleCommand command) {
        guard.requireGlobal(ctx, PermissionCatalog.ASSIGNMENTS_MANAGE);
        requireUser(organizationId, userId);
        var role = jdbc.query("""
                SELECT BIN_TO_UUID(id,1), code, name FROM app_role
                WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)
                """, (rs, n) -> new String[]{rs.getString(1), rs.getString(2), rs.getString(3)},
                organizationId, command.roleId()).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("ROLE_NOT_FOUND", "Rol no encontrado."));
        var rolePermissions = rolePermissionsOf(organizationId, role[0]);
        guard.requireDelegable(ctx, rolePermissions);
        var scope = command.scopeKind() == null ? ScopeKind.GLOBAL : command.scopeKind();
        List<String> centerIds = List.of();
        if (scope == ScopeKind.CENTROS) {
            if (command.centerIds() == null || command.centerIds().isEmpty()) {
                throw ApiException.unprocessable("VALIDATION_FAILED",
                        "El alcance CENTROS requiere al menos un centro.");
            }
            centerIds = command.centerIds().stream().distinct().toList();
            requireActiveCenters(organizationId, centerIds);
        } else if (command.centerIds() != null && !command.centerIds().isEmpty()) {
            throw ApiException.unprocessable("VALIDATION_FAILED",
                    "El alcance GLOBAL no admite centros.");
        }
        var validUntil = parseInstant(command.validUntil());
        if (validUntil != null && !validUntil.after(new java.util.Date())) {
            throw ApiException.unprocessable("VALIDATION_FAILED",
                    "valid_until debe ser una fecha futura.");
        }
        String assignmentId;
        try {
            assignmentId = java.util.UUID.randomUUID().toString();
            jdbc.update("""
                    INSERT INTO user_role_assignment (id, organization_id, user_id, role_id,
                        scope_kind, valid_from, valid_until, granted_by_user_id, revoke_reason)
                    VALUES (UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1),
                        UUID_TO_BIN(?, 1), ?, UTC_TIMESTAMP(6), ?, UUID_TO_BIN(?, 1), NULL)
                    """, assignmentId, organizationId, userId, role[0], scope.name(),
                    validUntil, actorId(ctx));
        } catch (DuplicateKeyException duplicate) {
            throw ApiException.conflict("ROLE_ALREADY_ASSIGNED",
                    "El usuario ya tiene una asignación activa de este rol.");
        }
        for (var centerId : centerIds) {
            jdbc.update("""
                    INSERT INTO user_role_assignment_center (organization_id, role_assignment_id, center_id)
                    VALUES (UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1))
                    """, organizationId, assignmentId, centerId);
        }
        audit.record(organizationId, actorId(ctx), "ROLE_ASSIGNED", "USER_ROLE_ASSIGNMENT",
                assignmentId, null, Map.of("user_id", userId, "role_code", role[1],
                        "scope_kind", scope.name(), "center_ids", centerIds.toString(),
                        "reason", command.reason() == null ? "" : command.reason()));
        return assignmentViews(organizationId, List.of(assignmentId)).getFirst();
    }

    /** Revoca una asignación; el perfil se recalcula en la siguiente petición. */
    @Transactional
    public void revokeAssignment(String organizationId, AccessContext ctx, String userId,
                                 String assignmentId, String reason) {
        guard.requireGlobal(ctx, PermissionCatalog.ASSIGNMENTS_MANAGE);
        requireUser(organizationId, userId);
        var updated = jdbc.update("""
                UPDATE user_role_assignment SET revoked_at = UTC_TIMESTAMP(6),
                    revoke_reason = ?
                WHERE organization_id = UUID_TO_BIN(?, 1) AND user_id = UUID_TO_BIN(?, 1)
                  AND id = UUID_TO_BIN(?, 1) AND revoked_at IS NULL
                """, reason, organizationId, userId, assignmentId);
        if (updated == 0) {
            throw ApiException.notFound("ASSIGNMENT_NOT_FOUND",
                    "Asignación activa no encontrada para el usuario.");
        }
        audit.record(organizationId, actorId(ctx), "ROLE_REVOKED", "USER_ROLE_ASSIGNMENT",
                assignmentId, null, Map.of("user_id", userId,
                        "reason", reason == null ? "" : reason));
    }

    // ---- internos ----

    private record StoredUser(String id, String username, String displayName, String email,
                              String subject, String state) {}

    private StoredUser requireUser(String organizationId, String userId) {
        return jdbc.query("""
                SELECT BIN_TO_UUID(id,1), username, display_name, email_address,
                       identity_subject, user_state
                FROM app_user
                WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)
                """, (rs, n) -> new StoredUser(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6)),
                organizationId, userId).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "Usuario no encontrado."));
    }

    private UserSummary summaryOf(String organizationId, String userId) {
        return jdbc.query("""
                SELECT BIN_TO_UUID(id,1), username, display_name, email_address, user_state,
                       CAST(created_at AS CHAR)
                FROM app_user WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)
                """, (rs, n) -> new UserSummary(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6)),
                organizationId, userId).getFirst();
    }

    private void requireNoUsername(String organizationId, String username) {
        var count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM app_user
                WHERE organization_id = UUID_TO_BIN(?, 1) AND username = ?
                """, Long.class, organizationId, username);
        if (count != null && count > 0) {
            throw ApiException.conflict("USERNAME_TAKEN", "El nombre de usuario ya está en uso.");
        }
    }

    private List<AssignmentView> assignmentsOf(String organizationId, String userId) {
        return jdbc.query("""
                SELECT BIN_TO_UUID(a.id,1), BIN_TO_UUID(a.role_id,1), r.code, r.name,
                       a.scope_kind, CAST(a.valid_from AS CHAR), CAST(a.valid_until AS CHAR),
                       CASE WHEN a.revoked_at IS NULL THEN 'VIGENTE' ELSE 'REVOCADA' END,
                       BIN_TO_UUID(a.granted_by_user_id,1)
                FROM user_role_assignment a
                JOIN app_role r ON r.organization_id = a.organization_id AND r.id = a.role_id
                WHERE a.organization_id = UUID_TO_BIN(?, 1) AND a.user_id = UUID_TO_BIN(?, 1)
                ORDER BY a.created_at DESC
                """, (rs, n) -> new AssignmentView(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), List.of(), rs.getString(6), rs.getString(7),
                        rs.getString(8), rs.getString(9)), organizationId, userId);
    }

    private List<AssignmentView> assignmentViews(String organizationId, List<String> ids) {
        if (ids.isEmpty()) return List.of();
        var placeholders = String.join(",", ids.stream().map(i -> "UUID_TO_BIN('" + i + "',1)").toList());
        var rows = jdbc.query("""
                SELECT BIN_TO_UUID(a.id,1), BIN_TO_UUID(a.role_id,1), r.code, r.name,
                       a.scope_kind, CAST(a.valid_from AS CHAR), CAST(a.valid_until AS CHAR),
                       CASE WHEN a.revoked_at IS NULL THEN 'VIGENTE' ELSE 'REVOCADA' END,
                       BIN_TO_UUID(a.granted_by_user_id,1)
                FROM user_role_assignment a
                JOIN app_role r ON r.organization_id = a.organization_id AND r.id = a.role_id
                WHERE a.organization_id = UUID_TO_BIN(?, 1) AND a.id IN (""" + placeholders + ")",
                (rs, n) -> new AssignmentView(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), List.of(), rs.getString(6), rs.getString(7),
                        rs.getString(8), rs.getString(9)), organizationId);
        var centers = jdbc.query("""
                SELECT BIN_TO_UUID(role_assignment_id,1), BIN_TO_UUID(center_id,1)
                FROM user_role_assignment_center
                WHERE organization_id = UUID_TO_BIN(?, 1)
                  AND role_assignment_id IN (""" + placeholders + ")",
                (rs, n) -> Map.entry(rs.getString(1), rs.getString(2)), organizationId);
        var byAssignment = new java.util.HashMap<String, List<String>>();
        centers.forEach(e -> byAssignment.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(e.getValue()));
        return rows.stream().map(v -> new AssignmentView(v.id(), v.roleId(), v.roleCode(), v.roleName(),
                v.scopeKind(), List.copyOf(byAssignment.getOrDefault(v.id(), List.of())),
                v.validFrom(), v.validUntil(), v.state(), v.grantedBy())).toList();
    }

    private List<MatrixMembershipView> matricesOf(String organizationId, String userId) {
        return jdbc.query("""
                SELECT BIN_TO_UUID(m.id,1), m.code, m.name, BIN_TO_UUID(m.center_id,1),
                       m.act_type, mm.member_role
                FROM signature_matrix_member mm
                JOIN signature_matrix m ON m.organization_id = mm.organization_id AND m.id = mm.matrix_id
                WHERE mm.organization_id = UUID_TO_BIN(?, 1) AND mm.user_id = UUID_TO_BIN(?, 1)
                  AND m.matrix_state = 'ACTIVO'
                ORDER BY m.code
                """, (rs, n) -> new MatrixMembershipView(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6)), organizationId, userId);
    }

    private Set<String> rolePermissionsOf(String organizationId, String roleId) {
        return Set.copyOf(jdbc.queryForList("""
                SELECT p.code FROM role_permission rp
                JOIN permission p ON p.id = rp.permission_id
                WHERE rp.organization_id = UUID_TO_BIN(?, 1) AND rp.role_id = UUID_TO_BIN(?, 1)
                """, String.class, organizationId, roleId));
    }

    private void requireActiveCenters(String organizationId, List<String> centerIds) {
        centerIds.forEach(UserServiceImpl::requireUuid);
        var placeholders = centerIds.stream().map(i -> "UUID_TO_BIN('" + i + "',1)")
                .reduce((a, b) -> a + "," + b).orElseThrow();
        var found = jdbc.queryForList("""
                SELECT BIN_TO_UUID(id,1) FROM center
                WHERE organization_id = UUID_TO_BIN(?, 1) AND center_state = 'ACTIVO'
                  AND id IN (""" + placeholders + ")",
                String.class, organizationId);
        if (found.size() != centerIds.size()) {
            throw ApiException.unprocessable("VALIDATION_FAILED",
                    "Uno o más centros no existen o están inactivos.");
        }
    }

    private void syncIdentityBestEffort(String organizationId, String userId, String what, Runnable call) {
        try {
            call.run();
        } catch (RuntimeException failure) {
            LOG.warn("Sincronización {} del usuario {} con el proveedor falló; usar /sync", what, userId);
        }
    }

    private static String actorId(AccessContext ctx) {
        return ctx.profile() == null ? null : ctx.profile().id();
    }

    private static String requireText(String value, String field, String pattern) {
        if (value == null || value.isBlank()) {
            throw ApiException.unprocessable("VALIDATION_FAILED", field + " es obligatorio.");
        }
        var trimmed = value.trim();
        if (pattern != null && !trimmed.matches(pattern)) {
            throw ApiException.unprocessable("VALIDATION_FAILED", field + " tiene un formato inválido.");
        }
        return trimmed;
    }

    private static String optionalText(String value, int max) {
        if (value == null || value.isBlank()) return null;
        var trimmed = value.trim();
        if (trimmed.length() > max) {
            throw ApiException.unprocessable("VALIDATION_FAILED", "El campo excede la longitud máxima.");
        }
        return trimmed;
    }

    private static java.sql.Timestamp parseInstant(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return java.sql.Timestamp.from(java.time.OffsetDateTime.parse(value).toInstant());
        } catch (java.time.format.DateTimeParseException e) {
            throw ApiException.unprocessable("VALIDATION_FAILED", "valid_until debe ser ISO-8601.");
        }
    }

    private static void requireUuid(String value) {
        if (value == null || !value.matches("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")) {
            throw ApiException.unprocessable("VALIDATION_FAILED", "Identificador con formato inválido.");
        }
    }
}
