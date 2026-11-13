import { useState } from "react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Link } from "react-router-dom"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { Plus, Search } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { FormField } from "@/components/FormField"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { EmptyState, ErrorState, LoadingState, PageHeader } from "@/components/PageState"
import { ApiError } from "@/lib/api"
import { hasGlobal } from "@/lib/permissions"
import { useSession } from "@/lib/session-context"
import { accessApi, type CreateUserInput, type UserSummary } from "@/features/access/api"

const createSchema = z.object({
  username: z.string().min(3, "Mínimo 3 caracteres").regex(/^[A-Za-z0-9][A-Za-z0-9._@-]*$/, "Caracteres no válidos"),
  displayName: z.string().min(2, "Ingrese el nombre completo"),
  email: z.string().email("Correo inválido").or(z.literal("")).optional(),
  temporaryPassword: z.string().min(8, "Mínimo 8 caracteres"),
})
type CreateForm = z.infer<typeof createSchema>

function StateBadge({ state }: { state: string }) {
  return state === "ACTIVO"
    ? <Badge variant="success">Activo</Badge>
    : <Badge variant="destructive">Inactivo</Badge>
}

function CreateUserDialog() {
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<CreateForm>({
    resolver: zodResolver(createSchema),
  })
  const mutation = useMutation({
    mutationFn: (input: CreateUserInput) => accessApi.users.create(input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["users"] })
      setOpen(false)
      reset()
    },
  })

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button><Plus aria-hidden="true" /> Nuevo usuario</Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Crear usuario</DialogTitle>
          <DialogDescription>
            Se creará la identidad en el proveedor (Keycloak) y el perfil local. El usuario deberá cambiar la contraseña temporal en su primer inicio.
          </DialogDescription>
        </DialogHeader>
        <form className="space-y-4" onSubmit={handleSubmit((v) => mutation.mutate({ ...v, email: v.email || undefined }))}>
          <div className="space-y-1.5">
            <Label htmlFor="username">Nombre de usuario</Label>
            <Input id="username" autoComplete="username" {...register("username")} aria-invalid={!!errors.username} aria-describedby={errors.username ? "username-error" : undefined} />
            {errors.username && <p id="username-error" role="alert" className="text-xs text-red-600">{errors.username.message}</p>}
          </div>
          <FormField id="displayName" label="Nombre completo" {...register("displayName")} error={errors.displayName?.message} />
          <div className="space-y-1.5">
            <Label htmlFor="email">Correo (opcional)</Label>
            <Input id="email" type="email" {...register("email")} aria-invalid={!!errors.email} aria-describedby={errors.email ? "email-error" : undefined} />
            {errors.email && <p id="email-error" role="alert" className="text-xs text-red-600">{errors.email.message}</p>}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="temporaryPassword">Contraseña temporal</Label>
            <Input id="temporaryPassword" type="password" autoComplete="new-password" {...register("temporaryPassword")} aria-invalid={!!errors.temporaryPassword} aria-describedby={errors.temporaryPassword ? "password-error" : undefined} />
            {errors.temporaryPassword && <p id="password-error" role="alert" className="text-xs text-red-600">{errors.temporaryPassword.message}</p>}
          </div>
          {mutation.isError && (
            <p role="alert" className="text-sm text-red-600">
              {mutation.error instanceof ApiError ? mutation.error.message : "No se pudo crear el usuario."}
            </p>
          )}
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => setOpen(false)}>Cancelar</Button>
            <Button type="submit" disabled={isSubmitting || mutation.isPending}>
              {isSubmitting || mutation.isPending ? "Creando…" : "Crear usuario"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

/** Listado de usuarios locales con filtros y alta. */
export function UsersPage() {
  const { profile } = useSession()
  const canManage = hasGlobal(profile, "access.users.manage")
  const [state, setState] = useState("")
  const [search, setSearch] = useState("")
  const [query, setQuery] = useState("")

  const users = useQuery({
    queryKey: ["users", state, query],
    queryFn: () => accessApi.users.list({ state: state || undefined, q: query || undefined, limit: 100 }),
  })

  return (
    <div>
      <PageHeader title="Usuarios" description="Perfiles locales sincronizados con el proveedor de identidad.">
        {canManage && <CreateUserDialog />}
      </PageHeader>

      <div className="mb-4 flex flex-wrap gap-2">
        <form
          className="flex flex-1 gap-2 sm:max-w-xs"
          onSubmit={(e) => { e.preventDefault(); setQuery(search.trim()) }}
          role="search"
        >
          <Input
            type="search"
            placeholder="Buscar por nombre o usuario…"
            aria-label="Buscar usuarios"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <Button type="submit" variant="outline" size="icon" aria-label="Buscar"><Search className="size-4" aria-hidden="true" /></Button>
        </form>
        <Select value={state || "all"} onValueChange={(v) => setState(v === "all" ? "" : v)}>
          <SelectTrigger className="w-40" aria-label="Filtrar por estado"><SelectValue placeholder="Estado" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Todos</SelectItem>
            <SelectItem value="ACTIVO">Activos</SelectItem>
            <SelectItem value="INACTIVO">Inactivos</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {users.isLoading && <LoadingState label="Cargando usuarios…" />}
      {users.isError && <ErrorState error={users.error} onRetry={() => void users.refetch()} />}
      {users.data && users.data.length === 0 && (
        <EmptyState title="Sin usuarios" description="No hay usuarios que coincidan con los filtros." />
      )}
      {users.data && users.data.length > 0 && (
        <div className="rounded-xl border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Usuario</TableHead>
                <TableHead>Nombre</TableHead>
                <TableHead>Correo</TableHead>
                <TableHead>Estado</TableHead>
                <TableHead>Creado</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {users.data.map((u: UserSummary) => (
                <TableRow key={u.id}>
                  <TableCell>
                    <Link to={`/access/users/${u.id}`} className="font-medium text-primary hover:underline">
                      {u.username}
                    </Link>
                  </TableCell>
                  <TableCell>{u.displayName}</TableCell>
                  <TableCell className="text-muted-foreground">{u.email ?? "—"}</TableCell>
                  <TableCell><StateBadge state={u.state} /></TableCell>
                  <TableCell className="text-muted-foreground">{u.createdAt?.slice(0, 10)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  )
}
