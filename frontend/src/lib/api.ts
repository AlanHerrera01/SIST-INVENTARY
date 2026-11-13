/** Same-origin BFF client: OIDC tokens remain on the server. */
export class ApiError extends Error {
  constructor(public readonly status: number, public readonly code: string, message: string) {
    super(message)
    this.name = "ApiError"
  }
}
export const SESSION_EXPIRED_EVENT = "inventory:session-expired"
export interface RequestOptions {
  method?: "GET" | "POST" | "PATCH" | "PUT" | "DELETE"
  body?: unknown
  params?: Record<string, string | number | boolean | undefined | null>
  signal?: AbortSignal
}
function notifyExpired(status: number) {
  if (status === 401) window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))
}
/**
 * El contrato del backend usa snake_case para el JSON de dominio y el frontend
 * usa camelCase. La conversión ocurre solo en el borde HTTP, de forma profunda,
 * para que los tipos de la interfaz no dependan del formato del transporte.
 */
const UNSAFE_KEYS = new Set(["__proto__", "constructor", "prototype"])
function snakeToCamel(key: string): string {
  return key.replace(/_+([a-z0-9])/g, (_, char: string) => char.toUpperCase())
}
function camelToSnake(key: string): string {
  return key.replace(/([A-Z])/g, (char) => `_${char.toLowerCase()}`)
}
function convertKeys(value: unknown, convert: (key: string) => string): unknown {
  if (Array.isArray(value)) return value.map((item) => convertKeys(item, convert))
  if (value === null || typeof value !== "object") return value
  const result: Record<string, unknown> = {}
  for (const [key, item] of Object.entries(value as Record<string, unknown>)) {
    if (UNSAFE_KEYS.has(key)) continue
    const name = convert(key)
    if (UNSAFE_KEYS.has(name)) continue
    result[name] = convertKeys(item, convert)
  }
  return result
}
async function request<T>(path: string, options: RequestOptions): Promise<T> {
  const { method = "GET", body, params, signal } = options
  const query = new URLSearchParams(Object.entries(params ?? {})
    .filter(([, value]) => value !== undefined && value !== null && value !== "")
    .map(([key, value]) => [key, String(value)]))
  const headers: Record<string, string> = { Accept: "application/json" }
  if (body !== undefined) headers["Content-Type"] = "application/json"
  if (method !== "GET") {
    // Fresh masked CSRF token per command. Never replay mutations automatically.
    const response = await fetch("/api/auth/csrf", { credentials: "same-origin", cache: "no-store", signal })
    notifyExpired(response.status)
    if (!response.ok) throw new ApiError(response.status, "CSRF_UNAVAILABLE", "No se pudo preparar la solicitud. Inténtelo nuevamente.")
    const csrf: unknown = await response.json()
    if (!csrf || typeof csrf !== "object" || !("headerName" in csrf) || !("token" in csrf)
      || typeof csrf.headerName !== "string" || typeof csrf.token !== "string")
      throw new ApiError(502, "INVALID_RESPONSE", "Respuesta de seguridad no válida.")
    headers[csrf.headerName] = csrf.token
  }
  const response = await fetch(path + (query.size ? `?${query}` : ""), {
    method, credentials: "same-origin", cache: "no-store", headers, signal,
    body: body === undefined ? undefined : JSON.stringify(convertKeys(body, camelToSnake)),
  })
  notifyExpired(response.status)
  if (response.status === 204) return undefined as T
  const text = await response.text()
  let payload: unknown = null
  try { payload = text ? convertKeys(JSON.parse(text), snakeToCamel) : null } catch {
    if (response.ok) throw new ApiError(502, "INVALID_RESPONSE", "El servicio devolvió una respuesta no válida.")
  }
  if (!response.ok) {
    const problem = payload && typeof payload === "object" ? payload as Record<string, unknown> : {}
    throw new ApiError(response.status, typeof problem.code === "string" ? problem.code : `HTTP_${response.status}`,
      typeof problem.detail === "string" ? problem.detail : response.status === 401
        ? "Su sesión ha caducado. Inicie sesión nuevamente." : `No se pudo completar la solicitud (${response.status}).`)
  }
  return payload as T
}
export function api<T>(path: string, options: RequestOptions = {}) { return request<T>(`/api/v1${path}`, options) }
export function authApi<T>(path: string, options: RequestOptions = {}) { return request<T>(path, options) }
export function apiPage<T>(path: string, params: RequestOptions["params"]) { return api<T>(path, { params }) }
