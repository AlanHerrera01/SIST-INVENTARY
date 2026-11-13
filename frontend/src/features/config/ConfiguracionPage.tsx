import { useSearchParams } from "react-router-dom"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { AccessDenied } from "@/components/PageState"
import { UsersPage } from "@/features/access/UsersPage"
import { RolesPage } from "@/features/access/RolesPage"
import { MatricesPage } from "@/features/access/MatricesPage"
import { MyAccessPage } from "@/features/access/MyAccessPage"
import { OrganizationPage } from "@/features/organization/OrganizationPage"
import { NAV_LINKS, canVisit } from "@/lib/nav"
import { useSession } from "@/lib/session-context"
import { useWorkspace } from "@/lib/workspace"

/**
 * Configuración agrupa la administración en pestañas: el menú lateral solo
 * muestra una entrada y cada pestaña reutiliza la página real de su módulo.
 * La pestaña activa vive en `?tab=` para poder enlazarla y recargarla.
 */
const SECTIONS = [
  { value: "usuarios", label: "Usuarios", to: "/access/users", element: <UsersPage /> },
  { value: "roles", label: "Roles y permisos", to: "/access/roles", element: <RolesPage /> },
  { value: "matrices", label: "Matrices de firma", to: "/access/matrices", element: <MatricesPage /> },
  { value: "centros", label: "Ciudades, sedes y centros", to: "/organization", element: <OrganizationPage /> },
  { value: "mi-acceso", label: "Mi acceso", to: "/mi-acceso", element: <MyAccessPage /> },
]

export function ConfiguracionPage() {
  const { profile } = useSession()
  const { centerId } = useWorkspace()
  const [params, setParams] = useSearchParams()

  const sections = SECTIONS.filter((section) => {
    const item = NAV_LINKS.find((entry) => entry.to === section.to)
    return !!item && canVisit(profile, item, centerId)
  })
  if (sections.length === 0) return <AccessDenied />

  const requested = params.get("tab")
  const current = sections.some((section) => section.value === requested) ? requested : sections[0].value

  return (
    <div>
      <h1 className="sr-only">Configuración</h1>
      <Tabs value={current ?? sections[0].value} onValueChange={(value) => setParams({ tab: value }, { replace: true })}>
        <TabsList aria-label="Secciones de configuración" className="mb-6 h-auto w-full flex-wrap justify-start gap-1">
          {sections.map((section) => (
            <TabsTrigger key={section.value} value={section.value}>{section.label}</TabsTrigger>
          ))}
        </TabsList>
        {sections.map((section) => (
          <TabsContent key={section.value} value={section.value}>{section.element}</TabsContent>
        ))}
      </Tabs>
    </div>
  )
}
