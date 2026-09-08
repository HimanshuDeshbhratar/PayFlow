import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { merchantsApi } from '@/api/merchants'
import { PageHeader, DataTable, SearchBar, Badge, type Column } from '@/components/ui'
import type { Merchant } from '@/types'
import { formatCurrency, formatRelativeTime } from '@/lib/utils'

export function MerchantsPage() {
  const [search, setSearch] = useState('')
  const navigate = useNavigate()
  const query = useQuery({
    queryKey: ['merchants', search],
    queryFn: () => merchantsApi.list({ search: search || undefined, size: 50 }),
    retry: 1,
  })

  const columns: Column<Merchant>[] = [
    { key: 'name', header: 'Business', render: (r) => <div><p className="font-medium">{r.businessName}</p><p className="text-xs text-muted">{r.legalName}</p></div> },
    { key: 'country', header: 'Country', render: (r) => r.countryCode },
    {
      key: 'status',
      header: 'Status',
      render: (r) => <Badge variant={r.status === 'ACTIVE' ? 'success' : r.status === 'SUSPENDED' ? 'danger' : 'warning'}>{r.status}</Badge>,
    },
    { key: 'volume', header: 'Volume', render: (r) => formatCurrency(r.volumeCents || 0, r.currency) },
    { key: 'settlement', header: 'Settlement', render: (r) => <Badge variant="accent">{String(r.settlementStatus)}</Badge> },
    { key: 'created', header: 'Created', render: (r) => <span className="text-muted">{formatRelativeTime(r.createdAt)}</span> },
  ]

  return (
    <div>
      <PageHeader title="Merchants" description="Platform merchants and settlement posture." />
      <SearchBar className="mb-4 md:max-w-sm" placeholder="Search merchants" value={search} onChange={(e) => setSearch(e.target.value)} />
      <DataTable
        columns={columns}
        data={query.data?.content || []}
        loading={query.isLoading}
        keyExtractor={(r) => r.id}
        onRowClick={(r) => navigate(`/merchants/${r.id}`)}
        emptyTitle={query.isError ? 'Unable to load merchants' : 'No merchants'}
      />
    </div>
  )
}
