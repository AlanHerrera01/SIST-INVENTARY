package com.veris.inventory.access.services;

import java.util.List;
import com.veris.inventory.access.domain.ScopeKind;
import com.veris.inventory.access.services.AccessGuard.AccessContext;

/** Casos de uso de administración de usuarios y sincronización de identidad. */
public interface UserService {

    record CreateUserCommand(String username, String displayName, String email,
                             String temporaryPassword) {}

    record UpdateUserCommand(String displayName, String email) {}

    record AssignRoleCommand(String roleId, ScopeKind scopeKind,
                             List<String> centerIds, String validUntil, String reason) {}

    record UserSummary(String id, String username, String displayName, String email,
                       String state, String createdAt) {}

    record AssignmentView(String id, String roleId, String roleCode, String roleName,
                          String scopeKind, List<String> centerIds, String validFrom,
                          String validUntil, String state, String grantedBy) {}

    record MatrixMembershipView(String matrixId, String code, String name,
                                String centerId, String actType, String memberRole) {}

    record UserDetail(UserSummary summary, List<AssignmentView> assignments,
                      List<MatrixMembershipView> matrices) {}

    List<UserSummary> list(String organizationId, AccessContext ctx, String state,
                           String query, int limit, int offset);

    UserDetail detail(String organizationId, AccessContext ctx, String userId);

    UserSummary create(String organizationId, AccessContext ctx, CreateUserCommand command);

    UserSummary update(String organizationId, AccessContext ctx, String userId,
                       UpdateUserCommand command);

    UserSummary suspend(String organizationId, AccessContext ctx, String userId, String reason);

    UserSummary activate(String organizationId, AccessContext ctx, String userId);

    UserDetail sync(String organizationId, AccessContext ctx, String userId);

    AssignmentView assignRole(String organizationId, AccessContext ctx, String userId,
                              AssignRoleCommand command);

    void revokeAssignment(String organizationId, AccessContext ctx, String userId,
                          String assignmentId, String reason);
}
