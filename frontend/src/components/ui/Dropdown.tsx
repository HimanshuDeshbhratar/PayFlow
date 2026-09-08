import { useState, useRef, useEffect } from 'react'
import { ChevronDown } from 'lucide-react'
import { cn } from '@/lib/utils'

interface DropdownItem {
  label: string
  onClick: () => void
  danger?: boolean
}

interface DropdownProps {
  trigger: React.ReactNode
  items: DropdownItem[]
  align?: 'left' | 'right'
  className?: string
}

export function Dropdown({ trigger, items, align = 'right', className }: DropdownProps) {
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  return (
    <div ref={ref} className={cn('relative inline-block', className)}>
      <button type="button" className="focus-ring rounded-xl" onClick={() => setOpen((v) => !v)}>
        {trigger}
      </button>
      {open && (
        <div
          className={cn(
            'glass-strong absolute z-40 mt-2 min-w-[180px] overflow-hidden py-1 shadow-glow-sm',
            align === 'right' ? 'right-0' : 'left-0',
          )}
        >
          {items.map((item) => (
            <button
              key={item.label}
              type="button"
              className={cn(
                'flex w-full items-center px-3 py-2 text-left text-sm hover:bg-white/5',
                item.danger ? 'text-danger' : 'text-foreground',
              )}
              onClick={() => {
                item.onClick()
                setOpen(false)
              }}
            >
              {item.label}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

export function DropdownChevron({ label }: { label: string }) {
  return (
    <span className="inline-flex items-center gap-1 text-sm text-muted">
      {label} <ChevronDown className="h-4 w-4" />
    </span>
  )
}
