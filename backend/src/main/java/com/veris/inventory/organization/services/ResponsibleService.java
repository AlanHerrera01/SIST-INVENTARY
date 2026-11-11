package com.veris.inventory.organization.services;

import java.util.List;
import com.veris.inventory.access.services.AccessGuard.AccessContext;
import com.veris.inventory.organization.domain.ResponsibilityRole;

/**
 * Responsables de centro (jefaturas, coordinadores, responsables TI) con
 * correo institucional y trazabilidad histórica de cada cambio (Fase 06 §6-8).
 */
public interface ResponsibleService {

    record ResponsibleView(String id, String centerId, String centerName, String userId,
                           String username, String role, String email,
                           String validFrom, String validUntil, String state,
                           String assignedBy) {}

    record HistoryView(String id, String action, String centerId, String userId,
                       String role, String email, String validFrom, String validUntil,
                       String changedBy, String changedAt, String reason) {}

    record ResponsibleCommand(String centerId, String userId, ResponsibilityRole role,
                              String email, String validUntil, String reason) {}

    record UpdateCommand(String userId, ResponsibilityRole role, String email,
                         String validUntil, String reason) {}

    List<ResponsibleView> list(String organizationId, AccessContext ctx, String centerId);

    ResponsibleView create(String organizationId, AccessContext ctx, ResponsibleCommand command);

    ResponsibleView update(String organizationId, AccessContext ctx, String responsibleId,
                           UpdateCommand command);

    /** Cierra la vigencia del responsable (estado INACTIVO + historial ENDED). */
    ResponsibleView end(String organizationId, AccessContext ctx, String responsibleId, String reason);

    List<HistoryView> history(String organizationId, AccessContext ctx, String responsibleId);
}
