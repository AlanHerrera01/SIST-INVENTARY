/**
 * Cliente HTTP de la API del backend. Same-origin a través del proxy de Vite
 * (o del ingress en producción). Los POST/PATCH/PUT/DELETE envían el token
 * CSRF de Spring Security en la cabecera que el backend declara.
 */

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
  ) {
    super(message)
    this.name = "ApiError"
  }
}

interface ProblemDetails {
  status?: number
  title?: string
  detail?: string
  code?: string
}

let csrfCache: { headerName: string; token: string } | null = null

async function fetchCsrf(): Promise<{ headerName: string; token: string }> {
  const response = await fetch("/api/auth/csrf", { credentials: "same-origin" })
  if (!response.ok) throw new ApiError(response.status, "CSRF_UNAVAILABLE", "No se pudo obtener el token CSRF.")
  csrfCache = (await response.json()) as { headerName: string; token: string }
  return csrfCache
}

export interface RequestOptions {
  method?: "GET" | "POST" | "PATCH" | "PUT" | "DELETE"
  body?: unknown
  params?: Record<string, string | number | boolean | undefined | null>
}

export async function api<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = "GET", body, params } = options
  const query = params
    ? "?" + new URLSearchParams(
        Object.entries(params)
          .filter(([, v]) => v !== undefined && v !== null && v !== "")
          .map(([k, v]) => [k, String(v)]),
      ).toString()
    : ""

  const headers: Record<string, string> = { Accept: "application/json" }
  if (body !== undefined) headers["Content-Type"] = "application/json"

  if (method !== "GET") {
    const csrf = csrfCache ?? (await fetchCsrf())
    headers[csrf.headerName] = csrf.token
  }

  const response = await fetch(`/api/v1${path}${query}`, {
    method,
    credentials: "same-origin",
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (response.status === 204) return undefined as T

  const text = await response.text()
  const payload = text ? (JSON.parse(text) as ProblemDetails & T) : null

  if (!response.ok) {
    if (response.status === 403 && csrfCache) csrfCache = null // token caducado: reintentar una vez
    throw new ApiError(
      response.status,
      payload?.code ?? `HTTP_${response.status}`,
      payload?.detail ?? payload?.title ?? `Error ${response.status}`,
    )
  }
  return payload as T
}

/** Petición a rutas de autenticación fuera de {@code /api/v1} (p. ej. logout). */
export async function authApi<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = "GET", body } = options
  const headers: Record<string, string> = { Accept: "application/json" }
  if (body !== undefined) headers["Content-Type"] = "application/json"
  if (method !== "GET") {
    const csrf = csrfCache ?? (await fetchCsrf())
    headers[csrf.headerName] = csrf.token
  }
  const response = await fetch(path, {
    method,
    credentials: "same-origin",
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const text = await response.text()
  const payload = text ? (JSON.parse(text) as ProblemDetails & T) : null
  if (!response.ok) {
    throw new ApiError(response.status, payload?.code ?? `HTTP_${response.status}`, payload?.detail ?? `Error ${response.status}`)
  }
  return payload as T
}

/** Igual que {@link api} pero conserva el cursor de paginación. */
export async function apiPage<T>(path: string, params: RequestOptions["params"]): Promise<T> {
  return api<T>(path, { params })
}

