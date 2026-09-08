import { forwardRef, type SelectHTMLAttributes } from 'react'
import { cn } from '@/lib/utils'

export interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string
  error?: string
  options: { value: string; label: string }[]
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ className, label, error, options, id, ...props }, ref) => (
    <label className="block space-y-1.5" htmlFor={id}>
      {label && <span className="text-sm font-medium text-foreground/90">{label}</span>}
      <select
        ref={ref}
        id={id}
        className={cn(
          'h-11 w-full rounded-xl border border-border bg-[#0f1220] px-3.5 text-sm text-foreground focus-ring',
          error && 'border-danger/50',
          className,
        )}
        {...props}
      >
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
      {error && <span className="text-xs text-danger">{error}</span>}
    </label>
  ),
)
Select.displayName = 'Select'
