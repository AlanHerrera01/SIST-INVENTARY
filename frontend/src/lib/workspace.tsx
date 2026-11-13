import { createContext, useContext, useState, type ReactNode } from "react"
import { useSession } from "@/lib/session-context"

const WorkspaceContext = createContext<{ centerId: string; centers: string[]; selectCenter: (id: string) => void }>({ centerId: "", centers: [], selectCenter: () => {} })

export function WorkspaceProvider({ children }: { children: ReactNode }) {
  const { profile } = useSession()
  const [selected, selectCenter] = useState("")
  const centers = [...new Set(profile?.grants.filter((g) => g.scope === "CENTROS" && g.centerId).map((g) => g.centerId as string) ?? [])].sort()
  // A revoked center must disappear immediately, including from the current selection.
  const centerId = centers.includes(selected) ? selected : ""
  return <WorkspaceContext.Provider value={{ centerId, centers, selectCenter }}>{children}</WorkspaceContext.Provider>
}

// Kept beside the provider because this is a single, local UI context.
// eslint-disable-next-line react-refresh/only-export-components
export function useWorkspace() { return useContext(WorkspaceContext) }
