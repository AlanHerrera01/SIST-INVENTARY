package com.veris.inventory.platform;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Auditoría de cambios administrativos (prompt Fase 04 §10; Fase 00 §10.2).
 * Los eventos confirmados se insertan dentro de la misma transacción de la
 * mutación: si la auditoría falla, la operación falla. Nunca se registran
 * contraseñas, tokens ni secretos.
 */
@Component
public class AuditRecorder {
    private final JdbcTemplate jdbc;

    public AuditRecorder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void record(String organizationId, String actorUserId, String action,
                       String entityType, String entityId, String centerId,
                       Map<String, Object> changedFields) {
        jdbc.update("""
                INSERT INTO audit_event (organization_id, center_id,
                    effective_actor_user_id, original_actor_user_id, action,
                    entity_type, entity_id, outcome, occurred_at, changed_fields)
                VALUES (UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1),
                    UUID_TO_BIN(?, 1), ?, ?, UUID_TO_BIN(?, 1), 'EXITO',
                    UTC_TIMESTAMP(6), ?)
                """,
                organizationId,
                centerId,
                actorUserId,
                actorUserId,
                action,
                entityType,
                entityId,
                changedFields == null ? null : toJson(changedFields));
    }

    private static String toJson(Map<String, Object> fields) {
        var builder = new StringBuilder("{");
        var first = true;
        for (var entry : fields.entrySet()) {
            if (!first) builder.append(',');
            first = false;
            builder.append('"').append(escape(entry.getKey())).append("\":");
            builder.append(jsonValue(entry.getValue()));
        }
        return builder.append('}').toString();
    }

    private static String jsonValue(Object value) {
        if (value == null) return "null";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        return '"' + escape(value.toString()) + '"';
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
