package com.veris.inventory.access.services;

import java.util.List;
import java.util.Set;
import com.veris.inventory.access.services.AccessGuard.AccessContext;

/** Casos de uso de administración de roles y permisos por organización. */
public interface RoleService {

    record CreateRoleCommand(String code, String name, String description) {}

    record UpdateRoleCommand(String name, String description) {}

    record RoleView(String id, String code, String name, String description,
                    Set<String> permissions, long activeAssignments, String createdAt) {}

    List<RoleView> list(String organizationId, AccessContext ctx);

    RoleView detail(String organizationId, AccessContext ctx, String roleId);

    RoleView create(String organizationId, AccessContext ctx, CreateRoleCommand command);

    RoleView update(String organizationId, AccessContext ctx, String roleId,
                    UpdateRoleCommand command);

    RoleView replacePermissions(String organizationId, AccessContext ctx, String roleId,
                                Set<String> permissionCodes);
}
