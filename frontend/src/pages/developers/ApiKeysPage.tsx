import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus, Trash2 } from 'lucide-react'
import { developerApi } from '@/api'
import { PageHeader, Button, DataTable, Modal, Input, useToast, type Column } from '@/components/ui'
import type { ApiKey } from '@/types'
import { formatRelativeTime } from '@/lib/utils'
import { getErrorMessage } from '@/lib/api'

export function ApiKeysPage() {
  const [open, setOpen] = useState(false)
  const [name, setName] = useState('Default key')
  const [createdKey, setCreatedKey] = useState<string | null>(null)
  const { toast } = useToast()
  const qc = useQueryClient()

  const query = useQuery({ queryKey: ['api-keys'], queryFn: () => developerApi.apiKeys(), retry: 1 })

  const create = useMutation({
    mutationFn: () => developerApi.createApiKey(name),
    onSuccess: (key) => {
      setCreatedKey(key.key || `${key.prefix}_••••`)
      void qc.invalidateQueries({ queryKey: ['api-keys'] })
      toast({ type: 'success', title: 'API key created' })
    },
    onError: (e) => toast({ type: 'error', title: 'Failed', description: getErrorMessage(e) }),
  })

  const remove = useMutation({
    mutationFn: (id: string) => developerApi.deleteApiKey(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['api-keys'] })
      toast({ type: 'success', title: 'API key revoked' })
    },
    onError: (e) => toast({ type: 'error', title: 'Failed', description: getErrorMessage(e) }),
  })

  const columns: Column<ApiKey>[] = [
    { key: 'name', header: 'Name', render: (r) => r.name },
    { key: 'prefix', header: 'Prefix', render: (r) => <span className="font-mono text-xs">{r.prefix}</span> },
    { key: 'used', header: 'Last used', render: (r) => (r.lastUsedAt ? formatRelativeTime(r.lastUsedAt) : 'Never') },
    { key: 'created', header: 'Created', render: (r) => formatRelativeTime(r.createdAt) },
    {
      key: 'actions',
      header: '',
      render: (r) => (
        <Button variant="ghost" size="icon" onClick={() => remove.mutate(r.id)} aria-label="Delete key">
          <Trash2 className="h-4 w-4 text-danger" />
        </Button>
      ),
    },
  ]

  return (
    <div>
      <PageHeader
        title="API Keys"
        description="Authenticate server-to-server calls. Secrets are shown once."
        actions={
          <Button onClick={() => { setOpen(true); setCreatedKey(null) }}>
            <Plus className="h-4 w-4" /> Create key
          </Button>
        }
      />
      <DataTable
        columns={columns}
        data={query.data || []}
        loading={query.isLoading}
        keyExtractor={(r) => r.id}
        emptyTitle={query.isError ? 'Unable to load keys' : 'No API keys'}
      />
      <Modal open={open} onClose={() => setOpen(false)} title="Create API key">
        {createdKey ? (
          <div className="space-y-3">
            <p className="text-sm text-muted">Copy this key now. You won&apos;t see it again.</p>
            <code className="block break-all rounded-xl border border-border bg-black/30 p-3 text-xs">{createdKey}</code>
            <Button className="w-full" onClick={() => setOpen(false)}>Done</Button>
          </div>
        ) : (
          <form
            className="space-y-4"
            onSubmit={(e) => {
              e.preventDefault()
              create.mutate()
            }}
          >
            <Input label="Name" value={name} onChange={(e) => setName(e.target.value)} />
            <Button type="submit" className="w-full" loading={create.isPending}>Create</Button>
          </form>
        )}
      </Modal>
    </div>
  )
}
