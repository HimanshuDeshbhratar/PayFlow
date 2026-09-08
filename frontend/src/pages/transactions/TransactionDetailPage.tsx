import { useParams, Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft } from 'lucide-react'
import { transactionsApi } from '@/api/transactions'
import { paymentsApi } from '@/api/payments'
import {
  PageHeader,
  Button,
  Card,
  PaymentStatusBadge,
  RiskScore,
  Timeline,
  Badge,
  SkeletonCard,
  EmptyState,
  useToast,
} from '@/components/ui'
import { formatCurrency, formatRelativeTime } from '@/lib/utils'
import { getErrorMessage } from '@/lib/api'

export function TransactionDetailPage() {
  const { id = '' } = useParams()
  const { toast } = useToast()
  const qc = useQueryClient()

  const { data, isLoading, isError } = useQuery({
    queryKey: ['transaction', id],
    queryFn: () => transactionsApi.get(id),
    enabled: !!id,
    retry: 1,
  })

  const action = useMutation({
    mutationFn: async (type: 'safe' | 'block' | 'refund') => {
      if (type === 'safe') return transactionsApi.markSafe(id)
      if (type === 'block') return transactionsApi.block(id)
      if (data?.paymentId) return paymentsApi.refund(data.paymentId)
      throw new Error('Missing payment id')
    },
    onSuccess: () => {
      toast({ type: 'success', title: 'Action applied' })
      void qc.invalidateQueries({ queryKey: ['transaction', id] })
    },
    onError: (e) => toast({ type: 'error', title: 'Action failed', description: getErrorMessage(e) }),
  })

  if (isLoading) {
    return (
      <div className="grid gap-4 md:grid-cols-2">
        <SkeletonCard />
        <SkeletonCard />
      </div>
    )
  }

  if (isError || !data) {
    return (
      <EmptyState
        title="Transaction not found"
        description="This transaction may not exist or the API is unavailable."
        actionLabel="Back to list"
        onAction={() => history.back()}
      />
    )
  }

  const rawEvents = data.timeline?.length ? data.timeline : data.events || []
  const events = rawEvents.map((e) => ({
    id: e.id,
    title: (e.eventType || 'EVENT').replace(/_/g, ' '),
    description: e.message || (e.toStatus ? `→ ${e.toStatus}` : undefined),
    timestamp: formatRelativeTime(e.createdAt),
    status:
      e.toStatus === 'BLOCKED' || e.toStatus === 'FAILED'
        ? ('danger' as const)
        : e.toStatus === 'COMPLETED' || e.toStatus === 'APPROVED'
          ? ('success' as const)
          : ('default' as const),
  }))

  const factors = data.riskAssessment?.factors || []
  const merchantLabel = data.merchantName || data.customer?.fullName || 'PayFlow Demo Merchant'
  const isNewDevice = data.isNewDevice || data.newDevice

  return (
    <div>
      <Link to="/transactions" className="mb-4 inline-flex items-center gap-2 text-sm text-muted hover:text-foreground">
        <ArrowLeft className="h-4 w-4" /> Transactions
      </Link>
      <PageHeader
        title={`#${data.reference || data.id.slice(0, 8)}`}
        description={`${merchantLabel} · ${formatRelativeTime(data.createdAt)}`}
        actions={
          <div className="flex flex-wrap gap-2">
            <PaymentStatusBadge status={data.status} />
            <Button variant="secondary" loading={action.isPending} onClick={() => action.mutate('refund')}>
              Refund
            </Button>
            <Button variant="danger" loading={action.isPending} onClick={() => action.mutate('block')}>
              Block
            </Button>
            <Button variant="success" loading={action.isPending} onClick={() => action.mutate('safe')}>
              Mark Safe
            </Button>
          </div>
        }
      />

      <div className="grid gap-4 lg:grid-cols-3">
        <Card className="space-y-3 lg:col-span-1">
          <h3 className="font-semibold">Details</h3>
          {[
            ['Amount', formatCurrency(data.amountCents, data.currency)],
            ['Method', data.paymentMethodType || '—'],
            ['Customer', data.customer?.fullName || data.customerName || '—'],
            ['Merchant', merchantLabel],
            ['Location', [data.locationCity, data.locationCountry].filter(Boolean).join(', ') || '—'],
            ['Device', data.deviceInfo || '—'],
            ['IP', data.ipAddress || '—'],
          ].map(([k, v]) => (
            <div key={k} className="flex justify-between gap-3 text-sm">
              <span className="text-muted">{k}</span>
              <span className="text-right font-medium">{v}</span>
            </div>
          ))}
          {isNewDevice && <Badge variant="warning">New device</Badge>}
          {data.locationCountry && ['NG', 'RU', 'IR'].includes(data.locationCountry) && (
            <Badge variant="danger">Risk location</Badge>
          )}
        </Card>

        <Card className="lg:col-span-1">
          <h3 className="mb-4 font-semibold">Timeline</h3>
          {events.length ? <Timeline items={events} /> : <p className="text-sm text-muted">No events yet.</p>}
        </Card>

        <Card className="lg:col-span-1">
          <h3 className="mb-4 font-semibold">Fraud analysis</h3>
          <RiskScore
            score={data.riskScore ?? data.riskAssessment?.riskScore ?? 0}
            level={data.riskLevel || data.riskAssessment?.riskLevel}
          />
          <div className="mt-6 space-y-2">
            <p className="text-sm font-medium">Risk factors</p>
            {factors.length === 0 && (
              <p className="text-sm text-muted">
                {data.riskScore
                  ? `Score ${data.riskScore}/100 (${data.riskLevel || 'N/A'}) — factors available for screened payments.`
                  : 'No factors recorded.'}
              </p>
            )}
            {factors.map((f) => (
              <div
                key={f.code}
                className="flex items-center justify-between rounded-xl border border-border bg-white/[0.03] px-3 py-2 text-sm"
              >
                <span>{f.name}</span>
                <span className="font-semibold text-danger">+{f.weight}</span>
              </div>
            ))}
          </div>
        </Card>
      </div>

      <Card className="mt-4">
        <h3 className="mb-4 font-semibold">Customer activity</h3>
        {(data.customerActivity || []).length === 0 ? (
          <p className="text-sm text-muted">No customer activity available.</p>
        ) : (
          <Timeline
            items={(data.customerActivity || []).map((a) => ({
              id: a.id,
              title: a.label || a.reference || 'Activity',
              description: a.amountCents != null ? formatCurrency(a.amountCents) : undefined,
              timestamp: formatRelativeTime(a.createdAt),
              status: a.status === 'BLOCKED' || a.status === 'FAILED' ? 'danger' : a.status === 'COMPLETED' ? 'success' : 'default',
            }))}
          />
        )}
      </Card>
    </div>
  )
}
