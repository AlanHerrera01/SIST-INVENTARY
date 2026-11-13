import { NavLink } from "react-router-dom"
import { Hospital, LogOut, PanelLeftClose, PanelLeftOpen } from "lucide-react"
import { NAV_SECTIONS, canVisit, canVisitConfiguration } from "@/lib/nav"
import { workspaceFor } from "@/lib/permissions"
import { useWorkspace } from "@/lib/workspace"
import { useSession } from "@/lib/session-context"
import { useLogout } from "@/lib/use-logout"
import { cn } from "@/lib/utils"

/** Contenido de navegación lateral; la visibilidad depende de los permisos del perfil. */
export function SidebarNav({ onNavigate, collapsed = false, onToggle }: { onNavigate?: () => void; collapsed?: boolean; onToggle?: () => void }) {
  const { profile } = useSession()
  const { centerId } = useWorkspace()
  const { logout, busy, error } = useLogout()
  const workspace = workspaceFor(profile)
  const initials = profile?.displayName.slice(0, 2).toUpperCase()
  const items = NAV_SECTIONS.flatMap((section) => section.items)
  const visible = (item: (typeof items)[number]) =>
    item.to === "/configuracion" ? canVisitConfiguration(profile, centerId) : canVisit(profile, item, centerId)
  return (
    <div className="flex h-full flex-col bg-surface-container-lowest text-on-surface">
      <div className={cn("flex min-h-16 items-center gap-2 px-4", collapsed ? "flex-col py-3" : "pr-2")}>
        <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary-container text-on-primary shadow-card">
          <Hospital className="size-5" aria-hidden="true" />
        </span>
        {!collapsed && <div className="min-w-0 flex-1 leading-tight">
          <p className="text-base font-bold tracking-tight text-primary-container">VERIS</p>
          <p className="mt-0.5 text-[10px] font-semibold uppercase tracking-[0.14em] text-on-surface-variant">Gestión TI</p>
        </div>}
        {onToggle && (
          <button
            type="button"
            aria-label={collapsed ? "Expandir menú" : "Colapsar menú"}
            aria-expanded={!collapsed}
            onClick={onToggle}
            className="flex size-9 shrink-0 items-center justify-center rounded-lg text-on-surface-variant transition-colors hover:bg-surface-container-low hover:text-on-surface"
          >
            {collapsed ? <PanelLeftOpen className="size-4" /> : <PanelLeftClose className="size-4" />}
          </button>
        )}
      </div>

      <nav aria-label="Navegación principal" className="flex flex-1 flex-col overflow-y-auto px-3 pb-4 pt-2">
        <p className={cn("px-2 pb-2 text-[11px] font-semibold uppercase tracking-wider text-on-surface-variant", collapsed && "sr-only")}>
          Menú principal
        </p>
        <ul className="space-y-1">
          {items.map((item) => {
            if (!visible(item)) return null
            if (item.upcoming) {
              return (
                <li key={item.label}>
                  <div
                    aria-disabled="true"
                    title="Módulo aún no disponible"
                    className={cn(
                      "flex min-h-10 items-center gap-2.5 rounded-lg px-2.5 py-2 text-sm font-medium text-outline/80",
                      collapsed && "justify-center px-0",
                    )}
                  >
                    <item.icon className="size-[18px] shrink-0" aria-hidden="true" />
                    {!collapsed && (
                      <>
                        <span className="truncate">{item.label}</span>
                        <span className="ml-auto rounded-full bg-surface-container-high px-1.5 py-0.5 text-[9px] font-semibold uppercase tracking-wide">
                          Pronto
                        </span>
                      </>
                    )}
                  </div>
                </li>
              )
            }
            return (
              <li key={item.to}>
                <NavLink
                  to={item.to}
                  end={item.end}
                  onClick={onNavigate}
                  title={collapsed ? item.label : undefined}
                  aria-label={collapsed ? item.label : undefined}
                  className={({ isActive }) =>
                    cn(
                      "flex min-h-10 items-center gap-2.5 rounded-lg px-2.5 py-2 text-sm font-medium transition-all",
                      "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-secondary focus-visible:ring-offset-2",
                      collapsed && "justify-center px-0",
                      isActive
                        ? "bg-primary-container text-on-primary shadow-card"
                        : "text-on-surface-variant hover:bg-surface-container-low hover:text-on-surface",
                    )
                  }
                >
                  <item.icon className="size-[18px] shrink-0" aria-hidden="true" />
                  {!collapsed && <span className="truncate">{item.label}</span>}
                </NavLink>
              </li>
            )
          })}
        </ul>
      </nav>

      <div className="border-t border-line p-3">
        <div className={cn("flex items-center gap-2 rounded-xl bg-surface-container-low p-2", collapsed && "flex-col")}>
          <span
            aria-hidden="true"
            className="flex size-9 shrink-0 items-center justify-center rounded-full bg-secondary-fixed text-[11px] font-bold text-on-secondary-fixed"
          >
            {initials}
          </span>
          {!collapsed && <div className="min-w-0 flex-1">
            <p className="truncate text-xs font-semibold text-on-surface" title={profile?.displayName}>{profile?.displayName}</p>
            <p className="mt-1 w-fit max-w-full truncate rounded-full bg-secondary-fixed px-2 py-0.5 text-[10px] font-semibold text-on-secondary-fixed">
              {workspace.label}
            </p>
          </div>}
          <button
            type="button"
            onClick={() => void logout()}
            disabled={busy}
            title="Cerrar sesión"
            aria-label="Cerrar sesión"
            className="flex size-8 shrink-0 items-center justify-center rounded-lg text-on-surface-variant transition-colors hover:bg-surface-container-high hover:text-error disabled:opacity-50"
          >
            <LogOut className="size-4" aria-hidden="true" />
          </button>
        </div>
        {error && <p role="alert" className="mt-2 text-[11px] leading-4 text-error">{error}</p>}
      </div>
    </div>
  )
}
