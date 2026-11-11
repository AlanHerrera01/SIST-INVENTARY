package com.veris.inventory.access.services.servicesImpl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Catálogo de permisos del sistema (INV-06). Los códigos son estables y los
 * define la aplicación; la tabla {@code permission} los persiste para las
 * asignaciones rol-permiso. {@link #ensureCatalog} es idempotente y lo
 * invoca el arranque; nunca elimina permisos retirados del catálogo.
 */
@Component
public class PermissionCatalog {
    public record Permission(String code, String name, String description) {}

    // Administración del propio módulo de acceso.
    public static final String USERS_READ = "access.users.read";
    public static final String USERS_MANAGE = "access.users.manage";
    public static final String ROLES_READ = "access.roles.read";
    public static final String ROLES_MANAGE = "access.roles.manage";
    public static final String ASSIGNMENTS_MANAGE = "access.assignments.manage";
    public static final String MATRICES_READ = "access.matrices.read";
    public static final String MATRICES_MANAGE = "access.matrices.manage";
    public static final String AUDIT_READ = "access.audit.read";

    // Centros y catálogos.
    public static final String CENTERS_READ = "centers.read";
    public static final String CENTERS_MANAGE = "centers.manage";
    public static final String ORGANIZATION_READ = "organization.read";
    public static final String ORGANIZATION_MANAGE = "organization.manage";
    public static final String CATALOGS_READ = "catalogs.read";
    public static final String CATALOGS_MANAGE = "catalogs.manage";

    // Inventario y custodia.
    public static final String INVENTORY_READ = "inventory.read";
    public static final String INVENTORY_WRITE = "inventory.write";
    public static final String CUSTODY_MANAGE = "custody.manage";

    // Traslados y recepciones.
    public static final String TRANSFERS_READ = "transfers.read";
    public static final String TRANSFERS_CREATE = "transfers.create";
    public static final String TRANSFERS_SUBMIT = "transfers.submit";
    public static final String TRANSFERS_AUTHORIZE = "transfers.authorize";
    public static final String TRANSFERS_DISPATCH = "transfers.dispatch";
    public static final String TRANSFERS_CANCEL = "transfers.cancel";
    public static final String RECEIPTS_CREATE = "receipts.create";
    public static final String RECEIPTS_CONFIRM = "receipts.confirm";

    // Discrepancias, resoluciones y bajas.
    public static final String DISCREPANCIES_MANAGE = "discrepancies.manage";
    public static final String RESOLUTIONS_MANAGE = "resolutions.manage";
    public static final String RETIREMENTS_REQUEST = "retirements.request";
    public static final String RETIREMENTS_APPROVE = "retirements.approve";

    // Actas, firmas y archivos.
    public static final String ACTS_READ = "acts.read";
    public static final String ACTS_MANAGE = "acts.manage";
    public static final String SIGNATURES_SIGN = "signatures.sign";
    public static final String FILES_UPLOAD = "files.upload";
    public static final String FILES_DOWNLOAD = "files.download";

    public static final List<Permission> ALL = List.of(
            new Permission(USERS_READ, "Consultar usuarios", "Listar y ver usuarios locales y su estado."),
            new Permission(USERS_MANAGE, "Administrar usuarios", "Crear, editar, activar, suspender y sincronizar usuarios."),
            new Permission(ROLES_READ, "Consultar roles", "Listar roles, permisos y asignaciones."),
            new Permission(ROLES_MANAGE, "Administrar roles", "Crear roles y modificar su conjunto de permisos."),
            new Permission(ASSIGNMENTS_MANAGE, "Administrar asignaciones", "Asignar y revocar roles con alcance por centros."),
            new Permission(MATRICES_READ, "Consultar matrices", "Listar matrices organizacionales y sus miembros."),
            new Permission(MATRICES_MANAGE, "Administrar matrices", "Crear matrices e incorporar o retirar miembros firmantes."),
            new Permission(AUDIT_READ, "Consultar auditoría", "Leer eventos de auditoría dentro del alcance autorizado."),
            new Permission(CENTERS_READ, "Consultar centros", "Listar centros de la organización."),
            new Permission(CENTERS_MANAGE, "Administrar centros", "Crear y modificar centros."),
            new Permission(ORGANIZATION_READ, "Consultar organización", "Ver ciudades, sedes, matrices receptoras y responsables."),
            new Permission(ORGANIZATION_MANAGE, "Administrar organización", "Crear y modificar la estructura organizacional y responsables."),
            new Permission(CATALOGS_READ, "Consultar catálogos", "Leer categorías, ubicaciones y catálogos del inventario."),
            new Permission(CATALOGS_MANAGE, "Administrar catálogos", "Modificar categorías, ubicaciones y catálogos."),
            new Permission(INVENTORY_READ, "Consultar inventario", "Ver activos, custodias y estado del inventario en el alcance."),
            new Permission(INVENTORY_WRITE, "Editar inventario", "Registrar y editar datos del inventario en el alcance."),
            new Permission(CUSTODY_MANAGE, "Gestionar custodias", "Asignar y cerrar custodias de activos."),
            new Permission(TRANSFERS_READ, "Consultar traslados", "Ver traslados e ítems dentro del alcance."),
            new Permission(TRANSFERS_CREATE, "Crear traslados", "Crear y editar borradores de traslado."),
            new Permission(TRANSFERS_SUBMIT, "Enviar traslados", "Enviar borradores a autorización con reserva de activos."),
            new Permission(TRANSFERS_AUTHORIZE, "Autorizar traslados", "Aprobar o rechazar traslados pendientes."),
            new Permission(TRANSFERS_DISPATCH, "Despachar traslados", "Despachar traslados autorizados."),
            new Permission(TRANSFERS_CANCEL, "Cancelar traslados", "Cancelar traslados antes del despacho."),
            new Permission(RECEIPTS_CREATE, "Crear recepciones", "Registrar borradores de recepción por ítem."),
            new Permission(RECEIPTS_CONFIRM, "Confirmar recepciones", "Confirmar recepciones de forma atómica."),
            new Permission(DISCREPANCIES_MANAGE, "Gestionar discrepancias", "Registrar y hacer seguimiento de discrepancias."),
            new Permission(RESOLUTIONS_MANAGE, "Gestionar resoluciones", "Proponer, aprobar y ejecutar resoluciones de discrepancias."),
            new Permission(RETIREMENTS_REQUEST, "Solicitar bajas", "Crear solicitudes de baja de activos."),
            new Permission(RETIREMENTS_APPROVE, "Aprobar bajas", "Aprobar o rechazar solicitudes de baja."),
            new Permission(ACTS_READ, "Consultar actas", "Ver actas, versiones y estado documental en el alcance."),
            new Permission(ACTS_MANAGE, "Gestionar actas", "Emitir, corregir y anular actas."),
            new Permission(SIGNATURES_SIGN, "Firmar", "Firmar únicamente los documentos asignados por matriz."),
            new Permission(FILES_UPLOAD, "Cargar archivos", "Iniciar cargas de evidencia en cuarentena."),
            new Permission(FILES_DOWNLOAD, "Descargar archivos", "Descargar evidencias y documentos limpios autorizados."));

    public static final Set<String> ALL_CODES = ALL.stream().map(Permission::code).collect(java.util.stream.Collectors.toUnmodifiableSet());

    /** Roles iniciales del módulo (prompt Fase 04 §5). Códigos estables. */
    public record InitialRole(String code, String name, String description, Set<String> permissions) {}

    public static final List<InitialRole> INITIAL_ROLES = List.of(
            new InitialRole("ADMINISTRADOR", "Administrador",
                    "Acceso total a la organización; sus acciones quedan auditadas.", ALL_CODES),
            new InitialRole("TECNICO_TI", "Técnico TI",
                    "Operación de inventario, traslados, recepciones y archivos en los centros asignados.",
                    Set.of(INVENTORY_READ, INVENTORY_WRITE, CUSTODY_MANAGE, ORGANIZATION_READ,
                            TRANSFERS_READ, TRANSFERS_CREATE, TRANSFERS_SUBMIT, TRANSFERS_DISPATCH, TRANSFERS_CANCEL,
                            RECEIPTS_CREATE, RECEIPTS_CONFIRM,
                            DISCREPANCIES_MANAGE, RESOLUTIONS_MANAGE,
                            RETIREMENTS_REQUEST, ACTS_READ, SIGNATURES_SIGN,
                            FILES_UPLOAD, FILES_DOWNLOAD)),
            new InitialRole("JEFATURA", "Jefatura",
                    "Consulta de inventario y documentos de sus centros y firma de sus documentos autorizados.",
                    Set.of(INVENTORY_READ, ORGANIZATION_READ, TRANSFERS_READ, ACTS_READ, FILES_DOWNLOAD, SIGNATURES_SIGN)),
            new InitialRole("COORDINADOR", "Coordinador",
                    "Supervisión de sus centros: consulta, envíos, discrepancias y firmas autorizadas.",
                    Set.of(INVENTORY_READ, ORGANIZATION_READ, TRANSFERS_READ, TRANSFERS_SUBMIT, ACTS_READ,
                            DISCREPANCIES_MANAGE, RESOLUTIONS_MANAGE, FILES_DOWNLOAD, SIGNATURES_SIGN)));

    private final JdbcTemplate jdbc;

    public PermissionCatalog(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Catálogo completo ordenado por código. */
    public List<Permission> catalog() {
        return jdbc.query("SELECT code, name, description FROM permission ORDER BY code",
                (rs, n) -> new Permission(rs.getString(1), rs.getString(2), rs.getString(3)));
    }

    /** Inserta los permisos conocidos sin tocar asignaciones existentes. */
    public void ensureCatalog() {
        for (var permission : ALL) {
            jdbc.update("""
                    INSERT INTO permission (code, name, description) VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description)
                    """, permission.code(), permission.name(), permission.description());
        }
    }

    public static Map<String, String> namesByCode() {
        return ALL.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(Permission::code, Permission::name));
    }
}
