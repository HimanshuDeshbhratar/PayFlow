import { cn } from '@/lib/utils'
import { EmptyState } from './EmptyState'
import { SkeletonTable } from './Skeleton'

export interface Column<T> {
  key: string
  header: string
  className?: string
  render: (row: T) => React.ReactNode
}

interface DataTableProps<T> {
  columns: Column<T>[]
  data: T[]
  loading?: boolean
  emptyTitle?: string
  emptyDescription?: string
  onRowClick?: (row: T) => void
  keyExtractor: (row: T) => string
  className?: string
}

export function DataTable<T>({
  columns,
  data,
  loading,
  emptyTitle = 'No results',
  emptyDescription = 'Try adjusting filters or check back later.',
  onRowClick,
  keyExtractor,
  className,
}: DataTableProps<T>) {
  if (loading) {
    return (
      <div className={cn('glass overflow-hidden', className)}>
        <SkeletonTable />
      </div>
    )
  }

  if (!data.length) {
    return (
      <div className={cn('glass', className)}>
        <EmptyState title={emptyTitle} description={emptyDescription} />
      </div>
    )
  }

  return (
    <div className={cn('glass overflow-hidden', className)}>
      <div className="overflow-x-auto scrollbar-thin">
        <table className="w-full min-w-[640px] text-left text-sm">
          <thead>
            <tr className="border-b border-border text-xs uppercase tracking-wider text-muted">
              {columns.map((col) => (
                <th key={col.key} className={cn('px-4 py-3 font-medium', col.className)}>
                  {col.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {data.map((row) => (
              <tr
                key={keyExtractor(row)}
                className={cn(
                  'border-b border-border/60 transition last:border-0',
                  onRowClick && 'cursor-pointer hover:bg-white/[0.03]',
                )}
                onClick={() => onRowClick?.(row)}
              >
                {columns.map((col) => (
                  <td key={col.key} className={cn('px-4 py-3.5 align-middle', col.className)}>
                    {col.render(row)}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
