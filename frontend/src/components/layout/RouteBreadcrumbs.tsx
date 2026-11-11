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
      <BreadcrumbList>
        <BreadcrumbItem>
          {isDetail ? (
            <BreadcrumbLink asChild>
              <Link to={item.to}>{item.label}</Link>
            </BreadcrumbLink>
          ) : (
            <BreadcrumbPage>{item.label}</BreadcrumbPage>
          )}
        </BreadcrumbItem>
        {isDetail && (
          <>
            <BreadcrumbSeparator />
            <BreadcrumbItem>
              <BreadcrumbPage>Detalle</BreadcrumbPage>
            </BreadcrumbItem>
          </>
        )}
      </BreadcrumbList>
    </Breadcrumb>
  )
}
