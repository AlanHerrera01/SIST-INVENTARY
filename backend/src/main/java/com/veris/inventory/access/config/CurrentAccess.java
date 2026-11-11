package com.veris.inventory.access.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inyecta el {@code AccessContext} del solicitante autenticado en un
 * controlador. El perfil lo resuelve {@code SessionGuard} en cada petición;
 * un contexto sin perfil equivale a denegación en {@code AccessGuard}.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentAccess {
}
