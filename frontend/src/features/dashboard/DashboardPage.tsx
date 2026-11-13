import { useState } from "react"
import { Link } from "react-router-dom"
import { useQuery } from "@tanstack/react-query"
import {
  Activity,
  ArrowRight,
  ArrowRightLeft,
  Building2,
  CalendarDays,
  Camera,
  ChevronDown,
  ClipboardCheck,
  FileSignature,
  Gauge,
  LockKeyhole,
  Monitor,
  Network,
  ShieldCheck,
  ShieldPlus,
  TriangleAlert,
  Truck,
  Upload,
  type LucideIcon,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { ErrorState, LoadingState } from "@/components/PageState"
import { workspaceFor } from "@/lib/permissions"
import { useWorkspace } from "@/lib/workspace"
import { useSession } from "@/lib/session-context"
import { accessApi } from "@/features/access/api"

function Metric({ title, icon: Icon, tone, detail }: { title: string; icon: LucideIcon; tone: string; detail: string }) {
  return (
    <section className="group relative flex flex-col justify-between overflow-hidden rounded-xl border border-line bg-surface-container-lowest p-5 shadow-card">
      <span className={`pointer-events-none absolute -right-4 -top-4 size-24 rounded-full opacity-60 transition-transform group-hover:scale-110 ${tone}`} aria-hidden="true" />
      <div className="relative">
        <div className="mb-3 flex items-center justify-between gap-3">
          <h2 className="metric-label">{title}</h2>
          <span className={`flex size-9 shrink-0 items-center justify-center rounded-lg ${tone}`}>
            <Icon className="size-5" aria-hidden="true" />
          </span>
        </div>
        <div className="mb-1 flex items-baseline gap-2">
          <span className="text-[32px] font-bold leading-10 tracking-tight text-primary" aria-label="Dato no disponible">—</span>
          <span className="rounded-full bg-surface-container-high px-2 py-0.5 text-[11px] font-semibold text-on-surface-variant">Sin datos</span>
        </div>
      </div>
      <div className="relative flex flex-col gap-1 border-t border-line pt-3">
        <p className="text-xs font-medium text-on-surface">Datos no disponibles</p>
        <p className="text-xs leading-5 text-on-surface-variant">{detail}</p>
      </div>
    </section>
  )
}

function SectionTitle({ title, description, icon: Icon, tone = "bg-surface-container-high text-primary-container" }: {
  title: string; description: string; icon: LucideIcon; tone?: string
}) {
  return (
    <div className="flex items-center gap-3">
      <span className={`flex size-9 shrink-0 items-center justify-center rounded-lg ${tone}`}>
        <Icon className="size-5" aria-hidden="true" />
      </span>
      <div className="min-w-0">
        <h2 className="text-lg font-bold leading-7 tracking-tight text-primary">{title}</h2>
        <p className="mt-0.5 text-xs leading-4 text-on-surface-variant">{description}</p>
      </div>
    </div>
  )
}

export function DashboardPage() {
  const { profile } = useSession()
  const { centerId, centers } = useWorkspace()
  const workspace = workspaceFor(profile)
  const global = profile?.grants.some((grant) => grant.scope === "GLOBAL")
  const [health, setHealth] = useState("Sin comprobar")
  const matrices = useQuery({ queryKey: ["me", "matrices"], queryFn: accessApi.matrices.mine, retry: 1 })
  const visibleCenters = centerId ? [centerId] : centers
  async function checkHealth() {
    setHealth("Comprobando…")
    try {
      const response = await fetch("/actuator/health/readiness", { signal: AbortSignal.timeout(10000) })
      const result = await response.json() as { status?: string }
      setHealth(response.ok && result.status === "UP" ? "Backend disponible" : "Backend no disponible")
    } catch { setHealth("Backend no disponible") }
  }
  return <div className="space-y-6">
    {/* Encabezado ejecutivo y acciones rápidas */}
    <div className="flex flex-col justify-between gap-4 xl:flex-row xl:items-center">
      <div className="flex flex-col gap-1">
        <div className="flex flex-wrap items-center gap-2">
          <span className="inline-flex items-center gap-1.5 rounded-full bg-surface-container-high px-2.5 py-0.5 text-xs font-semibold text-primary-container">
            <span className="size-2 rounded-full bg-secondary" aria-hidden="true" />
            Supervisión TI
          </span>
          <span className="text-xs text-outline" aria-hidden="true">•</span>
          <span className="text-xs text-on-surface-variant">{workspace.label} · Datos operativos pendientes</span>
        </div>
        <h1 className="text-2xl font-bold leading-tight tracking-tight text-primary xl:text-[32px] xl:leading-10">
          {workspace.kind === "admin" ? "Dashboard General de Activos TI" : "Mi Dashboard de Activos TI"}
        </h1>
        <p className="max-w-3xl text-sm leading-5 text-on-surface-variant">
          Supervisión del parque tecnológico y biomédico en matrices, bodegas y centrales médicas dentro de su alcance autorizado.
        </p>
      </div>

      <div className="flex flex-wrap items-center gap-2 self-start xl:self-auto">
        <button
          disabled
          className="inline-flex h-10 items-center gap-2 rounded-lg border border-line bg-surface-container-lowest px-3.5 text-xs font-medium text-on-surface shadow-card disabled:cursor-not-allowed disabled:opacity-60"
          title="Disponible cuando se habiliten los indicadores"
        >
          <CalendarDays className="size-4 text-outline" aria-hidden="true" /> Últimos 30 días <ChevronDown className="size-4 text-outline" aria-hidden="true" />
        </button>
        <Link
          to="/mi-acceso"
          className="inline-flex h-10 items-center gap-2 rounded-lg border border-line bg-surface-container-lowest px-3.5 text-xs font-medium text-primary shadow-card transition-colors hover:bg-surface-container-low"
        >
          <Building2 className="size-4 text-secondary" aria-hidden="true" />
          <span className="font-semibold">
            {centerId ? `Centro ${centerId}` : global ? "Organización · Todas las sedes" : "Mis centros autorizados"}
          </span>
          <ChevronDown className="size-4 text-outline" aria-hidden="true" />
        </Link>
        <button
          disabled
          className="inline-flex h-10 items-center gap-2 rounded-lg border border-line bg-surface-container-lowest px-4 text-xs font-semibold text-on-surface-variant shadow-card disabled:cursor-not-allowed disabled:opacity-60"
          title="Exportación todavía no disponible"
        >
          <Upload className="size-4" aria-hidden="true" /> Exportar Reporte
        </button>
        <button
          disabled
          className="inline-flex h-10 items-center gap-2 rounded-lg bg-secondary px-4 text-xs font-semibold text-on-secondary shadow-pop disabled:cursor-not-allowed disabled:opacity-60"
          title="Traslados e ingresos todavía no disponibles"
        >
          <ArrowRightLeft className="size-4" aria-hidden="true" /> + Nuevo Traslado / Ingreso
        </button>
      </div>
    </div>

    {/* Resumen estadístico */}
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <Metric
        title={global ? "Total Activos TI Nacional" : "Total Activos TI en mi alcance"}
        icon={Monitor}
        tone="bg-surface-container-low text-primary-container"
        detail="El inventario aún no dispone de indicadores."
      />
      <Metric
        title="Operatividad & Salud"
        icon={ShieldPlus}
        tone="bg-sky-50 text-secondary"
        detail="Disponibilidad operativa pendiente de integración."
      />
      <Metric
        title="Traslados en Tránsito"
        icon={Truck}
        tone="bg-secondary-fixed text-on-secondary-fixed"
        detail="El seguimiento logístico aún no está habilitado."
      />
      <Metric
        title="Incidencias & Bajas"
        icon={TriangleAlert}
        tone="bg-error-container text-on-error-container"
        detail="Sin conexión al registro de incidencias."
      />
    </div>

    {/* Área central asimétrica 8 / 4 */}
    <div className="grid items-start gap-6 lg:grid-cols-12">
      <div className="flex min-w-0 flex-col gap-6 lg:col-span-8">
        {/* Traslados logísticos */}
        <section className="surface-card overflow-hidden">
          <div className="flex flex-col justify-between gap-3 p-6 pb-4 sm:flex-row sm:items-center">
            <SectionTitle
              title="Traslados Logísticos en Curso"
              description="Control de despachos entre bodegas y verificación física en destino"
              icon={Truck}
            />
            <div className="inline-flex self-start rounded-lg bg-surface-container-low p-1 text-xs sm:self-center" role="group" aria-label="Filtrar traslados">
              {["Todos", "Parciales", "En Ruta"].map((label) => (
                <button
                  key={label}
                  type="button"
                  disabled
                  title="Seguimiento aún no disponible"
                  className="rounded-md px-3 py-1 text-xs font-medium text-on-surface-variant first:bg-surface-container-lowest first:font-semibold first:text-primary first:shadow-card disabled:cursor-not-allowed"
                >
                  {label}
                </button>
              ))}
            </div>
          </div>
          <div className="overflow-x-auto" role="region" aria-label="Traslados logísticos" tabIndex={0}>
            <table className="w-full min-w-[680px] text-left text-xs">
              <thead className="bg-surface-container-low text-[11px] font-semibold uppercase tracking-wider text-on-surface-variant">
                <tr>
                  {["Guía / Traslado", "Ruta (Origen → Destino)", "Lote / Equipamiento", "Verificación Física", "Custodio TI", "Acción"].map((title) => (
                    <th key={title} scope="col" className={`px-4 py-3 first:pl-6 ${title === "Acción" ? "pr-6 text-right" : ""}`}>{title}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td colSpan={6} className="px-6 py-16 text-center">
                    <span className="mx-auto mb-3 flex size-11 items-center justify-center rounded-xl bg-surface-container-low text-primary-container">
                      <Truck className="size-5" aria-hidden="true" />
                    </span>
                    <p className="text-sm font-semibold text-primary">Seguimiento de traslados no disponible</p>
                    <p className="mx-auto mt-2 max-w-md text-xs leading-5 text-on-surface-variant">
                      Los movimientos aparecerán aquí cuando se habilite el módulo de traslados y guías.
                    </p>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div className="flex flex-wrap items-center justify-between gap-2 bg-surface-container-low/40 px-6 py-3 text-xs text-on-surface-variant">
            <span>Sin datos de seguimiento conectados</span>
            <span aria-disabled="true" className="inline-flex items-center gap-1 font-semibold text-secondary">
              Ver Centro Logístico Completo <ArrowRight className="size-4" aria-hidden="true" />
            </span>
          </div>
        </section>

        {/* Alertas y auditoría */}
        <section className="surface-card p-6">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <SectionTitle
              title="Activos Críticos con Alertas & Auditoría"
              description="Equipos con observaciones físicas, discrepancias o procesos de baja en curso"
              icon={TriangleAlert}
              tone="bg-error-container text-on-error-container"
            />
            <button
              type="button"
              disabled
              title="Módulo de activos críticos aún no disponible"
              className="inline-flex items-center gap-1 text-xs font-semibold text-secondary disabled:cursor-not-allowed disabled:opacity-60"
            >
              Ver casos <ChevronDown className="size-4" aria-hidden="true" />
            </button>
          </div>

          <div className="mt-4 grid gap-4 md:grid-cols-2">
            {[
              {
                title: "Observaciones físicas",
                icon: Camera,
                description: "Los reportes de daño físico, peritajes fotográficos y solicitudes de baja aparecerán en este panel.",
              },
              {
                title: "Auditoría de activos",
                icon: ShieldCheck,
                description: "Las discrepancias de inventario, reasignaciones y conciliaciones pendientes aparecerán en este panel.",
              },
            ].map((panel) => (
              <article key={panel.title} className="flex min-h-56 flex-col justify-between rounded-xl border border-line bg-surface-container-low p-4 transition-colors hover:bg-surface-container">
                <div className="flex flex-col gap-2">
                  <div className="flex items-start justify-between gap-2">
                    <span className="inline-flex items-center gap-1 rounded-full bg-surface-container-highest px-2 py-0.5 text-[11px] font-semibold text-on-surface-variant">
                      <panel.icon className="size-3.5" aria-hidden="true" /> Sin datos
                    </span>
                    <span className="rounded-full bg-surface-container-lowest px-2 py-0.5 text-[11px] font-semibold text-on-surface-variant">Pendiente</span>
                  </div>
                  <h3 className="text-sm font-bold text-primary">{panel.title}</h3>
                  <p className="flex items-center gap-1 text-xs text-on-surface-variant">
                    <Building2 className="size-3.5" aria-hidden="true" /> Alcance: {centerId ? `Centro ${centerId}` : workspace.label}
                  </p>
                  <div className="rounded-lg bg-surface-container-lowest p-2.5">
                    <p className="text-xs leading-5 text-on-surface-variant">{panel.description}</p>
                  </div>
                </div>
                <div className="mt-3 flex items-center justify-between gap-2 border-t border-line pt-3">
                  <span className="text-[11px] font-medium text-on-surface-variant">Sin responsable asignado</span>
                  <button
                    type="button"
                    disabled
                    className="inline-flex h-7 items-center gap-1 rounded-md bg-surface-container-lowest px-3 text-xs font-semibold text-primary shadow-card disabled:cursor-not-allowed disabled:opacity-60"
                    title="Disponible cuando se habilite el módulo"
                  >
                    No disponible
                  </button>
                </div>
              </article>
            ))}
          </div>
        </section>
      </div>

      <div className="flex min-w-0 flex-col gap-6 lg:col-span-4">
        {/* Capacidad por jurisdicción */}
        <section className="surface-card p-6">
          <SectionTitle
            title="Capacidad por Jurisdicción"
            description="Inventario distribuido dentro de su alcance"
            icon={Network}
          />
          <div className="mt-5 flex flex-col gap-3.5">
            {visibleCenters.map((id, index) => (
              <div key={id} className="rounded-xl border border-line bg-surface-container-low p-3.5 transition-colors hover:bg-surface-container">
                <div className="mb-1.5 flex items-center justify-between gap-2">
                  <div className="flex min-w-0 items-center gap-2">
                    <span className={`size-2.5 shrink-0 rounded-full ${["bg-primary", "bg-secondary", "bg-primary-container", "bg-outline"][index % 4]}`} aria-hidden="true" />
                    <span className="truncate text-sm font-semibold text-primary">Centro autorizado</span>
                  </div>
                  <span className="shrink-0 text-[11px] font-semibold text-on-surface-variant">Sin total</span>
                </div>
                <p className="mb-2 break-all text-xs text-on-surface-variant">{id}</p>
                <div className="mb-2 h-1.5 w-full overflow-hidden rounded-full bg-surface-container-highest">
                  <div className="h-full w-0 rounded-full bg-primary" />
                </div>
                <p className="flex items-center gap-1.5 text-[11px] text-on-surface-variant">
                  <Gauge className="size-3.5 text-secondary" aria-hidden="true" /> Capacidad y operatividad no disponibles
                </p>
              </div>
            ))}
            {!visibleCenters.length && (
              <div className="rounded-xl border border-line bg-surface-container-low p-4">
                <div className="flex items-center justify-between gap-2">
                  <h3 className="text-sm font-semibold text-primary">{global ? "Todas las sedes autorizadas" : "Sin centros asignados"}</h3>
                  <span className="text-[11px] font-semibold text-on-surface-variant">Sin total</span>
                </div>
                <p className="mt-2 text-xs leading-5 text-on-surface-variant">Los totales por jurisdicción todavía no están disponibles.</p>
                <div className="mt-3 h-1.5 w-full overflow-hidden rounded-full bg-surface-container-highest" />
              </div>
            )}
          </div>
        </section>

        {/* Actas por firmar */}
        <section className="surface-card p-6">
          <SectionTitle
            title="Actas por Firmar"
            description="Firma digital obligatoria según su autorización"
            icon={FileSignature}
          />
          <div className="mt-5 rounded-xl border border-line bg-surface-container-low p-4">
            <span className="mb-3 flex size-9 items-center justify-center rounded-lg bg-surface-container-lowest text-primary-container shadow-card">
              <ClipboardCheck className="size-5" aria-hidden="true" />
            </span>
            <h3 className="text-sm font-semibold text-primary">Bandeja de firmas no disponible</h3>
            <p className="mt-2 text-xs leading-5 text-on-surface-variant">
              Las actas pendientes aparecerán cuando se habilite este módulo. Las matrices asignadas no equivalen a actas por firmar.
            </p>
          </div>

          <div className="mt-4 rounded-lg border border-line p-3">
            <h3 className="text-xs font-semibold text-primary">Mis matrices de autorización</h3>
            {matrices.isLoading ? <LoadingState label="Cargando matrices…" /> : matrices.isError ? (
              <ErrorState error={matrices.error} onRetry={() => void matrices.refetch()} />
            ) : (
              <p className="mt-2 text-xs leading-5 text-on-surface-variant">
                {matrices.data?.length ? "Consulte sus asignaciones vigentes." : "Sin matrices asignadas"}
              </p>
            )}
            <Link to="/mis-matrices" className="mt-3 inline-flex items-center gap-1 text-xs font-semibold text-secondary hover:text-primary">
              Ver mis matrices <ArrowRight className="size-3" aria-hidden="true" />
            </Link>
          </div>

          <div className="mt-4 flex items-center gap-3 rounded-lg bg-surface-container-high/60 p-3">
            <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-surface-container-lowest text-secondary">
              <LockKeyhole className="size-4" aria-hidden="true" />
            </span>
            <div className="flex min-w-0 flex-col text-[11px]">
              <span className="font-semibold text-primary">Firma electrónica</span>
              <span className="text-on-surface-variant">Disponible cuando se habilite el módulo de actas.</span>
            </div>
          </div>
        </section>
      </div>
    </div>

    {workspace.kind === "admin" && (
      <details className="surface-card p-4 text-xs text-on-surface-variant">
        <summary className="cursor-pointer font-semibold text-primary">Estado de servicios</summary>
        <div className="mt-3 flex flex-wrap items-center gap-3">
          <Activity className="size-4 text-secondary" aria-hidden="true" />
          <Button size="sm" variant="outline" onClick={checkHealth}>Comprobar servicios</Button>
          <span role="status" className="font-medium">{health}</span>
        </div>
      </details>
    )}
  </div>
}
