package com.veris.inventory.access.services;

import java.util.Optional;

/**
 * Puerto hacia el proveedor de identidad (Keycloak Admin REST API). La
 * aplicación no almacena credenciales: el adaptador usa un cliente de
 * servicio del realm. Los identificadores devueltos son los que el proveedor
 * emite como `sub` en el ID token.
 */
public interface IdentityAdminGateway {

    /** Usuario del proveedor por su identificador estable (subject). */
    record ExternalUser(String id, String username, String email, boolean enabled,
                        String firstName, String lastName) {}

    record NewUser(String username, String email, String firstName, String lastName,
                   boolean enabled, String temporaryPassword) {}

    Optional<ExternalUser> findBySubject(String subject);

    Optional<ExternalUser> findByUsername(String username);

    /** Crea el usuario y devuelve su subject; lanza ApiException si ya existe. */
    ExternalUser create(NewUser user);

    /** Actualiza nombre visible y correo; no toca credenciales ni estado. */
    void updateProfile(String subject, String firstName, String email);

    /** Habilita o deshabilita el inicio de sesión en el proveedor. */
    void setEnabled(String subject, boolean enabled);

    /** Invalida las sesiones abiertas del usuario en el proveedor. */
    void logout(String subject);

    /** Fija una contraseña temporal que exige cambio en el primer acceso. */
    void resetPassword(String subject, String temporaryPassword);

    /** Elimina el usuario del proveedor; se usa solo para compensar una alta. */
    void delete(String subject);
}
