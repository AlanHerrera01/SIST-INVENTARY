import { useState } from "react"
import { useQueryClient } from "@tanstack/react-query"
import { authApi } from "@/lib/api"

/** Cierre de sesión existente: POST con CSRF y redirección al proveedor. */
export function useLogout() {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState("")
  const client = useQueryClient()
  async function logout() {
    setBusy(true)
    setError("")
    try {
      const { logoutUrl } = await authApi<{ logoutUrl: string }>("/api/auth/logout", { method: "POST" })
      await client.cancelQueries()
      client.clear()
      window.location.assign(logoutUrl)
    } catch {
      setError("No se pudo cerrar la sesión. Inténtelo nuevamente.")
      setBusy(false)
    }
  }
  return { logout, busy, error }
}
