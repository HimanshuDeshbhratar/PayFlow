import { cn } from '@/lib/utils'

export interface TimelineItem {
  id: string
  title: string
  description?: string | null
  timestamp?: string
  status?: 'success' | 'danger' | 'warning' | 'default'
}

interface TimelineProps {
  items: TimelineItem[]
  className?: string
}

const statusColor = {
  success: 'bg-success',
  danger: 'bg-danger',
  warning: 'bg-warning',
  default: 'bg-primary',
}

export function Timeline({ items, className }: TimelineProps) {
  return (
    <ol className={cn('relative space-y-0', className)}>
      {items.map((item, index) => (
        <li key={item.id} className="relative flex gap-4 pb-6 last:pb-0">
          {index < items.length - 1 && (
            <span className="absolute left-[7px] top-4 h-full w-px bg-border" />
          )}
          <span
            className={cn(
              'relative z-10 mt-1.5 h-4 w-4 shrink-0 rounded-full ring-4 ring-[#0B0D17]',
              statusColor[item.status ?? 'default'],
            )}
          />
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-baseline justify-between gap-2">
              <p className="font-medium">{item.title}</p>
              {item.timestamp && <time className="text-xs text-muted">{item.timestamp}</time>}
            </div>
            {item.description && <p className="mt-1 text-sm text-muted">{item.description}</p>}
          </div>
        </li>
      ))}
    </ol>
  )
}
