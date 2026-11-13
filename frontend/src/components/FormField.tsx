import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import type { ComponentProps } from "react"

/** Compatible with react-hook-form register/ref; links validation to its input. */
export function FormField({ id, label, error, hint, ...input }: ComponentProps<"input"> & {
  id: string; label: string; error?: string; hint?: string
}) {
  const description = [hint ? `${id}-hint` : "", error ? `${id}-error` : ""].filter(Boolean).join(" ")
  return <div className="space-y-1.5">
    <Label htmlFor={id}>{label}</Label>
    <Input {...input} id={id} aria-invalid={!!error} aria-describedby={description || undefined} />
    {hint && <p id={`${id}-hint`} className="text-xs text-muted-foreground">{hint}</p>}
    {error && <p id={`${id}-error`} role="alert" className="text-sm text-destructive">{error}</p>}
  </div>
}
