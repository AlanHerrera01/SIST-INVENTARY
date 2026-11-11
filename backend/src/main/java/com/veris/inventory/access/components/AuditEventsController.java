package com.veris.inventory.access.components;

import com.veris.inventory.access.config.CurrentAccess;
import com.veris.inventory.access.services.AccessGuard.AccessContext;
import com.veris.inventory.access.services.AuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-events")
public class AuditEventsController {
    private final AuditService audit;

    public AuditEventsController(AuditService audit) {
        this.audit = audit;
    }

    @GetMapping
    public AuditService.AuditPage list(@CurrentAccess AccessContext ctx,
                                       @RequestParam(required = false) String entityType,
                                       @RequestParam(required = false) String entityId,
                                       @RequestParam(required = false) String action,
                                       @RequestParam(required = false) String outcome,
                                       @RequestParam(required = false) String actorUserId,
                                       @RequestParam(required = false) String occurredFrom,
                                       @RequestParam(required = false) String occurredTo,
                                       @RequestParam(required = false) String cursor,
                                       @RequestParam(defaultValue = "50") int limit) {
        return audit.list(UsersController.organization(ctx), ctx, entityType, entityId,
                action, outcome, actorUserId, occurredFrom, occurredTo, cursor, limit);
    }
}
