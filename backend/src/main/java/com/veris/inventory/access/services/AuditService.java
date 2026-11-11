package com.veris.inventory.access.services;

import java.util.List;
import com.veris.inventory.access.services.AccessGuard.AccessContext;

/** Consulta de eventos de auditoría con alcance por recurso. */
public interface AuditService {

    record AuditEventView(String id, String occurredAt, String action,
                          String entityType, String entityId, String outcome,
                          String actorUserId, String actorName, String centerId,
                          String changedFields) {}

    record AuditPage(List<AuditEventView> items, String nextCursor) {}

    AuditPage list(String organizationId, AccessContext ctx, String entityType,
                   String entityId, String action, String outcome,
                   String actorUserId, String occurredFrom, String occurredTo,
                   String cursor, int limit);
}
