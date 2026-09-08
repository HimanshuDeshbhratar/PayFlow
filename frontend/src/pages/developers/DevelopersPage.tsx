import { Link } from 'react-router-dom'
import { KeyRound, Webhook, BookOpen } from 'lucide-react'
import { PageHeader, Card } from '@/components/ui'

const links = [
  { to: '/developers/api-keys', title: 'API Keys', desc: 'Create and revoke merchant API keys.', icon: KeyRound },
  { to: '/developers/webhooks', title: 'Webhooks', desc: 'Receive payment and fraud events.', icon: Webhook },
]

export function DevelopersPage() {
  return (
    <div>
      <PageHeader title="Developers" description="Integrate PayFlow with keys, webhooks, and docs." />
      <div className="grid gap-4 md:grid-cols-3">
        {links.map(({ to, title, desc, icon: Icon }) => (
          <Link key={to} to={to}>
            <Card className="h-full transition hover:border-primary/40 hover:shadow-glow-sm">
              <div className="mb-3 inline-flex rounded-xl bg-primary/15 p-2.5 text-primary-soft">
                <Icon className="h-5 w-5" />
              </div>
              <h3 className="font-semibold">{title}</h3>
              <p className="mt-1 text-sm text-muted">{desc}</p>
            </Card>
          </Link>
        ))}
        <Card>
          <div className="mb-3 inline-flex rounded-xl bg-accent/15 p-2.5 text-accent">
            <BookOpen className="h-5 w-5" />
          </div>
          <h3 className="font-semibold">API base</h3>
          <p className="mt-1 break-all font-mono text-xs text-muted">
            {import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'}
          </p>
        </Card>
      </div>
    </div>
  )
}
