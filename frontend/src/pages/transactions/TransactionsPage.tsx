import { useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { transactionsApi } from '@/api/transactions'
import {
  PageHeader,
  DataTable,
  SearchBar,
  Select,
  PaymentStatusBadge,
  Badge,
  type Column,
} from '@/components/ui'
import type { Transaction } from '@/types'
import { formatCurrency, formatRelativeTime, truncateId } from '@/lib/utils'

export function TransactionsPage() {
  const [params, setParams] = useSearchParams()
  const navigate = useNavigate()
  const [search, setSearch] = useState(params.get('search') || '')
  const status = params.get('status') || ''
  const riskLevel = params.get('riskLevel') || ''

  const query = useQuery({
    queryKey: ['transactions', search, status, riskLevel],
    queryFn: () =>
      transactionsApi.list({
        search: search || undefined,
        status: status || undefined,
        riskLevel: riskLevel || undefined,
        size: 50,
      }),
    retry: 1,
  })

  const columns: Column<Transaction>[] = useMemo(
    () => [
      {
        key: 'ref',
        header: 'Reference',
        render: (r) => <span className="font-mono text-xs text-primary-soft">#{truncateId(r.reference || r.id)}</span>,
      },
      {
        key: 'customer',
        header: 'Customer',
        render: (r) => r.customerName || r.customerEmail || '—',
      },
      {
        key: 'amount',
        header: 'Amount',
        render: (r) => <span className="font-semibold">{formatCurrency(r.amountCents, r.currency)}</span>,
      },
      {
        key: 'status',
        header: 'Status',
        render: (r) => <PaymentStatusBadge status={r.status} />,
      },
      {
        key: 'risk',
        header: 'Risk',
        render: (r) =>
          r.riskScore != null ? (
            <Badge variant={r.riskScore >= 70 ? 'danger' : r.riskScore >= 40 ? 'warning' : 'success'}>{r.riskScore}</Badge>
          ) : (
            '—'
          ),
      },
      {
        key: 'time',
        header: 'Time',
        render: (r) => <span className="text-muted">{formatRelativeTime(r.createdAt)}</span>,
      },
    ],
    [],
  )

  return (
    <div>
      <PageHeader title="Transactions" description="Monitor payment attempts across merchants and corridors." />
      <div className="mb-4 flex flex-col gap-3 md:flex-row">
        <SearchBar
          className="md:max-w-sm"
          placeholder="Search reference or customer"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <Select
          options={[
            { value: '', label: 'All statuses' },
            { value: 'COMPLETED', label: 'Completed' },
            { value: 'BLOCKED', label: 'Blocked' },
            { value: 'PENDING', label: 'Pending' },
            { value: 'FAILED', label: 'Failed' },
          ]}
          value={status}
          onChange={(e) => {
            const next = new URLSearchParams(params)
            if (e.target.value) next.set('status', e.target.value)
            else next.delete('status')
            setParams(next)
          }}
        />
        <Select
          options={[
            { value: '', label: 'All risk levels' },
            { value: 'LOW', label: 'Low' },
            { value: 'MEDIUM', label: 'Medium' },
            { value: 'HIGH', label: 'High' },
            { value: 'CRITICAL', label: 'Critical' },
          ]}
          value={riskLevel}
          onChange={(e) => {
            const next = new URLSearchParams(params)
            if (e.target.value) next.set('riskLevel', e.target.value)
            else next.delete('riskLevel')
            setParams(next)
          }}
        />
      </div>
      <DataTable
        columns={columns}
        data={query.data?.content || []}
        loading={query.isLoading}
        keyExtractor={(r) => r.id}
        onRowClick={(r) => navigate(`/transactions/${r.id}`)}
        emptyTitle={query.isError ? 'Unable to load transactions' : 'No transactions'}
        emptyDescription={
          query.isError
            ? 'The API is offline or returned an error. Retry once the backend is running.'
            : 'Payments will show up here after checkout.'
        }
      />
    </div>
  )
}
