import { useState } from "react"
import type { ReactNode } from "react"
import { useQuery } from "@tanstack/react-query"
import { api, ApiError } from "@/lib/api"
import { SessionContext, type Profile, type SessionState } from "@/lib/session-context"

async function fetchProfile(): Promise<Profile | null> {
  try {
    return await api<Profile>("/me")
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) return null
    throw error
  }
}

export function SessionProvider({ children }: { children: ReactNode }) {
  const [requiresLogin, setRequiresLogin] = useState(false)
  const query = useQuery({
    queryKey: ["me"],
    queryFn: async () => {
      const profile = await fetchProfile()
      setRequiresLogin(profile === null)
      return profile
    },
    retry: (count, error) => !(error instanceof ApiError && error.status === 401) && count < 2,
    staleTime: 60_000,
  })
  const value: SessionState = {
    profile: query.data ?? null,
    isLoading: query.isLoading,
    isError: query.isError,
    requiresLogin,
    error: query.error instanceof ApiError ? query.error : null,
    refetch: () => void query.refetch(),
  }
  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>
}
