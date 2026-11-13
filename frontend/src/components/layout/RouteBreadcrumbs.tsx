import { Home } from "lucide-react"
import { Link, useLocation } from "react-router-dom"
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb"
import { navItemFor } from "@/lib/nav"

/** Migas de pan derivadas de la ruta actual y la configuración de navegación. */
export function RouteBreadcrumbs() {
  const { pathname } = useLocation()
  const item = navItemFor(pathname)
  if (!item) return null
  const isDetail = pathname !== item.to
  return (
    <Breadcrumb>
      <BreadcrumbList className="text-xs text-on-surface-variant">
        {pathname !== "/" && (
          <>
            <BreadcrumbItem>
              <BreadcrumbLink asChild>
                <Link to="/" className="inline-flex items-center gap-1.5">
                  <Home className="size-3.5" aria-hidden="true" />
                  Inicio
                </Link>
              </BreadcrumbLink>
            </BreadcrumbItem>
            <BreadcrumbSeparator />
          </>
        )}
        <BreadcrumbItem>
          {isDetail ? (
            <BreadcrumbLink asChild>
              <Link to={item.to}>{item.label}</Link>
            </BreadcrumbLink>
          ) : (
            <BreadcrumbPage className="text-primary">{item.label}</BreadcrumbPage>
          )}
        </BreadcrumbItem>
        {isDetail && (
          <>
            <BreadcrumbSeparator />
            <BreadcrumbItem>
              <BreadcrumbPage className="text-primary">Detalle</BreadcrumbPage>
            </BreadcrumbItem>
          </>
        )}
      </BreadcrumbList>
    </Breadcrumb>
  )
}
