import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus, Trash2 } from 'lucide-react'
import { developerApi } from '@/api'
import { PageHeader, Button, DataTable, Modal, Input, Badge, useToast, type Column } from '@/components/ui'
import type { WebhookEndpoint } from '@/types'
import { formatRelativeTime } from '@/lib/utils'
import { getErrorMessage } from '@/lib/api'

export function WebhooksPage() {
  const [open, setOpen] = useState(false)
  const [url, setUrl] = useState('https://example.com/webhooks/payflow')
  const { toast } = useToast()
  const qc = useQueryClient()

  const query = useQuery({ queryKey: ['webhooks'], queryFn: () => developerApi.webhooks(), retry: 1 })

  const create = useMutation({
    mutationFn: () => developerApi.createWebhook({ url, events: ['payment.completed', 'fraud.alert'] }),
    onSuccess: () => {
      setOpen(false)
      void qc.invalidateQueries({ queryKey: ['webhooks'] })
      toast({ type: 'success', title: 'Webhook created' })
    },
    onError: (e) => toast({ type: 'error', title: 'Failed', description: getErrorMessage(e) }),
  })

  const remove = useMutation({
    mutationFn: (id: string) => developerApi.deleteWebhook(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['webhooks'] })
      toast({ type: 'success', title: 'Webhook removed' })
    },
    onError: (e) => toast({ type: 'error', title: 'Failed', description: getErrorMessage(e) }),
  })

  const columns: Column<WebhookEndpoint>[] = [
    { key: 'url', header: 'URL', render: (r) => <span className="break-all font-mono text-xs">{r.url}</span> },
    {
      key: 'events',
      header: 'Events',
      render: (r) => (
        <div className="flex flex-wrap gap-1">
          {(r.events || []).map((e) => (
            <Badge key={e} variant="muted">{e}</Badge>
          ))}
        </div>
      ),
    },
    {
      key: 'active',
      header: 'Status',
      render: (r) => <Badge variant={r.active ? 'success' : 'muted'}>{r.active ? 'Active' : 'Inactive'}</Badge>,
    },
    { key: 'created', header: 'Created', render: (r) => formatRelativeTime(r.createdAt) },
    {
      key: 'actions',
      header: '',
      render: (r) => (
        <Button variant="ghost" size="icon" onClick={() => remove.mutate(r.id)}>
          <Trash2 className="h-4 w-4 text-danger" />
        </Button>
      ),
    },
  ]

  return (
    <div>
      <PageHeader
        title="Webhooks"
        description="Push payment and fraud events to your endpoints."
        actions={
          <Button onClick={() => setOpen(true)}>
            <Plus className="h-4 w-4" /> Add endpoint
          </Button>
        }
      />
      <DataTable
        columns={columns}
        data={query.data || []}
        loading={query.isLoading}
        keyExtractor={(r) => r.id}
        emptyTitle={query.isError ? 'Unable to load webhooks' : 'No webhooks'}
      />
      <Modal open={open} onClose={() => setOpen(false)} title="Add webhook">
        <form
          className="space-y-4"
          onSubmit={(e) => {
            e.preventDefault()
            create.mutate()
          }}
        >
          <Input label="Endpoint URL" value={url} onChange={(e) => setUrl(e.target.value)} />
          <Button type="submit" className="w-full" loading={create.isPending}>Create</Button>
        </form>
      </Modal>
    </div>
  )
}
