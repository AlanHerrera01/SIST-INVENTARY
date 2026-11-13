import type { ReactNode } from "react"
import { EmptyState, ErrorState, LoadingState } from "@/components/PageState"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"

export interface Column<T> { id: string; title: string; render: (row: T) => ReactNode }
export function DataTable<T>({ caption, rows, columns, rowKey, loading, error, onRetry, emptyTitle = "Sin resultados" }: {
  caption: string; rows: T[]; columns: Column<T>[]; rowKey: (row: T) => string
  loading?: boolean; error?: unknown; onRetry?: () => void; emptyTitle?: string
}) {
  if (loading) return <LoadingState />
  if (error) return <ErrorState error={error} onRetry={onRetry} />
  if (!rows.length) return <EmptyState title={emptyTitle} description="No hay registros que coincidan con la selección actual." />
  return <div className="rounded-xl border bg-card">
    <Table>
      <caption className="sr-only">{caption}</caption>
      <TableHeader><TableRow>{columns.map((column) => <TableHead scope="col" key={column.id}>{column.title}</TableHead>)}</TableRow></TableHeader>
      <TableBody>{rows.map((row) => <TableRow key={rowKey(row)}>{columns.map((column) => <TableCell key={column.id}>{column.render(row)}</TableCell>)}</TableRow>)}</TableBody>
    </Table>
  </div>
}
