import { Link, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeft } from 'lucide-react'
import { merchantsApi } from '@/api/merchants'
import { PageHeader, Card, Badge, SkeletonCard, EmptyState } from '@/components/ui'
import { formatCurrency, formatRelativeTime } from '@/lib/utils'

export function MerchantDetailPage() {
  const { id = '' } = useParams()
  const { data, isLoading, isError } = useQuery({
    queryKey: ['merchant', id],
    queryFn: () => merchantsApi.get(id),
    enabled: !!id,
    retry: 1,
  })

  if (isLoading) return <SkeletonCard />
  if (isError || !data) return <EmptyState title="Merchant not found" />

  return (
    <div>
      <Link to="/merchants" className="mb-4 inline-flex items-center gap-2 text-sm text-muted hover:text-foreground">
        <ArrowLeft className="h-4 w-4" /> Merchants
      </Link>
      <PageHeader
        title={data.businessName}
        description={data.legalName || undefined}
        actions={<Badge variant={data.status === 'ACTIVE' ? 'success' : 'warning'}>{data.status}</Badge>}
      />
      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <p className="text-sm text-muted">Volume</p>
          <p className="mt-2 text-2xl font-bold">{formatCurrency(data.volumeCents || 0, data.currency)}</p>
        </Card>
        <Card>
          <p className="text-sm text-muted">Transactions</p>
          <p className="mt-2 text-2xl font-bold">{data.transactionCount ?? 0}</p>
        </Card>
        <Card>
          <p className="text-sm text-muted">Settlement</p>
          <p className="mt-2 text-2xl font-bold">{String(data.settlementStatus)}</p>
        </Card>
      </div>
      <Card className="mt-4 space-y-2 text-sm">
        <div className="flex justify-between"><span className="text-muted">Country</span><span>{data.countryCode}</span></div>
        <div className="flex justify-between"><span className="text-muted">Currency</span><span>{data.currency}</span></div>
        <div className="flex justify-between"><span className="text-muted">Created</span><span>{formatRelativeTime(data.createdAt)}</span></div>
      </Card>
    </div>
  )
}
