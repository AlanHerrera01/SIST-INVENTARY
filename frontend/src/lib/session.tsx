import { useEffect, type ReactNode } from "react"
import { useQuery, useQueryClient } from "@tanstack/react-query"
import { z } from "zod"
import { api, ApiError, SESSION_EXPIRED_EVENT } from "@/lib/api"
import { SessionContext, type SessionState } from "@/lib/session-context"

const profileSchema = z.object({
  id: z.string(), organizationId: z.string(), displayName: z.string(),
  grants: z.array(z.object({ role: z.string(), scope: z.string(), centerId: z.string().nullable(), permission: z.string().nullable() })),
})
export function SessionProvider({ children }: { children: ReactNode }) {
  const client = useQueryClient()
  useEffect(() => {
    const expired = () => {
      void client.cancelQueries()
      client.getMutationCache().clear()
      client.removeQueries({ predicate: (query) => query.queryKey.length !== 1 || query.queryKey[0] !== "me" })
      client.setQueryData(["me"], null)
    }
    window.addEventListener(SESSION_EXPIRED_EVENT, expired)
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, expired)
  }, [client])
  const query = useQuery({
    queryKey: ["me"],
    queryFn: async ({ signal }) => {
      try {
        const value = await api<unknown>("/me", { signal })
        const result = profileSchema.safeParse(value)
        if (!result.success) throw new ApiError(502, "INVALID_PROFILE", "No se pudo verificar el perfil de acceso.")
        return result.data
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) return null
        throw error
      }
    },
    retry: false, staleTime: 30_000, refetchOnWindowFocus: "always", refetchInterval: 60_000,
  })
  const value: SessionState = {
    profile: query.isError ? null : query.data ?? null,
    isLoading: query.isLoading, isError: query.isError,
    requiresLogin: query.isSuccess && query.data === null,
    error: query.error instanceof ApiError ? query.error : null,
    refetch: () => void query.refetch(),
  }
  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>
}
