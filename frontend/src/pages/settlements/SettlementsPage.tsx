import { useQuery } from '@tanstack/react-query'
import { settlementsApi } from '@/api/settlements'
import { PageHeader, DataTable, Badge, type Column } from '@/components/ui'
import type { Settlement } from '@/types'
import { formatCurrency, formatRelativeTime } from '@/lib/utils'

export function SettlementsPage() {
  const query = useQuery({
    queryKey: ['settlements'],
    queryFn: () => settlementsApi.list({ size: 50 }),
    retry: 1,
  })

  const columns: Column<Settlement>[] = [
    { key: 'merchant', header: 'Merchant', render: (r) => r.merchantName || r.merchantId.slice(0, 8) },
    {
      key: 'amount',
      header: 'Amount',
      render: (r) => <span className="font-semibold">{formatCurrency(r.amountCents, r.currency)}</span>,
    },
    {
      key: 'status',
      header: 'Status',
      render: (r) => (
        <Badge variant={String(r.status) === 'COMPLETED' || String(r.status) === 'CURRENT' ? 'success' : 'warning'}>
          {String(r.status)}
        </Badge>
      ),
    },
    {
      key: 'period',
      header: 'Period',
      render: (r) => (
        <span className="text-xs text-muted">
          {r.periodStart?.slice(0, 10)} → {r.periodEnd?.slice(0, 10)}
        </span>
      ),
    },
    { key: 'created', header: 'Created', render: (r) => formatRelativeTime(r.createdAt) },
  ]

  return (
    <div>
      <PageHeader title="Settlements" description="Merchant payout batches and settlement status." />
      <DataTable
        columns={columns}
        data={query.data?.content || []}
        loading={query.isLoading}
        keyExtractor={(r) => r.id}
        emptyTitle={query.isError ? 'Unable to load settlements' : 'No settlements'}
      />
    </div>
  )
}
