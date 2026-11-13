import { useState } from "react"
import { DataTable } from "@/components/DataTable"
import { FilterBar } from "@/components/FilterBar"
import { PageHeader } from "@/components/PageState"
import { useSession } from "@/lib/session-context"
import { useWorkspace } from "@/lib/workspace"
import { roleLabel } from "@/lib/permissions"

export function MyAccessPage() {
  const { profile } = useSession()
  const { centerId } = useWorkspace()
  const [search, setSearch] = useState("")
  const [filter, setFilter] = useState("")
  const rows = (profile?.grants ?? []).filter((g) => (!centerId || g.scope === "GLOBAL" || g.centerId === centerId)
    && `${roleLabel(g.role)} ${g.permission ?? ""} ${g.centerId ?? ""}`.toLowerCase().includes(filter.toLowerCase()))
    .map((grant, index) => ({ ...grant, key: String(index) }))
  return <section>
    <PageHeader title="Mi acceso" description="Permisos vigentes de su sesión. Seleccione un centro en la barra superior para revisar su alcance." />
    <FilterBar label="Buscar rol o permiso" value={search} onChange={setSearch} onSearch={() => setFilter(search.trim())} />
    <DataTable caption="Roles, permisos y centros autorizados" rows={rows} rowKey={(g) => g.key} columns={[
      { id: "role", title: "Rol", render: (g) => roleLabel(g.role) },
      { id: "permission", title: "Permiso", render: (g) => g.permission ?? "Sin permisos asignados" },
      { id: "scope", title: "Alcance", render: (g) => g.scope === "GLOBAL" ? "Organización" : g.scope === "CENTROS" ? "Centro" : "Alcance no reconocido" },
      { id: "center", title: "Centro autorizado", render: (g) => <span className="break-all">{g.scope === "GLOBAL" ? "Todos los centros" : g.centerId ?? "Sin centro"}</span> },
    ]} />
  </section>
}
