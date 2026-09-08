import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { notificationsApi } from '@/api'
import { PageHeader, Button, Card, EmptyState, Skeleton, Badge, useToast } from '@/components/ui'
import { formatRelativeTime } from '@/lib/utils'
import { getErrorMessage } from '@/lib/api'

export function NotificationsPage() {
  const { toast } = useToast()
  const qc = useQueryClient()
  const query = useQuery({
    queryKey: ['notifications'],
    queryFn: () => notificationsApi.list({ size: 50 }),
    retry: 1,
  })

  const mark = useMutation({
    mutationFn: (id: string) => notificationsApi.markRead(id),
    onSuccess: () => void qc.invalidateQueries({ queryKey: ['notifications'] }),
    onError: (e) => toast({ type: 'error', title: 'Failed', description: getErrorMessage(e) }),
  })

  const markAll = useMutation({
    mutationFn: () => notificationsApi.markAllRead(),
    onSuccess: () => void qc.invalidateQueries({ queryKey: ['notifications'] }),
  })

  return (
    <div>
      <PageHeader
        title="Notifications"
        description="Alerts from payments, fraud, and settlements."
        actions={
          <Button variant="secondary" onClick={() => markAll.mutate()} loading={markAll.isPending}>
            Mark all read
          </Button>
        }
      />
      {query.isLoading ? (
        <div className="space-y-3">
          <Skeleton className="h-20 w-full" />
          <Skeleton className="h-20 w-full" />
        </div>
      ) : query.isError || !query.data?.content?.length ? (
        <EmptyState
          title={query.isError ? 'Unable to load notifications' : 'Inbox zero'}
          description={query.isError ? 'Start the API to sync notifications.' : 'You’re all caught up.'}
        />
      ) : (
        <div className="space-y-3">
          {query.data.content.map((n) => (
            <Card key={n.id} className="flex items-start justify-between gap-4 p-4">
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <p className="font-medium">{n.title}</p>
                  {!n.read && <Badge variant="primary">New</Badge>}
                </div>
                <p className="mt-1 text-sm text-muted">{n.message}</p>
                <p className="mt-2 text-xs text-muted">{formatRelativeTime(n.createdAt)}</p>
              </div>
              {!n.read && (
                <Button variant="ghost" size="sm" onClick={() => mark.mutate(n.id)}>
                  Mark read
                </Button>
              )}
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}
