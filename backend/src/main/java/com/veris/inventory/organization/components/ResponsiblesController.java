package com.veris.inventory.organization.components;

import java.util.List;
import com.veris.inventory.access.config.CurrentAccess;
import com.veris.inventory.access.services.AccessGuard.AccessContext;
import com.veris.inventory.organization.services.ResponsibleService;
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

/** Responsables de centro: jefaturas, coordinadores y responsables TI. */
@RestController
@RequestMapping("/api/v1/org/responsibles")
public class ResponsiblesController {
    private final ResponsibleService responsibles;

    public ResponsiblesController(ResponsibleService responsibles) {
        this.responsibles = responsibles;
    }

    @GetMapping
    public List<ResponsibleService.ResponsibleView> list(@CurrentAccess AccessContext ctx,
                                                         @RequestParam(required = false) String centerId) {
        return responsibles.list(StructureController.organization(ctx), ctx, centerId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponsibleService.ResponsibleView create(@CurrentAccess AccessContext ctx,
                                                     @RequestBody ResponsibleService.ResponsibleCommand body) {
        return responsibles.create(StructureController.organization(ctx), ctx, body);
    }

    @PatchMapping("/{id}")
    public ResponsibleService.ResponsibleView update(@CurrentAccess AccessContext ctx,
                                                     @PathVariable String id,
                                                     @RequestBody ResponsibleService.UpdateCommand body) {
        return responsibles.update(StructureController.organization(ctx), ctx, id, body);
    }

    @PostMapping("/{id}/end")
    public ResponsibleService.ResponsibleView end(@CurrentAccess AccessContext ctx,
                                                  @PathVariable String id,
                                                  @RequestBody(required = false) ReasonBody body) {
        return responsibles.end(StructureController.organization(ctx), ctx, id,
                body == null ? null : body.reason());
    }

    @GetMapping("/{id}/history")
    public List<ResponsibleService.HistoryView> history(@CurrentAccess AccessContext ctx,
                                                        @PathVariable String id) {
        return responsibles.history(StructureController.organization(ctx), ctx, id);
    }

    record ReasonBody(String reason) {}
}
