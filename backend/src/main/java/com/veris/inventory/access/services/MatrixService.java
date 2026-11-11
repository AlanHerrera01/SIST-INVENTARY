package com.veris.inventory.access.services;

import java.util.List;
import com.veris.inventory.access.domain.MatrixMemberRole;
import com.veris.inventory.access.services.AccessGuard.AccessContext;

/**
 * Casos de uso de matrices organizacionales de firmas y aprobaciones, y de
 * consulta de las matrices propias del usuario autenticado.
 */
public interface MatrixService {

    record CreateMatrixCommand(String code, String name, String centerId,
                               String actType, String description) {}

    record UpdateMatrixCommand(String name, String description, String actType,
                               String matrixState) {}

    record AddMemberCommand(String userId, MatrixMemberRole memberRole,
                            Integer signerPosition) {}

    record MemberView(String id, String userId, String username, String displayName,
                      String memberRole, Integer signerPosition) {}

    record MatrixView(String id, String code, String name, String centerId,
                      String centerCode, String actType, String matrixState,
                      String description, List<MemberView> members) {}

    List<MatrixView> list(String organizationId, AccessContext ctx, boolean includeInactive);

    MatrixView detail(String organizationId, AccessContext ctx, String matrixId);

    MatrixView create(String organizationId, AccessContext ctx, CreateMatrixCommand command);

    MatrixView update(String organizationId, AccessContext ctx, String matrixId,
                      UpdateMatrixCommand command);

    MemberView addMember(String organizationId, AccessContext ctx, String matrixId,
                         AddMemberCommand command);

    void removeMember(String organizationId, AccessContext ctx, String matrixId, String memberId);

    List<MatrixView> membershipsOf(String organizationId, AccessContext ctx);
}
