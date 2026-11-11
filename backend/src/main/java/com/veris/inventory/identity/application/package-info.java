/**
 * Capa de aplicación del módulo de identidad: casos de uso y orquestación.
 *
 * <p>Debe depender de puertos, no de detalles de persistencia. Deuda conocida:
 * {@code LocalProfiles} todavía usa {@code JdbcTemplate} directamente; la extracción
 * del adaptador JDBC corresponde a la fase que consolide identidad.</p>
 */
package com.veris.inventory.identity.application;
