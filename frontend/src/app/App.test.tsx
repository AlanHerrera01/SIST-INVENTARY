import { render, screen, waitFor, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { QueryClient } from "@tanstack/react-query"
import { afterEach, describe, expect, it, vi } from "vitest"
import { App } from "@/app/App"
import type { Profile } from "@/lib/session-context"
import { SESSION_EXPIRED_EVENT } from "@/lib/api"

const ADMIN: Profile = {
  id: "u1",
  organizationId: "org1",
  displayName: "Ana Admin",
  grants: [
    "access.users.read", "access.users.manage", "access.roles.read", "access.roles.manage",
    "access.assignments.manage", "access.matrices.read", "access.matrices.manage",
    "access.audit.read", "organization.read",
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
    if (url === "/api/v1/me") return profile ? json(profile) : json({}, 401)
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

  it("muestra los módulos del menú al administrador", async () => {
    mockApi(ADMIN)
    renderApp("/")

    await waitFor(() => expect(screen.getAllByText("Ana Admin")[0]).toBeInTheDocument())
    const nav = within(screen.getByRole("navigation", { name: "Navegación principal" }))
    expect(nav.getByRole("link", { name: "Configuración" })).toBeInTheDocument()
    expect(nav.getByRole("link", { name: /auditoría/i })).toBeInTheDocument()
    expect(nav.getByRole("link", { name: /actas y firmas/i })).toBeInTheDocument()
    expect(nav.queryByRole("link", { name: /usuarios/i })).not.toBeInTheDocument()
    expect(nav.queryByRole("link", { name: /roles y permisos/i })).not.toBeInTheDocument()
    expect(nav.getAllByText("Pronto")).toHaveLength(3)
  })

  it("agrupa la administración en pestañas dentro de Configuración", async () => {
    mockApi(ADMIN)
    renderApp("/configuracion")

    expect(await screen.findByRole("tab", { name: "Usuarios" })).toBeInTheDocument()
    expect(screen.getByRole("tab", { name: "Roles y permisos" })).toBeInTheDocument()
    expect(screen.getByRole("tab", { name: "Matrices de firma" })).toBeInTheDocument()
    expect(screen.getByRole("tab", { name: "Ciudades, sedes y centros" })).toBeInTheDocument()
    expect(screen.getByRole("tab", { name: "Mi acceso" })).toBeInTheDocument()
  })

  it("oculta la administración a un perfil de jefatura de centro", async () => {
    mockApi(JEFATURA)
    renderApp("/")

    await waitFor(() => expect(screen.getAllByText("Jefe Uno")[0]).toBeInTheDocument())
    const nav = within(screen.getByRole("navigation", { name: "Navegación principal" }))
    expect(nav.queryByRole("link", { name: /usuarios/i })).not.toBeInTheDocument()
    expect(nav.queryByRole("link", { name: /roles y permisos/i })).not.toBeInTheDocument()
    expect(nav.queryByRole("link", { name: /auditoría/i })).not.toBeInTheDocument()
    expect(nav.queryByRole("link", { name: "Configuración" })).not.toBeInTheDocument()
    expect(nav.getByRole("link", { name: /actas y firmas/i })).toBeInTheDocument()
    expect(nav.getByRole("link", { name: /dashboard general/i })).toBeInTheDocument()
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
      if (url === "/api/v1/me") return json(ADMIN)
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

    await waitFor(() => expect(screen.getAllByText("Ana Admin")[0]).toBeInTheDocument())
    await userEvent.click(screen.getByRole("button", { name: /comprobar servicios/i }))

    await waitFor(() => expect(screen.getByText("Backend disponible")).toBeInTheDocument())
  })
})

describe("estructura visual y sesión — Fase 05", () => {
  afterEach(() => vi.unstubAllGlobals())

  it("muestra carga mientras se verifica la sesión", () => {
    vi.stubGlobal("fetch", vi.fn(() => new Promise(() => {})))
    renderApp()
    expect(screen.getByRole("status")).toHaveTextContent("Verificando sesión")
    expect(screen.queryByRole("navigation")).not.toBeInTheDocument()
  })

  it("no muestra el layout privado cuando falla la verificación de sesión", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => new Response("Servicio caído", { status: 503 })))
    renderApp()
    expect(await screen.findByRole("heading", { name: "No se pudo verificar su acceso" })).toBeInTheDocument()
    expect(screen.queryByRole("navigation", { name: "Navegación principal" })).not.toBeInTheDocument()
    expect(screen.getByRole("button", { name: "Reintentar" })).toBeInTheDocument()
  })

  it("rechaza un perfil con estructura inválida", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => new Response(JSON.stringify({ displayName: "Incompleto" }))))
    renderApp()
    expect(await screen.findByText("No se pudo verificar el perfil de acceso.")).toBeInTheDocument()
  })

  it("un permiso de usuarios limitado a centro no habilita la ruta global", async () => {
    mockApi({ ...JEFATURA, grants: [{ role: "JEFATURA", scope: "CENTROS", centerId: "c1", permission: "access.users.read" }] })
    renderApp("/access/users")
    expect(await screen.findByText(/no tiene acceso a esta sección/i)).toBeInTheDocument()
    expect(screen.queryByRole("button", { name: /nuevo usuario/i })).not.toBeInTheDocument()
  })

  it("muestra el acceso existente cuando el servidor responde 401", async () => {
    mockApi(null)
    renderApp("/access/users")
    expect(await screen.findByRole("heading", { name: "Acceso Seguro al Sistema" })).toBeInTheDocument()
    expect(screen.queryByRole("navigation", { name: "Navegación principal" })).not.toBeInTheDocument()
  })

  it("no presenta una consulta fallida de matrices como un total cero", async () => {
    vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL) => String(input) === "/api/v1/me"
      ? new Response(JSON.stringify(ADMIN)) : new Response("Unavailable", { status: 503 })))
    renderApp()
    expect(await screen.findByText("No se pudo cargar la información", {}, { timeout: 4000 })).toBeInTheDocument()
    expect(screen.queryByText("0", { exact: true })).not.toBeInTheDocument()
  })

  it("diferencia el espacio técnico y filtra navegación y permisos por centro", async () => {
    mockApi({ ...JEFATURA, grants: [
      { role: "TECNICO_TI", scope: "CENTROS", centerId: "c1", permission: "organization.read" },
      { role: "TECNICO_TI", scope: "CENTROS", centerId: "c2", permission: "inventory.read" },
    ] })
    renderApp("/mi-acceso")
    const selector = await screen.findByRole("combobox", { name: "Centro de trabajo" })
    expect(screen.getAllByText("Espacio técnico").length).toBeGreaterThan(0)
    expect(screen.getByRole("table")).toHaveTextContent("organization.read")
    const nav = within(screen.getByRole("navigation", { name: "Navegación principal" }))
    expect(nav.getByRole("link", { name: "Configuración" })).toBeInTheDocument()
    await userEvent.selectOptions(selector, "c2")
    expect(nav.queryByRole("link", { name: "Configuración" })).not.toBeInTheDocument()
    expect(screen.getByRole("table")).not.toHaveTextContent("organization.read")
    expect(screen.getByRole("table")).toHaveTextContent("inventory.read")
    expect(within(selector).queryByRole("option", { name: "Centro c3" })).not.toBeInTheDocument()
  })

  it("renderiza consulta y el estado vacío sin inventar permisos globales", async () => {
    mockApi({ ...JEFATURA, grants: [] })
    renderApp("/mi-acceso")
    expect(await screen.findByText("Sin resultados")).toBeInTheDocument()
    expect(screen.getAllByText("Espacio de consulta").length).toBeGreaterThan(0)
    expect(screen.queryByRole("combobox", { name: "Centro de trabajo" })).not.toBeInTheDocument()
  })

  it("permite buscar permisos y muestra vacío al no encontrar resultados", async () => {
    mockApi(ADMIN)
    renderApp("/mi-acceso")
    await userEvent.type(await screen.findByRole("searchbox", { name: "Buscar rol o permiso" }), "inexistente")
    await userEvent.click(screen.getByRole("button", { name: "Buscar" }))
    expect(await screen.findByText("Sin resultados")).toBeInTheDocument()
  })

  it("navega, actualiza breadcrumbs y mueve el foco al contenido", async () => {
    mockApi(ADMIN)
    renderApp()
    const nav = within(await screen.findByRole("navigation", { name: "Navegación principal" }))
    await userEvent.click(nav.getByRole("link", { name: "Actas y Firmas" }))
    expect(await screen.findByRole("heading", { name: "Actas y Firmas" })).toBeInTheDocument()
    expect(screen.getByRole("main")).toHaveFocus()
    expect(document.title).toBe("Actas y Firmas | Inventario Multicentro")
    expect(within(screen.getByRole("navigation", { name: "Ruta de navegación" })).getByRole("link", { name: "Inicio" })).toHaveAttribute("href", "/")
  })

  it("cierra el menú móvil con Escape y devuelve foco al disparador", async () => {
    mockApi(ADMIN)
    renderApp()
    const trigger = await screen.findByRole("button", { name: "Abrir menú de navegación" })
    await userEvent.click(trigger)
    expect(await screen.findByRole("dialog", { name: "Navegación" })).toBeInTheDocument()
    await userEvent.keyboard("{Escape}")
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument())
    expect(trigger).toHaveFocus()
  })

  it("retira información privada al caducar la sesión", async () => {
    mockApi(ADMIN)
    renderApp()
    await screen.findByRole("navigation", { name: "Navegación principal" })
    window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))
    await waitFor(() => expect(screen.queryByRole("navigation", { name: "Navegación principal" })).not.toBeInTheDocument())
    expect(screen.queryByText("Ana Admin")).not.toBeInTheDocument()
  })

  it("busca solo secciones autorizadas y permite abrirlas desde la barra superior", async () => {
    mockApi(JEFATURA)
    renderApp()
    const search = await screen.findByRole("searchbox", { name: "Buscar sección" })
    await userEvent.type(search, "Usuarios")
    expect(screen.getByText("No se encontraron secciones")).toBeInTheDocument()
    await userEvent.clear(search)
    await userEvent.type(search, "Mi acceso")
    await userEvent.click(within(screen.getByRole("list", { name: "Resultados de navegación" })).getByRole("link", { name: "Mi acceso" }))
    expect(await screen.findByRole("heading", { name: "Mi acceso" })).toBeInTheDocument()
    expect(screen.queryByRole("list", { name: "Resultados de navegación" })).not.toBeInTheDocument()
  })

  it("colapsa el menú conservando nombres accesibles de los enlaces", async () => {
    mockApi(ADMIN)
    renderApp()
    await userEvent.click(await screen.findByRole("button", { name: "Colapsar menú" }))
    expect(screen.getByRole("button", { name: "Expandir menú" })).toHaveAttribute("aria-expanded", "false")
    const nav = within(screen.getByRole("navigation", { name: "Navegación principal" }))
    expect(nav.getByRole("link", { name: "Configuración" })).toBeInTheDocument()
    await userEvent.click(screen.getByRole("button", { name: "Expandir menú" }))
    expect(screen.getByRole("button", { name: "Colapsar menú" })).toHaveAttribute("aria-expanded", "true")
  })

  it("muestra 404 para rutas inexistentes", async () => {
    mockApi(ADMIN)
    renderApp("/ruta-inexistente")
    expect(await screen.findByText(/página no encontrada/i)).toBeInTheDocument()
  })
})
