import { useMemo, useState } from "react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { Pencil, Plus } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Textarea } from "@/components/ui/textarea"
import { AccessDenied, EmptyState, ErrorState, LoadingState, PageHeader } from "@/components/PageState"
import { ApiError } from "@/lib/api"
import { hasGlobal } from "@/lib/permissions"
import { useSession } from "@/lib/session-context"
import { accessApi, type Permission, type RoleView } from "@/features/access/api"

const schema = z.object({
  code: z.string().min(2).regex(/^[A-Z0-9_]+$/, "Solo mayúsculas, números y guion bajo").optional(),
  name: z.string().min(2, "Ingrese el nombre del rol"),
  description: z.string().optional(),
})
type FormValues = z.infer<typeof schema>

function PermissionPicker({ selected, onChange, permissions }: {
  selected: Set<string>
  onChange: (next: Set<string>) => void
  permissions: Permission[]
}) {
  const grouped = useMemo(() => {
    const map = new Map<string, Permission[]>()
    for (const p of permissions) {
      const group = p.code.split(".")[0]
      map.set(group, [...(map.get(group) ?? []), p])
    }
    return [...map.entries()]
  }, [permissions])

  function toggle(code: string) {
    const next = new Set(selected)
    if (next.has(code)) next.delete(code); else next.add(code)
    onChange(next)
  }

  return (
    <div className="max-h-72 space-y-4 overflow-y-auto rounded-md border p-3">
      {grouped.map(([group, items]) => (
        <fieldset key={group}>
          <legend className="mb-1 text-xs font-semibold uppercase text-muted-foreground">{group}</legend>
          <ul className="space-y-1">
            {items.map((p) => (
              <li key={p.code}>
                <label className="flex cursor-pointer items-start gap-2 rounded px-1 py-0.5 hover:bg-accent">
                  <input
                    type="checkbox"
                    className="mt-1"
                    checked={selected.has(p.code)}
                    onChange={() => toggle(p.code)}
                  />
                  <span>
                    <span className="block text-sm font-medium">{p.name}</span>
                    <span className="block text-xs text-muted-foreground">{p.code}</span>
                  </span>
                </label>
              </li>
            ))}
          </ul>
        </fieldset>
      ))}
    </div>
  )
}

function RoleFormDialog({ role, permissions }: { role?: RoleView; permissions: Permission[] }) {
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const [selected, setSelected] = useState<Set<string>>(new Set(role?.permissions ?? []))
  const isEdit = !!role
  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    values: { code: role?.code, name: role?.name ?? "", description: role?.description ?? "" },
  })

  const save = useMutation({
    mutationFn: async (v: FormValues) => {
      const saved = isEdit
        ? await accessApi.roles.update(role!.id, { name: v.name, description: v.description || undefined })
        : await accessApi.roles.create({ code: v.code!, name: v.name, description: v.description || undefined })
      if (isEdit) await accessApi.roles.replacePermissions(role!.id, [...selected])
      return saved
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["roles"] })
      setOpen(false)
      setSelected(new Set(role?.permissions ?? []))
      reset()
    },
  })

  return (
    <Dialog open={open} onOpenChange={(v) => { setOpen(v); if (v) setSelected(new Set(role?.permissions ?? [])) }}>
      <DialogTrigger asChild>
        {isEdit
          ? <Button variant="outline" size="sm"><Pencil aria-hidden="true" /> Editar</Button>
          : <Button><Plus aria-hidden="true" /> Nuevo rol</Button>}
      </DialogTrigger>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{isEdit ? `Editar ${role!.name}` : "Crear rol"}</DialogTitle>
          <DialogDescription>
            {isEdit ? "Modifique datos y el conjunto de permisos del rol." : "Defina un rol propio de la organización con permisos configurables."}
          </DialogDescription>
        </DialogHeader>
        <form className="space-y-4" onSubmit={handleSubmit((v) => save.mutate(v))}>
          {!isEdit && (
            <div className="space-y-1.5">
              <Label htmlFor="role-code">Código</Label>
              <Input id="role-code" placeholder="MI_ROL" {...register("code")} aria-invalid={!!errors.code} />
              {errors.code && <p className="text-xs text-red-600">{errors.code.message}</p>}
            </div>
          )}
          <div className="space-y-1.5">
            <Label htmlFor="role-name">Nombre</Label>
            <Input id="role-name" {...register("name")} aria-invalid={!!errors.name} />
            {errors.name && <p className="text-xs text-red-600">{errors.name.message}</p>}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="role-desc">Descripción</Label>
            <Textarea id="role-desc" {...register("description")} />
          </div>
          <fieldset className="space-y-1.5">
            <legend className="text-sm font-medium">Permisos</legend>
            <PermissionPicker selected={selected} onChange={setSelected} permissions={permissions} />
          </fieldset>
          {save.isError && <p role="alert" className="text-sm text-red-600">{save.error instanceof ApiError ? save.error.message : "Error al guardar."}</p>}
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => setOpen(false)}>Cancelar</Button>
            <Button type="submit" disabled={isSubmitting || save.isPending}>{save.isPending ? "Guardando…" : "Guardar"}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

/** Roles de la organización y su catálogo de permisos. */
export function RolesPage() {
  const { profile } = useSession()
  const canRead = hasGlobal(profile, "access.roles.read") || profile?.grants.some((g) => g.permission === "access.roles.read")
  const canManage = hasGlobal(profile, "access.roles.manage")
  const roles = useQuery({ queryKey: ["roles"], queryFn: accessApi.roles.list, enabled: canRead })
  const permissions = useQuery({ queryKey: ["permissions"], queryFn: accessApi.permissions, enabled: canRead })

  if (!canRead) return <AccessDenied permission="access.roles.read" />
  if (roles.isLoading) return <LoadingState label="Cargando roles…" />
  if (roles.isError) return <ErrorState error={roles.error} onRetry={() => void roles.refetch()} />

  return (
    <div>
      <PageHeader title="Roles y permisos" description="Roles de la organización y los permisos que otorgan.">
        {canManage && permissions.data && <RoleFormDialog permissions={permissions.data} />}
      </PageHeader>

      {roles.data!.length === 0 ? (
        <EmptyState title="Sin roles" description="Aún no hay roles definidos en la organización." />
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {roles.data!.map((role) => (
            <Card key={role.id}>
              <CardHeader className="flex-row items-start justify-between space-y-0">
                <div>
                  <CardTitle className="text-base">{role.name}</CardTitle>
                  <CardDescription>{role.description ?? role.code}</CardDescription>
                </div>
                {canManage && permissions.data && <RoleFormDialog role={role} permissions={permissions.data} />}
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="flex flex-wrap gap-1.5">
                  {role.permissions.slice(0, 8).map((p) => <Badge key={p} variant="secondary" className="font-mono text-[10px]">{p}</Badge>)}
                  {role.permissions.length > 8 && <Badge variant="outline">+{role.permissions.length - 8}</Badge>}
                </div>
                <p className="text-xs text-muted-foreground">{role.activeAssignments} asignaciones activas</p>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {permissions.isLoading && <LoadingState label="Cargando catálogo de permisos…" />}
      {permissions.data && (
        <Card className="mt-6">
          <CardHeader>
            <CardTitle className="text-base">Catálogo de permisos</CardTitle>
            <CardDescription>{permissions.data.length} permisos definidos por el sistema</CardDescription>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow><TableHead>Código</TableHead><TableHead>Nombre</TableHead><TableHead>Descripción</TableHead></TableRow>
              </TableHeader>
              <TableBody>
                {permissions.data.map((p) => (
                  <TableRow key={p.code}>
                    <TableCell className="font-mono text-xs">{p.code}</TableCell>
                    <TableCell>{p.name}</TableCell>
                    <TableCell className="text-muted-foreground">{p.description}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
