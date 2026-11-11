import { useState } from "react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Link, useParams } from "react-router-dom"
import { Controller, useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { ArrowLeft, Pencil, RefreshCw } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { ConfirmDialog } from "@/components/ConfirmDialog"
import { AccessDenied, EmptyState, ErrorState, LoadingState, PageHeader } from "@/components/PageState"
import { ApiError } from "@/lib/api"
import { hasGlobal } from "@/lib/permissions"
import { roleLabel } from "@/lib/permissions"
import { useSession } from "@/lib/session-context"
import { accessApi } from "@/features/access/api"

const UUID_RE = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/

const editSchema = z.object({
  displayName: z.string().min(2, "Ingrese el nombre completo"),
  email: z.string().email("Correo inválido").or(z.literal("")).optional(),
})
type EditForm = z.infer<typeof editSchema>

const assignSchema = z.object({
  roleId: z.string().min(1, "Seleccione un rol"),
  scopeKind: z.enum(["GLOBAL", "CENTROS"]),
  centerIds: z.string().optional(),
  validUntil: z.string().optional(),
  reason: z.string().optional(),
})
type AssignForm = z.infer<typeof assignSchema>

function FieldError({ message }: { message?: string }) {
  return message ? <p className="text-xs text-red-600">{message}</p> : null
}

function EditUserDialog({ userId, displayName, email }: { userId: string; displayName: string; email: string | null }) {
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<EditForm>({
    resolver: zodResolver(editSchema),
    values: { displayName, email: email ?? "" },
  })
  const mutation = useMutation({
    mutationFn: (v: EditForm) => accessApi.users.update(userId, { displayName: v.displayName, email: v.email || undefined }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["users"] })
      setOpen(false)
    },
  })
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild><Button variant="outline" size="sm"><Pencil aria-hidden="true" /> Editar</Button></DialogTrigger>
      <DialogContent>
        <DialogHeader><DialogTitle>Editar usuario</DialogTitle><DialogDescription>Actualiza el perfil local y el proveedor de identidad.</DialogDescription></DialogHeader>
        <form className="space-y-4" onSubmit={handleSubmit((v) => mutation.mutate(v))}>
          <div className="space-y-1.5">
            <Label htmlFor="edit-name">Nombre completo</Label>
            <Input id="edit-name" {...register("displayName")} aria-invalid={!!errors.displayName} />
            <FieldError message={errors.displayName?.message} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="edit-email">Correo</Label>
            <Input id="edit-email" type="email" {...register("email")} aria-invalid={!!errors.email} />
            <FieldError message={errors.email?.message} />
          </div>
          {mutation.isError && <p role="alert" className="text-sm text-red-600">{mutation.error instanceof ApiError ? mutation.error.message : "Error al actualizar."}</p>}
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => setOpen(false)}>Cancelar</Button>
            <Button type="submit" disabled={isSubmitting || mutation.isPending}>Guardar</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

function SuspendDialog({ userId, displayName }: { userId: string; displayName: string }) {
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const [reason, setReason] = useState("")
  const mutation = useMutation({
    mutationFn: () => accessApi.users.suspend(userId, reason || undefined),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["users"] })
      setOpen(false)
    },
  })
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild><Button variant="destructive" size="sm">Suspender</Button></DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Suspender usuario</DialogTitle>
          <DialogDescription>
            {displayName} perderá acceso en su siguiente petición y se deshabilitará en el proveedor de identidad.
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-1.5">
          <Label htmlFor="suspend-reason">Motivo (opcional, queda auditado)</Label>
          <Input id="suspend-reason" value={reason} onChange={(e) => setReason(e.target.value)} />
        </div>
        {mutation.isError && <p role="alert" className="text-sm text-red-600">{mutation.error instanceof ApiError ? mutation.error.message : "Error al suspender."}</p>}
        <DialogFooter>
          <Button variant="outline" onClick={() => setOpen(false)}>Cancelar</Button>
          <Button variant="destructive" onClick={() => mutation.mutate()} disabled={mutation.isPending}>
            {mutation.isPending ? "Suspendiendo…" : "Suspender"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function AssignRoleDialog({ userId }: { userId: string }) {
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const roles = useQuery({ queryKey: ["roles"], queryFn: accessApi.roles.list, enabled: open })
  const { register, handleSubmit, control, reset, formState: { errors } } = useForm<AssignForm>({
    resolver: zodResolver(assignSchema),
    defaultValues: { scopeKind: "GLOBAL" },
  })
  const mutation = useMutation({
    mutationFn: (v: AssignForm) => {
      const centerIds = v.scopeKind === "CENTROS"
        ? (v.centerIds ?? "").split(",").map((s) => s.trim()).filter(Boolean)
        : undefined
      if (v.scopeKind === "CENTROS" && (!centerIds || centerIds.length === 0 || !centerIds.every((c) => UUID_RE.test(c)))) {
        return Promise.reject(new ApiError(422, "VALIDATION_FAILED", "Indique UUIDs de centros válidos, separados por comas."))
      }
      return accessApi.users.assignRole(userId, {
        roleId: v.roleId,
        scopeKind: v.scopeKind,
        centerIds,
        validUntil: v.validUntil || undefined,
        reason: v.reason || undefined,
      })
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["users"] })
      setOpen(false)
      reset()
    },
  })
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild><Button size="sm">Asignar rol</Button></DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Asignar rol</DialogTitle>
          <DialogDescription>El permiso rige desde la asignación; puede limitarse a centros concretos.</DialogDescription>
        </DialogHeader>
        <form className="space-y-4" onSubmit={handleSubmit((v) => mutation.mutate(v))}>
          <div className="space-y-1.5">
            <Label htmlFor="role">Rol</Label>
            <Controller
              control={control}
              name="roleId"
              render={({ field }) => (
                <Select value={field.value} onValueChange={field.onChange}>
                  <SelectTrigger id="role" aria-invalid={!!errors.roleId}><SelectValue placeholder="Seleccione un rol" /></SelectTrigger>
                  <SelectContent>
                    {(roles.data ?? []).map((r) => (
                      <SelectItem key={r.id} value={r.id}>{r.name} ({r.code})</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            <FieldError message={errors.roleId?.message} />
          </div>
          <Controller
            control={control}
            name="scopeKind"
            render={({ field }) => (
              <>
                <div className="space-y-1.5">
                  <Label>Alcance</Label>
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger aria-label="Alcance"><SelectValue /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="GLOBAL">Global (toda la organización)</SelectItem>
                      <SelectItem value="CENTROS">Limitado a centros</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                {field.value === "CENTROS" && (
                  <div className="space-y-1.5">
                    <Label htmlFor="centers">IDs de centros (UUID, separados por coma)</Label>
                    <Input id="centers" placeholder="uuid1, uuid2" {...register("centerIds")} />
                  </div>
                )}
              </>
            )}
          />
          <div className="space-y-1.5">
            <Label htmlFor="valid-until">Vigente hasta (opcional, ISO-8601)</Label>
            <Input id="valid-until" placeholder="2026-12-31T23:59:59" {...register("validUntil")} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="assign-reason">Motivo (opcional)</Label>
            <Input id="assign-reason" {...register("reason")} />
          </div>
          {mutation.isError && <p role="alert" className="text-sm text-red-600">{mutation.error instanceof ApiError ? mutation.error.message : "Error al asignar."}</p>}
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => setOpen(false)}>Cancelar</Button>
            <Button type="submit" disabled={mutation.isPending}>{mutation.isPending ? "Asignando…" : "Asignar"}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

/** Detalle de usuario: estado, acciones administrativas, roles y matrices. */
export function UserDetailPage() {
  const { id = "" } = useParams()
  const { profile } = useSession()
  const queryClient = useQueryClient()
  const canManage = hasGlobal(profile, "access.users.manage")
  const canAssign = hasGlobal(profile, "access.assignments.manage")
  const detail = useQuery({ queryKey: ["users", id], queryFn: () => accessApi.users.detail(id) })

  const activate = useMutation({
    mutationFn: () => accessApi.users.activate(id),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ["users"] }),
  })
  const sync = useMutation({
    mutationFn: () => accessApi.users.sync(id),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ["users"] }),
  })
  const revoke = useMutation({
    mutationFn: (assignmentId: string) => accessApi.users.revokeAssignment(id, assignmentId),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ["users"] }),
  })

  if (detail.isLoading) return <LoadingState label="Cargando usuario…" />
  if (detail.isError) {
    const forbidden = detail.error instanceof ApiError && detail.error.status === 403
    if (forbidden) return <AccessDenied permission="access.users.read" />
    return <ErrorState error={detail.error} onRetry={() => void detail.refetch()} />
  }
  const user = detail.data!
  const active = user.summary.state === "ACTIVO"

  return (
    <div>
      <PageHeader
        title={user.summary.displayName}
        description={`@${user.summary.username} · ${user.summary.email ?? "sin correo"}`}
      >
        <Button variant="ghost" size="sm" asChild>
          <Link to="/access/users"><ArrowLeft aria-hidden="true" /> Volver</Link>
        </Button>
        {canManage && <EditUserDialog userId={id} displayName={user.summary.displayName} email={user.summary.email} />}
        {canManage && active && <SuspendDialog userId={id} displayName={user.summary.displayName} />}
        {canManage && !active && (
          <ConfirmDialog
            trigger={<Button size="sm">Reactivar</Button>}
            title="Reactivar usuario"
            description={`${user.summary.displayName} recuperará el acceso según sus asignaciones vigentes.`}
            confirmLabel="Reactivar"
            onConfirm={() => activate.mutateAsync()}
          />
        )}
        {canManage && (
          <Button variant="outline" size="sm" onClick={() => sync.mutate()} disabled={sync.isPending}>
            <RefreshCw aria-hidden="true" /> {sync.isPending ? "Sincronizando…" : "Sincronizar"}
          </Button>
        )}
      </PageHeader>

      <div className="grid gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-1">
          <CardHeader><CardTitle className="text-base">Estado</CardTitle></CardHeader>
          <CardContent className="space-y-2 text-sm">
            <p>Estado: <Badge variant={active ? "success" : "destructive"}>{active ? "Activo" : "Inactivo"}</Badge></p>
            <p className="text-muted-foreground">Creado: {user.summary.createdAt?.slice(0, 10)}</p>
          </CardContent>
        </Card>

        <Card className="lg:col-span-2">
          <CardHeader className="flex-row items-center justify-between space-y-0">
            <div>
              <CardTitle className="text-base">Roles asignados</CardTitle>
              <CardDescription>Asignaciones vigentes y su alcance</CardDescription>
            </div>
            {canAssign && <AssignRoleDialog userId={id} />}
          </CardHeader>
          <CardContent>
            {user.assignments.length === 0 ? (
              <EmptyState title="Sin roles" description="Este usuario no tiene asignaciones activas." />
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Rol</TableHead>
                    <TableHead>Alcance</TableHead>
                    <TableHead>Vigencia</TableHead>
                    <TableHead />
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {user.assignments.map((a) => (
                    <TableRow key={a.id}>
                      <TableCell>
                        <p className="font-medium">{a.roleName}</p>
                        <p className="text-xs text-muted-foreground">{roleLabel(a.roleCode)}</p>
                      </TableCell>
                      <TableCell>
                        {a.scopeKind === "GLOBAL" ? "Global" : `Centros: ${a.centerIds.length}`}
                      </TableCell>
                      <TableCell className="text-xs text-muted-foreground">
                        {a.validFrom?.slice(0, 10)} → {a.validUntil ? a.validUntil.slice(0, 10) : "sin fin"}
                      </TableCell>
                      <TableCell className="text-right">
                        {canAssign && (
                          <ConfirmDialog
                            trigger={<Button variant="ghost" size="sm" className="text-red-600">Revocar</Button>}
                            title="Revocar asignación"
                            description={`Se revocará el rol ${a.roleName}. El cambio surte efecto en la siguiente petición del usuario.`}
                            confirmLabel="Revocar"
                            destructive
                            onConfirm={() => revoke.mutateAsync(a.id)}
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

        <Card className="lg:col-span-3">
          <CardHeader>
            <CardTitle className="text-base">Matrices de firma</CardTitle>
            <CardDescription>Matrices en las que figura como firmante o aprobador</CardDescription>
          </CardHeader>
          <CardContent>
            {user.matrices.length === 0 ? (
              <EmptyState title="Sin matrices" description="No pertenece a ninguna matriz activa." />
            ) : (
              <ul className="grid gap-2 sm:grid-cols-2">
                {user.matrices.map((m) => (
                  <li key={m.matrixId} className="rounded-lg border p-3 text-sm">
                    <p className="font-medium">{m.name} <span className="text-muted-foreground">({m.code})</span></p>
                    <p className="text-xs text-muted-foreground">
                      {m.actType}{m.centerId ? " · centro específico" : " · organización"} · {m.memberRole}
                    </p>
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
