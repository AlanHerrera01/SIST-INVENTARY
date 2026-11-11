import { useState } from "react"
import { LogOut, Menu, User as UserIcon } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Sheet, SheetContent, SheetDescription, SheetTitle } from "@/components/ui/sheet"
import { SidebarNav } from "@/components/layout/Sidebar"
import { RouteBreadcrumbs } from "@/components/layout/RouteBreadcrumbs"
import { authApi } from "@/lib/api"
import { roleLabel } from "@/lib/permissions"
import { useSession } from "@/lib/session-context"

async function logout() {
  try {
    const { logoutUrl } = await authApi<{ logoutUrl: string }>("/api/auth/logout", { method: "POST" })
    window.location.assign(logoutUrl)
  } catch {
    window.location.assign("/api/auth/logout")
  }
}

function LogoutItem() {
  return (
    <DropdownMenuItem onSelect={() => void logout()}>
      <LogOut aria-hidden="true" /> Cerrar sesión
    </DropdownMenuItem>
  )
}

export function Topbar() {
  const { profile } = useSession()
  const [menuOpen, setMenuOpen] = useState(false)
  const roles = [...new Set(profile?.grants.map((g) => g.role) ?? [])]

  return (
    <header className="sticky top-0 z-40 flex h-14 items-center gap-3 border-b bg-card/95 px-4 backdrop-blur">
      <Sheet open={menuOpen} onOpenChange={setMenuOpen}>
        <Button variant="ghost" size="icon" className="md:hidden" onClick={() => setMenuOpen(true)} aria-label="Abrir menú de navegación">
          <Menu aria-hidden="true" />
        </Button>
        <SheetContent side="left" className="p-0">
          <SheetTitle className="sr-only">Navegación</SheetTitle>
          <SheetDescription className="sr-only">Menú principal de la aplicación</SheetDescription>
          <SidebarNav onNavigate={() => setMenuOpen(false)} />
        </SheetContent>
      </Sheet>

      <div className="min-w-0 flex-1"><RouteBreadcrumbs /></div>

      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" className="gap-2" aria-label="Menú de usuario">
            <span className="flex size-8 items-center justify-center rounded-full bg-accent text-xs font-semibold">
              {profile?.displayName?.slice(0, 2).toUpperCase() ?? <UserIcon className="size-4" aria-hidden="true" />}
            </span>
            <span className="hidden max-w-40 truncate text-sm sm:inline">{profile?.displayName}</span>
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-56">
          <DropdownMenuLabel>
            <p className="truncate text-sm font-medium">{profile?.displayName}</p>
            <p className="truncate text-xs font-normal text-muted-foreground">
              {roles.map(roleLabel).join(", ") || "Sin roles"}
            </p>
          </DropdownMenuLabel>
          <DropdownMenuSeparator />
          <LogoutItem />
        </DropdownMenuContent>
      </DropdownMenu>
    </header>
  )
}
