import { Outlet } from "react-router-dom"
import { SidebarNav } from "@/components/layout/Sidebar"
import { Topbar } from "@/components/layout/Topbar"

/** Estructura administrativa: sidebar fijo en escritorio, hoja en móvil. */
export function AppLayout() {
  return (
    <div className="flex min-h-svh bg-background">
      <aside className="hidden w-64 shrink-0 border-r bg-card md:block">
        <SidebarNav />
      </aside>
      <div className="flex min-w-0 flex-1 flex-col">
        <Topbar />
        <main className="flex-1 px-4 py-6 sm:px-6 lg:px-8">
          <div className="mx-auto w-full max-w-6xl">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
