package com.veris.inventory.organization.components;

import java.util.List;
import com.veris.inventory.access.config.CurrentAccess;
import com.veris.inventory.access.services.AccessGuard.AccessContext;
import com.veris.inventory.common.error.ApiException;
import com.veris.inventory.organization.domain.CenterKind;
import com.veris.inventory.organization.domain.SiteKind;
import com.veris.inventory.organization.services.OrganizationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Estructura organizacional multicentro: ciudades, sedes, centros, ubicaciones y matrices receptoras. */
@RestController
@RequestMapping("/api/v1/org")
public class StructureController {
    private final OrganizationService org;

    public StructureController(OrganizationService org) {
        this.org = org;
    }

    @GetMapping("/structure")
    public OrganizationService.StructureView structure(@CurrentAccess AccessContext ctx) {
        return org.structure(organization(ctx), ctx);
    }

    // ---- ciudades ----
    @PostMapping("/cities")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationService.CityView createCity(@CurrentAccess AccessContext ctx,
                                                   @RequestBody OrganizationService.CityCommand body) {
        return org.createCity(organization(ctx), ctx, body);
    }

    @PatchMapping("/cities/{id}")
    public OrganizationService.CityView updateCity(@CurrentAccess AccessContext ctx,
                                                   @PathVariable String id,
                                                   @RequestBody OrganizationService.UpdateCityCommand body) {
        return org.updateCity(organization(ctx), ctx, id, body);
    }

    @DeleteMapping("/cities/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCity(@CurrentAccess AccessContext ctx, @PathVariable String id) {
        org.deleteCity(organization(ctx), ctx, id);
    }

    // ---- sedes ----
    @PostMapping("/sites")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationService.SiteView createSite(@CurrentAccess AccessContext ctx,
                                                   @RequestBody OrganizationService.SiteCommand body) {
        return org.createSite(organization(ctx), ctx, body);
    }

    @PatchMapping("/sites/{id}")
    public OrganizationService.SiteView updateSite(@CurrentAccess AccessContext ctx,
                                                   @PathVariable String id,
                                                   @RequestBody OrganizationService.UpdateSiteCommand body) {
        return org.updateSite(organization(ctx), ctx, id, body);
    }

    @DeleteMapping("/sites/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSite(@CurrentAccess AccessContext ctx, @PathVariable String id) {
        org.deleteSite(organization(ctx), ctx, id);
    }

    // ---- centros ----
    @PostMapping("/centers")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationService.CenterView createCenter(@CurrentAccess AccessContext ctx,
                                                       @RequestBody OrganizationService.CenterCommand body) {
        return org.createCenter(organization(ctx), ctx, body);
    }

    @PatchMapping("/centers/{id}")
    public OrganizationService.CenterView updateCenter(@CurrentAccess AccessContext ctx,
                                                       @PathVariable String id,
                                                       @RequestBody OrganizationService.UpdateCenterCommand body) {
        return org.updateCenter(organization(ctx), ctx, id, body);
    }

    @DeleteMapping("/centers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCenter(@CurrentAccess AccessContext ctx, @PathVariable String id) {
        org.deleteCenter(organization(ctx), ctx, id);
    }

    @PostMapping("/locations")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationService.LocationView createLocation(@CurrentAccess AccessContext ctx,
                                                           @RequestBody OrganizationService.LocationCommand body) {
        return org.createLocation(organization(ctx), ctx, body);
    }

    // ---- matrices receptoras ----
    @PostMapping("/receiving-matrices")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationService.MatrixView createMatrix(@CurrentAccess AccessContext ctx,
                                                       @RequestBody OrganizationService.MatrixCommand body) {
        return org.createMatrix(organization(ctx), ctx, body);
    }

    @PatchMapping("/receiving-matrices/{id}")
    public OrganizationService.MatrixView updateMatrix(@CurrentAccess AccessContext ctx,
                                                       @PathVariable String id,
                                                       @RequestBody OrganizationService.UpdateMatrixCommand body) {
        return org.updateMatrix(organization(ctx), ctx, id, body);
    }

    @DeleteMapping("/receiving-matrices/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMatrix(@CurrentAccess AccessContext ctx, @PathVariable String id) {
        org.deleteMatrix(organization(ctx), ctx, id);
    }

    @PostMapping("/receiving-matrices/{id}/centers/{centerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void linkCenter(@CurrentAccess AccessContext ctx, @PathVariable String id,
                           @PathVariable String centerId) {
        org.linkCenter(organization(ctx), ctx, id, centerId);
    }

    @DeleteMapping("/receiving-matrices/{id}/centers/{centerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlinkCenter(@CurrentAccess AccessContext ctx, @PathVariable String id,
                             @PathVariable String centerId) {
        org.unlinkCenter(organization(ctx), ctx, id, centerId);
    }

    static String organization(AccessContext ctx) {
        if (ctx.profile() == null) {
            throw ApiException.forbidden("AUTHENTICATION_REQUIRED", "Sesión no autenticada.");
        }
        return ctx.profile().organizationId();
    }
}
