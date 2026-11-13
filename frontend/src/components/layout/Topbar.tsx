import { useState } from "react"
import { Link } from "react-router-dom"
import { Building2, ChevronDown, KeyRound, LogOut, Menu, User as UserIcon } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Sheet, SheetContent, SheetDescription, SheetTitle, SheetTrigger } from "@/components/ui/sheet"
import { SidebarNav } from "@/components/layout/Sidebar"
import { NavigationSearch } from "@/components/layout/NavigationSearch"
import { roleLabel } from "@/lib/permissions"
import { useSession } from "@/lib/session-context"
import { useWorkspace } from "@/lib/workspace"
import { useLogout } from "@/lib/use-logout"

export function Topbar() {
  const { profile } = useSession()
  const [menuOpen, setMenuOpen] = useState(false)
  const { logout, busy, error } = useLogout()
  const { centerId, centers, selectCenter } = useWorkspace()
  const roles = [...new Set(profile?.grants.map((g) => g.role) ?? [])]
  const initials = profile?.displayName?.slice(0, 2).toUpperCase()

  return (
    <>
      <header className="sticky top-0 z-40 flex min-h-16 flex-wrap items-center gap-3 border-b border-line/70 bg-surface-container-lowest/90 px-4 py-2 shadow-card backdrop-blur-xl lg:gap-5 lg:px-6">
        <Sheet open={menuOpen} onOpenChange={setMenuOpen}>
          <SheetTrigger asChild>
            <Button variant="ghost" size="icon" className="md:hidden" aria-label="Abrir menú de navegación">
              <Menu aria-hidden="true" />
            </Button>
          </SheetTrigger>
          <SheetContent side="left" className="bg-surface-container-lowest p-0">
            <SheetTitle className="sr-only">Navegación</SheetTitle>
            <SheetDescription className="sr-only">Menú principal de la aplicación</SheetDescription>
            <SidebarNav onNavigate={() => setMenuOpen(false)} />
          </SheetContent>
        </Sheet>

        <NavigationSearch />

        <div className="hidden flex-1 lg:block" />

        {centers.length > 0 && (
          <div className="relative order-last flex w-full items-center sm:order-none sm:w-auto">
            <Building2 className="pointer-events-none absolute left-3.5 size-4 text-secondary" aria-hidden="true" />
            <select
              aria-label="Centro de trabajo"
              className="h-10 w-full min-w-0 cursor-pointer appearance-none rounded-lg border border-line bg-surface-container-lowest pl-10 pr-9 text-xs font-semibold text-primary shadow-card focus:border-secondary focus:outline-none focus:ring-2 focus:ring-secondary/30 sm:w-auto sm:max-w-56"
              value={centerId}
              onChange={(event) => selectCenter(event.target.value)}
            >
              <option value="">Todas las Sedes</option>
              {centers.map((id) => <option key={id} value={id}>Centro {id}</option>)}
            </select>
            <ChevronDown className="pointer-events-none absolute right-3 size-4 text-outline" aria-hidden="true" />
          </div>
        )}

        <Link
          to="/mi-acceso"
          className="hidden h-10 items-center gap-2 rounded-lg bg-secondary px-4 text-xs font-semibold text-on-secondary shadow-card transition-colors hover:bg-on-secondary-fixed-variant lg:flex"
        >
          <KeyRound className="size-4" aria-hidden="true" /> Mi acceso
        </Link>

        <div className="mx-1 hidden h-7 w-px bg-surface-container-highest lg:block" aria-hidden="true" />

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" className="gap-2.5 px-1.5" aria-label="Menú de usuario">
              <span className="flex size-9 items-center justify-center rounded-full bg-secondary-fixed text-xs font-semibold text-on-secondary-fixed">
                {initials ?? <UserIcon className="size-4" aria-hidden="true" />}
              </span>
              <span className="hidden max-w-40 text-left sm:block">
                <span className="block truncate text-xs font-semibold text-on-surface">{profile?.displayName}</span>
                <span className="mt-0.5 block truncate text-[11px] font-normal text-on-surface-variant">
                  {roles.map(roleLabel).join(", ") || "Sin roles"}
                </span>
              </span>
              <ChevronDown className="hidden size-3.5 text-outline sm:block" aria-hidden="true" />
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
            <DropdownMenuItem disabled={busy} onSelect={() => void logout()}>
              <LogOut aria-hidden="true" />
              {busy ? "Cerrando sesión…" : "Cerrar sesión"}
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </header>
      {error && <p role="alert" className="border-b border-line bg-error-container p-3 text-sm text-on-error-container">{error}</p>}
    </>
  )
}
