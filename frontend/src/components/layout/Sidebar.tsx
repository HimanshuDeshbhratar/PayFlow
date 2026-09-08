import { NavLink } from 'react-router-dom'
import {
  LayoutDashboard,
  ArrowLeftRight,
  CreditCard,
  ShieldAlert,
  Users,
  Store,
  Landmark,
  BookOpen,
  BarChart3,
  Code2,
  Bell,
  Settings,
  ScrollText,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react'
import { cn } from '@/lib/utils'
import { useAuth } from '@/hooks/useAuth'

const nav = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/transactions', label: 'Transactions', icon: ArrowLeftRight },
  { to: '/payments', label: 'Payments', icon: CreditCard },
  { to: '/risk-fraud', label: 'Risk & Fraud', icon: ShieldAlert },
  { to: '/customers', label: 'Customers', icon: Users },
  { to: '/merchants', label: 'Merchants', icon: Store },
  { to: '/settlements', label: 'Settlements', icon: Landmark },
  { to: '/ledger', label: 'Ledger', icon: BookOpen },
  { to: '/reports', label: 'Reports', icon: BarChart3 },
  { to: '/developers', label: 'Developers', icon: Code2 },
  { to: '/notifications', label: 'Notifications', icon: Bell },
  { to: '/settings', label: 'Settings', icon: Settings },
]

interface SidebarProps {
  collapsed: boolean
  onToggle: () => void
  onNavigate?: () => void
  className?: string
}

export function Sidebar({ collapsed, onToggle, onNavigate, className }: SidebarProps) {
  const { user } = useAuth()
  const items = [
    ...nav,
    ...(user?.role === 'ADMIN'
      ? [{ to: '/audit-logs', label: 'Audit Logs', icon: ScrollText }]
      : []),
  ]

  return (
    <aside
      className={cn(
        'glass-strong flex h-full flex-col border-r border-border transition-all duration-300',
        collapsed ? 'w-[72px]' : 'w-64',
        className,
      )}
    >
      <div className={cn('flex items-center gap-3 border-b border-border px-4 py-5', collapsed && 'justify-center px-2')}>
        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-primary shadow-glow-sm">
          <span className="text-sm font-bold">P</span>
        </div>
        {!collapsed && (
          <div>
            <p className="text-sm font-bold tracking-tight">PayFlow</p>
            <p className="text-[10px] uppercase tracking-widest text-muted">Payments</p>
          </div>
        )}
      </div>

      <nav className="flex-1 space-y-1 overflow-y-auto scrollbar-thin p-3">
        {items.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            onClick={onNavigate}
            className={({ isActive }) =>
              cn(
                'group flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition',
                collapsed && 'justify-center px-2',
                isActive
                  ? 'bg-gradient-primary text-white shadow-glow-sm'
                  : 'text-muted hover:bg-white/5 hover:text-foreground',
              )
            }
            title={collapsed ? label : undefined}
          >
            <Icon className="h-4 w-4 shrink-0" />
            {!collapsed && <span>{label}</span>}
          </NavLink>
        ))}
      </nav>

      <button
        type="button"
        onClick={onToggle}
        className="m-3 hidden items-center justify-center gap-2 rounded-xl border border-border py-2 text-xs text-muted hover:bg-white/5 lg:flex"
      >
        {collapsed ? <ChevronRight className="h-4 w-4" /> : <><ChevronLeft className="h-4 w-4" /> Collapse</>}
      </button>
    </aside>
  )
}
