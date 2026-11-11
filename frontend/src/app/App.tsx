import { useState } from "react"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { BrowserRouter, Route, Routes } from "react-router-dom"
import { AppLayout } from "@/components/layout/AppLayout"
import { RequireAccess } from "@/components/RequireAccess"
import { LoadingState } from "@/components/PageState"
import { SessionProvider } from "@/lib/session"
import { useSession } from "@/lib/session-context"
import { DashboardPage } from "@/features/dashboard/DashboardPage"
import { UsersPage } from "@/features/access/UsersPage"
import { UserDetailPage } from "@/features/access/UserDetailPage"
import { RolesPage } from "@/features/access/RolesPage"
import { MatricesPage } from "@/features/access/MatricesPage"
import { MyMatricesPage } from "@/features/access/MyMatricesPage"
import { AuditPage } from "@/features/access/AuditPage"
import { OrganizationPage } from "@/features/organization/OrganizationPage"
import { LoginPage } from "@/features/auth/LoginPage"
import { NotFoundPage } from "@/features/NotFoundPage"

function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: 1, refetchOnWindowFocus: false, staleTime: 30_000 },
    },
  })
}

function Shell() {
  const { isLoading, requiresLogin } = useSession()
  if (isLoading) {
    return (
      <div className="flex min-h-svh items-center justify-center">
        <LoadingState label="Verificando sesión…" />
      </div>
    )
  }
  if (requiresLogin) {
    return (
      <Routes>
        <Route path="*" element={<LoginPage />} />
      </Routes>
    )
  }
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<DashboardPage />} />
        <Route path="mis-matrices" element={<MyMatricesPage />} />
        <Route path="organization" element={<RequireAccess permission="organization.read"><OrganizationPage /></RequireAccess>} />
        <Route path="access/users" element={<RequireAccess permission="access.users.read"><UsersPage /></RequireAccess>} />
        <Route path="access/users/:id" element={<RequireAccess permission="access.users.read"><UserDetailPage /></RequireAccess>} />
        <Route path="access/roles" element={<RequireAccess permission="access.roles.read"><RolesPage /></RequireAccess>} />
        <Route path="access/matrices" element={<RequireAccess permission="access.matrices.read"><MatricesPage /></RequireAccess>} />
        <Route path="access/audit" element={<RequireAccess permission="access.audit.read"><AuditPage /></RequireAccess>} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}

export function App({ queryClient }: { queryClient?: QueryClient } = {}) {
  const [defaultClient] = useState(createQueryClient)
  return (
    <QueryClientProvider client={queryClient ?? defaultClient}>
      <SessionProvider>
        <BrowserRouter>
          <Shell />
        </BrowserRouter>
      </SessionProvider>
    </QueryClientProvider>
  )
}
