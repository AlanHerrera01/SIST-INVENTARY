import { useState } from "react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Controller, useForm, type Control, type FieldPath, type FieldValues } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { Building2, MapPin, Plus, Trash2, UserCheck } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { ConfirmDialog } from "@/components/ConfirmDialog"

import { AccessDenied, EmptyState, ErrorState, LoadingState, PageHeader } from "@/components/PageState"
import { ApiError } from "@/lib/api"
import { hasGlobal } from "@/lib/permissions"
import { useSession } from "@/lib/session-context"
import { orgApi, type CenterView, type StructureView } from "@/features/organization/api"

const CENTER_KINDS = ["MEDICO", "BODEGA_TI", "ADMINISTRATIVO", "OTRO"] as const
const SITE_KINDS = ["MATRIZ", "SUCURSAL", "SEDE"] as const
const RESPONSIBILITY_ROLES = ["JEFE_CENTRO", "COORDINADOR", "RESPONSABLE_TI"] as const

const codeName = z.object({
  code: z.string().min(2, "Código requerido"),
  name: z.string().min(3, "Nombre requerido"),
})
const siteSchema = codeName.extend({ cityId: z.string().min(1, "Seleccione ciudad"), kind: z.enum(SITE_KINDS) })
const centerSchema = z.object({
  code: z.string().min(2), name: z.string().min(3),
  cityId: z.string().optional(), siteId: z.string().optional(), kind: z.enum(CENTER_KINDS),
})
const matrixSchema = z.object({
  code: z.string().min(2), name: z.string().min(3),
  cityId: z.string().optional(), siteId: z.string().optional(), description: z.string().optional(),
})
const responsibleSchema = z.object({
  centerId: z.string().min(1, "Seleccione centro"),
  role: z.enum(RESPONSIBILITY_ROLES),
  email: z.string().email("Correo institucional inválido"),
  userId: z.string().optional(),
  validUntil: z.string().optional(),
  reason: z.string().optional(),
})

function StateBadge({ state }: { state: string }) {
  return state === "ACTIVO" ? <Badge variant="success">Activo</Badge> : <Badge variant="secondary">Inactivo</Badge>
}
function Err({ error }: { error: unknown }) {
  return error ? <p role="alert" className="text-sm text-red-600">{error instanceof ApiError ? error.message : "Error."}</p> : null
}

function SimpleFormDialog({ title, description, trigger, onSubmit, pending, error, children }: {
  title: string; description: string; trigger: React.ReactNode
  onSubmit: () => void; pending?: boolean; error?: unknown; children: React.ReactNode
}) {
  const [open, setOpen] = useState(false)
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>{trigger}</DialogTrigger>
      <DialogContent>
        <DialogHeader><DialogTitle>{title}</DialogTitle><DialogDescription>{description}</DialogDescription></DialogHeader>
        {children}
        <Err error={error} />
        <DialogFooter>
          <Button variant="outline" onClick={() => setOpen(false)}>Cancelar</Button>
          <Button onClick={onSubmit} disabled={pending}>{pending ? "Guardando…" : "Guardar"}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function CreateCityDialog({ onDone }: { onDone: () => void }) {
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<z.infer<typeof codeName>>({ resolver: zodResolver(codeName) })
  const mutation = useMutation({
    mutationFn: orgApi.createCity,
    onSuccess: () => { onDone(); },
  })
  return (
    <SimpleFormDialog title="Nueva ciudad" description="Ciudad donde operan sedes y centros." trigger={<Button size="sm"><Plus aria-hidden="true" /> Ciudad</Button>}
      onSubmit={handleSubmit((v) => mutation.mutate(v))} pending={isSubmitting || mutation.isPending} error={mutation.error}>
      <div className="space-y-3">
        <div className="space-y-1"><Label>Código</Label><Input {...register("code")} placeholder="UIO" />{errors.code && <p className="text-xs text-red-600">{errors.code.message}</p>}</div>
        <div className="space-y-1"><Label>Nombre</Label><Input {...register("name")} placeholder="Quito" />{errors.name && <p className="text-xs text-red-600">{errors.name.message}</p>}</div>
      </div>
    </SimpleFormDialog>
  )
}

function CreateSiteDialog({ structure, onDone }: { structure: StructureView; onDone: () => void }) {
  const { register, handleSubmit, control, formState: { errors, isSubmitting } } = useForm<z.infer<typeof siteSchema>>({
    resolver: zodResolver(siteSchema), defaultValues: { kind: "SEDE" },
  })
  const mutation = useMutation({ mutationFn: orgApi.createSite, onSuccess: onDone })
  return (
    <SimpleFormDialog title="Nueva sede" description="Establecimiento físico dentro de una ciudad." trigger={<Button size="sm" variant="outline"><Plus aria-hidden="true" /> Sede</Button>}
      onSubmit={handleSubmit((v) => mutation.mutate(v))} pending={isSubmitting || mutation.isPending} error={mutation.error}>
      <div className="space-y-3">
        <div className="space-y-1"><Label>Ciudad</Label>
          <ControllerSelect control={control} name="cityId" options={structure.cities.map((c) => ({ value: c.id, label: c.name }))} placeholder="Seleccione" />
          {errors.cityId && <p className="text-xs text-red-600">{errors.cityId.message}</p>}
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-1"><Label>Código</Label><Input {...register("code")} placeholder="SD-XXX" />{errors.code && <p className="text-xs text-red-600">{errors.code.message}</p>}</div>
          <div className="space-y-1"><Label>Tipo</Label>
            <ControllerSelect control={control} name="kind" options={SITE_KINDS.map((k) => ({ value: k, label: k }))} />
          </div>
        </div>
        <div className="space-y-1"><Label>Nombre</Label><Input {...register("name")} />{errors.name && <p className="text-xs text-red-600">{errors.name.message}</p>}</div>
      </div>
    </SimpleFormDialog>
  )
}

function ControllerSelect<T extends FieldValues>({ control, name, options, placeholder }: {
  control: Control<T>
  name: FieldPath<T>
  options: { value: string; label: string }[]
  placeholder?: string
}) {
  return (
    <Controller
      control={control}
      name={name}
      render={({ field }) => (
        <Select value={String(field.value ?? "")} onValueChange={field.onChange}>
          <SelectTrigger><SelectValue placeholder={placeholder ?? "Seleccione"} /></SelectTrigger>
          <SelectContent>{options.map((o) => <SelectItem key={o.value} value={o.value}>{o.label}</SelectItem>)}</SelectContent>
        </Select>
      )}
    />
  )
}

function CreateCenterDialog({ structure, onDone }: { structure: StructureView; onDone: () => void }) {
  const { register, handleSubmit, control, formState: { errors, isSubmitting } } = useForm<z.infer<typeof centerSchema>>({
    resolver: zodResolver(centerSchema), defaultValues: { kind: "MEDICO" },
  })
  const mutation = useMutation({
    mutationFn: (v: z.infer<typeof centerSchema>) => orgApi.createCenter({
      ...v, cityId: v.cityId || null, siteId: v.siteId || null,
    }),
    onSuccess: onDone,
  })
  return (
    <SimpleFormDialog title="Nuevo centro" description="Centro médico, bodega TI u otro establecimiento." trigger={<Button size="sm"><Plus aria-hidden="true" /> Centro</Button>}
      onSubmit={handleSubmit((v) => mutation.mutate(v))} pending={isSubmitting || mutation.isPending} error={mutation.error}>
      <div className="space-y-3">
        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-1"><Label>Código</Label><Input {...register("code")} placeholder="CM-XXX-01" />{errors.code && <p className="text-xs text-red-600">{errors.code.message}</p>}</div>
          <div className="space-y-1"><Label>Tipo</Label>
            <ControllerSelect control={control} name="kind" options={CENTER_KINDS.map((k) => ({ value: k, label: k }))} />
          </div>
        </div>
        <div className="space-y-1"><Label>Nombre</Label><Input {...register("name")} />{errors.name && <p className="text-xs text-red-600">{errors.name.message}</p>}</div>
        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-1"><Label>Ciudad (opcional)</Label>
            <ControllerSelect control={control} name="cityId" placeholder="Sin ciudad" options={structure.cities.map((c) => ({ value: c.id, label: c.name }))} />
          </div>
          <div className="space-y-1"><Label>Sede (opcional)</Label>
            <ControllerSelect control={control} name="siteId" placeholder="Sin sede" options={structure.sites.map((s) => ({ value: s.id, label: s.name }))} />
          </div>
        </div>
      </div>
    </SimpleFormDialog>
  )
}

function CreateMatrixDialog({ structure, onDone }: { structure: StructureView; onDone: () => void }) {
  const { register, handleSubmit, control, formState: { errors, isSubmitting } } = useForm<z.infer<typeof matrixSchema>>({
    resolver: zodResolver(matrixSchema),
  })
  const mutation = useMutation({
    mutationFn: (v: z.infer<typeof matrixSchema>) => orgApi.createMatrix({ ...v, cityId: v.cityId || null, siteId: v.siteId || null }),
    onSuccess: onDone,
  })
  return (
    <SimpleFormDialog title="Nueva matriz receptora" description="Punto de recepción de inventario; los centros pueden operar sin ella." trigger={<Button size="sm" variant="outline"><Plus aria-hidden="true" /> Matriz</Button>}
      onSubmit={handleSubmit((v) => mutation.mutate(v))} pending={isSubmitting || mutation.isPending} error={mutation.error}>
      <div className="space-y-3">
        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-1"><Label>Código</Label><Input {...register("code")} placeholder="MQR-XXX" />{errors.code && <p className="text-xs text-red-600">{errors.code.message}</p>}</div>
          <div className="space-y-1"><Label>Nombre</Label><Input {...register("name")} />{errors.name && <p className="text-xs text-red-600">{errors.name.message}</p>}</div>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-1"><Label>Ciudad</Label>
            <ControllerSelect control={control} name="cityId" placeholder="Ninguna" options={structure.cities.map((c) => ({ value: c.id, label: c.name }))} />
          </div>
          <div className="space-y-1"><Label>Sede</Label>
            <ControllerSelect control={control} name="siteId" placeholder="Ninguna" options={structure.sites.map((s) => ({ value: s.id, label: s.name }))} />
          </div>
        </div>
        <div className="space-y-1"><Label>Descripción</Label><Input {...register("description")} /></div>
      </div>
    </SimpleFormDialog>
  )
}

function StructureTab({ structure, canManage, onChanged }: { structure: StructureView; canManage: boolean; onChanged: () => void }) {
  const queryClient = useQueryClient()
  const refresh = () => { void queryClient.invalidateQueries({ queryKey: ["org", "structure"] }); onChanged() }
  const toggleCenterState = useMutation({
    mutationFn: (c: CenterView) => orgApi.updateCenter(c.id, { state: c.state === "ACTIVO" ? "INACTIVO" : "ACTIVO" }),
    onSuccess: refresh,
  })
  const deleteCenter = useMutation({ mutationFn: orgApi.deleteCenter, onSuccess: refresh })
  const linkCenter = useMutation({
    mutationFn: ({ matrixId, centerId }: { matrixId: string; centerId: string }) => orgApi.linkCenter(matrixId, centerId),
    onSuccess: refresh,
  })
  const unlinkCenter = useMutation({
    mutationFn: ({ matrixId, centerId }: { matrixId: string; centerId: string }) => orgApi.unlinkCenter(matrixId, centerId),
    onSuccess: refresh,
  })
  const centerName = (id: string) => structure.centers.find((c) => c.id === id)?.name ?? id.slice(0, 8)

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader className="flex-row items-center justify-between space-y-0">
          <div><CardTitle className="text-base flex items-center gap-2"><MapPin className="size-4" /> Ciudades</CardTitle>
            <CardDescription>{structure.cities.length} registradas</CardDescription></div>
          {canManage && <CreateCityDialog onDone={refresh} />}
        </CardHeader>
        <CardContent>
          {structure.cities.length === 0 ? <EmptyState title="Sin ciudades" /> : (
            <div className="flex flex-wrap gap-2">
              {structure.cities.map((c) => <Badge key={c.id} variant="outline" className="px-3 py-1">{c.name} ({c.code})</Badge>)}
            </div>
          )}
        </CardContent>
      </Card>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader className="flex-row items-center justify-between space-y-0">
            <div><CardTitle className="text-base flex items-center gap-2"><Building2 className="size-4" /> Sedes</CardTitle>
              <CardDescription>{structure.sites.length} registradas</CardDescription></div>
            {canManage && <CreateSiteDialog structure={structure} onDone={refresh} />}
          </CardHeader>
          <CardContent>
            {structure.sites.length === 0 ? <EmptyState title="Sin sedes" /> : (
              <ul className="space-y-2 text-sm">
                {structure.sites.map((s) => (
                  <li key={s.id} className="flex items-center justify-between rounded border p-2">
                    <span><strong>{s.name}</strong> <span className="text-muted-foreground">· {s.cityName} · {s.kind}</span></span>
                    <StateBadge state={s.state} />
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex-row items-center justify-between space-y-0">
            <div><CardTitle className="text-base">Centros</CardTitle>
              <CardDescription>{structure.centers.length} registrados ({structure.locations.length} ubicaciones)</CardDescription></div>
            {canManage && <CreateCenterDialog structure={structure} onDone={refresh} />}
          </CardHeader>
          <CardContent>
            {structure.centers.length === 0 ? <EmptyState title="Sin centros" /> : (
              <Table>
                <TableHeader><TableRow><TableHead>Centro</TableHead><TableHead>Tipo</TableHead><TableHead>Ubicación</TableHead><TableHead>Estado</TableHead><TableHead /></TableRow></TableHeader>
                <TableBody>
                  {structure.centers.map((c) => (
                    <TableRow key={c.id}>
                      <TableCell><p className="font-medium">{c.name}</p><p className="text-xs text-muted-foreground">{c.code}</p></TableCell>
                      <TableCell><Badge variant="secondary">{c.kind}</Badge></TableCell>
                      <TableCell className="text-xs text-muted-foreground">{[c.cityName, c.siteName].filter(Boolean).join(" · ") || "—"}</TableCell>
                      <TableCell><StateBadge state={c.state} /></TableCell>
                      <TableCell className="text-right">
                        {canManage && (
                          <div className="flex justify-end gap-1">
                            <Button variant="ghost" size="sm" onClick={() => toggleCenterState.mutate(c)}>
                              {c.state === "ACTIVO" ? "Inactivar" : "Activar"}
                            </Button>
                            <ConfirmDialog
                              trigger={<Button variant="ghost" size="icon" aria-label={`Eliminar ${c.name}`}><Trash2 className="size-4 text-red-600" /></Button>}
                              title="Eliminar centro"
                              description="Solo procede si el centro no tiene activos, movimientos ni vínculos. Si tiene historia, inactívelo."
                              confirmLabel="Eliminar"
                              destructive
                              onConfirm={() => deleteCenter.mutateAsync(c.id)}
                            />
                          </div>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
            {structure.locations.length > 0 && (
              <p className="mt-3 text-xs text-muted-foreground">
                Ubicaciones: {structure.locations.map((l) => l.name).join(", ")}
              </p>
            )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader className="flex-row items-center justify-between space-y-0">
          <div><CardTitle className="text-base">Matrices receptoras</CardTitle>
            <CardDescription>{structure.matrices.length} registradas; los centros pueden operar sin matriz</CardDescription></div>
          {canManage && <CreateMatrixDialog structure={structure} onDone={refresh} />}
        </CardHeader>
        <CardContent>
          {structure.matrices.length === 0 ? <EmptyState title="Sin matrices receptoras" description="Los centros pueden operar sin matriz receptora." /> : (
            <div className="grid gap-3 md:grid-cols-2">
              {structure.matrices.map((m) => (
                <div key={m.id} className="rounded-lg border p-3">
                  <div className="flex items-center justify-between">
                    <p className="font-medium">{m.name} <span className="text-xs text-muted-foreground">({m.code})</span></p>
                    <StateBadge state={m.state} />
                  </div>
                  <p className="mt-1 text-xs text-muted-foreground">{m.cityName ?? "Sin ciudad"}</p>
                  <div className="mt-2 flex flex-wrap gap-1">
                    {m.centerIds.map((cid) => (
                      <Badge key={cid} variant="outline" className="text-xs">
                        {centerName(cid)}
                        {canManage && (
                          <button
                            className="ml-1 text-red-600"
                            aria-label={`Quitar ${centerName(cid)} de la matriz`}
                            onClick={() => unlinkCenter.mutate({ matrixId: m.id, centerId: cid })}
                          >×</button>
                        )}
                      </Badge>
                    ))}
                  </div>
                  {canManage && (
                    <div className="mt-2">
                      <Select onValueChange={(centerId) => linkCenter.mutate({ matrixId: m.id, centerId })}>
                        <SelectTrigger className="h-8 text-xs"><SelectValue placeholder="Vincular centro…" /></SelectTrigger>
                        <SelectContent>
                          {structure.centers.filter((c) => !m.centerIds.includes(c.id)).map((c) => (
                            <SelectItem key={c.id} value={c.id}>{c.name}</SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

function ResponsiblesTab({ structure }: { structure: StructureView }) {
  const queryClient = useQueryClient()
  const [centerId, setCenterId] = useState("")
  const responsibles = useQuery({
    queryKey: ["org", "responsibles", centerId],
    queryFn: () => orgApi.responsibles.list(centerId || undefined),
  })
  const { register, handleSubmit, control, formState: { errors, isSubmitting } } = useForm<z.infer<typeof responsibleSchema>>({
    resolver: zodResolver(responsibleSchema), defaultValues: { role: "JEFE_CENTRO" },
  })
  const create = useMutation({
    mutationFn: (v: z.infer<typeof responsibleSchema>) => orgApi.responsibles.create({
      centerId: v.centerId, role: v.role, email: v.email,
      userId: v.userId || null, validUntil: v.validUntil || undefined, reason: v.reason || undefined,
    }),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ["org", "responsibles"] }),
  })
  const end = useMutation({
    mutationFn: (id: string) => orgApi.responsibles.end(id),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ["org", "responsibles"] }),
  })

  return (
    <div className="grid gap-4 lg:grid-cols-3">
      <Card className="lg:col-span-2">
        <CardHeader className="flex-row items-center justify-between space-y-0">
          <div><CardTitle className="text-base flex items-center gap-2"><UserCheck className="size-4" /> Responsables</CardTitle>
            <CardDescription>Jefaturas, coordinadores y responsables TI por centro</CardDescription></div>
          <div className="w-56">
            <Select value={centerId} onValueChange={setCenterId}>
              <SelectTrigger aria-label="Filtrar por centro"><SelectValue placeholder="Todos los centros" /></SelectTrigger>
              <SelectContent>
                {structure.centers.map((c) => <SelectItem key={c.id} value={c.id}>{c.name}</SelectItem>)}
              </SelectContent>
            </Select>
          </div>
        </CardHeader>
        <CardContent>
          {responsibles.isLoading && <LoadingState />}
          {responsibles.isError && <ErrorState error={responsibles.error} onRetry={() => void responsibles.refetch()} />}
          {responsibles.data && responsibles.data.length === 0 && <EmptyState title="Sin responsables" description="Registre jefaturas, coordinadores o responsables TI." />}
          {responsibles.data && responsibles.data.length > 0 && (
            <Table>
              <TableHeader><TableRow><TableHead>Centro</TableHead><TableHead>Rol</TableHead><TableHead>Correo</TableHead><TableHead>Usuario</TableHead><TableHead>Estado</TableHead><TableHead /></TableRow></TableHeader>
              <TableBody>
                {responsibles.data.map((r) => (
                  <TableRow key={r.id}>
                    <TableCell className="font-medium">{r.centerName}</TableCell>
                    <TableCell><Badge variant="secondary">{r.role}</Badge></TableCell>
                    <TableCell className="text-sm">{r.email}</TableCell>
                    <TableCell className="text-sm">{r.username ? `@${r.username}` : "—"}</TableCell>
                    <TableCell><StateBadge state={r.state} /></TableCell>
                    <TableCell className="text-right">
                      {r.state === "ACTIVO" && (
                        <ConfirmDialog
                          trigger={<Button variant="ghost" size="sm" className="text-red-600">Cerrar</Button>}
                          title="Cerrar responsabilidad"
                          description="El responsable quedará inactivo y el cambio quedará en el historial."
                          confirmLabel="Cerrar"
                          destructive
                          onConfirm={() => end.mutateAsync(r.id)}
                        />
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle className="text-base">Registrar responsable</CardTitle>
          <CardDescription>Vincula un usuario local y correo institucional a un centro.</CardDescription></CardHeader>
        <CardContent>
          <form className="space-y-3" onSubmit={handleSubmit((v) => create.mutate(v))}>
            <div className="space-y-1"><Label>Centro</Label>
              <ControllerSelect control={control} name="centerId" options={structure.centers.map((c) => ({ value: c.id, label: c.name }))} />
              {errors.centerId && <p className="text-xs text-red-600">{errors.centerId.message}</p>}
            </div>
            <div className="space-y-1"><Label>Función</Label>
              <ControllerSelect control={control} name="role" options={RESPONSIBILITY_ROLES.map((r) => ({ value: r, label: r }))} />
            </div>
            <div className="space-y-1"><Label>Correo institucional</Label>
              <Input type="email" {...register("email")} placeholder="jefatura@…" />{errors.email && <p className="text-xs text-red-600">{errors.email.message}</p>}
            </div>
            <div className="space-y-1"><Label>ID de usuario (opcional)</Label>
              <Input {...register("userId")} placeholder="UUID del usuario local" />
            </div>
            <div className="space-y-1"><Label>Vigente hasta (opcional)</Label>
              <Input {...register("validUntil")} placeholder="2026-12-31T23:59:59" />
            </div>
            <div className="space-y-1"><Label>Motivo</Label>
              <Input {...register("reason")} />
            </div>
            <Err error={create.error} />
            <Button type="submit" className="w-full" disabled={isSubmitting || create.isPending}>
              {create.isPending ? "Registrando…" : "Registrar"}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}

/** Estructura organizacional multicentro (Fase 06). */
export function OrganizationPage() {
  const { profile } = useSession()
  const canRead = profile?.grants.some((g) => g.permission === "organization.read") ?? false
  const canManage = hasGlobal(profile, "organization.manage")
  const structure = useQuery({ queryKey: ["org", "structure"], queryFn: orgApi.structure, enabled: canRead })

  if (!canRead) return <AccessDenied permission="organization.read" />
  return (
    <div>
      <PageHeader title="Organización" description="Ciudades, sedes, centros, ubicaciones, matrices receptoras y responsables." />
      {structure.isLoading && <LoadingState label="Cargando estructura…" />}
      {structure.isError && <ErrorState error={structure.error} onRetry={() => void structure.refetch()} />}
      {structure.data && (
        <Tabs defaultValue="structure">
          <TabsList>
            <TabsTrigger value="structure">Estructura</TabsTrigger>
            <TabsTrigger value="responsibles">Responsables</TabsTrigger>
          </TabsList>
          <TabsContent value="structure" className="mt-4">
            <StructureTab structure={structure.data} canManage={canManage} onChanged={() => {}} />
          </TabsContent>
          <TabsContent value="responsibles" className="mt-4">
            <ResponsiblesTab structure={structure.data} />
          </TabsContent>
        </Tabs>
      )}
    </div>
  )
}
