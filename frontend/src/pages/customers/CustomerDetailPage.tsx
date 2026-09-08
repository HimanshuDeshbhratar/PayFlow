import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { customersApi } from '@/api/customers'
import { PageHeader, Card, Badge, SkeletonCard, EmptyState, Button, useToast } from '@/components/ui'
import { formatCurrency, formatRelativeTime } from '@/lib/utils'
import { getErrorMessage } from '@/lib/api'

export function CustomerDetailPage() {
  const { id = '' } = useParams()
  const { toast } = useToast()
  const qc = useQueryClient()
  const { data, isLoading, isError } = useQuery({
    queryKey: ['customer', id],
    queryFn: () => customersApi.get(id),
    enabled: !!id,
    retry: 1,
  })

  const block = useMutation({
    mutationFn: () => customersApi.block(id, 'Blocked from console'),
    onSuccess: () => {
      toast({ type: 'success', title: 'Customer blocked' })
      void qc.invalidateQueries({ queryKey: ['customer', id] })
    },
    onError: (e) => toast({ type: 'error', title: 'Failed', description: getErrorMessage(e) }),
  })

  if (isLoading) return <SkeletonCard />
  if (isError || !data) return <EmptyState title="Customer not found" />

  return (
    <div>
      <Link to="/customers" className="mb-4 inline-flex items-center gap-2 text-sm text-muted hover:text-foreground">
        <ArrowLeft className="h-4 w-4" /> Customers
      </Link>
      <PageHeader
        title={data.fullName}
        description={data.email}
        actions={
          <>
            <Badge variant={data.blocked ? 'danger' : 'success'}>{data.blocked ? 'Blocked' : data.status}</Badge>
            {!data.blocked && (
              <Button variant="danger" loading={block.isPending} onClick={() => block.mutate()}>
                Block customer
              </Button>
            )}
          </>
        }
      />
      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <p className="text-sm text-muted">Total spend</p>
          <p className="mt-2 text-2xl font-bold">{formatCurrency(data.totalSpendCents || 0)}</p>
        </Card>
        <Card>
          <p className="text-sm text-muted">Transactions</p>
          <p className="mt-2 text-2xl font-bold">{data.transactionCount ?? 0}</p>
        </Card>
        <Card>
          <p className="text-sm text-muted">Joined</p>
          <p className="mt-2 text-2xl font-bold">{formatRelativeTime(data.createdAt)}</p>
        </Card>
      </div>
      <Card className="mt-4 space-y-2 text-sm">
        <div className="flex justify-between"><span className="text-muted">Phone</span><span>{data.phone || '—'}</span></div>
        <div className="flex justify-between"><span className="text-muted">Country</span><span>{data.countryCode || '—'}</span></div>
        <div className="flex justify-between"><span className="text-muted">Merchant</span><span className="font-mono text-xs">{data.merchantId}</span></div>
        {data.blockedReason && <p className="text-danger">Reason: {data.blockedReason}</p>}
      </Card>
    </div>
  )
}
