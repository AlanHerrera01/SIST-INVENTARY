import { useEffect, useRef, useState } from "react"
import { Link } from "react-router-dom"
import { Search } from "lucide-react"
import { NAV_LINKS, canVisit } from "@/lib/nav"
import { useSession } from "@/lib/session-context"
import { useWorkspace } from "@/lib/workspace"

/** Searches only implemented destinations visible in the current scope. */
export function NavigationSearch() {
  const { profile } = useSession()
  const { centerId } = useWorkspace()
  const [query, setQuery] = useState("")
  const [open, setOpen] = useState(false)
  const input = useRef<HTMLInputElement>(null)
  const matches = NAV_LINKS.filter((item) => !!item.to && canVisit(profile, item, centerId)
    && item.label.toLocaleLowerCase("es").includes(query.trim().toLocaleLowerCase("es")))
  useEffect(() => {
    const shortcut = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") {
        event.preventDefault(); input.current?.focus(); setOpen(true)
      }
    }
    window.addEventListener("keydown", shortcut)
    return () => window.removeEventListener("keydown", shortcut)
  }, [])
  return <div className="relative min-w-0 flex-1 sm:max-w-md" onBlur={(event) => {
    if (!event.currentTarget.contains(event.relatedTarget)) setOpen(false)
  }} onKeyDown={(event) => { if (event.key === "Escape") { setOpen(false); input.current?.focus() } }}>
    <Search className="pointer-events-none absolute left-3.5 top-3 size-4 text-outline" aria-hidden="true" />
    <input ref={input} type="search" aria-label="Buscar sección" aria-controls={open ? "navigation-results" : undefined}
      className="h-10 w-full rounded-lg border border-line bg-surface-container-lowest pl-10 pr-3 text-xs text-on-surface shadow-card placeholder:text-on-surface-variant focus:border-secondary focus:outline-none focus:ring-2 focus:ring-secondary/30 sm:pr-16"
      placeholder="Buscar una sección…" value={query} onFocus={() => setOpen(true)} onChange={(event) => { setQuery(event.target.value); setOpen(true) }} />
    <kbd aria-hidden="true" className="pointer-events-none absolute right-2 top-2.5 hidden rounded border border-line bg-surface-container-highest px-1.5 py-0.5 text-[10px] font-semibold tracking-wide text-on-surface-variant sm:block">Ctrl K</kbd>
    {open && <div id="navigation-results" className="absolute inset-x-0 top-11 z-50 rounded-xl border border-line bg-surface-container-lowest p-2 shadow-pop">
      <p role="status" className="px-2 py-1 text-xs text-on-surface-variant">{matches.length ? "Secciones disponibles" : "No se encontraron secciones"}</p>
      <ul aria-label="Resultados de navegación">{matches.map((item) => <li key={item.to}><Link to={item.to} onClick={() => { setOpen(false); setQuery("") }} className="flex min-h-11 items-center gap-2 rounded-lg px-2 text-sm text-on-surface-variant hover:bg-surface-container-low hover:text-on-surface"><item.icon className="size-4" aria-hidden="true" />{item.label}</Link></li>)}</ul>
    </div>}
  </div>
}
