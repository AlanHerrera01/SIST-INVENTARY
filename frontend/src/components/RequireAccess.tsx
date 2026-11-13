import type { ReactNode } from "react"
import { AccessDenied } from "@/components/PageState"
import { canVisit, NAV_LINKS } from "@/lib/nav"
import { useWorkspace } from "@/lib/workspace"
import { useSession } from "@/lib/session-context"

/**
 * Usa la misma política de alcance que la navegación. La autorización real
 * corresponde al servidor; el layout nunca concede permisos.
 */
export function RequireAccess({ permission, children }: { permission: string; children: ReactNode }) {
  const { profile, isLoading } = useSession()
  const { centerId } = useWorkspace()
  if (isLoading) return null
  const item = NAV_LINKS.find((entry) => entry.permission === permission)
  if (!item || !canVisit(profile, item, centerId)) {
    return <AccessDenied permission={permission} />
  }
  return <>{children}</>
}
