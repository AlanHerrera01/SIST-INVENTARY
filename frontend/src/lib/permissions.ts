import type { Profile } from "@/lib/session-context"

/**
 * Réplica en cliente de la semántica de {@code AccessGuard} del backend. Solo
 * se usa para MOSTRAR/OCLULTAR navegación y acciones; la autorización real la
 * exige el servidor en cada petición.
 */
export function hasGlobal(profile: Profile | null, permission: string): boolean {
  return !!profile?.grants.some((g) => g.permission === permission && g.scope === "GLOBAL")
}

export function centersFor(profile: Profile | null, permission: string): string[] {
  if (!profile) return []
  return [
    ...new Set(
      profile.grants
        .filter((g) => g.permission === permission && g.scope === "CENTROS" && g.centerId)
        .map((g) => g.centerId as string),
    ),
  ]
}

export function canAccess(profile: Profile | null, permission: string, centerId?: string | null): boolean {
  if (hasGlobal(profile, permission)) return true
  if (!centerId) return false
  return centersFor(profile, permission).includes(centerId)
}

export function hasAnyPermission(profile: Profile | null, permissions: string[]): boolean {
  return !!profile?.grants.some((g) => permissions.includes(g.permission))
}

export const ROLE_LABELS: Record<string, string> = {
  ADMINISTRADOR: "Administrador",
  TECNICO_TI: "Técnico TI",
  JEFATURA: "Jefatura",
  COORDINADOR: "Coordinador",
}

export function roleLabel(code: string): string {
  return ROLE_LABELS[code] ?? code
}
