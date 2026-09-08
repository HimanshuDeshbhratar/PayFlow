import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { CheckCircle2, Info, X, XCircle } from 'lucide-react'
import { cn } from '@/lib/utils'

type ToastType = 'success' | 'error' | 'info'

interface ToastItem {
  id: string
  title: string
  description?: string
  type: ToastType
}

interface ToastContextValue {
  toast: (input: Omit<ToastItem, 'id'>) => void
}

const ToastContext = createContext<ToastContextValue | null>(null)

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([])

  const toast = useCallback((input: Omit<ToastItem, 'id'>) => {
    const id = crypto.randomUUID()
    setItems((prev) => [...prev, { ...input, id }])
    window.setTimeout(() => {
      setItems((prev) => prev.filter((t) => t.id !== id))
    }, 4200)
  }, [])

  const value = useMemo(() => ({ toast }), [toast])

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="pointer-events-none fixed right-4 top-4 z-[100] flex w-[min(100%-2rem,22rem)] flex-col gap-2">
        <AnimatePresence>
          {items.map((item) => (
            <motion.div
              key={item.id}
              initial={{ opacity: 0, y: -12, x: 20 }}
              animate={{ opacity: 1, y: 0, x: 0 }}
              exit={{ opacity: 0, x: 20 }}
              className={cn(
                'pointer-events-auto glass-strong flex gap-3 p-4 shadow-glow-sm',
                item.type === 'success' && 'border-success/30',
                item.type === 'error' && 'border-danger/30',
              )}
            >
              {item.type === 'success' && <CheckCircle2 className="h-5 w-5 shrink-0 text-success" />}
              {item.type === 'error' && <XCircle className="h-5 w-5 shrink-0 text-danger" />}
              {item.type === 'info' && <Info className="h-5 w-5 shrink-0 text-accent" />}
              <div className="min-w-0 flex-1">
                <p className="text-sm font-semibold">{item.title}</p>
                {item.description && <p className="mt-0.5 text-xs text-muted">{item.description}</p>}
              </div>
              <button
                type="button"
                className="text-muted hover:text-foreground"
                onClick={() => setItems((prev) => prev.filter((t) => t.id !== item.id))}
              >
                <X className="h-4 w-4" />
              </button>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used within ToastProvider')
  return ctx
}
