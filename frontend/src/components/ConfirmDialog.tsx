import { useId, useState, type ReactNode } from "react"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

interface ConfirmDialogProps {
  trigger: ReactNode
  title: string
  description?: string
  confirmLabel?: string
  destructive?: boolean
  /** Si se define, exige escribir este texto para habilitar la confirmación. */
  requireText?: string
  onConfirm: () => void | Promise<unknown>
}

/** Diálogo de confirmación accesible; opcionalmente exige teclear una frase. */
export function ConfirmDialog({
  trigger,
  title,
  description,
  confirmLabel = "Confirmar",
  destructive = false,
  requireText,
  onConfirm,
}: ConfirmDialogProps) {
  const [open, setOpen] = useState(false)
  const [typed, setTyped] = useState("")
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState("")
  const inputId = useId()

  const ready = !requireText || typed === requireText

  async function confirm() {
    if (busy || !ready) return
    setError("")
    setBusy(true)
    try {
      await onConfirm()
      setOpen(false)
      setTyped("")
    } catch {
      setError("No se pudo completar la operación. Revise la información e inténtelo nuevamente.")
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={(v) => { if (busy) return; setOpen(v); if (!v) { setTyped(""); setError("") } }}>
      <DialogTrigger asChild>{trigger}</DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>{description ?? "Confirme si desea continuar con esta operación."}</DialogDescription>
        </DialogHeader>
        {requireText && (
          <div className="space-y-2">
            <Label htmlFor={inputId}>Escriba «{requireText}» para confirmar</Label>
            <Input id={inputId} value={typed} onChange={(e) => setTyped(e.target.value)} autoComplete="off" />
          </div>
        )}
        {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
        <DialogFooter>
          <Button variant="outline" onClick={() => setOpen(false)} disabled={busy}>
            Cancelar
          </Button>
          <Button variant={destructive ? "destructive" : "default"} onClick={confirm} disabled={!ready || busy}>
            {busy ? "Procesando…" : confirmLabel}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
