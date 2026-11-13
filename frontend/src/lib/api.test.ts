import { afterEach, describe, expect, it, vi } from "vitest"
import { api, ApiError, SESSION_EXPIRED_EVENT } from "./api"

afterEach(() => vi.unstubAllGlobals())
describe("cliente API", () => {
  it("envía la cookie y CSRF fresco antes de una mutación", async () => {
    const fetch = vi.fn().mockResolvedValueOnce(new Response(JSON.stringify({ headerName: "X-CSRF-TOKEN", token: "test-csrf" })))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
    vi.stubGlobal("fetch", fetch)
    await api("/example", { method: "POST", body: { name: "prueba" } })
    expect(fetch).toHaveBeenCalledTimes(2)
    expect(fetch.mock.calls[1][1]).toMatchObject({ credentials: "same-origin", method: "POST", headers: { "X-CSRF-TOKEN": "test-csrf" } })
  })
  it("preserva un 401 no JSON y notifica sesión expirada", async () => {
    const listener = vi.fn()
    window.addEventListener(SESSION_EXPIRED_EVENT, listener)
    vi.stubGlobal("fetch", vi.fn(async () => new Response("Unauthorized", { status: 401 })))
    try {
      await expect(api("/example")).rejects.toMatchObject({ status: 401 })
      expect(listener).toHaveBeenCalledTimes(1)
    } finally { window.removeEventListener(SESSION_EXPIRED_EVENT, listener) }
  })
  it("convierte las respuestas snake_case del backend a camelCase", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => new Response(JSON.stringify({
      organization_id: "org-1",
      display_name: "Ana Admin",
      grants: [{ role: "ADMINISTRADOR", center_id: null, permission: "inventory.read" }],
    }))))
    await expect(api("/me")).resolves.toEqual({
      organizationId: "org-1",
      displayName: "Ana Admin",
      grants: [{ role: "ADMINISTRADOR", centerId: null, permission: "inventory.read" }],
    })
  })
  it("convierte los cuerpos camelCase a snake_case antes de enviarlos", async () => {
    const fetch = vi.fn().mockResolvedValueOnce(new Response(JSON.stringify({ headerName: "X-CSRF-TOKEN", token: "test" })))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
    vi.stubGlobal("fetch", fetch)
    await api("/users/user-1/role-assignments", {
      method: "POST",
      body: { roleId: "role-1", centerIds: ["center-1"], scope: { scopeKind: "CENTROS" } },
    })
    const init = fetch.mock.calls[1][1] as RequestInit
    expect(JSON.parse(String(init.body))).toEqual({
      role_id: "role-1",
      center_ids: ["center-1"],
      scope: { scope_kind: "CENTROS" },
    })
  })
  it("no reenvía automáticamente una mutación denegada", async () => {
    const fetch = vi.fn().mockResolvedValueOnce(new Response(JSON.stringify({ headerName: "X-CSRF-TOKEN", token: "test" })))
      .mockResolvedValueOnce(new Response("Forbidden", { status: 403 }))
    vi.stubGlobal("fetch", fetch)
    await expect(api("/example", { method: "DELETE" })).rejects.toBeInstanceOf(ApiError)
    expect(fetch).toHaveBeenCalledTimes(2)
  })
})
