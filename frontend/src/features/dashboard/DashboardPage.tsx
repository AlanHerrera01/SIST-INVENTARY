import { useState } from "react"
import { Link } from "react-router-dom"
import { useQuery } from "@tanstack/react-query"
import { Activity, ArrowRight, Boxes } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { NAV_SECTIONS } from "@/lib/nav"
import { hasGlobal, roleLabel } from "@/lib/permissions"
import { useSession } from "@/lib/session-context"
import { accessApi } from "@/features/access/api"

type Health = "unknown" | "checking" | "up" | "down"

/** Panel principal: sesión, permisos efectivos y acceso rápido a módulos. */
export function DashboardPage() {
  const { profile, isLoading } = useSession()
  const [health, setHealth] = useState<Health>("unknown")

  const myMatrices = useQuery({ queryKey: ["me", "matrices"], queryFn: accessApi.matrices.mine, retry: 1 })

  async function checkHealth() {
    setHealth("checking")
    try {
      const response = await fetch("/actuator/health/readiness", { signal: AbortSignal.timeout(10000) })
      const result: { status?: string } = await response.json()
      setHealth(response.ok && result.status === "UP" ? "up" : "down")
    } catch {
      setHealth("down")
    }
  }

  const roles = [...new Set(profile?.grants.map((g) => g.role) ?? [])]
  const permissions = [...new Set(profile?.grants.map((g) => g.permission) ?? [])]
  const centerGrants = profile?.grants.filter((g) => g.scope === "CENTROS") ?? []
  const quickLinks = NAV_SECTIONS.flatMap((s) => s.items).filter(
    (item) => !item.permission || hasGlobal(profile, item.permission),
  )

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">
          {isLoading ? "Cargando sesión…" : `Hola, ${profile?.displayName ?? "usuario"}`}
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Resumen de su sesión y accesos autorizados en la organización.
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Rol asignado</CardTitle>
            <CardDescription>Roles vigentes en su perfil local</CardDescription>
          </CardHeader>
          <CardContent className="flex flex-wrap gap-2">
            {roles.length === 0 && <span className="text-sm text-muted-foreground">Sin roles activos</span>}
            {roles.map((r) => (
              <Badge key={r} variant="secondary">{roleLabel(r)}</Badge>
            ))}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Permisos</CardTitle>
            <CardDescription>{permissions.length} permisos en su alcance</CardDescription>
          </CardHeader>
          <CardContent className="text-sm text-muted-foreground">
            {centerGrants.length > 0
              ? `Incluye ${centerGrants.length} asignaciones limitadas a centros específicos.`
              : "Todas sus asignaciones son de alcance global."}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Matrices de firma</CardTitle>
            <CardDescription>Donde puede firmar o aprobar</CardDescription>
          </CardHeader>
          <CardContent>
            {myMatrices.isLoading ? (
              <Skeleton className="h-5 w-24" />
            ) : (
              <p className="text-2xl font-semibold">{myMatrices.data?.length ?? 0}</p>
            )}
            <Link to="/mis-matrices" className="mt-1 inline-flex items-center gap-1 text-sm text-primary hover:underline">
              Ver matrices <ArrowRight className="size-3.5" aria-hidden="true" />
            </Link>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Boxes className="size-4" aria-hidden="true" /> Accesos rápidos
          </CardTitle>
          <CardDescription>Módulos disponibles según sus permisos</CardDescription>
        </CardHeader>
        <CardContent>
          <ul className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
            {quickLinks.map((item) => (
              <li key={item.to}>
                <Link
                  to={item.to}
                  className="flex items-center gap-3 rounded-lg border p-3 text-sm font-medium transition-colors hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                  <item.icon className="size-4 text-primary" aria-hidden="true" />
                  {item.label}
                </Link>
              </li>
            ))}
          </ul>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Activity className="size-4" aria-hidden="true" /> Estado de servicios
          </CardTitle>
        </CardHeader>
        <CardContent className="flex flex-wrap items-center gap-4">
          <Button type="button" variant="outline" onClick={checkHealth} disabled={health === "checking"}>
            {health === "checking" ? "Comprobando…" : "Comprobar servicios"}
          </Button>
          <p role="status" aria-live="polite" className="text-sm text-muted-foreground">
            {health === "unknown" && "Sin comprobar"}
            {health === "up" && "Backend disponible"}
            {health === "down" && "Backend no disponible"}
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
