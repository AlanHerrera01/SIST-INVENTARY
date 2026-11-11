import { NavLink } from "react-router-dom"
import { Boxes } from "lucide-react"
import { NAV_SECTIONS } from "@/lib/nav"
import { hasGlobal } from "@/lib/permissions"
import { useSession } from "@/lib/session-context"
import { cn } from "@/lib/utils"

/** Contenido de navegación lateral; la visibilidad depende de los permisos del perfil. */
export function SidebarNav({ onNavigate }: { onNavigate?: () => void }) {
  const { profile } = useSession()
  return (
    <div className="flex h-full flex-col">
      <div className="flex items-center gap-2 px-4 py-5">
        <Boxes className="size-6 text-primary" aria-hidden="true" />
        <div className="leading-tight">
          <p className="text-sm font-semibold">Inventario Tecnológico</p>
          <p className="text-xs text-muted-foreground">Multicentro</p>
        </div>
      </div>
      <nav aria-label="Navegación principal" className="flex-1 overflow-y-auto px-2 pb-4">
        {NAV_SECTIONS.map((section) => {
          const items = section.items.filter((item) => !item.permission || hasGlobal(profile, item.permission))
          if (items.length === 0) return null
          return (
            <div key={section.title} className="mt-4 first:mt-0">
              <p className="px-3 pb-1 text-xs font-medium uppercase tracking-wide text-muted-foreground">
                {section.title}
              </p>
              <ul className="space-y-0.5">
                {items.map((item) => (
                  <li key={item.to}>
                    <NavLink
                      to={item.to}
                      end={item.end}
                      onClick={onNavigate}
                      className={({ isActive }) =>
                        cn(
                          "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors",
                          "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
                          isActive ? "bg-accent text-foreground" : "text-muted-foreground hover:bg-accent/60 hover:text-foreground",
                        )
                      }
                    >
                      <item.icon className="size-4" aria-hidden="true" />
                      {item.label}
                    </NavLink>
                  </li>
                ))}
              </ul>
            </div>
          )
        })}
      </nav>
    </div>
  )
}
