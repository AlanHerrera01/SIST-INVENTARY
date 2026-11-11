package com.veris.inventory.access.services.servicesImpl;

import com.veris.inventory.access.services.AccessGuard;
import com.veris.inventory.access.services.AccessGuard.AccessContext;
import com.veris.inventory.access.services.AuditService;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.veris.inventory.common.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Consulta de auditoría con alcance por recurso (prompt Fase 04 §10). Quien
 * tiene {@code access.audit.read} GLOBAL lee toda la organización; un alcance
 * CENTROS limita los eventos a sus centros (los eventos sin centro son
 * administrativos globales y quedan fuera).
 */
@Service
public class AuditServiceImpl implements AuditService {
    private static final int MAX_LIMIT = 200;
    private final JdbcTemplate jdbc;
    private final AccessGuard guard;

    public AuditServiceImpl(JdbcTemplate jdbc, AccessGuard guard) {
        this.jdbc = jdbc;
        this.guard = guard;
    }

    public AuditPage list(String organizationId, AccessContext ctx, String entityType,
                          String entityId, String action, String outcome,
                          String actorUserId, String occurredFrom, String occurredTo,
                          String cursor, int limit) {
        guard.require(ctx, PermissionCatalog.AUDIT_READ);
        var global = guard.hasGlobal(ctx, PermissionCatalog.AUDIT_READ);
        var centers = guard.centersFor(ctx, PermissionCatalog.AUDIT_READ);
        if (!global && centers.isEmpty()) {
            return new AuditPage(List.of(), null);
        }
        var effectiveLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);

        var sql = new StringBuilder("""
                SELECT BIN_TO_UUID(e.id,1), CAST(e.occurred_at AS CHAR), e.action, e.entity_type,
                       BIN_TO_UUID(e.entity_id,1), e.outcome,
                       BIN_TO_UUID(e.effective_actor_user_id,1),
                       u.display_name, BIN_TO_UUID(e.center_id,1), e.changed_fields
                FROM audit_event e
                LEFT JOIN app_user u ON u.id = e.effective_actor_user_id
                WHERE e.organization_id = UUID_TO_BIN(?, 1)
                """);
        var args = new ArrayList<Object>();
        args.add(organizationId);
        if (!global) {
            sql.append(" AND e.center_id IN (")
                    .append(centers.stream().map(c -> "UUID_TO_BIN('" + c + "',1)")
                            .reduce((a, b) -> a + "," + b).orElseThrow())
                    .append(')');
        }
        if (entityType != null && !entityType.isBlank()) {
            sql.append(" AND e.entity_type = ?");
            args.add(entityType.trim());
        }
        if (entityId != null && !entityId.isBlank()) {
            sql.append(" AND e.entity_id = UUID_TO_BIN(?, 1)");
            args.add(entityId.trim());
        }
        if (action != null && !action.isBlank()) {
            sql.append(" AND e.action = ?");
            args.add(action.trim());
        }
        if (outcome != null && !outcome.isBlank()) {
            sql.append(" AND e.outcome = ?");
            args.add(outcome.trim().toUpperCase());
        }
        if (actorUserId != null && !actorUserId.isBlank()) {
            sql.append(" AND e.effective_actor_user_id = UUID_TO_BIN(?, 1)");
            args.add(actorUserId.trim());
        }
        if (occurredFrom != null && !occurredFrom.isBlank()) {
            sql.append(" AND e.occurred_at >= ?");
            args.add(java.sql.Timestamp.valueOf(occurredFrom.trim().replace('T', ' ')));
        }
        if (occurredTo != null && !occurredTo.isBlank()) {
            sql.append(" AND e.occurred_at <= ?");
            args.add(java.sql.Timestamp.valueOf(occurredTo.trim().replace('T', ' ')));
        }
        if (cursor != null && !cursor.isBlank()) {
            var decoded = decodeCursor(cursor);
            sql.append(" AND (e.occurred_at < ? OR (e.occurred_at = ? AND e.id < UUID_TO_BIN(?, 1)))");
            args.add(decoded.at());
            args.add(decoded.at());
            args.add(decoded.id());
        }
        sql.append(" ORDER BY e.occurred_at DESC, e.id DESC LIMIT ?");
        args.add(effectiveLimit + 1);

        var rows = jdbc.query(sql.toString(), (rs, n) -> new AuditEventView(
                rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8),
                rs.getString(9), rs.getString(10)), args.toArray());
        String next = null;
        if (rows.size() > effectiveLimit) {
            var last = rows.get(effectiveLimit - 1);
            next = encodeCursor(last.occurredAt(), last.id());
            rows = rows.subList(0, effectiveLimit);
        }
        return new AuditPage(List.copyOf(rows), next);
    }

    private record Cursor(java.sql.Timestamp at, String id) {}

    private static Cursor decodeCursor(String cursor) {
        try {
            var raw = new String(Base64.getUrlDecoder().decode(cursor));
            var parts = raw.split("_", 2);
            if (parts.length != 2) throw new IllegalArgumentException();
            return new Cursor(java.sql.Timestamp.valueOf(parts[0].replace('T', ' ')), parts[1]);
        } catch (RuntimeException invalid) {
            throw ApiException.badRequest("INVALID_CURSOR", "Cursor de paginación inválido.");
        }
    }

    private static String encodeCursor(String occurredAt, String id) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((occurredAt.replace(' ', 'T') + "_" + id).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
