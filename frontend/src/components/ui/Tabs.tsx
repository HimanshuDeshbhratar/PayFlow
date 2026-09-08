import { cn } from '@/lib/utils'

interface TabsProps {
  tabs: { id: string; label: string; icon?: React.ReactNode }[]
  value: string
  onChange: (id: string) => void
  className?: string
}

export function Tabs({ tabs, value, onChange, className }: TabsProps) {
  return (
    <div className={cn('flex flex-wrap gap-1 rounded-xl border border-border bg-white/[0.03] p-1', className)}>
      {tabs.map((tab) => {
        const active = tab.id === value
        return (
          <button
            key={tab.id}
            type="button"
            onClick={() => onChange(tab.id)}
            className={cn(
              'inline-flex flex-1 items-center justify-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition',
              active
                ? 'bg-gradient-primary text-white shadow-glow-sm'
                : 'text-muted hover:bg-white/5 hover:text-foreground',
            )}
          >
            {tab.icon}
            {tab.label}
          </button>
        )
      })}
    </div>
  )
}
