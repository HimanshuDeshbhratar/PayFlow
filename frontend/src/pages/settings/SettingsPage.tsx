import { useForm } from 'react-hook-form'
import { useAuth } from '@/hooks/useAuth'
import { PageHeader, Card, Input, Button, useToast } from '@/components/ui'

export function SettingsPage() {
  const { user } = useAuth()
  const { toast } = useToast()
  const form = useForm({
    defaultValues: {
      fullName: user?.fullName || '',
      email: user?.email || '',
      timezone: 'UTC',
    },
  })

  return (
    <div>
      <PageHeader title="Settings" description="Profile and workspace preferences." />
      <Card className="max-w-xl">
        <form
          className="space-y-4"
          onSubmit={form.handleSubmit(() => {
            toast({ type: 'success', title: 'Preferences saved locally', description: 'Wire to /api/settings when available.' })
          })}
        >
          <Input label="Full name" {...form.register('fullName')} />
          <Input label="Email" type="email" disabled {...form.register('email')} />
          <Input label="Timezone" {...form.register('timezone')} />
          <div className="rounded-xl border border-border bg-white/[0.03] p-3 text-sm text-muted">
            Role: <span className="text-foreground">{user?.role}</span>
          </div>
          <Button type="submit">Save changes</Button>
        </form>
      </Card>
    </div>
  )
}
