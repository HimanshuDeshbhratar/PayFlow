import { forwardRef, type InputHTMLAttributes } from 'react'
import { cn } from '@/lib/utils'

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  hint?: string
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, label, error, hint, id, ...props }, ref) => (
    <label className="block space-y-1.5" htmlFor={id}>
      {label && <span className="text-sm font-medium text-foreground/90">{label}</span>}
      <input
        ref={ref}
        id={id}
        className={cn(
          'h-11 w-full rounded-xl border border-border bg-white/5 px-3.5 text-sm text-foreground placeholder:text-muted focus-ring',
          error && 'border-danger/50 focus-visible:ring-danger/40',
          className,
        )}
        {...props}
      />
      {error ? <span className="text-xs text-danger">{error}</span> : hint ? <span className="text-xs text-muted">{hint}</span> : null}
    </label>
  ),
)
Input.displayName = 'Input'
