package com.veris.inventory.access.components;

import java.util.List;
import com.veris.inventory.access.config.CurrentAccess;
import com.veris.inventory.access.services.AccessGuard.AccessContext;
import com.veris.inventory.access.services.UserService;
import com.veris.inventory.access.services.UserService.AssignRoleCommand;
import com.veris.inventory.access.services.UserService.AssignmentView;
import com.veris.inventory.access.services.UserService.CreateUserCommand;
import com.veris.inventory.access.services.UserService.UpdateUserCommand;
import com.veris.inventory.access.services.UserService.UserDetail;
import com.veris.inventory.access.services.UserService.UserSummary;
import com.veris.inventory.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UsersController {
    private final UserService users;

    public UsersController(UserService users) {
        this.users = users;
    }

    @GetMapping
    public List<UserSummary> list(@CurrentAccess AccessContext ctx,
                                  @RequestParam(required = false) String state,
                                  @RequestParam(required = false) String q,
                                  @RequestParam(defaultValue = "50") int limit,
                                  @RequestParam(defaultValue = "0") int offset) {
        return users.list(organization(ctx), ctx, state, q, limit, offset);
    }

    @GetMapping("/{id}")
    public UserDetail detail(@CurrentAccess AccessContext ctx, @PathVariable String id) {
        return users.detail(organization(ctx), ctx, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserSummary create(@CurrentAccess AccessContext ctx,
                              @RequestBody CreateUserCommand command) {
        return users.create(organization(ctx), ctx, command);
    }

    @PatchMapping("/{id}")
    public UserSummary update(@CurrentAccess AccessContext ctx, @PathVariable String id,
                              @RequestBody UpdateUserCommand command) {
        return users.update(organization(ctx), ctx, id, command);
    }

    @PostMapping("/{id}/activate")
    public UserSummary activate(@CurrentAccess AccessContext ctx, @PathVariable String id) {
        return users.activate(organization(ctx), ctx, id);
    }

    @PostMapping("/{id}/suspend")
    public UserSummary suspend(@CurrentAccess AccessContext ctx, @PathVariable String id,
                               @RequestBody(required = false) ReasonBody body) {
        return users.suspend(organization(ctx), ctx, id, body == null ? null : body.reason());
    }

    @PostMapping("/{id}/sync")
    public UserDetail sync(@CurrentAccess AccessContext ctx, @PathVariable String id) {
        return users.sync(organization(ctx), ctx, id);
    }

    @PostMapping("/{id}/role-assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentView assignRole(@CurrentAccess AccessContext ctx, @PathVariable String id,
                                     @RequestBody AssignRoleCommand command) {
        return users.assignRole(organization(ctx), ctx, id, command);
    }

    @PostMapping("/{id}/role-assignments/{assignmentId}/revoke")
    public void revokeAssignment(@CurrentAccess AccessContext ctx, @PathVariable String id,
                                 @PathVariable String assignmentId,
                                 @RequestBody(required = false) ReasonBody body) {
        users.revokeAssignment(organization(ctx), ctx, id, assignmentId,
                body == null ? null : body.reason());
    }

    record ReasonBody(String reason) {}

    static String organization(AccessContext ctx) {
        if (ctx.profile() == null) {
            throw ApiException.forbidden("AUTHENTICATION_REQUIRED", "Sesión no autenticada.");
        }
        return ctx.profile().organizationId();
    }
}
