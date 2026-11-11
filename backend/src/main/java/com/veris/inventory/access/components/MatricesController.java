package com.veris.inventory.access.components;

import java.util.List;
import com.veris.inventory.access.config.CurrentAccess;
import com.veris.inventory.access.services.AccessGuard.AccessContext;
import com.veris.inventory.access.services.MatrixService;
import com.veris.inventory.access.services.MatrixService.AddMemberCommand;
import com.veris.inventory.access.services.MatrixService.CreateMatrixCommand;
import com.veris.inventory.access.services.MatrixService.MatrixView;
import com.veris.inventory.access.services.MatrixService.UpdateMatrixCommand;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1")
public class MatricesController {
    private final MatrixService matrices;

    public MatricesController(MatrixService matrices) {
        this.matrices = matrices;
    }

    @GetMapping("/signature-matrices")
    public List<MatrixView> list(@CurrentAccess AccessContext ctx,
                                 @RequestParam(defaultValue = "false") boolean includeInactive) {
        return matrices.list(UsersController.organization(ctx), ctx, includeInactive);
    }

    @GetMapping("/signature-matrices/{id}")
    public MatrixView detail(@CurrentAccess AccessContext ctx, @PathVariable String id) {
        return matrices.detail(UsersController.organization(ctx), ctx, id);
    }

    @PostMapping("/signature-matrices")
    @ResponseStatus(HttpStatus.CREATED)
    public MatrixView create(@CurrentAccess AccessContext ctx,
                             @RequestBody CreateMatrixCommand command) {
        return matrices.create(UsersController.organization(ctx), ctx, command);
    }

    @PatchMapping("/signature-matrices/{id}")
    public MatrixView update(@CurrentAccess AccessContext ctx, @PathVariable String id,
                             @RequestBody UpdateMatrixCommand command) {
        return matrices.update(UsersController.organization(ctx), ctx, id, command);
    }

    @PostMapping("/signature-matrices/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MatrixService.MemberView addMember(@CurrentAccess AccessContext ctx,
                                              @PathVariable String id,
                                              @RequestBody AddMemberCommand command) {
        return matrices.addMember(UsersController.organization(ctx), ctx, id, command);
    }

    @DeleteMapping("/signature-matrices/{id}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@CurrentAccess AccessContext ctx, @PathVariable String id,
                             @PathVariable String memberId) {
        matrices.removeMember(UsersController.organization(ctx), ctx, id, memberId);
    }

    /** Matrices activas del propio usuario: base de «firmas autorizadas». */
    @GetMapping("/me/matrices")
    public List<MatrixView> myMatrices(@CurrentAccess AccessContext ctx) {
        return matrices.membershipsOf(UsersController.organization(ctx), ctx);
    }
}
