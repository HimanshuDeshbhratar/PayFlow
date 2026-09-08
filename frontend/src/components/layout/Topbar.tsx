import { Bell, Menu, LogOut, User } from 'lucide-react'
import { Link } from 'react-router-dom'
import { SearchBar } from '@/components/ui/SearchBar'
import { Dropdown } from '@/components/ui/Dropdown'
import { useAuth } from '@/hooks/useAuth'
import { Button } from '@/components/ui/Button'

interface TopbarProps {
  onMenuClick: () => void
  onSearch?: (q: string) => void
}

export function Topbar({ onMenuClick, onSearch }: TopbarProps) {
  const { user, logout } = useAuth()

  return (
    <header className="sticky top-0 z-30 flex items-center gap-3 border-b border-border bg-[#05050C]/70 px-4 py-3 backdrop-blur-xl md:px-6">
      <Button variant="ghost" size="icon" className="lg:hidden" onClick={onMenuClick} aria-label="Open menu">
        <Menu className="h-5 w-5" />
      </Button>

      <SearchBar
        placeholder="Search transactions, customers…"
        className="max-w-md flex-1"
        onSearch={onSearch}
      />

      <div className="ml-auto flex items-center gap-2">
        <Link
          to="/notifications"
          className="relative rounded-xl border border-border bg-white/5 p-2.5 text-muted hover:text-foreground"
        >
          <Bell className="h-4 w-4" />
          <span className="absolute right-2 top-2 h-1.5 w-1.5 rounded-full bg-primary" />
        </Link>

        <Dropdown
          trigger={
            <div className="flex items-center gap-2 rounded-xl border border-border bg-white/5 px-2.5 py-1.5">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/20 text-primary-soft">
                <User className="h-4 w-4" />
              </div>
              <div className="hidden text-left sm:block">
                <p className="text-sm font-semibold leading-tight">{user?.fullName ?? 'User'}</p>
                <p className="text-[10px] uppercase tracking-wide text-muted">{user?.role?.replace('_', ' ')}</p>
              </div>
            </div>
          }
          items={[
            { label: 'Settings', onClick: () => { window.location.href = '/settings' } },
            { label: 'Sign out', onClick: () => void logout(), danger: true },
          ]}
        />
        <Button variant="ghost" size="icon" className="hidden md:inline-flex" onClick={() => void logout()} aria-label="Logout">
          <LogOut className="h-4 w-4" />
        </Button>
      </div>
    </header>
  )
}
