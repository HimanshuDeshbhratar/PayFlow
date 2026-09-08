import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { customersApi } from '@/api/customers'
import { PageHeader, DataTable, SearchBar, Badge, type Column } from '@/components/ui'
import type { Customer } from '@/types'
import { formatCurrency, formatRelativeTime } from '@/lib/utils'

export function CustomersPage() {
  const [search, setSearch] = useState('')
  const navigate = useNavigate()
  const query = useQuery({
    queryKey: ['customers', search],
    queryFn: () => customersApi.list({ search: search || undefined, size: 50 }),
    retry: 1,
  })

  const columns: Column<Customer>[] = [
    { key: 'name', header: 'Customer', render: (r) => <div><p className="font-medium">{r.fullName}</p><p className="text-xs text-muted">{r.email}</p></div> },
    { key: 'country', header: 'Country', render: (r) => r.countryCode || '—' },
    {
      key: 'status',
      header: 'Status',
      render: (r) => <Badge variant={r.blocked ? 'danger' : 'success'}>{r.blocked ? 'Blocked' : r.status}</Badge>,
    },
    {
      key: 'spend',
      header: 'Spend',
      render: (r) => formatCurrency(r.totalSpendCents || 0),
    },
    { key: 'created', header: 'Joined', render: (r) => <span className="text-muted">{formatRelativeTime(r.createdAt)}</span> },
  ]

  return (
    <div>
      <PageHeader title="Customers" description="Profiles, spend, and block status across merchants." />
      <SearchBar className="mb-4 md:max-w-sm" placeholder="Search customers" value={search} onChange={(e) => setSearch(e.target.value)} />
      <DataTable
        columns={columns}
        data={query.data?.content || []}
        loading={query.isLoading}
        keyExtractor={(r) => r.id}
        onRowClick={(r) => navigate(`/customers/${r.id}`)}
        emptyTitle={query.isError ? 'Unable to load customers' : 'No customers'}
      />
    </div>
  )
}
