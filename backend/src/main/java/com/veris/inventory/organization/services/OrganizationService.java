package com.veris.inventory.organization.services;

import java.util.List;
import com.veris.inventory.access.services.AccessGuard.AccessContext;
import com.veris.inventory.organization.domain.CenterKind;
import com.veris.inventory.organization.domain.SiteKind;

/**
 * Casos de uso de la estructura organizacional multicentro (Fase 06):
 * ciudades, sedes, centros (médicos, bodegas TI), ubicaciones y matrices
 * receptoras. La eliminación física solo procede cuando no existen vínculos;
 * en caso contrario se exige desactivación (regla de conservación).
 */
public interface OrganizationService {

    // ---- Vistas ----
    record CityView(String id, String code, String name, String state) {}
    record SiteView(String id, String cityId, String cityName, String code, String name,
                    String kind, String state) {}
    record CenterView(String id, String code, String name, String kind, String state,
                      String cityId, String cityName, String siteId, String siteName) {}
    record LocationView(String id, String centerId, String name, String type,
                        String parentLocationId) {}
    record MatrixView(String id, String code, String name, String state, String description,
                      String cityId, String cityName, String siteId,
                      List<String> centerIds) {}
    record StructureView(List<CityView> cities, List<SiteView> sites,
                         List<CenterView> centers, List<LocationView> locations,
                         List<MatrixView> matrices) {}

    // ---- Comandos ----
    record CityCommand(String code, String name) {}
    record UpdateCityCommand(String name, String state) {}
    record SiteCommand(String cityId, String code, String name, SiteKind kind) {}
    record UpdateSiteCommand(String name, SiteKind kind, String state) {}
    record CenterCommand(String code, String name, String cityId, String siteId, CenterKind kind) {}
    record UpdateCenterCommand(String name, String cityId, String siteId, CenterKind kind, String state) {}
    record LocationCommand(String centerId, String name, String type, String parentLocationId) {}
    record MatrixCommand(String code, String name, String cityId, String siteId, String description) {}
    record UpdateMatrixCommand(String name, String cityId, String siteId, String state, String description) {}

    // ---- Consultas ----
    StructureView structure(String organizationId, AccessContext ctx);

    // ---- Ciudades ----
    CityView createCity(String organizationId, AccessContext ctx, CityCommand command);
    CityView updateCity(String organizationId, AccessContext ctx, String cityId, UpdateCityCommand command);
    void deleteCity(String organizationId, AccessContext ctx, String cityId);

    // ---- Sedes ----
    SiteView createSite(String organizationId, AccessContext ctx, SiteCommand command);
    SiteView updateSite(String organizationId, AccessContext ctx, String siteId, UpdateSiteCommand command);
    void deleteSite(String organizationId, AccessContext ctx, String siteId);

    // ---- Centros y ubicaciones ----
    CenterView createCenter(String organizationId, AccessContext ctx, CenterCommand command);
    CenterView updateCenter(String organizationId, AccessContext ctx, String centerId, UpdateCenterCommand command);
    void deleteCenter(String organizationId, AccessContext ctx, String centerId);
    LocationView createLocation(String organizationId, AccessContext ctx, LocationCommand command);

    // ---- Matrices receptoras ----
    MatrixView createMatrix(String organizationId, AccessContext ctx, MatrixCommand command);
    MatrixView updateMatrix(String organizationId, AccessContext ctx, String matrixId, UpdateMatrixCommand command);
    void deleteMatrix(String organizationId, AccessContext ctx, String matrixId);
    void linkCenter(String organizationId, AccessContext ctx, String matrixId, String centerId);
    void unlinkCenter(String organizationId, AccessContext ctx, String matrixId, String centerId);
}
