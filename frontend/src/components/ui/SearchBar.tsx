import { Search } from 'lucide-react'
import { cn } from '@/lib/utils'

interface SearchBarProps extends React.InputHTMLAttributes<HTMLInputElement> {
  onSearch?: (value: string) => void
}

export function SearchBar({ className, onSearch, onChange, ...props }: SearchBarProps) {
  return (
    <div className={cn('relative w-full', className)}>
      <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" />
      <input
        className="h-11 w-full rounded-xl border border-border bg-white/5 pl-10 pr-4 text-sm text-foreground placeholder:text-muted focus-ring"
        onChange={(e) => {
          onChange?.(e)
          onSearch?.(e.target.value)
        }}
        {...props}
      />
    </div>
  )
}
