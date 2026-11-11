import { useState } from "react"
import { useInfiniteQuery } from "@tanstack/react-query"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { AccessDenied, EmptyState, ErrorState, LoadingState, PageHeader } from "@/components/PageState"
import { useSession } from "@/lib/session-context"
import { accessApi } from "@/features/access/api"

const OUTCOMES = ["", "SUCCESS", "DENIED", "FAILURE"] as const

function OutcomeBadge({ outcome }: { outcome: string }) {
  const variant = outcome === "SUCCESS" ? "success" : outcome === "DENIED" ? "warning" : "destructive"
  return <Badge variant={variant}>{outcome}</Badge>
}

/** Consulta de auditoría con filtros y paginación por cursor. */
export function AuditPage() {
  const { profile } = useSession()
  const canRead = profile?.grants.some((g) => g.permission === "access.audit.read") ?? false
  const [filters, setFilters] = useState({ action: "", outcome: "", actorUserId: "", occurredFrom: "", occurredTo: "" })
  const [applied, setApplied] = useState(filters)

  const audit = useInfiniteQuery({
    queryKey: ["audit", applied],
    queryFn: ({ pageParam }) =>
      accessApi.audit.list({ ...applied, cursor: pageParam || undefined, limit: 50 }),
    initialPageParam: "",
    getNextPageParam: (last) => last.nextCursor ?? undefined,
    enabled: canRead,
  })

  if (!canRead) return <AccessDenied permission="access.audit.read" />
  const items = audit.data?.pages.flatMap((p) => p.items) ?? []

  return (
    <div>
      <PageHeader title="Auditoría" description="Eventos administrativos dentro de su alcance autorizado.">
        <Button variant="outline" size="sm" onClick={() => { setApplied({ ...filters }); }}>Aplicar filtros</Button>
      </PageHeader>

      <div className="mb-4 grid gap-2 sm:grid-cols-2 lg:grid-cols-5">
        <div className="space-y-1">
          <Label htmlFor="f-action">Acción</Label>
          <Input id="f-action" placeholder="USER_SUSPENDED" value={filters.action} onChange={(e) => setFilters({ ...filters, action: e.target.value })} />
        </div>
        <div className="space-y-1">
          <Label>Resultado</Label>
          <Select value={filters.outcome || "all"} onValueChange={(v) => setFilters({ ...filters, outcome: v === "all" ? "" : v })}>
            <SelectTrigger aria-label="Resultado"><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Todos</SelectItem>
              {OUTCOMES.filter(Boolean).map((o) => <SelectItem key={o} value={o}>{o}</SelectItem>)}
            </SelectContent>
          </Select>
        </div>
        <div className="space-y-1">
          <Label htmlFor="f-actor">ID actor</Label>
          <Input id="f-actor" placeholder="UUID" value={filters.actorUserId} onChange={(e) => setFilters({ ...filters, actorUserId: e.target.value })} />
        </div>
        <div className="space-y-1">
          <Label htmlFor="f-from">Desde</Label>
          <Input id="f-from" type="datetime-local" value={filters.occurredFrom} onChange={(e) => setFilters({ ...filters, occurredFrom: e.target.value })} />
        </div>
        <div className="space-y-1">
          <Label htmlFor="f-to">Hasta</Label>
          <Input id="f-to" type="datetime-local" value={filters.occurredTo} onChange={(e) => setFilters({ ...filters, occurredTo: e.target.value })} />
        </div>
      </div>

      {audit.isLoading && <LoadingState label="Cargando eventos…" />}
      {audit.isError && <ErrorState error={audit.error} onRetry={() => void audit.refetch()} />}
      {audit.data && items.length === 0 && <EmptyState title="Sin eventos" description="No hay eventos que coincidan con los filtros." />}
      {items.length > 0 && (
        <div className="rounded-xl border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Fecha</TableHead>
                <TableHead>Acción</TableHead>
                <TableHead>Entidad</TableHead>
                <TableHead>Actor</TableHead>
                <TableHead>Resultado</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {items.map((e) => (
                <TableRow key={e.id}>
                  <TableCell className="whitespace-nowrap text-xs text-muted-foreground">{e.occurredAt?.replace("T", " ").slice(0, 19)}</TableCell>
                  <TableCell className="font-mono text-xs">{e.action}</TableCell>
                  <TableCell className="text-xs">{e.entityType}{e.entityId ? ` · ${e.entityId.slice(0, 8)}…` : ""}</TableCell>
                  <TableCell className="text-sm">{e.actorName ?? "—"}</TableCell>
                  <TableCell><OutcomeBadge outcome={e.outcome} /></TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
      {audit.hasNextPage && (
        <div className="mt-4 text-center">
          <Button variant="outline" onClick={() => void audit.fetchNextPage()} disabled={audit.isFetchingNextPage}>
            {audit.isFetchingNextPage ? "Cargando…" : "Cargar más"}
          </Button>
        </div>
      )}
    </div>
  )
}
