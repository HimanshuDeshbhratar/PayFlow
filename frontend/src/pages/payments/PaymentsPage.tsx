import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { Plus } from 'lucide-react'
import { Link } from 'react-router-dom'
import { paymentsApi } from '@/api/payments'
import {
  PageHeader,
  Button,
  DataTable,
  SearchBar,
  Select,
  Modal,
  Input,
  PaymentStatusBadge,
  useToast,
  type Column,
} from '@/components/ui'
import type { Payment } from '@/types'
import { formatCurrency, formatRelativeTime, truncateId } from '@/lib/utils'
import { getErrorMessage } from '@/lib/api'

const createSchema = z.object({
  amount: z.coerce.number().positive(),
  currency: z.string().min(3).max(3),
  customerEmail: z.string().email().optional().or(z.literal('')),
  customerName: z.string().optional(),
  description: z.string().optional(),
})

type CreateForm = z.infer<typeof createSchema>

export function PaymentsPage() {
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('')
  const [open, setOpen] = useState(false)
  const { toast } = useToast()
  const qc = useQueryClient()

  const query = useQuery({
    queryKey: ['payments', search, status],
    queryFn: () => paymentsApi.list({ search: search || undefined, status: status || undefined, size: 50 }),
    retry: 1,
  })

  const form = useForm<CreateForm>({
    resolver: zodResolver(createSchema),
    defaultValues: { currency: 'USD', amount: 99 },
  })

  const create = useMutation({
    mutationFn: (values: CreateForm) =>
      paymentsApi.create({
        amountCents: Math.round(values.amount * 100),
        currency: values.currency.toUpperCase(),
        customerEmail: values.customerEmail || undefined,
        customerName: values.customerName,
        description: values.description,
      }),
    onSuccess: (payment) => {
      toast({ type: 'success', title: 'Payment created', description: `Checkout: /checkout/${payment.id}` })
      setOpen(false)
      form.reset({ currency: 'USD', amount: 99 })
      void qc.invalidateQueries({ queryKey: ['payments'] })
    },
    onError: (e) => toast({ type: 'error', title: 'Create failed', description: getErrorMessage(e) }),
  })

  const columns: Column<Payment>[] = [
    {
      key: 'ref',
      header: 'Reference',
      render: (r) => (
        <Link to={`/checkout/${r.id}`} className="font-mono text-xs text-primary-soft hover:underline">
          #{truncateId(r.reference || r.id)}
        </Link>
      ),
    },
    { key: 'customer', header: 'Customer', render: (r) => r.customerName || r.customerEmail || '—' },
    {
      key: 'amount',
      header: 'Amount',
      render: (r) => <span className="font-semibold">{formatCurrency(r.amountCents, r.currency)}</span>,
    },
    { key: 'status', header: 'Status', render: (r) => <PaymentStatusBadge status={r.status} /> },
    { key: 'time', header: 'Created', render: (r) => <span className="text-muted">{formatRelativeTime(r.createdAt)}</span> },
  ]

  return (
    <div>
      <PageHeader
        title="Payments"
        description="Create checkout sessions and track payment lifecycle."
        actions={
          <Button onClick={() => setOpen(true)}>
            <Plus className="h-4 w-4" /> Create payment
          </Button>
        }
      />
      <div className="mb-4 flex flex-col gap-3 md:flex-row">
        <SearchBar className="md:max-w-sm" placeholder="Search payments" value={search} onChange={(e) => setSearch(e.target.value)} />
        <Select
          options={[
            { value: '', label: 'All statuses' },
            { value: 'PENDING', label: 'Pending' },
            { value: 'COMPLETED', label: 'Completed' },
            { value: 'BLOCKED', label: 'Blocked' },
            { value: 'FAILED', label: 'Failed' },
          ]}
          value={status}
          onChange={(e) => setStatus(e.target.value)}
        />
      </div>
      <DataTable
        columns={columns}
        data={query.data?.content || []}
        loading={query.isLoading}
        keyExtractor={(r) => r.id}
        emptyTitle={query.isError ? 'Unable to load payments' : 'No payments yet'}
        emptyDescription={query.isError ? 'Start the backend API to load live data.' : 'Create a payment to open checkout.'}
      />

      <Modal open={open} onClose={() => setOpen(false)} title="Create payment" description="Generates a checkout link for simulated payment.">
        <form className="space-y-4" onSubmit={form.handleSubmit((v) => create.mutate(v))}>
          <Input label="Amount" type="number" step="0.01" error={form.formState.errors.amount?.message} {...form.register('amount')} />
          <Input label="Currency" error={form.formState.errors.currency?.message} {...form.register('currency')} />
          <Input label="Customer name" {...form.register('customerName')} />
          <Input label="Customer email" type="email" {...form.register('customerEmail')} />
          <Input label="Description" {...form.register('description')} />
          <Button type="submit" className="w-full" loading={create.isPending}>
            Create
          </Button>
        </form>
      </Modal>
    </div>
  )
}
