import { lazy, Suspense, useState } from "react"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { BrowserRouter, Route, Routes } from "react-router-dom"
import { AppLayout } from "@/components/layout/AppLayout"
import { RequireAccess } from "@/components/RequireAccess"
import { ErrorState, LoadingState } from "@/components/PageState"
import { WorkspaceProvider } from "@/lib/workspace"
import { MyAccessPage } from "@/features/access/MyAccessPage"
import { SessionProvider } from "@/lib/session"
import { useSession } from "@/lib/session-context"
import { DashboardPage } from "@/features/dashboard/DashboardPage"
const UsersPage = lazy(() => import("@/features/access/UsersPage").then((m) => ({ default: m.UsersPage })))
const UserDetailPage = lazy(() => import("@/features/access/UserDetailPage").then((m) => ({ default: m.UserDetailPage })))
const RolesPage = lazy(() => import("@/features/access/RolesPage").then((m) => ({ default: m.RolesPage })))
const MatricesPage = lazy(() => import("@/features/access/MatricesPage").then((m) => ({ default: m.MatricesPage })))
import { MyMatricesPage } from "@/features/access/MyMatricesPage"
const AuditPage = lazy(() => import("@/features/access/AuditPage").then((m) => ({ default: m.AuditPage })))
const OrganizationPage = lazy(() => import("@/features/organization/OrganizationPage").then((m) => ({ default: m.OrganizationPage })))
const ConfiguracionPage = lazy(() => import("@/features/config/ConfiguracionPage").then((m) => ({ default: m.ConfiguracionPage })))
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
  const { isLoading, requiresLogin, isError, error, refetch } = useSession()
  if (isLoading) {
    return (
      <div className="flex min-h-svh items-center justify-center">
        <LoadingState label="Verificando sesión…" />
      </div>
    )
  }
  if (isError) return <main className="mx-auto max-w-xl p-6"><h1 className="text-xl font-semibold">No se pudo verificar su acceso</h1><ErrorState error={error} onRetry={refetch} /></main>
  if (requiresLogin) {
    return (
      <Routes>
        <Route path="*" element={<LoginPage />} />
      </Routes>
    )
  }
  return (
    <Routes>
      <Route element={<WorkspaceProvider><AppLayout /></WorkspaceProvider>}>
        <Route index element={<DashboardPage />} />
        <Route path="mi-acceso" element={<MyAccessPage />} />
        <Route path="mis-matrices" element={<MyMatricesPage />} />
        <Route path="configuracion" element={<ConfiguracionPage />} />
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
          <Suspense fallback={<main className="p-6"><LoadingState label="Cargando sección…" /></main>}><Shell /></Suspense>
        </BrowserRouter>
      </SessionProvider>
    </QueryClientProvider>
  )
}
