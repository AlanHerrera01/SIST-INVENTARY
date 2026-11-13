import { useQuery } from "@tanstack/react-query"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { EmptyState, ErrorState, LoadingState, PageHeader } from "@/components/PageState"
import { accessApi } from "@/features/access/api"
import { useWorkspace } from "@/lib/workspace"

const MEMBER_ROLE_LABELS: Record<string, string> = { FIRMANTE: "Firmante", APROBADOR: "Aprobador" }

/** Matrices activas en las que el usuario autenticado puede firmar o aprobar. */
export function MyMatricesPage() {
  const matrices = useQuery({ queryKey: ["me", "matrices"], queryFn: accessApi.matrices.mine })
  const { centerId } = useWorkspace()
  const visible = matrices.data?.filter((matrix) => !centerId || !matrix.centerId || matrix.centerId === centerId)

  return (
    <div>
      <PageHeader title="Actas y Firmas" description="Matrices organizacionales donde está autorizado a firmar o aprobar." />
      {matrices.isLoading && <LoadingState label="Cargando matrices…" />}
      {matrices.isError && <ErrorState error={matrices.error} onRetry={() => void matrices.refetch()} />}
      {!matrices.isError && visible && visible.length === 0 && (
        <EmptyState
          title="Sin matrices asignadas"
          description="Su perfil no incluye pertenencia a ninguna matriz activa. Contacte al administrador."
        />
      )}
      {!matrices.isError && visible && visible.length > 0 && (
        <div className="grid gap-4 md:grid-cols-2">
          {visible.map((m) => (
            <Card key={m.id}>
              <CardHeader>
                <CardTitle className="text-base">{m.name}</CardTitle>
                <CardDescription>{m.code} · {m.actType} · {m.centerId ? `centro ${m.centerCode ?? m.centerId}` : "organización"}</CardDescription>
              </CardHeader>
              <CardContent>
                <ul className="space-y-1.5">
                  {m.members.filter((mem) => mem.memberRole).map((mem) => (
                    <li key={mem.id} className="flex items-center justify-between text-sm">
                      <span>{mem.displayName}</span>
                      <Badge variant="secondary">{MEMBER_ROLE_LABELS[mem.memberRole] ?? mem.memberRole}</Badge>
                    </li>
                  ))}
                </ul>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}
