package com.veris.inventory.organization.services.servicesImpl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.veris.inventory.access.services.AccessGuard;
import com.veris.inventory.access.services.AccessGuard.AccessContext;
import com.veris.inventory.access.services.servicesImpl.PermissionCatalog;
import com.veris.inventory.common.error.ApiException;
import com.veris.inventory.organization.domain.CenterKind;
import com.veris.inventory.organization.domain.SiteKind;
import com.veris.inventory.organization.services.OrganizationService;
import com.veris.inventory.platform.AuditRecorder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación JDBC de la estructura organizacional. Toda escritura exige
 * {@code organization.manage} GLOBAL y queda auditada; las lecturas exigen
 * {@code organization.read}. Las eliminaciones verifican vínculos y rechazan
 * con 409 cuando la estructura tiene activos o movimientos (conservación).
 */
@Service
public class OrganizationServiceImpl implements OrganizationService {

    private final JdbcTemplate jdbc;
    private final AccessGuard guard;
    private final AuditRecorder audit;

    public OrganizationServiceImpl(JdbcTemplate jdbc, AccessGuard guard, AuditRecorder audit) {
        this.jdbc = jdbc;
        this.guard = guard;
        this.audit = audit;
    }

    // ---------- consultas ----------

    public StructureView structure(String organizationId, AccessContext ctx) {
        guard.require(ctx, PermissionCatalog.ORGANIZATION_READ);
        var cities = jdbc.query("""
                SELECT BIN_TO_UUID(id,1), code, name, city_state FROM city
                WHERE organization_id = UUID_TO_BIN(?, 1) ORDER BY name
                """, (rs, n) -> new CityView(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)),
                organizationId);
        var sites = jdbc.query("""
                SELECT BIN_TO_UUID(s.id,1), BIN_TO_UUID(s.city_id,1), c.name, s.code, s.name,
                       s.site_kind, s.site_state
                FROM site s JOIN city c ON c.id = s.city_id AND c.organization_id = s.organization_id
                WHERE s.organization_id = UUID_TO_BIN(?, 1) ORDER BY s.name
                """, (rs, n) -> new SiteView(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getString(7)), organizationId);
        var centers = jdbc.query("""
                SELECT BIN_TO_UUID(c.id,1), c.code, c.name, c.center_kind, c.center_state,
                       BIN_TO_UUID(c.city_id,1), ci.name, BIN_TO_UUID(c.site_id,1), s.name
                FROM center c
                LEFT JOIN city ci ON ci.id = c.city_id AND ci.organization_id = c.organization_id
                LEFT JOIN site s ON s.id = c.site_id AND s.organization_id = c.organization_id
                WHERE c.organization_id = UUID_TO_BIN(?, 1) ORDER BY c.name
                """, (rs, n) -> new CenterView(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7),
                        rs.getString(8), rs.getString(9)), organizationId);
        var locations = jdbc.query("""
                SELECT BIN_TO_UUID(id,1), BIN_TO_UUID(center_id,1), name, location_type,
                       BIN_TO_UUID(parent_location_id,1)
                FROM location WHERE organization_id = UUID_TO_BIN(?, 1) ORDER BY name
                """, (rs, n) -> new LocationView(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5)), organizationId);
        var matrices = jdbc.query("""
                SELECT BIN_TO_UUID(m.id,1), m.code, m.name, m.matrix_state, m.description,
                       BIN_TO_UUID(m.city_id,1), ci.name, BIN_TO_UUID(m.site_id,1)
                FROM receiving_matrix m
                LEFT JOIN city ci ON ci.id = m.city_id AND ci.organization_id = m.organization_id
                WHERE m.organization_id = UUID_TO_BIN(?, 1) ORDER BY m.code
                """, (rs, n) -> new Object[]{rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8)},
                organizationId);
        var matrixList = new ArrayList<MatrixView>();
        for (var row : matrices) {
            var centerIds = jdbc.queryForList("""
                    SELECT BIN_TO_UUID(center_id,1) FROM receiving_matrix_center
                    WHERE organization_id = UUID_TO_BIN(?, 1) AND matrix_id = UUID_TO_BIN(?, 1)
                    """, String.class, organizationId, row[0]);
            matrixList.add(new MatrixView((String) row[0], (String) row[1], (String) row[2],
                    (String) row[3], (String) row[4], (String) row[5], (String) row[6],
                    (String) row[7], List.copyOf(centerIds)));
        }
        return new StructureView(cities, sites, centers, locations, List.copyOf(matrixList));
    }

    // ---------- ciudades ----------

    @Transactional
    public CityView createCity(String organizationId, AccessContext ctx, CityCommand command) {
        guard.requireGlobal(ctx, PermissionCatalog.ORGANIZATION_MANAGE);
        var code = requireText(command.code(), "code", 20);
        var name = requireText(command.name(), "name", 120);
        var id = java.util.UUID.randomUUID().toString();
        try {
            jdbc.update("""
                    INSERT INTO city (id, organization_id, code, name) VALUES (UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), ?, ?)
                    """, id, organizationId, code, name);
        } catch (org.springframework.dao.DuplicateKeyException dup) {
            throw ApiException.conflict("CITY_CODE_TAKEN", "Ya existe una ciudad con ese código.");
        }
        audit.record(organizationId, actor(ctx), "CITY_CREATED", "CITY", id, null,
                Map.of("code", code, "name", name));
        return new CityView(id, code, name, "ACTIVO");
    }

    @Transactional
    public CityView updateCity(String organizationId, AccessContext ctx, String cityId, UpdateCityCommand command) {
        guard.requireGlobal(ctx, PermissionCatalog.ORGANIZATION_MANAGE);
        requireUuid(cityId);
        var current = findCity(organizationId, cityId);
        var name = command.name() == null ? current.name() : requireText(command.name(), "name", 120);
        var state = command.state() == null ? current.state() : validateState(command.state());
        jdbc.update("""
                UPDATE city SET name = ?, city_state = ?
                WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)
                """, name, state, organizationId, cityId);
        audit.record(organizationId, actor(ctx), "CITY_UPDATED", "CITY", cityId, null,
                Map.of("name", name, "state", state));
        return new CityView(cityId, current.code(), name, state);
    }

    @Transactional
    public void deleteCity(String organizationId, AccessContext ctx, String cityId) {
        guard.requireGlobal(ctx, PermissionCatalog.ORGANIZATION_MANAGE);
        requireUuid(cityId);
        findCity(organizationId, cityId);
        assertNoLinks(organizationId, "site", "city_id", cityId, "CIUDAD");
        assertNoLinks(organizationId, "center", "city_id", cityId, "CIUDAD");
        assertNoLinks(organizationId, "receiving_matrix", "city_id", cityId, "CIUDAD");
        jdbc.update("DELETE FROM city WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)",
                organizationId, cityId);
        audit.record(organizationId, actor(ctx), "CITY_DELETED", "CITY", cityId, null, Map.of());
    }

    // ---------- sedes ----------

    @Transactional
    public SiteView createSite(String organizationId, AccessContext ctx, SiteCommand command) {
        guard.requireGlobal(ctx, PermissionCatalog.ORGANIZATION_MANAGE);
        var cityId = requireUuid(command.cityId());
        findCity(organizationId, cityId);
        var code = requireText(command.code(), "code", 20);
        var name = requireText(command.name(), "name", 200);
        var kind = command.kind() == null ? SiteKind.SEDE : command.kind();
        var id = java.util.UUID.randomUUID().toString();
        try {
            jdbc.update("""
                    INSERT INTO site (id, organization_id, city_id, code, name, site_kind)
                    VALUES (UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), ?, ?, ?)
                    """, id, organizationId, cityId, code, name, kind.name());
        } catch (org.springframework.dao.DuplicateKeyException dup) {
            throw ApiException.conflict("SITE_CODE_TAKEN", "Ya existe una sede con ese código.");
        }
        audit.record(organizationId, actor(ctx), "SITE_CREATED", "SITE", id, null,
                Map.of("code", code, "name", name, "city_id", cityId, "kind", kind.name()));
        return new SiteView(id, cityId, cityName(organizationId, cityId), code, name, kind.name(), "ACTIVO");
    }

    @Transactional
    public SiteView updateSite(String organizationId, AccessContext ctx, String siteId, UpdateSiteCommand command) {
        guard.requireGlobal(ctx, PermissionCatalog.ORGANIZATION_MANAGE);
        requireUuid(siteId);
        var current = findSite(organizationId, siteId);
        var name = command.name() == null ? current.name() : requireText(command.name(), "name", 200);
        var kind = command.kind() == null ? SiteKind.valueOf(current.kind()) : command.kind();
        var state = command.state() == null ? current.state() : validateState(command.state());
        jdbc.update("""
                UPDATE site SET name = ?, site_kind = ?, site_state = ?
                WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)
                """, name, kind.name(), state, organizationId, siteId);
        audit.record(organizationId, actor(ctx), "SITE_UPDATED", "SITE", siteId, null,
                Map.of("name", name, "kind", kind.name(), "state", state));
        return new SiteView(siteId, current.cityId(), current.cityName(), current.code(), name, kind.name(), state);
    }

    @Transactional
    public void deleteSite(String organizationId, AccessContext ctx, String siteId) {
        guard.requireGlobal(ctx, PermissionCatalog.ORGANIZATION_MANAGE);
        requireUuid(siteId);
        findSite(organizationId, siteId);
        assertNoLinks(organizationId, "center", "site_id", siteId, "SEDE");
        assertNoLinks(organizationId, "receiving_matrix", "site_id", siteId, "SEDE");
        jdbc.update("DELETE FROM site WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)",
                organizationId, siteId);
        audit.record(organizationId, actor(ctx), "SITE_DELETED", "SITE", siteId, null, Map.of());
    }

    // ---------- centros y ubicaciones ----------

    @Transactional
    public CenterView createCenter(String organizationId, AccessContext ctx, CenterCommand command) {
        guard.requireGlobal(ctx, PermissionCatalog.ORGANIZATION_MANAGE);
        var code = requireText(command.code(), "code", 20);
        var name = requireText(command.name(), "name", 200);
        var kind = command.kind() == null ? CenterKind.OTRO : command.kind();
        var cityId = optionalUuid(command.cityId(), "city_id");
        var siteId = optionalUuid(command.siteId(), "site_id");
        if (cityId != null) findCity(organizationId, cityId);
        if (siteId != null) findSite(organizationId, siteId);
        var id = java.util.UUID.randomUUID().toString();
        try {
            jdbc.update("""
                    INSERT INTO center (id, organization_id, code, name, city_id, site_id, center_kind)
                    VALUES (UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), ?, ?, UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), ?)
                    """, id, organizationId, code, name, cityId, siteId, kind.name());
        } catch (org.springframework.dao.DuplicateKeyException dup) {
            throw ApiException.conflict("CENTER_CODE_TAKEN", "Ya existe un centro con ese código.");
        }
        audit.record(organizationId, actor(ctx), "CENTER_CREATED", "CENTER", id, null,
                Map.of("code", code, "name", name, "kind", kind.name()));
        return centerView(organizationId, id);
    }

    @Transactional
    public CenterView updateCenter(String organizationId, AccessContext ctx, String centerId,
                                    UpdateCenterCommand command) {
        guard.requireGlobal(ctx, PermissionCatalog.ORGANIZATION_MANAGE);
        requireUuid(centerId);
        var current = centerView(organizationId, centerId);
        var name = command.name() == null ? current.name() : requireText(command.name(), "name", 200);
        var kind = command.kind() == null ? CenterKind.valueOf(current.kind()) : command.kind();
        var state = command.state() == null ? current.state() : validateState(command.state());
        var cityId = command.cityId() == null ? current.cityId() : optionalUuid(command.cityId(), "city_id");
        var siteId = command.siteId() == null ? current.siteId() : optionalUuid(command.siteId(), "site_id");
        if (cityId != null) findCity(organizationId, cityId);
        if (siteId != null) findSite(organizationId, siteId);
        jdbc.update("""
                UPDATE center SET name = ?, center_kind = ?, center_state = ?, city_id = UUID_TO_BIN(?, 1), site_id = UUID_TO_BIN(?, 1)
                WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)
                """, name, kind.name(), state, cityId, siteId, organizationId, centerId);
        audit.record(organizationId, actor(ctx), "CENTER_UPDATED", "CENTER", centerId, null,
                Map.of("name", name, "kind", kind.name(), "state", state));
        return centerView(organizationId, centerId);
    }

    /**
     * Eliminación física solo si el centro no tiene activos, movimientos,
     * custodias, responsables, ubicaciones ni matrices asociadas. En caso
     * contrario debe inactivarse (center_state = INACTIVO).
     */
    @Transactional
    public void deleteCenter(String organizationId, AccessContext ctx, String centerId) {
        guard.requireGlobal(ctx, PermissionCatalog.ORGANIZATION_MANAGE);
        requireUuid(centerId);
        centerView(organizationId, centerId);
        var blockers = new LinkedHashMap<String, Long>();
        countInto(blockers, "activos", "SELECT COUNT(*) FROM asset WHERE organization_id = UUID_TO_BIN(?, 1) AND registral_center_id = UUID_TO_BIN(?, 1)", organizationId, centerId);
        countInto(blockers, "traslados", "SELECT COUNT(*) FROM transfer WHERE organization_id = UUID_TO_BIN(?, 1) AND (source_center_id = UUID_TO_BIN(?, 1) OR destination_center_id = UUID_TO_BIN(?, 1))", organizationId, centerId, centerId);
        countInto(blockers, "custodias", "SELECT COUNT(*) FROM custodian WHERE organization_id = UUID_TO_BIN(?, 1) AND center_id = UUID_TO_BIN(?, 1)", organizationId, centerId);
        countInto(blockers, "ubicaciones", "SELECT COUNT(*) FROM location WHERE organization_id = UUID_TO_BIN(?, 1) AND center_id = UUID_TO_BIN(?, 1)", organizationId, centerId);
        countInto(blockers, "responsables", "SELECT COUNT(*) FROM center_responsible WHERE organization_id = UUID_TO_BIN(?, 1) AND center_id = UUID_TO_BIN(?, 1)", organizationId, centerId);
        countInto(blockers, "matrices_receptoras", "SELECT COUNT(*) FROM receiving_matrix_center WHERE organization_id = UUID_TO_BIN(?, 1) AND center_id = UUID_TO_BIN(?, 1)", organizationId, centerId);
        countInto(blockers, "matrices_firma", "SELECT COUNT(*) FROM signature_matrix WHERE organization_id = UUID_TO_BIN(?, 1) AND center_id = UUID_TO_BIN(?, 1)", organizationId, centerId);
        countInto(blockers, "asignaciones_rol", "SELECT COUNT(*) FROM user_role_assignment_center WHERE organization_id = UUID_TO_BIN(?, 1) AND center_id = UUID_TO_BIN(?, 1)", organizationId, centerId);
        var used = blockers.entrySet().stream().filter(e -> e.getValue() > 0).toList();
        if (!used.isEmpty()) {
            var detail = used.stream().map(e -> e.getKey() + "=" + e.getValue())
                    .reduce((a, b) -> a + ", " + b).orElse("");
            throw ApiException.conflict("STRUCTURE_IN_USE",
                    "El centro tiene vínculos y no puede eliminarse; inactívelo. Referencias: " + detail);
        }
        jdbc.update("DELETE FROM center WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)",
                organizationId, centerId);
        audit.record(organizationId, actor(ctx), "CENTER_DELETED", "CENTER", centerId, null, Map.of());
    }

    @Transactional
    public LocationView createLocation(String organizationId, AccessContext ctx, LocationCommand command) {
        guard.requireGlobal(ctx, PermissionCatalog.ORGANIZATION_MANAGE);
        var centerId = requireUuid(command.centerId());
        centerView(organizationId, centerId);
        var name = requireText(command.name(), "name", 200);
        var type = requireText(command.type(), "location_type", 50);
        String parentId = null;
        if (command.parentLocationId() != null && !command.parentLocationId().isBlank()) {
            parentId = requireUuid(command.parentLocationId());
            var parentCenter = jdbc.queryForObject("""
                    SELECT BIN_TO_UUID(center_id,1) FROM location
                    WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)
                    """, String.class, organizationId, parentId);
            if (parentCenter == null || !parentCenter.equals(centerId)) {
                throw ApiException.unprocessable("VALIDATION_FAILED", "La ubicación padre debe pertenecer al mismo centro.");
            }
        }
        var id = java.util.UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO location (id, organization_id, center_id, name, location_type, parent_location_id)
                VALUES (UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), ?, ?, UUID_TO_BIN(?, 1))
                """, id, organizationId, centerId, name, type, parentId);
        audit.record(organizationId, actor(ctx), "LOCATION_CREATED", "LOCATION", id, centerId,
                Map.of("name", name, "type", type));
        return new LocationView(id, centerId, name, type, parentId);
    }

    // ---------- matrices receptoras ----------

    @Transactional
    public MatrixView createMatrix(String organizationId, AccessContext ctx, MatrixCommand command) {
        guard.requireGlobal(ctx, PermissionCatalog.ORGANIZATION_MANAGE);
        var code = requireText(command.code(), "code", 20);
        var name = requireText(command.name(), "name", 200);
        var cityId = optionalUuid(command.cityId(), "city_id");
        var siteId = optionalUuid(command.siteId(), "site_id");
        if (cityId != null) findCity(organizationId, cityId);
        if (siteId != null) findSite(organizationId, siteId);
        var id = java.util.UUID.randomUUID().toString();
        try {
            jdbc.update("""
                    INSERT INTO receiving_matrix (id, organization_id, city_id, site_id, code, name, description)
                    VALUES (UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), ?, ?, ?)
                    """, id, organizationId, cityId, siteId, code, name, command.description());
        } catch (org.springframework.dao.DuplicateKeyException dup) {
            throw ApiException.conflict("MATRIX_CODE_TAKEN", "Ya existe una matriz receptora con ese código.");
        }
        audit.record(organizationId, actor(ctx), "RECEIVING_MATRIX_CREATED", "RECEIVING_MATRIX", id, null,
                Map.of("code", code, "name", name));
        return matrixView(organizationId, id);
    }

    @Transactional
    public MatrixView updateMatrix(String organizationId, AccessContext ctx, String matrixId,
                                    UpdateMatrixCommand command) {
        guard.requireGlobal(ctx, PermissionCatalog.ORGANIZATION_MANAGE);
        requireUuid(matrixId);
        var current = matrixView(organizationId, matrixId);
        var name = command.name() == null ? current.name() : requireText(command.name(), "name", 200);
        var state = command.state() == null ? current.state() : validateState(command.state());
        var cityId = command.cityId() == null ? current.cityId() : optionalUuid(command.cityId(), "city_id");
        var siteId = command.siteId() == null ? current.siteId() : optionalUuid(command.siteId(), "site_id");
        var description = command.description() == null ? current.description() : command.description();
        if (cityId != null) findCity(organizationId, cityId);
        if (siteId != null) findSite(organizationId, siteId);
        jdbc.update("""
                UPDATE receiving_matrix SET name = ?, matrix_state = ?, city_id = UUID_TO_BIN(?, 1),
                       site_id = UUID_TO_BIN(?, 1), description = ?
                WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)
                """, name, state, cityId, siteId, description, organizationId, matrixId);
        audit.record(organizationId, actor(ctx), "RECEIVING_MATRIX_UPDATED", "RECEIVING_MATRIX", matrixId, null,
                Map.of("name", name, "state", state));
        return matrixView(organizationId, matrixId);
    }

    @Transactional
    public void deleteMatrix(String organizationId, AccessContext ctx, String matrixId) {
        guard.requireGlobal(ctx, PermissionCatalog.ORGANIZATION_MANAGE);
        requireUuid(matrixId);
        matrixView(organizationId, matrixId);
        assertNoLinks(organizationId, "receiving_matrix_center", "matrix_id", matrixId, "MATRIZ_RECEPTORA");
        jdbc.update("DELETE FROM receiving_matrix WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)",
                organizationId, matrixId);
        audit.record(organizationId, actor(ctx), "RECEIVING_MATRIX_DELETED", "RECEIVING_MATRIX", matrixId, null, Map.of());
    }

    @Transactional
    public void linkCenter(String organizationId, AccessContext ctx, String matrixId, String centerId) {
        guard.requireGlobal(ctx, PermissionCatalog.ORGANIZATION_MANAGE);
        requireUuid(matrixId);
        requireUuid(centerId);
        matrixView(organizationId, matrixId);
        centerView(organizationId, centerId);
        try {
            jdbc.update("""
                    INSERT INTO receiving_matrix_center (organization_id, matrix_id, center_id)
                    VALUES (UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1))
                    """, organizationId, matrixId, centerId);
        } catch (org.springframework.dao.DuplicateKeyException dup) {
            throw ApiException.conflict("MATRIX_CENTER_LINKED", "El centro ya pertenece a la matriz.");
        }
        audit.record(organizationId, actor(ctx), "MATRIX_CENTER_LINKED", "RECEIVING_MATRIX", matrixId, centerId,
                Map.of("center_id", centerId));
    }

    @Transactional
    public void unlinkCenter(String organizationId, AccessContext ctx, String matrixId, String centerId) {
        guard.requireGlobal(ctx, PermissionCatalog.ORGANIZATION_MANAGE);
        requireUuid(matrixId);
        requireUuid(centerId);
        matrixView(organizationId, matrixId);
        var removed = jdbc.update("""
                DELETE FROM receiving_matrix_center
                WHERE organization_id = UUID_TO_BIN(?, 1) AND matrix_id = UUID_TO_BIN(?, 1)
                  AND center_id = UUID_TO_BIN(?, 1)
                """, organizationId, matrixId, centerId);
        if (removed == 0) {
            throw ApiException.notFound("MATRIX_CENTER_LINK", "El vínculo matriz-centro no existe.");
        }
        audit.record(organizationId, actor(ctx), "MATRIX_CENTER_UNLINKED", "RECEIVING_MATRIX", matrixId, centerId,
                Map.of("center_id", centerId));
    }

    // ---------- internos ----------

    private record CityRow(String id, String code, String name, String state) {}
    private record SiteRow(String id, String cityId, String cityName, String code, String name, String kind, String state) {}

    private CityRow findCity(String organizationId, String cityId) {
        var rows = jdbc.query("""
                SELECT BIN_TO_UUID(id,1), code, name, city_state FROM city
                WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)
                """, (rs, n) -> new CityRow(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)),
                organizationId, cityId);
        if (rows.isEmpty()) throw ApiException.notFound("CITY_NOT_FOUND", "La ciudad no existe.");
        return rows.getFirst();
    }

    private SiteRow findSite(String organizationId, String siteId) {
        var rows = jdbc.query("""
                SELECT BIN_TO_UUID(s.id,1), BIN_TO_UUID(s.city_id,1), c.name, s.code, s.name, s.site_kind, s.site_state
                FROM site s JOIN city c ON c.id = s.city_id AND c.organization_id = s.organization_id
                WHERE s.organization_id = UUID_TO_BIN(?, 1) AND s.id = UUID_TO_BIN(?, 1)
                """, (rs, n) -> new SiteRow(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getString(7)), organizationId, siteId);
        if (rows.isEmpty()) throw ApiException.notFound("SITE_NOT_FOUND", "La sede no existe.");
        return rows.getFirst();
    }

    private CenterView centerView(String organizationId, String centerId) {
        var rows = jdbc.query("""
                SELECT BIN_TO_UUID(c.id,1), c.code, c.name, c.center_kind, c.center_state,
                       BIN_TO_UUID(c.city_id,1), ci.name, BIN_TO_UUID(c.site_id,1), s.name
                FROM center c
                LEFT JOIN city ci ON ci.id = c.city_id AND ci.organization_id = c.organization_id
                LEFT JOIN site s ON s.id = c.site_id AND s.organization_id = c.organization_id
                WHERE c.organization_id = UUID_TO_BIN(?, 1) AND c.id = UUID_TO_BIN(?, 1)
                """, (rs, n) -> new CenterView(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7),
                        rs.getString(8), rs.getString(9)), organizationId, centerId);
        if (rows.isEmpty()) throw ApiException.notFound("CENTER_NOT_FOUND", "El centro no existe.");
        return rows.getFirst();
    }

    private MatrixView matrixView(String organizationId, String matrixId) {
        var rows = jdbc.query("""
                SELECT BIN_TO_UUID(m.id,1), m.code, m.name, m.matrix_state, m.description,
                       BIN_TO_UUID(m.city_id,1), ci.name, BIN_TO_UUID(m.site_id,1)
                FROM receiving_matrix m
                LEFT JOIN city ci ON ci.id = m.city_id AND ci.organization_id = m.organization_id
                WHERE m.organization_id = UUID_TO_BIN(?, 1) AND m.id = UUID_TO_BIN(?, 1)
                """, (rs, n) -> new Object[]{rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8)},
                organizationId, matrixId);
        if (rows.isEmpty()) throw ApiException.notFound("MATRIX_NOT_FOUND", "La matriz receptora no existe.");
        var row = rows.getFirst();
        var centerIds = jdbc.queryForList("""
                SELECT BIN_TO_UUID(center_id,1) FROM receiving_matrix_center
                WHERE organization_id = UUID_TO_BIN(?, 1) AND matrix_id = UUID_TO_BIN(?, 1)
                """, String.class, organizationId, matrixId);
        return new MatrixView((String) row[0], (String) row[1], (String) row[2], (String) row[3],
                (String) row[4], (String) row[5], (String) row[6], (String) row[7], List.copyOf(centerIds));
    }

    private String cityName(String organizationId, String cityId) {
        return jdbc.queryForObject("SELECT name FROM city WHERE organization_id = UUID_TO_BIN(?, 1) AND id = UUID_TO_BIN(?, 1)",
                String.class, organizationId, cityId);
    }

    private void assertNoLinks(String organizationId, String table, String column, String id, String label) {
        var sql = "SELECT COUNT(*) FROM " + table + " WHERE organization_id = UUID_TO_BIN(?, 1) AND "
                + column + " = UUID_TO_BIN(?, 1)";
        var count = jdbc.queryForObject(sql, Long.class, organizationId, id);
        if (count != null && count > 0) {
            throw ApiException.conflict("STRUCTURE_IN_USE",
                    "La " + label.toLowerCase() + " tiene " + count + " registro(s) vinculado(s); desactívela en lugar de eliminarla.");
        }
    }

    private void countInto(Map<String, Long> map, String key, String sql, Object... args) {
        map.put(key, jdbc.queryForObject(sql, Long.class, args));
    }

    private static String requireText(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            throw ApiException.unprocessable("VALIDATION_FAILED", field + " es obligatorio.");
        }
        var trimmed = value.trim();
        if (trimmed.length() > max) {
            throw ApiException.unprocessable("VALIDATION_FAILED", field + " excede " + max + " caracteres.");
        }
        return trimmed;
    }

    private static String validateState(String state) {
        var normalized = state.trim().toUpperCase();
        if (!normalized.equals("ACTIVO") && !normalized.equals("INACTIVO")) {
            throw ApiException.unprocessable("VALIDATION_FAILED", "state debe ser ACTIVO o INACTIVO.");
        }
        return normalized;
    }

    private static String requireUuid(String value) {
        if (value == null || !value.matches("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")) {
            throw ApiException.unprocessable("VALIDATION_FAILED", "Identificador UUID inválido.");
        }
        return value;
    }

    private static String optionalUuid(String value, String field) {
        if (value == null || value.isBlank()) return null;
        return requireUuid(value);
    }

    private static String actor(AccessContext ctx) {
        return ctx.profile() == null ? null : ctx.profile().id();
    }
}
