import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { auditApi } from '@/api'
import { PageHeader, DataTable, SearchBar, type Column } from '@/components/ui'
import type { AuditLog } from '@/types'
import { formatRelativeTime } from '@/lib/utils'

export function AuditLogsPage() {
  const [search, setSearch] = useState('')
  const query = useQuery({
    queryKey: ['audit-logs', search],
    queryFn: () => auditApi.list({ search: search || undefined, size: 50 }),
    retry: 1,
  })

  const columns: Column<AuditLog>[] = [
    { key: 'actor', header: 'Actor', render: (r) => r.actorEmail || r.actorId || '—' },
    { key: 'action', header: 'Action', render: (r) => <span className="font-medium">{r.action}</span> },
    {
      key: 'resource',
      header: 'Resource',
      render: (r) => (
        <span className="text-xs text-muted">
          {r.resourceType} {r.resourceId ? `· ${r.resourceId.slice(0, 8)}` : ''}
        </span>
      ),
    },
    { key: 'ip', header: 'IP', render: (r) => r.ipAddress || '—' },
    { key: 'time', header: 'Time', render: (r) => formatRelativeTime(r.createdAt) },
  ]

  return (
    <div>
      <PageHeader title="Audit logs" description="Admin-only trail of sensitive actions." />
      <SearchBar className="mb-4 md:max-w-sm" placeholder="Search actions" value={search} onChange={(e) => setSearch(e.target.value)} />
      <DataTable
        columns={columns}
        data={query.data?.content || []}
        loading={query.isLoading}
        keyExtractor={(r) => r.id}
        emptyTitle={query.isError ? 'Unable to load audit logs' : 'No audit events'}
      />
    </div>
  )
}
