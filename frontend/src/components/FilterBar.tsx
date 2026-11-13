import type { ReactNode } from "react"
import { Search } from "lucide-react"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"

export function FilterBar({ label, value, onChange, onSearch, children }: {
  label: string; value: string; onChange: (value: string) => void; onSearch: () => void; children?: ReactNode
}) {
  return <div className="mb-4 flex flex-wrap items-center gap-3">
    <form role="search" aria-label={label} className="flex min-w-0 flex-1 gap-2 sm:max-w-md" onSubmit={(event) => { event.preventDefault(); onSearch() }}>
      <Input type="search" aria-label={label} placeholder={label} value={value} onChange={(event) => onChange(event.target.value)} />
      <Button variant="outline" size="icon" type="submit" aria-label="Buscar"><Search aria-hidden="true" /></Button>
    </form>
    {children}
  </div>
}
