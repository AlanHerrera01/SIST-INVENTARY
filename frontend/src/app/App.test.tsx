import { render, screen, waitFor, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { QueryClient } from "@tanstack/react-query"
import { afterEach, describe, expect, it, vi } from "vitest"
import { App } from "@/app/App"
import type { Profile } from "@/lib/session-context"

const ADMIN: Profile = {
  id: "u1",
  organizationId: "org1",
  displayName: "Ana Admin",
  grants: [
    "access.users.read", "access.users.manage", "access.roles.read", "access.roles.manage",
    "access.assignments.manage", "access.matrices.read", "access.matrices.manage",
    "access.audit.read",
  ].map((permission) => ({ role: "ADMINISTRADOR", scope: "GLOBAL", centerId: null, permission })),
}

const JEFATURA: Profile = {
  id: "u2",
  organizationId: "org1",
  displayName: "Jefe Uno",
  grants: [
    { role: "JEFATURA", scope: "CENTROS", centerId: "c1", permission: "inventory.read" },
    { role: "JEFATURA", scope: "CENTROS", centerId: "c1", permission: "signatures.sign" },
  ],
}

function renderApp(entry = "/") {
  window.history.pushState({}, "", entry)
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(<App queryClient={client} />)
}

function mockApi(profile: Profile | null, extra: Record<string, unknown> = {}) {
  vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL) => {
    const url = String(input)
    const json = (body: unknown, status = 200) =>
      Promise.resolve({
        ok: status < 400,
        status,
        json: async () => body,
        text: async () => JSON.stringify(body),
      } as Response)
    if (url.includes("/api/v1/me")) return json(profile)
    if (url.includes("/api/v1/me/matrices")) return json([])
    if (url.includes("/api/v1/users")) return json([])
    if (url.includes("/api/v1/roles")) return json([])
    if (url.includes("/api/v1/permissions")) return json([])
    if (url.includes("/api/v1/signature-matrices")) return json([])
    if (url.includes("/api/v1/audit-events")) return json({ items: [], nextCursor: null })
    if (url.includes("/actuator/health")) return json({ status: "UP" })
    return json(extra[url] ?? {}, 404)
  }))
}

describe("navegación por permisos", () => {
  afterEach(() => vi.unstubAllGlobals())

  it("muestra todos los módulos de administración al administrador", async () => {
    mockApi(ADMIN)
    renderApp("/")

    await waitFor(() => expect(screen.getByText("Ana Admin")).toBeInTheDocument())
    const nav = within(screen.getByRole("navigation", { name: "Navegación principal" }))
    expect(nav.getByRole("link", { name: /usuarios/i })).toBeInTheDocument()
    expect(nav.getByRole("link", { name: /roles y permisos/i })).toBeInTheDocument()
    expect(nav.getByRole("link", { name: /matrices de firma/i })).toBeInTheDocument()
    expect(nav.getByRole("link", { name: /auditoría/i })).toBeInTheDocument()
    expect(nav.getByRole("link", { name: /mis matrices/i })).toBeInTheDocument()
  })

  it("oculta la administración a un perfil de jefatura de centro", async () => {
    mockApi(JEFATURA)
    renderApp("/")

    await waitFor(() => expect(screen.getByText("Jefe Uno")).toBeInTheDocument())
    const nav = within(screen.getByRole("navigation", { name: "Navegación principal" }))
    expect(nav.queryByRole("link", { name: /usuarios/i })).not.toBeInTheDocument()
    expect(nav.queryByRole("link", { name: /roles y permisos/i })).not.toBeInTheDocument()
    expect(nav.queryByRole("link", { name: /auditoría/i })).not.toBeInTheDocument()
    expect(nav.getByRole("link", { name: /mis matrices/i })).toBeInTheDocument()
    expect(nav.getByRole("link", { name: /panel/i })).toBeInTheDocument()
  })

  it("deniega la página de usuarios aunque se navegue por URL directa", async () => {
    mockApi(JEFATURA)
    renderApp("/access/users")

    await waitFor(() =>
      expect(screen.getByText(/no tiene acceso a esta sección/i)).toBeInTheDocument(),
    )
  })

  it("muestra los usuarios al administrador en la ruta de usuarios", async () => {
    mockApi(ADMIN)
    renderApp("/access/users")

    await waitFor(() => expect(screen.getByRole("heading", { name: "Usuarios" })).toBeInTheDocument())
    expect(screen.getByRole("button", { name: /nuevo usuario/i })).toBeInTheDocument()
    const crumbs = within(screen.getByRole("navigation", { name: "Ruta de navegación" }))
    expect(crumbs.getByRole("link", { name: "Usuarios" })).toBeInTheDocument() // breadcrumb
  })

  it("muestra acceso denegado con 403 del backend", async () => {
    vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input)
      const json = (body: unknown, status = 200) =>
        Promise.resolve({
          ok: status < 400,
          status,
          json: async () => body,
          text: async () => JSON.stringify(body),
        } as Response)
      if (url.includes("/api/v1/me")) return json(ADMIN)
      if (url.includes("/api/v1/me/matrices")) return json([])
      if (url.includes("/api/v1/users"))
        return json({ status: 403, code: "FORBIDDEN", detail: "Sin permiso." }, 403)
      return json({})
    }))
    renderApp("/access/users")

    await waitFor(() =>
      expect(screen.getByText(/acceso denegado|no tiene acceso/i)).toBeInTheDocument(),
    )
  })
})

describe("health check del panel", () => {
  afterEach(() => vi.unstubAllGlobals())

  it("informa disponibilidad cuando readiness responde UP", async () => {
    mockApi(ADMIN)
    renderApp("/")

    await waitFor(() => expect(screen.getByText("Ana Admin")).toBeInTheDocument())
    await userEvent.click(screen.getByRole("button", { name: /comprobar servicios/i }))

    await waitFor(() => expect(screen.getByText("Backend disponible")).toBeInTheDocument())
  })
})
