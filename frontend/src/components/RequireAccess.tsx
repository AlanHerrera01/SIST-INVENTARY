import type { ReactNode } from "react"
import { AccessDenied } from "@/components/PageState"
import { hasGlobal } from "@/lib/permissions"
import { useSession } from "@/lib/session-context"

/**
 * Guardia de página: muestra AccessDenied si el perfil no tiene el permiso
 * GLOBAL requerido. La seguridad real la impone el backend; esto evita
 * renderizar formularios que el servidor rechazaría.
 */
export function RequireAccess({ permission, children }: { permission: string; children: ReactNode }) {
  const { profile, isLoading } = useSession()
  if (isLoading) return null
  if (!hasGlobal(profile, permission) && !profile?.grants.some((g) => g.permission === permission)) {
    return <AccessDenied permission={permission} />
  }
  return <>{children}</>
}
