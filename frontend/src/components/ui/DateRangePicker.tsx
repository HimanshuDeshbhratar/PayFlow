import { cn } from '@/lib/utils'

interface DateRangePickerProps {
  from: string
  to: string
  onChange: (range: { from: string; to: string }) => void
  className?: string
}

export function DateRangePicker({ from, to, onChange, className }: DateRangePickerProps) {
  return (
    <div className={cn('flex flex-wrap items-center gap-2', className)}>
      <input
        type="date"
        value={from}
        onChange={(e) => onChange({ from: e.target.value, to })}
        className="h-10 rounded-xl border border-border bg-white/5 px-3 text-sm focus-ring"
      />
      <span className="text-muted text-sm">to</span>
      <input
        type="date"
        value={to}
        onChange={(e) => onChange({ from, to: e.target.value })}
        className="h-10 rounded-xl border border-border bg-white/5 px-3 text-sm focus-ring"
      />
    </div>
  )
}
