package com.veris.inventory.access.components;

import java.util.List;
import com.veris.inventory.access.services.servicesImpl.PermissionCatalog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Catálogo global de permisos disponibles para asignar a roles. */
@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionsController {
    private final PermissionCatalog catalog;

    public PermissionsController(PermissionCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public List<PermissionCatalog.Permission> list() {
        return catalog.catalog();
    }
}
