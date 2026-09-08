import type { PaymentStatus } from '@/types'
import { Badge, type BadgeProps } from './Badge'

const map: Record<string, { label: string; variant: NonNullable<BadgeProps['variant']> }> = {
  COMPLETED: { label: 'Completed', variant: 'success' },
  APPROVED: { label: 'Approved', variant: 'success' },
  BLOCKED: { label: 'Blocked', variant: 'danger' },
  FAILED: { label: 'Failed', variant: 'danger' },
  PENDING: { label: 'Pending', variant: 'warning' },
  PROCESSING: { label: 'Processing', variant: 'primary' },
  FRAUD_SCREENING: { label: 'Screening', variant: 'accent' },
  CANCELLED: { label: 'Cancelled', variant: 'muted' },
  REFUNDED: { label: 'Refunded', variant: 'muted' },
  PARTIALLY_REFUNDED: { label: 'Partial refund', variant: 'muted' },
  REFUND_PENDING: { label: 'Refund pending', variant: 'warning' },
}

export function PaymentStatusBadge({ status }: { status: PaymentStatus | string }) {
  const cfg = map[status] ?? { label: status, variant: 'default' as const }
  return <Badge variant={cfg.variant}>{cfg.label}</Badge>
}
