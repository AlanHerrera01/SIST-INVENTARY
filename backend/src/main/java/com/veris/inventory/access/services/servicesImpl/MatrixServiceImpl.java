package com.veris.inventory.access.services.servicesImpl;

import com.veris.inventory.access.services.AccessGuard;
import com.veris.inventory.access.services.AccessGuard.AccessContext;
import com.veris.inventory.access.services.MatrixService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.veris.inventory.access.domain.MatrixMemberRole;
import com.veris.inventory.common.error.ApiException;
import com.veris.inventory.platform.AuditRecorder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *Matrices organizacionales de firmas y aprobaciones (prompt Fase 04 §4 y §6).
 * Una matriz autoriza a sus miembros a firmar o aprobar actas de un tipo en
 * un centro o en toda la organización; la pertenencia es la asignación del
 * usuario. Los endpoints de firma de fases posteriores consultan estas
 * tablas para validar «firmas autorizadas».
 */
@Service
public class MatrixServiceImpl implements MatrixService {
    private static final Set<String> ACT_TYPES = Set.of("DESPACHO", "RECEPCION", "FINAL", "BAJA");
    private static final Set<String> STATES = Set.of("ACTIVO", "INACTIVO");

    private final JdbcTemplate jdbc;
    private final AuditRecorder audit;
    private final AccessGuard guard;

    public MatrixServiceImpl(JdbcTemplate jdbc, AuditRecorder audit, AccessGuard guard) {
        this.jdbc = jdbc;
        this.audit = audit;
        this.guard = guard;
    }

    public List<MatrixView> list(String organizationId, AccessContext ctx, boolean includeInactive) {
        guard.require(ctx, PermissionCatalog.MATRICES_READ);
        var sql = """
                SELECT BIN_TO_UUID(m.id,1), m.code, m.name, BIN_TO_UUID(m.center_id,1),
                       c.code, m.act_type, m.matrix_state, m.description
                FROM signature_matrix m
                LEFT JOIN center c ON c.organization_id = m.organization_id AND c.id = m.center_id
                WHERE m.organization_id = UUID_TO_BIN(?, 1)
                """ + (includeInactive ? "" : " AND m.matrix_state = 'ACTIVO'") + " ORDER BY m.code";
        var rows = jdbc.query(sql, (rs, n) -> new MatrixView(rs.getString(1), rs.getString(2),
                rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7),
                rs.getString(8), List.of()), organizationId);
        return withMembers(organizationId, rows);
    }

    public MatrixView detail(String organizationId, AccessContext ctx, String matrixId) {
        guard.require(ctx, PermissionCatalog.MATRICES_READ);
        return viewOf(organizationId, matrixId);
    }

    @Transactional
    public MatrixView create(String organizationId, AccessContext ctx, CreateMatrixCommand command) {
        guard.requireGlobal(ctx, PermissionCatalog.MATRICES_MANAGE);
        var code = requireCode(command.code());
        var name = requireName(command.name());
        var actType = validateActType(command.actType());
        requireOptionalCenter(organizationId, command.centerId());
        var id = java.util.UUID.randomUUID().toString();
        try {
            jdbc.update("""
                    INSERT INTO signature_matrix (id, organization_id, center_id, code, name,
                        act_type, description, matrix_state)
                    VALUES (UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), ?, ?, ?, ?, 'ACTIVO')
                    """, id, organizationId, command.centerId(), code, name, actType,
                    command.description() == null || command.description().isBlank()
                            ? null : command.description().trim());
        } catch (DuplicateKeyException duplicate) {
            throw ApiException.conflict("MATRIX_CODE_TAKEN", "Ya existe una matriz con ese código.");
        }
        audit.record(organizationId, actor(ctx), "MATRIX_CREATED", "SIGNATURE_MATRIX", id,
                command.centerId(), Map.of("code", code, "name", name,
                        "act_type", actType == null ? "TODOS" : actType));
        return viewOf(organizationId, id);
    }

    @Transactional
    public MatrixView update(String organizationId, AccessContext ctx, String matrixId,
                             UpdateMatrixCommand command) {
        guard.requireGlobal(ctx, PermissionCatalog.MATRICES_MANAGE);
        var current = viewOf(organizationId, matrixId);
        var name = command.name() == null ? current.name() : requireName(command.name());
        var actType = command.actType() == null ? current.actType() : validateActType(command.actType());
        var state = command.matrixState() == null || command.matrixState().isBlank()
                ? current.matrixState()
                : command.matrixState().trim().toUpperCase();
        if (!STATES.contains(state)) {
            throw ApiException.unprocessable("VALIDATION_FAILED", "matrix_state inválido.");
        }
        var description = command.description() == null
                ? current.description()
                : (command.description().isBlank() ? null : command.description().trim());
        jdbc.update("""
                UPDATE signature_matrix SET name = ?, description = ?, act_type = ?, matrix_state = ?
                WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)
                """, name, description, actType, state,
                organizationId, matrixId);
        audit.record(organizationId, actor(ctx), "MATRIX_UPDATED", "SIGNATURE_MATRIX", matrixId, null,
                Map.of("name", name, "act_type", actType == null ? "TODOS" : actType,
                        "matrix_state", state));
        return viewOf(organizationId, matrixId);
    }

    /** Incorpora un usuario como firmante o aprobador de la matriz. */
    @Transactional
    public MemberView addMember(String organizationId, AccessContext ctx, String matrixId,
                                AddMemberCommand command) {
        guard.requireGlobal(ctx, PermissionCatalog.MATRICES_MANAGE);
        viewOf(organizationId, matrixId);
        if (command.memberRole() == null) {
            throw ApiException.unprocessable("VALIDATION_FAILED", "member_role es obligatorio.");
        }
        if (command.signerPosition() != null && command.signerPosition() < 1) {
            throw ApiException.unprocessable("VALIDATION_FAILED", "signer_position debe ser >= 1.");
        }
        var userState = jdbc.queryForObject("""
                SELECT user_state FROM app_user
                WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)
                """, String.class, organizationId, command.userId());
        if (userState == null) {
            throw ApiException.notFound("USER_NOT_FOUND", "Usuario no encontrado.");
        }
        if (!"ACTIVO".equals(userState)) {
            throw ApiException.conflict("INVALID_TRANSITION",
                    "Solo usuarios activos pueden integrarse a una matriz.");
        }
        var id = java.util.UUID.randomUUID().toString();
        try {
            jdbc.update("""
                    INSERT INTO signature_matrix_member (id, organization_id, matrix_id, user_id,
                        member_role, signer_position)
                    VALUES (UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1),
                        UUID_TO_BIN(?, 1), ?, ?)
                    """, id, organizationId, matrixId, command.userId(),
                    command.memberRole().name(), command.signerPosition());
        } catch (DuplicateKeyException duplicate) {
            throw ApiException.conflict("MEMBER_ALREADY_ASSIGNED",
                    "El usuario ya tiene ese rol en la matriz.");
        }
        audit.record(organizationId, actor(ctx), "MATRIX_MEMBER_ADDED", "SIGNATURE_MATRIX_MEMBER",
                id, null, Map.of("matrix_id", matrixId, "user_id", command.userId(),
                        "member_role", command.memberRole().name()));
        return membersOf(organizationId, matrixId).stream()
                .filter(m -> m.id().equals(id)).findFirst().orElseThrow();
    }

    @Transactional
    public void removeMember(String organizationId, AccessContext ctx, String matrixId,
                             String memberId) {
        guard.requireGlobal(ctx, PermissionCatalog.MATRICES_MANAGE);
        viewOf(organizationId, matrixId);
        var deleted = jdbc.update("""
                DELETE FROM signature_matrix_member
                WHERE organization_id = UUID_TO_BIN(?, 1) AND matrix_id = UUID_TO_BIN(?, 1)
                  AND id = UUID_TO_BIN(?, 1)
                """, organizationId, matrixId, memberId);
        if (deleted == 0) {
            throw ApiException.notFound("MEMBER_NOT_FOUND", "Miembro no encontrado en la matriz.");
        }
        audit.record(organizationId, actor(ctx), "MATRIX_MEMBER_REMOVED",
                "SIGNATURE_MATRIX_MEMBER", memberId, null, Map.of("matrix_id", matrixId));
    }

    /** Matrices activas en las que el usuario es miembro (para «firmas autorizadas»). */
    public List<MatrixView> membershipsOf(String organizationId, AccessContext ctx) {
        if (ctx.profile() == null) return List.of();
        var userId = ctx.profile().id();
        var rows = jdbc.query("""
                SELECT DISTINCT BIN_TO_UUID(m.id,1), m.code, m.name, BIN_TO_UUID(m.center_id,1),
                       c.code, m.act_type, m.matrix_state, m.description
                FROM signature_matrix_member mm
                JOIN signature_matrix m ON m.organization_id = mm.organization_id AND m.id = mm.matrix_id
                LEFT JOIN center c ON c.organization_id = m.organization_id AND c.id = m.center_id
                WHERE mm.organization_id = UUID_TO_BIN(?, 1) AND mm.user_id = UUID_TO_BIN(?, 1)
                  AND m.matrix_state = 'ACTIVO'
                ORDER BY m.code
                """, (rs, n) -> new MatrixView(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7),
                        rs.getString(8), List.of()), organizationId, userId);
        return withMembers(organizationId, rows);
    }

    // ---- internos ----

    private MatrixView viewOf(String organizationId, String matrixId) {
        var rows = jdbc.query("""
                SELECT BIN_TO_UUID(m.id,1), m.code, m.name, BIN_TO_UUID(m.center_id,1),
                       c.code, m.act_type, m.matrix_state, m.description
                FROM signature_matrix m
                LEFT JOIN center c ON c.organization_id = m.organization_id AND c.id = m.center_id
                WHERE m.organization_id = UUID_TO_BIN(?, 1) AND m.id = UUID_TO_BIN(?, 1)
                """, (rs, n) -> new MatrixView(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7),
                        rs.getString(8), List.of()), organizationId, matrixId);
        if (rows.isEmpty()) {
            throw ApiException.notFound("MATRIX_NOT_FOUND", "Matriz no encontrada.");
        }
        var view = rows.getFirst();
        return new MatrixView(view.id(), view.code(), view.name(), view.centerId(), view.centerCode(),
                view.actType(), view.matrixState(), view.description(), membersOf(organizationId, matrixId));
    }

    private List<MatrixView> withMembers(String organizationId, List<MatrixView> rows) {
        return rows.stream().map(v -> new MatrixView(v.id(), v.code(), v.name(), v.centerId(),
                v.centerCode(), v.actType(), v.matrixState(), v.description(),
                membersOf(organizationId, v.id()))).toList();
    }

    private List<MemberView> membersOf(String organizationId, String matrixId) {
        return jdbc.query("""
                SELECT BIN_TO_UUID(mm.id,1), BIN_TO_UUID(mm.user_id,1), u.username, u.display_name,
                       mm.member_role, mm.signer_position
                FROM signature_matrix_member mm
                JOIN app_user u ON u.organization_id = mm.organization_id AND u.id = mm.user_id
                WHERE mm.organization_id = UUID_TO_BIN(?, 1) AND mm.matrix_id = UUID_TO_BIN(?, 1)
                ORDER BY mm.signer_position IS NULL, mm.signer_position, u.display_name
                """, (rs, n) -> new MemberView(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5),
                        rs.getObject(6) == null ? null : rs.getInt(6)), organizationId, matrixId);
    }

    private static String requireCode(String code) {
        if (code == null || !code.matches("^[A-Za-z0-9_-]{2,50}$")) {
            throw ApiException.unprocessable("VALIDATION_FAILED",
                    "code debe tener 2-50 caracteres alfanuméricos.");
        }
        return code;
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank() || name.trim().length() > 200) {
            throw ApiException.unprocessable("VALIDATION_FAILED", "name es obligatorio (máx. 200).");
        }
        return name.trim();
    }

    private static String validateActType(String actType) {
        if (actType == null || actType.isBlank()) return null;
        var normalized = actType.trim().toUpperCase();
        if (!ACT_TYPES.contains(normalized)) {
            throw ApiException.unprocessable("VALIDATION_FAILED",
                    "act_type debe ser DESPACHO, RECEPCION, FINAL o BAJA.");
        }
        return normalized;
    }

    private void requireOptionalCenter(String organizationId, String centerId) {
        if (centerId == null || centerId.isBlank()) return;
        var found = jdbc.queryForObject("""
                SELECT COUNT(*) FROM center
                WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)
                """, Long.class, organizationId, centerId.trim());
        if (found == null || found == 0) {
            throw ApiException.unprocessable("VALIDATION_FAILED", "El centro no existe en la organización.");
        }
    }

    private static String actor(AccessContext ctx) {
        return ctx.profile() == null ? null : ctx.profile().id();
    }
}
