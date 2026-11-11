import {
  LayoutDashboard,
  Users,
  ShieldCheck,
  FileSignature,
  ScrollText,
  Building2,
  type LucideIcon,
} from "lucide-react"

export interface NavItem {
  to: string
  label: string
  icon: LucideIcon
  /** Permiso requerido para ver el elemento (ninguno = todos los autenticados). */
  permission?: string
  /** Coincidencia exacta de ruta (para rutas detalle). */
  end?: boolean
}

export interface NavSection {
  title: string
  items: NavItem[]
}

export const NAV_SECTIONS: NavSection[] = [
  {
    title: "General",
    items: [{ to: "/", label: "Panel", icon: LayoutDashboard, end: true }],
  },
  {
    title: "Operación",
    items: [
      { to: "/mis-matrices", label: "Mis matrices", icon: FileSignature },
      { to: "/organization", label: "Organización", icon: Building2, permission: "organization.read" },
    ],
  },
  {
    title: "Administración",
    items: [
      { to: "/access/users", label: "Usuarios", icon: Users, permission: "access.users.read" },
      { to: "/access/roles", label: "Roles y permisos", icon: ShieldCheck, permission: "access.roles.read" },
      { to: "/access/matrices", label: "Matrices de firma", icon: FileSignature, permission: "access.matrices.read" },
      { to: "/access/audit", label: "Auditoría", icon: ScrollText, permission: "access.audit.read" },
    ],
  },
]

const ALL_ITEMS = NAV_SECTIONS.flatMap((s) => s.items)

export function navItemFor(pathname: string): NavItem | undefined {
  return ALL_ITEMS.find((item) =>
    item.end ? pathname === item.to : pathname === item.to || pathname.startsWith(item.to + "/"),
  )
}
