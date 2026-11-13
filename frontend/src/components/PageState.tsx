import { AlertTriangle, Inbox, Loader2 } from "lucide-react"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { ApiError } from "@/lib/api"

export function PageHeader({ title, description, children }: { title: string; description?: string; children?: React.ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-primary">{title}</h1>
        {description && <p className="mt-1 text-sm text-on-surface-variant">{description}</p>}
      </div>
      {children && <div className="flex items-center gap-2">{children}</div>}
    </div>
  )
}

export function LoadingState({ label = "Cargando…" }: { label?: string }) {
  return (
    <div className="flex flex-col gap-3 py-8" role="status" aria-live="polite">
      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <Loader2 className="size-4 animate-spin" aria-hidden="true" />
        {label}
      </div>
      <Skeleton className="h-10 w-full" />
      <Skeleton className="h-10 w-full" />
      <Skeleton className="h-10 w-2/3" />
    </div>
  )
}

export function EmptyState({ title, description }: { title: string; description?: string }) {
  return (
    <div className="flex flex-col items-center gap-2 py-12 text-center" role="status">
      <Inbox className="size-8 text-outline" aria-hidden="true" />
      <p className="font-medium text-primary">{title}</p>
      {description && <p className="text-sm text-on-surface-variant">{description}</p>}
    </div>
  )
}

export function ErrorState({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  const apiError = error instanceof ApiError ? error : null
  const forbidden = apiError?.status === 403
  return (
    <Alert variant={forbidden ? "warning" : "destructive"} className="my-4">
      <AlertTriangle aria-hidden="true" />
      <AlertTitle>{forbidden ? "Acceso denegado" : "No se pudo cargar la información"}</AlertTitle>
      <AlertDescription className="flex flex-wrap items-center justify-between gap-3">
        <span>{apiError?.message ?? "Error inesperado. Inténtelo de nuevo."}</span>
        {onRetry && (
          <Button variant="outline" size="sm" onClick={onRetry}>
            Reintentar
          </Button>
        )}
      </AlertDescription>
    </Alert>
  )
}

/** Acceso denegado a nivel de página (permiso insuficiente en el cliente; el servidor también lo exige). */
export function AccessDenied({ permission }: { permission?: string }) {
  return (
    <div className="flex flex-col items-center gap-2 py-16 text-center" role="alert">
      <AlertTriangle className="size-10 text-amber-500" aria-hidden="true" />
      <h2 className="text-lg font-semibold">No tiene acceso a esta sección</h2>
      <p className="max-w-md text-sm text-muted-foreground">
        Su perfil no incluye el permiso necesario{permission ? " para esta sección" : ""} en el alcance seleccionado.
        Contacte al administrador si considera que se trata de un error.
      </p>
    </div>
  )
}
