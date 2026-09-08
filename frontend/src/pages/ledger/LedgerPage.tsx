import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ledgerApi } from '@/api/settlements'
import { PageHeader, DataTable, Badge, type Column } from '@/components/ui'
import type { LedgerAccount } from '@/types'
import { formatCurrency } from '@/lib/utils'

export function LedgerPage() {
  const navigate = useNavigate()
  const query = useQuery({
    queryKey: ['ledger-accounts'],
    queryFn: () => ledgerApi.accounts({ size: 50 }),
    retry: 1,
  })

  const columns: Column<LedgerAccount>[] = [
    { key: 'name', header: 'Account', render: (r) => r.name },
    { key: 'type', header: 'Type', render: (r) => <Badge variant="primary">{r.accountType}</Badge> },
    {
      key: 'balance',
      header: 'Balance',
      render: (r) => <span className="font-semibold">{formatCurrency(r.balanceCents, r.currency)}</span>,
    },
    { key: 'currency', header: 'Currency', render: (r) => r.currency },
  ]

  return (
    <div>
      <PageHeader title="Ledger" description="Double-entry style account balances for the platform." />
      <DataTable
        columns={columns}
        data={query.data?.content || []}
        loading={query.isLoading}
        keyExtractor={(r) => r.id}
        onRowClick={(r) => navigate(`/ledger/${r.id}`)}
        emptyTitle={query.isError ? 'Unable to load ledger' : 'No accounts'}
      />
    </div>
  )
}
