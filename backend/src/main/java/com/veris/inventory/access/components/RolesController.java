package com.veris.inventory.access.components;

import java.util.List;
import java.util.Set;
import com.veris.inventory.access.config.CurrentAccess;
import com.veris.inventory.access.services.AccessGuard.AccessContext;
import com.veris.inventory.access.services.RoleService;
import com.veris.inventory.access.services.RoleService.CreateRoleCommand;
import com.veris.inventory.access.services.RoleService.RoleView;
import com.veris.inventory.access.services.RoleService.UpdateRoleCommand;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/roles")
public class RolesController {
    private final RoleService roles;

    public RolesController(RoleService roles) {
        this.roles = roles;
    }

    @GetMapping
    public List<RoleView> list(@CurrentAccess AccessContext ctx) {
        return roles.list(UsersController.organization(ctx), ctx);
    }

    @GetMapping("/{id}")
    public RoleView detail(@CurrentAccess AccessContext ctx, @PathVariable String id) {
        return roles.detail(UsersController.organization(ctx), ctx, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoleView create(@CurrentAccess AccessContext ctx, @RequestBody CreateRoleCommand command) {
        return roles.create(UsersController.organization(ctx), ctx, command);
    }

    @PatchMapping("/{id}")
    public RoleView update(@CurrentAccess AccessContext ctx, @PathVariable String id,
                           @RequestBody UpdateRoleCommand command) {
        return roles.update(UsersController.organization(ctx), ctx, id, command);
    }

    @PutMapping("/{id}/permissions")
    public RoleView replacePermissions(@CurrentAccess AccessContext ctx, @PathVariable String id,
                                       @RequestBody ReplacePermissionsBody body) {
        return roles.replacePermissions(UsersController.organization(ctx), ctx, id,
                body == null ? Set.of() : body.permissionCodes());
    }

    record ReplacePermissionsBody(Set<String> permissionCodes) {}
}
