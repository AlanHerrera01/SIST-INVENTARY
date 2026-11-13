import {
  Boxes,
  Building2,
  FileSignature,
  KeyRound,
  LayoutDashboard,
  ScrollText,
  Settings,
  ShieldCheck,
  Truck,
  Users,
  Wrench,
  type LucideIcon,
} from "lucide-react"
import type { Profile } from "@/lib/session-context"
import { canAccess, hasGlobal } from "@/lib/permissions"

export interface NavItem {
  to: string
  label: string
  icon: LucideIcon
  /** Permiso requerido para ver el elemento (ninguno = todos los autenticados). */
  permission?: string
  scope?: "global" | "center"
  /** Coincidencia exacta de ruta (para rutas detalle). */
  end?: boolean
  /** Entrada del diseño cuyo módulo aún no existe: se muestra deshabilitada. */
  upcoming?: boolean
}

export interface NavSection {
  title: string
  items: NavItem[]
}

/** Menú lateral con el orden y las etiquetas del diseño. */
export const NAV_SECTIONS: NavSection[] = [
  {
    title: "Menú principal",
    items: [
      { to: "/", label: "Dashboard General", icon: LayoutDashboard, end: true },
      { to: "", label: "Inventario Nacional", icon: Boxes, upcoming: true },
      { to: "", label: "Traslados y Guías", icon: Truck, upcoming: true },
      { to: "/mis-matrices", label: "Actas y Firmas", icon: FileSignature },
      { to: "", label: "Equipos en Mantenimiento", icon: Wrench, upcoming: true },
      { to: "/access/audit", label: "Auditoría", icon: ScrollText, permission: "access.audit.read", scope: "center" },
      { to: "/configuracion", label: "Configuración", icon: Settings },
    ],
  },
]

/**
 * Rutas reales de la aplicación. Se usan para migas, búsqueda y guardas de
 * acceso aunque no todas aparezcan directamente en el menú lateral: la
 * administración vive en las pestañas de Configuración.
 */
export const NAV_LINKS: NavItem[] = [
  { to: "/", label: "Dashboard General", icon: LayoutDashboard, end: true },
  { to: "/mis-matrices", label: "Actas y Firmas", icon: FileSignature },
  { to: "/access/audit", label: "Auditoría", icon: ScrollText, permission: "access.audit.read", scope: "center" },
  { to: "/configuracion", label: "Configuración", icon: Settings },
  { to: "/mi-acceso", label: "Mi acceso", icon: KeyRound },
  { to: "/access/users", label: "Usuarios", icon: Users, permission: "access.users.read" },
  { to: "/access/roles", label: "Roles y permisos", icon: ShieldCheck, permission: "access.roles.read" },
  { to: "/access/matrices", label: "Matrices de firma", icon: FileSignature, permission: "access.matrices.read", scope: "center" },
  { to: "/organization", label: "Ciudades, sedes y centros", icon: Building2, permission: "organization.read", scope: "center" },
]

/** Rutas que forman las pestañas de Configuración. */
export const CONFIG_PATHS = ["/access/users", "/access/roles", "/access/matrices", "/organization"]

export function canVisit(profile: Profile | null, item: NavItem, centerId = ""): boolean {
  if (!profile) return false
  if (!item.permission) return true
  if (item.scope !== "center") return hasGlobal(profile, item.permission)
  if (centerId) return canAccess(profile, item.permission, centerId)
  return hasGlobal(profile, item.permission) || profile.grants.some(
    (g) => g.permission === item.permission && g.scope === "CENTROS" && !!g.centerId,
  )
}

/** Configuración se muestra si el perfil puede abrir al menos una de sus pestañas de administración. */
export function canVisitConfiguration(profile: Profile | null, centerId = ""): boolean {
  return NAV_LINKS.some((item) => CONFIG_PATHS.includes(item.to) && canVisit(profile, item, centerId))
}

export function navItemFor(pathname: string): NavItem | undefined {
  return NAV_LINKS.find((item) =>
    !!item.to && (item.end ? pathname === item.to : pathname === item.to || pathname.startsWith(item.to + "/")),
  )
}
