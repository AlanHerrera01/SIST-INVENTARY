import { useEffect, useRef, useState } from "react"
import { Outlet, useLocation } from "react-router-dom"
import { workspaceFor } from "@/lib/permissions"
import { useSession } from "@/lib/session-context"
import { navItemFor } from "@/lib/nav"
import { SidebarNav } from "@/components/layout/Sidebar"
import { Topbar } from "@/components/layout/Topbar"
import { RouteBreadcrumbs } from "@/components/layout/RouteBreadcrumbs"

/** Estructura administrativa: sidebar fijo en escritorio, hoja en móvil. */
export function AppLayout() {
  const { profile } = useSession()
  const { pathname } = useLocation()
  const main = useRef<HTMLElement>(null)
  const workspace = workspaceFor(profile)
  const [collapsed, setCollapsed] = useState(false)
  useEffect(() => {
    document.title = `${navItemFor(pathname)?.label ?? "Página no encontrada"} | Inventario Multicentro`
    main.current?.focus({ preventScroll: true })
  }, [pathname])
  return (
    <div className="flex min-h-svh bg-background" data-workspace={workspace.kind}>
      <a href="#contenido" className="sr-only fixed left-4 top-2 z-[100] rounded-lg bg-surface-container-lowest p-3 text-sm font-semibold text-primary shadow-pop focus:not-sr-only">Saltar al contenido</a>
      <aside className={`sticky top-0 hidden h-svh shrink-0 border-r border-line bg-surface-container-lowest shadow-card md:block ${collapsed ? "w-[72px]" : "w-[260px]"}`}>
        <SidebarNav collapsed={collapsed} onToggle={() => setCollapsed(!collapsed)} />
      </aside>
      <div className="flex min-w-0 flex-1 flex-col">
        <Topbar />
        <main id="contenido" ref={main} tabIndex={-1} className="flex-1 px-4 pb-10 pt-4 outline-none sm:px-6 lg:px-8 lg:pt-5">
          <div className="mx-auto mb-4 max-w-[1440px]"><RouteBreadcrumbs /></div>
          <div className="mx-auto w-full max-w-[1440px]">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
