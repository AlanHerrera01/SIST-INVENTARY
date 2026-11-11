import { Link } from "react-router-dom"
import { Button } from "@/components/ui/button"

export function NotFoundPage() {
  return (
    <div className="flex flex-col items-center gap-3 py-20 text-center">
      <h1 className="text-2xl font-semibold">Página no encontrada</h1>
      <p className="text-sm text-muted-foreground">La ruta solicitada no existe o fue movida.</p>
      <Button asChild variant="outline"><Link to="/">Volver al panel</Link></Button>
    </div>
  )
}
