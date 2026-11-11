import { useState } from "react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Controller, useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { Plus, UserPlus, X } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Textarea } from "@/components/ui/textarea"
import { ConfirmDialog } from "@/components/ConfirmDialog"
import { AccessDenied, EmptyState, ErrorState, LoadingState, PageHeader } from "@/components/PageState"
import { ApiError } from "@/lib/api"
import { hasGlobal } from "@/lib/permissions"
import { useSession } from "@/lib/session-context"
import { accessApi, type MatrixView } from "@/features/access/api"

const ACT_TYPES = ["DESPACHO", "RECEPCION", "FINAL", "BAJA"] as const
const MEMBER_ROLES = ["FIRMANTE", "APROBADOR"] as const

const createSchema = z.object({
  code: z.string().min(2, "Código requerido"),
  name: z.string().min(3, "Nombre requerido"),
  centerId: z.string().optional(),
  actType: z.enum(ACT_TYPES),
  description: z.string().optional(),
})
type CreateForm = z.infer<typeof createSchema>

function StateBadge({ state }: { state: string }) {
  return state === "ACTIVO" ? <Badge variant="success">Activa</Badge> : <Badge variant="secondary">Inactiva</Badge>
}

function CreateMatrixDialog() {
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const { register, handleSubmit, reset, control, formState: { errors, isSubmitting } } = useForm<CreateForm>({
    resolver: zodResolver(createSchema),
    defaultValues: { actType: "DESPACHO" },
  })
  const mutation = useMutation({
    mutationFn: accessApi.matrices.create,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["matrices"] })
      setOpen(false)
      reset()
    },
  })
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild><Button><Plus aria-hidden="true" /> Nueva matriz</Button></DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Nueva matriz organizacional</DialogTitle>
          <DialogDescription>Define quién firma o aprueba un tipo de acta, a nivel organización o de un centro.</DialogDescription>
        </DialogHeader>
        <form className="space-y-4" onSubmit={handleSubmit((v) => mutation.mutate({ ...v, centerId: v.centerId || null }))}>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label htmlFor="m-code">Código</Label>
              <Input id="m-code" {...register("code")} aria-invalid={!!errors.code} />
              {errors.code && <p className="text-xs text-red-600">{errors.code.message}</p>}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="m-act">Tipo de acta</Label>
              <Controller
                control={control}
                name="actType"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger id="m-act"><SelectValue /></SelectTrigger>
                    <SelectContent>{ACT_TYPES.map((t) => <SelectItem key={t} value={t}>{t}</SelectItem>)}</SelectContent>
                  </Select>
                )}
              />
            </div>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="m-name">Nombre</Label>
            <Input id="m-name" {...register("name")} aria-invalid={!!errors.name} />
            {errors.name && <p className="text-xs text-red-600">{errors.name.message}</p>}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="m-center">ID de centro (opcional; vacío = toda la organización)</Label>
            <Input id="m-center" placeholder="UUID del centro" {...register("centerId")} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="m-desc">Descripción</Label>
            <Textarea id="m-desc" {...register("description")} />
          </div>
          {mutation.isError && <p role="alert" className="text-sm text-red-600">{mutation.error instanceof ApiError ? mutation.error.message : "Error al crear."}</p>}
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => setOpen(false)}>Cancelar</Button>
            <Button type="submit" disabled={isSubmitting || mutation.isPending}>Crear</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

function MatrixDetailDialog({ matrixId, canManage }: { matrixId: string; canManage: boolean }) {
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const detail = useQuery({ queryKey: ["matrices", matrixId], queryFn: () => accessApi.matrices.detail(matrixId), enabled: open })
  const users = useQuery({ queryKey: ["users", "all"], queryFn: () => accessApi.users.list({ limit: 100 }), enabled: open && canManage })
  const [memberUser, setMemberUser] = useState("")
  const [memberRole, setMemberRole] = useState<string>("FIRMANTE")
  const [position, setPosition] = useState("")

  const addMember = useMutation({
    mutationFn: () => accessApi.matrices.addMember(matrixId, {
      userId: memberUser,
      memberRole,
      signerPosition: position ? Number(position) : undefined,
    }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["matrices"] })
      setMemberUser(""); setPosition("")
    },
  })
  const removeMember = useMutation({
    mutationFn: (memberId: string) => accessApi.matrices.removeMember(matrixId, memberId),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ["matrices"] }),
  })

  const matrix = detail.data as MatrixView | undefined
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild><Button variant="outline" size="sm">Ver</Button></DialogTrigger>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{matrix?.name ?? "Matriz"}</DialogTitle>
          <DialogDescription>
            {matrix ? `${matrix.code} · ${matrix.actType} · ${matrix.centerId ? `centro ${matrix.centerCode ?? matrix.centerId}` : "organización"}` : "Cargando…"}
          </DialogDescription>
        </DialogHeader>
        {detail.isLoading && <LoadingState />}
        {detail.isError && <ErrorState error={detail.error} />}
        {matrix && (
          <>
            <Table>
              <TableHeader>
                <TableRow><TableHead>Miembro</TableHead><TableHead>Rol</TableHead><TableHead>Posición</TableHead><TableHead /></TableRow>
              </TableHeader>
              <TableBody>
                {matrix.members.length === 0 && (
                  <TableRow><TableCell colSpan={4} className="text-center text-muted-foreground">Sin miembros</TableCell></TableRow>
                )}
                {matrix.members.map((m) => (
                  <TableRow key={m.id}>
                    <TableCell>
                      <p className="font-medium">{m.displayName}</p>
                      <p className="text-xs text-muted-foreground">@{m.username}</p>
                    </TableCell>
                    <TableCell><Badge variant="secondary">{m.memberRole}</Badge></TableCell>
                    <TableCell>{m.signerPosition ?? "—"}</TableCell>
                    <TableCell className="text-right">
                      {canManage && (
                        <ConfirmDialog
                          trigger={<Button variant="ghost" size="icon" aria-label={`Quitar a ${m.displayName}`}><X className="size-4" /></Button>}
                          title="Quitar miembro"
                          description={`${m.displayName} dejará de poder firmar en esta matriz.`}
                          confirmLabel="Quitar"
                          destructive
                          onConfirm={() => removeMember.mutateAsync(m.id)}
                        />
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>

            {canManage && (
              <form
                className="flex flex-wrap items-end gap-2 rounded-lg border p-3"
                onSubmit={(e) => {
                  e.preventDefault()
                  if (memberUser) addMember.mutate()
                }}
              >
                <div className="min-w-48 flex-1 space-y-1">
                  <Label htmlFor="member-user">Usuario</Label>
                  <Select value={memberUser} onValueChange={setMemberUser}>
                    <SelectTrigger id="member-user"><SelectValue placeholder="Seleccione usuario" /></SelectTrigger>
                    <SelectContent>
                      {(users.data ?? []).map((u) => (
                        <SelectItem key={u.id} value={u.id}>{u.displayName} (@{u.username})</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="w-36 space-y-1">
                  <Label>Rol en matriz</Label>
                  <Select value={memberRole} onValueChange={setMemberRole}>
                    <SelectTrigger aria-label="Rol en matriz"><SelectValue /></SelectTrigger>
                    <SelectContent>{MEMBER_ROLES.map((r) => <SelectItem key={r} value={r}>{r}</SelectItem>)}</SelectContent>
                  </Select>
                </div>
                <div className="w-24 space-y-1">
                  <Label htmlFor="position">Posición</Label>
                  <Input id="position" type="number" min={1} value={position} onChange={(e) => setPosition(e.target.value)} />
                </div>
                <Button type="submit" size="sm" disabled={!memberUser || addMember.isPending}>
                  <UserPlus aria-hidden="true" /> Agregar
                </Button>
                {addMember.isError && <p role="alert" className="w-full text-xs text-red-600">{addMember.error instanceof ApiError ? addMember.error.message : "Error."}</p>}
              </form>
            )}
          </>
        )}
      </DialogContent>
    </Dialog>
  )
}

/** Administración de matrices organizacionales de firmas. */
export function MatricesPage() {
  const { profile } = useSession()
  const canRead = profile?.grants.some((g) => g.permission === "access.matrices.read") ?? false
  const canManage = hasGlobal(profile, "access.matrices.manage")
  const [includeInactive, setIncludeInactive] = useState(false)
  const matrices = useQuery({
    queryKey: ["matrices", includeInactive],
    queryFn: () => accessApi.matrices.list(includeInactive),
    enabled: canRead,
  })

  if (!canRead) return <AccessDenied permission="access.matrices.read" />
  return (
    <div>
      <PageHeader title="Matrices de firma" description="Matrices que autorizan firmas y aprobaciones por centro u organización.">
        {canManage && <CreateMatrixDialog />}
      </PageHeader>

      <label className="mb-4 flex items-center gap-2 text-sm">
        <input type="checkbox" checked={includeInactive} onChange={(e) => setIncludeInactive(e.target.checked)} />
        Incluir inactivas
      </label>

      {matrices.isLoading && <LoadingState label="Cargando matrices…" />}
      {matrices.isError && <ErrorState error={matrices.error} onRetry={() => void matrices.refetch()} />}
      {matrices.data && matrices.data.length === 0 && (
        <EmptyState title="Sin matrices" description="No hay matrices que coincidan con el filtro." />
      )}
      {matrices.data && matrices.data.length > 0 && (
        <div className="grid gap-4 md:grid-cols-2">
          {matrices.data.map((m: MatrixView) => (
            <Card key={m.id}>
              <CardHeader className="flex-row items-start justify-between space-y-0">
                <div>
                  <CardTitle className="text-base">{m.name}</CardTitle>
                  <CardDescription>{m.code} · {m.actType} · {m.centerId ? `centro ${m.centerCode ?? m.centerId}` : "organización"}</CardDescription>
                </div>
                <StateBadge state={m.matrixState} />
              </CardHeader>
              <CardContent className="space-y-3">
                <p className="text-sm text-muted-foreground">{m.description ?? "Sin descripción"}</p>
                <div className="flex flex-wrap gap-1.5">
                  {m.members.map((mem) => (
                    <Badge key={mem.id} variant="outline">{mem.displayName} · {mem.memberRole}</Badge>
                  ))}
                  {m.members.length === 0 && <span className="text-xs text-muted-foreground">Sin miembros</span>}
                </div>
                <MatrixDetailDialog matrixId={m.id} canManage={canManage} />
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}
