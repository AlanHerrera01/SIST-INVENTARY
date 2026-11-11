package com.veris.inventory.access.services;

import java.util.Set;
import java.util.TreeSet;
import com.veris.inventory.common.error.ApiException;
import com.veris.inventory.identity.application.LocalProfiles;
import org.springframework.stereotype.Component;

/**
 * Punto único de decisión RBAC del backend (INV-06). Todo módulo de negocio
 * y todo endpoint del módulo de acceso verifica permisos aquí; el frontend
 * nunca es mecanismo de seguridad. La evaluación usa el perfil vigente que
 * {@code SessionGuard} recarga en cada petición, por lo que la revocación de
 * roles o la suspensión surten efecto en la siguiente llamada.
 */
@Component
public class AccessGuard {
    /** Contexto de acceso del solicitante; profile nulo equivale a denegación. */
    public record AccessContext(LocalProfiles.Profile profile) {
        public boolean authenticated() { return profile != null; }
    }

    public Set<String> permissions(AccessContext context) {
        if (context == null || context.profile() == null) return Set.of();
        var result = new TreeSet<String>();
        for (var grant : context.profile().grants()) {
            if (grant.permission() != null) result.add(grant.permission());
        }
        return Set.copyOf(result);
    }

    public boolean has(AccessContext context, String permission) {
        if (context == null || context.profile() == null) return false;
        return context.profile().grants().stream().anyMatch(g -> permission.equals(g.permission()));
    }

    /** Permiso con alcance GLOBAL en la organización. */
    public boolean hasGlobal(AccessContext context, String permission) {
        return context != null && context.profile() != null && context.profile().grants().stream()
                .anyMatch(g -> permission.equals(g.permission()) && "GLOBAL".equals(g.scope()));
    }

    /** Permiso GLOBAL o concedido explícitamente sobre el centro indicado. */
    public boolean hasForCenter(AccessContext context, String permission, String centerId) {
        if (hasGlobal(context, permission)) return true;
        if (context == null || context.profile() == null || centerId == null) return false;
        return context.profile().grants().stream()
                .anyMatch(g -> permission.equals(g.permission()) && centerId.equals(g.centerId()));
    }

    /** Centros sobre los que el contexto tiene el permiso con alcance CENTROS. */
    public Set<String> centersFor(AccessContext context, String permission) {
        var centers = new TreeSet<String>();
        if (context == null || context.profile() == null) return Set.of();
        for (var grant : context.profile().grants()) {
            if (permission.equals(grant.permission()) && grant.centerId() != null) centers.add(grant.centerId());
        }
        return Set.copyOf(centers);
    }

    public void require(AccessContext context, String permission) {
        if (!has(context, permission)) {
            throw ApiException.forbidden("ACCESS_DENIED", "Permiso insuficiente: " + permission);
        }
    }

    public void requireGlobal(AccessContext context, String permission) {
        if (!hasGlobal(context, permission)) {
            throw ApiException.forbidden("ACCESS_DENIED", "Se requiere alcance global para: " + permission);
        }
    }

    public void requireForCenter(AccessContext context, String permission, String centerId) {
        if (!hasForCenter(context, permission, centerId)) {
            throw ApiException.forbidden("CENTER_SCOPE_DENIED", "Sin alcance sobre el centro indicado.");
        }
    }

    /**
     * Regla anti-escalación (Fase 00 §4): quien asigna un rol debe poseer con
     * alcance GLOBAL cada permiso que el rol concede. Así nadie puede crear
     * ni conceder privilegios que no posee, ni siquiera sobre sí mismo.
     */
    public void requireDelegable(AccessContext context, Set<String> grantedPermissions) {
        for (var permission : grantedPermissions) {
            requireGlobal(context, permission);
        }
    }
}
