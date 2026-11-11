/**
 * Módulo de acceso: usuarios locales, roles, permisos, alcances por centro,
 * matrices organizacionales de firmas y auditoría administrativa.
 *
 * <p>La identidad externa vive en el proveedor OIDC; este módulo mantiene el
 * perfil local, la autorización efectiva y la auditoría de cambios
 * administrativos. Ningún módulo de negocio decide permisos por su cuenta:
 * consulta {@code AccessGuard}.</p>
 *
 * <p>Estructura: {@code components} (REST), {@code services} (contratos y
 * guard), {@code servicesImpl} (implementaciones), {@code domain} (tipos) y
 * {@code config} (soporte web).</p>
 */
package com.veris.inventory.access;
