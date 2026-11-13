import { createContext, useContext } from "react"
import type { ApiError } from "@/lib/api"

export interface Grant {
  role: string
  scope: "GLOBAL" | "CENTROS" | string
  centerId: string | null
  permission: string | null
}

export interface Profile {
  id: string
  organizationId: string
  displayName: string
  grants: Grant[]
}

export interface SessionState {
  profile: Profile | null
  isLoading: boolean
  isError: boolean
  requiresLogin: boolean
  error: ApiError | null
  refetch: () => void
}

export const SessionContext = createContext<SessionState>({
  profile: null,
  isLoading: true,
  isError: false,
  requiresLogin: false,
  error: null,
  refetch: () => {},
})

export function useSession(): SessionState {
  return useContext(SessionContext)
}
