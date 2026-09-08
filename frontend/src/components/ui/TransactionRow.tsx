import { Link } from 'react-router-dom'
import type { Transaction } from '@/types'
import { formatCurrency, formatRelativeTime, truncateId } from '@/lib/utils'
import { PaymentStatusBadge } from './PaymentStatusBadge'
import { Badge } from './Badge'

export function TransactionRow({ tx }: { tx: Transaction }) {
  return (
    <Link
      to={`/transactions/${tx.id}`}
      className="grid grid-cols-2 gap-2 border-b border-border/50 px-4 py-3 transition hover:bg-white/[0.03] sm:grid-cols-6 sm:items-center"
    >
      <div className="font-mono text-xs text-primary-soft sm:col-span-1">#{truncateId(tx.reference || tx.id)}</div>
      <div className="truncate text-sm sm:col-span-1">{tx.customerName || tx.customerEmail || '—'}</div>
      <div className="text-sm font-semibold sm:col-span-1">{formatCurrency(tx.amountCents, tx.currency)}</div>
      <div className="sm:col-span-1">
        <PaymentStatusBadge status={tx.status} />
      </div>
      <div className="sm:col-span-1">
        {tx.riskScore != null ? (
          <Badge variant={tx.riskScore >= 70 ? 'danger' : tx.riskScore >= 40 ? 'warning' : 'success'}>
            {tx.riskScore}
          </Badge>
        ) : (
          <span className="text-muted text-sm">—</span>
        )}
      </div>
      <div className="text-xs text-muted sm:col-span-1 sm:text-right">{formatRelativeTime(tx.createdAt)}</div>
    </Link>
  )
}
