package com.veris.inventory.organization.services.servicesImpl;

import java.util.List;
import java.util.Map;
import com.veris.inventory.access.services.AccessGuard;
import com.veris.inventory.access.services.AccessGuard.AccessContext;
import com.veris.inventory.access.services.servicesImpl.PermissionCatalog;
import com.veris.inventory.common.error.ApiException;
import com.veris.inventory.organization.domain.ResponsibilityRole;
import com.veris.inventory.organization.services.ResponsibleService;
import com.veris.inventory.platform.AuditRecorder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsables de centro con vigencia y correo institucional. Cada alta,
 * cambio o cierre escribe una fila en {@code center_responsible_history} con
 * la instantánea previa, además del evento en {@code audit_event}.
 */
@Service
public class ResponsibleServiceImpl implements ResponsibleService {

    private final JdbcTemplate jdbc;
    private final AccessGuard guard;
    private final AuditRecorder audit;

    public ResponsibleServiceImpl(JdbcTemplate jdbc, AccessGuard guard, AuditRecorder audit) {
        this.jdbc = jdbc;
        this.guard = guard;
        this.audit = audit;
    }

    public List<ResponsibleView> list(String organizationId, AccessContext ctx, String centerId) {
        guard.require(ctx, PermissionCatalog.ORGANIZATION_READ);
        var sql = """
                SELECT BIN_TO_UUID(r.id,1), BIN_TO_UUID(r.center_id,1), c.name,
                       BIN_TO_UUID(r.user_id,1), u.username, r.responsibility_role,
                       r.institutional_email, CAST(r.valid_from AS CHAR),
                       CAST(r.valid_until AS CHAR), r.responsible_state, BIN_TO_UUID(r.assigned_by,1)
                FROM center_responsible r
                JOIN center c ON c.id = r.center_id AND c.organization_id = r.organization_id
                LEFT JOIN app_user u ON u.id = r.user_id
                WHERE r.organization_id = UUID_TO_BIN(?, 1)
                """;
        var args = new java.util.ArrayList<Object>();
        args.add(organizationId);
        if (centerId != null && !centerId.isBlank()) {
            sql += " AND r.center_id = UUID_TO_BIN(?, 1)";
            args.add(requireUuid(centerId));
        }
        sql += " ORDER BY c.name, r.responsibility_role, r.valid_from DESC";
        return jdbc.query(sql, (rs, n) -> new ResponsibleView(
                rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8),
                rs.getString(9), rs.getString(10), rs.getString(11)), args.toArray());
    }

    @Transactional
    public ResponsibleView create(String organizationId, AccessContext ctx, ResponsibleCommand command) {
        guard.requireGlobal(ctx, PermissionCatalog.ORGANIZATION_MANAGE);
        var centerId = requireUuid(command.centerId());
        requireCenter(organizationId, centerId);
        var role = requireRole(command.role());
        var email = requireEmail(command.email());
        var userId = optionalUuid(command.userId());
        if (userId != null) requireUser(organizationId, userId);
        var validUntil = parseInstant(command.validUntil());
        var id = java.util.UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO center_responsible (id, organization_id, center_id, user_id,
                    responsibility_role, institutional_email, valid_until, assigned_by)
                VALUES (UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1),
                        ?, ?, ?, UUID_TO_BIN(?, 1))
                """, id, organizationId, centerId, userId, role.name(), email, validUntil,
                actor(ctx));
        insertHistory(organizationId, id, "CREATED", centerId, userId, role.name(), email,
                null, validUntil, ctx, command.reason());
        audit.record(organizationId, actor(ctx), "RESPONSIBLE_CREATED", "CENTER_RESPONSIBLE", id, centerId,
                Map.of("role", role.name(), "email", email));
        return view(organizationId, id);
    }

    @Transactional
    public ResponsibleView update(String organizationId, AccessContext ctx, String responsibleId,
                                  UpdateCommand command) {
        guard.requireGlobal(ctx, PermissionCatalog.ORGANIZATION_MANAGE);
        requireUuid(responsibleId);
        var current = find(organizationId, responsibleId);
        if (!"ACTIVO".equals(current.state())) {
            throw ApiException.conflict("RESPONSIBLE_INACTIVE", "El responsable está cerrado; no puede modificarse.");
        }
        var userId = command.userId() == null ? current.userId() : optionalUuid(command.userId());
        var role = command.role() == null ? ResponsibilityRole.valueOf(current.role()) : command.role();
        var email = command.email() == null ? current.email() : requireEmail(command.email());
        var validUntil = command.validUntil() == null ? current.validUntil() : parseInstant(command.validUntil());
        if (userId != null) requireUser(organizationId, userId);
        jdbc.update("""
                UPDATE center_responsible SET user_id = UUID_TO_BIN(?, 1), responsibility_role = ?,
                       institutional_email = ?, valid_until = ?
                WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)
                """, userId, role.name(), email, validUntil, organizationId, responsibleId);
        insertHistory(organizationId, responsibleId, "UPDATED", current.centerId(), current.userId(),
                current.role(), current.email(), current.validFrom(), current.validUntil(), ctx, command.reason());
        audit.record(organizationId, actor(ctx), "RESPONSIBLE_UPDATED", "CENTER_RESPONSIBLE", responsibleId,
                current.centerId(), Map.of("role", role.name()));
        return view(organizationId, responsibleId);
    }

    @Transactional
    public ResponsibleView end(String organizationId, AccessContext ctx, String responsibleId, String reason) {
        guard.requireGlobal(ctx, PermissionCatalog.ORGANIZATION_MANAGE);
        requireUuid(responsibleId);
        var current = find(organizationId, responsibleId);
        if (!"ACTIVO".equals(current.state())) {
            throw ApiException.conflict("RESPONSIBLE_INACTIVE", "El responsable ya está cerrado.");
        }
        jdbc.update("""
                UPDATE center_responsible SET responsible_state = 'INACTIVO', valid_until = UTC_TIMESTAMP(6)
                WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)
                """, organizationId, responsibleId);
        insertHistory(organizationId, responsibleId, "ENDED", current.centerId(), current.userId(),
                current.role(), current.email(), current.validFrom(), current.validUntil(), ctx, reason);
        audit.record(organizationId, actor(ctx), "RESPONSIBLE_ENDED", "CENTER_RESPONSIBLE", responsibleId,
                current.centerId(), Map.of());
        return view(organizationId, responsibleId);
    }

    public List<HistoryView> history(String organizationId, AccessContext ctx, String responsibleId) {
        guard.require(ctx, PermissionCatalog.ORGANIZATION_READ);
        requireUuid(responsibleId);
        find(organizationId, responsibleId);
        return jdbc.query("""
                SELECT BIN_TO_UUID(id,1), action, BIN_TO_UUID(center_id,1), BIN_TO_UUID(user_id,1),
                       responsibility_role, institutional_email,
                       CAST(valid_from AS CHAR), CAST(valid_until AS CHAR),
                       BIN_TO_UUID(changed_by,1), CAST(changed_at AS CHAR), reason
                FROM center_responsible_history
                WHERE organization_id = UUID_TO_BIN(?, 1) AND responsible_id = UUID_TO_BIN(?, 1)
                ORDER BY changed_at DESC, id DESC
                """, (rs, n) -> new HistoryView(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7),
                        rs.getString(8), rs.getString(9), rs.getString(10), rs.getString(11)),
                organizationId, responsibleId);
    }

    // ---------- internos ----------

    private record ResponsibleRow(String id, String centerId, String centerName, String userId,
                                   String username, String role, String email, String state,
                                   String validFrom, String validUntil, String assignedBy) {}

    private ResponsibleRow find(String organizationId, String responsibleId) {
        var rows = jdbc.query("""
                SELECT BIN_TO_UUID(r.id,1), BIN_TO_UUID(r.center_id,1), c.name,
                       BIN_TO_UUID(r.user_id,1), u.username, r.responsibility_role,
                       r.institutional_email, r.responsible_state,
                       CAST(r.valid_from AS CHAR), CAST(r.valid_until AS CHAR), BIN_TO_UUID(r.assigned_by,1)
                FROM center_responsible r
                JOIN center c ON c.id = r.center_id AND c.organization_id = r.organization_id
                LEFT JOIN app_user u ON u.id = r.user_id
                WHERE r.organization_id = UUID_TO_BIN(?, 1) AND r.id = UUID_TO_BIN(?, 1)
                """, (rs, n) -> new ResponsibleRow(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7),
                        rs.getString(8), rs.getString(9), rs.getString(10), rs.getString(11)),
                organizationId, responsibleId);
        if (rows.isEmpty()) throw ApiException.notFound("RESPONSIBLE_NOT_FOUND", "El responsable no existe.");
        return rows.getFirst();
    }

    private ResponsibleView view(String organizationId, String responsibleId) {
        var r = find(organizationId, responsibleId);
        return new ResponsibleView(r.id(), r.centerId(), r.centerName(), r.userId(), r.username(),
                r.role(), r.email(), r.validFrom(), r.validUntil(), r.state(), r.assignedBy());
    }

    private void insertHistory(String organizationId, String responsibleId, String action,
                               String centerId, String userId, String role, String email,
                               String validFrom, String validUntil, AccessContext ctx, String reason) {
        jdbc.update("""
                INSERT INTO center_responsible_history (organization_id, responsible_id, action,
                    center_id, user_id, responsibility_role, institutional_email,
                    valid_from, valid_until, changed_by, reason)
                VALUES (UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), ?, UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1),
                        ?, ?, ?, ?, UUID_TO_BIN(?, 1), ?)
                """, organizationId, responsibleId, action, centerId, userId, role, email,
                validFrom == null ? null : java.sql.Timestamp.valueOf(validFrom.replace('T', ' ')),
                validUntil == null ? null : java.sql.Timestamp.valueOf(validUntil.replace('T', ' ')),
                actor(ctx), reason);
    }

    private void requireCenter(String organizationId, String centerId) {
        var exists = jdbc.queryForObject("""
                SELECT COUNT(*) FROM center WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)
                """, Long.class, organizationId, centerId);
        if (exists == null || exists == 0) {
            throw ApiException.notFound("CENTER_NOT_FOUND", "El centro no existe.");
        }
    }

    private void requireUser(String organizationId, String userId) {
        var exists = jdbc.queryForObject("""
                SELECT COUNT(*) FROM app_user WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)
                """, Long.class, organizationId, userId);
        if (exists == null || exists == 0) {
            throw ApiException.unprocessable("VALIDATION_FAILED", "El usuario no existe en la organización.");
        }
    }

    private static ResponsibilityRole requireRole(ResponsibilityRole role) {
        if (role == null) {
            throw ApiException.unprocessable("VALIDATION_FAILED", "responsibility_role es obligatorio.");
        }
        return role;
    }

    private static String requireEmail(String email) {
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw ApiException.unprocessable("VALIDATION_FAILED", "institutional_email inválido.");
        }
        return email.trim();
    }

    private static String parseInstant(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return java.sql.Timestamp.valueOf(value.trim().replace('T', ' ')).toString();
        } catch (IllegalArgumentException invalid) {
            throw ApiException.unprocessable("VALIDATION_FAILED", "Fecha inválida; use ISO-8601.");
        }
    }

    private static String requireUuid(String value) {
        if (value == null || !value.matches("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")) {
            throw ApiException.unprocessable("VALIDATION_FAILED", "Identificador UUID inválido.");
        }
        return value;
    }

    private static String optionalUuid(String value) {
        if (value == null || value.isBlank()) return null;
        return requireUuid(value);
    }

    private static String actor(AccessContext ctx) {
        return ctx.profile() == null ? null : ctx.profile().id();
    }
}
