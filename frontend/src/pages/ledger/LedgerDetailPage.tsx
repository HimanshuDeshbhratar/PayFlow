import { Link, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeft } from 'lucide-react'
import { ledgerApi } from '@/api/settlements'
import { PageHeader, DataTable, Badge, EmptyState, type Column } from '@/components/ui'
import type { LedgerEntry } from '@/types'
import { formatCurrency, formatRelativeTime } from '@/lib/utils'

export function LedgerDetailPage() {
  const { id = '' } = useParams()
  const query = useQuery({
    queryKey: ['ledger-entries', id],
    queryFn: () => ledgerApi.entries(id, { size: 100 }),
    enabled: !!id,
    retry: 1,
  })

  const columns: Column<LedgerEntry>[] = [
    {
      key: 'type',
      header: 'Type',
      render: (r) => <Badge variant={r.entryType === 'CREDIT' ? 'success' : 'warning'}>{r.entryType}</Badge>,
    },
    {
      key: 'amount',
      header: 'Amount',
      render: (r) => formatCurrency(r.amountCents, r.currency),
    },
    { key: 'desc', header: 'Description', render: (r) => r.description || '—' },
    { key: 'time', header: 'Time', render: (r) => formatRelativeTime(r.createdAt) },
  ]

  if (query.isError) return <EmptyState title="Unable to load entries" />

  return (
    <div>
      <Link to="/ledger" className="mb-4 inline-flex items-center gap-2 text-sm text-muted hover:text-foreground">
        <ArrowLeft className="h-4 w-4" /> Ledger
      </Link>
      <PageHeader title="Account entries" description={`Account ${id.slice(0, 8)}…`} />
      <DataTable
        columns={columns}
        data={query.data?.content || []}
        loading={query.isLoading}
        keyExtractor={(r) => r.id}
        emptyTitle="No entries"
      />
    </div>
  )
}
