import { useState } from "react"
import { Activity, Boxes } from "lucide-react"
import { Button } from "@/components/ui/button"

export function App() {
  const [status, setStatus] = useState("Sin comprobar")
  const [checking, setChecking] = useState(false)

  async function checkHealth() {
    setChecking(true)
    try {
      const response = await fetch("/actuator/health/readiness", { signal: AbortSignal.timeout(10000) })
      const result: { status?: string } = await response.json()
      setStatus(response.ok && result.status === "UP" ? "Servicios disponibles" : "Dependencias no disponibles")
    } catch {
      setStatus("No se pudo conectar con el backend")
    } finally {
      setChecking(false)
    }
  }

  return (
    <main className="min-h-svh bg-background px-6 py-16 text-foreground sm:py-24">
      <section className="mx-auto max-w-2xl rounded-xl border bg-card p-8 shadow-sm sm:p-12" aria-labelledby="title">
        <Boxes className="mb-6 size-9 text-primary" aria-hidden="true" />
        <p className="mb-3 text-sm font-medium text-muted-foreground">Inventario Tecnológico Multicentro</p>
        <h1 id="title" className="text-3xl font-semibold tracking-tight">Entorno de desarrollo</h1>
        <p className="mt-4 leading-7 text-muted-foreground">Estructura inicial preparada. Los módulos funcionales se incorporarán en las siguientes fases.</p>
        <div className="mt-8 flex flex-wrap items-center gap-4">
          <Button onClick={checkHealth} disabled={checking}>
            <Activity aria-hidden="true" />{checking ? "Comprobando…" : "Comprobar servicios"}
          </Button>
          <p role="status" aria-live="polite" className="text-sm text-muted-foreground">{status}</p>
        </div>
      </section>
    </main>
  )
}
