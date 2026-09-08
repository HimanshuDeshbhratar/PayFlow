import { AlertTriangle, ShieldAlert } from 'lucide-react'
import { Link } from 'react-router-dom'
import type { FraudAlert } from '@/types'
import { formatRelativeTime } from '@/lib/utils'
import { Badge } from './Badge'
import { Card } from './Card'

export function FraudAlertCard({ alert }: { alert: FraudAlert }) {
  const critical = alert.severity === 'CRITICAL' || alert.severity === 'HIGH'
  return (
    <Card className="p-4 transition hover:border-danger/30">
      <div className="flex gap-3">
        <div className={`rounded-xl p-2 ${critical ? 'bg-danger/15 text-danger' : 'bg-warning/15 text-warning'}`}>
          {critical ? <ShieldAlert className="h-5 w-5" /> : <AlertTriangle className="h-5 w-5" />}
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <Link to={`/transactions/${alert.transactionId}`} className="font-medium hover:text-primary-soft">
              {alert.title}
            </Link>
            <Badge variant={critical ? 'danger' : 'warning'}>{alert.severity}</Badge>
          </div>
          {alert.description && <p className="mt-1 line-clamp-2 text-sm text-muted">{alert.description}</p>}
          <p className="mt-2 text-xs text-muted">{formatRelativeTime(alert.createdAt)}</p>
        </div>
      </div>
    </Card>
  )
}
