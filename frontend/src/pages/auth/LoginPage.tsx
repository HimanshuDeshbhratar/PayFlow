import { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { useAuth } from '@/hooks/useAuth'
import { Button, Card, Input, useToast } from '@/components/ui'
import { getErrorMessage } from '@/lib/api'

const schema = z.object({
  email: z.string().email(),
  password: z.string().min(6),
})

type FormValues = z.infer<typeof schema>

const demos = [
  { email: 'admin@payflow.demo', password: 'PayFlowAdmin!2026', label: 'Admin' },
  { email: 'merchant@payflow.demo', password: 'PayFlowMerchant!2026', label: 'Merchant' },
  { email: 'risk@payflow.demo', password: 'PayFlowRisk!2026', label: 'Risk' },
]

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const { toast } = useToast()
  const [loading, setLoading] = useState(false)
  const from = (location.state as { from?: string } | null)?.from || '/dashboard'

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) })

  const onSubmit = async (values: FormValues) => {
    setLoading(true)
    try {
      await login(values.email, values.password)
      toast({ type: 'success', title: 'Welcome back' })
      navigate(from, { replace: true })
    } catch (e) {
      toast({ type: 'error', title: 'Login failed', description: getErrorMessage(e) })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="relative flex min-h-screen items-center justify-center px-4 py-12">
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_rgba(124,58,237,0.25),_transparent_55%)]" />
      <Card className="relative z-10 w-full max-w-md p-8">
        <Link to="/" className="mb-6 flex items-center gap-2">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-primary font-bold">P</div>
          <span className="font-bold">PayFlow</span>
        </Link>
        <h1 className="text-2xl font-bold">Sign in</h1>
        <p className="mt-1 text-sm text-muted">Access your payment operations workspace.</p>

        <form className="mt-6 space-y-4" onSubmit={handleSubmit(onSubmit)}>
          <Input label="Email" type="email" error={errors.email?.message} {...register('email')} />
          <Input label="Password" type="password" error={errors.password?.message} {...register('password')} />
          <Button type="submit" className="w-full" loading={loading}>
            Sign in
          </Button>
        </form>

        <div className="mt-6">
          <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-muted">Demo accounts</p>
          <div className="flex flex-wrap gap-2">
            {demos.map((d) => (
              <button
                key={d.email}
                type="button"
                className="rounded-lg border border-border bg-white/5 px-2.5 py-1.5 text-xs hover:border-primary/40"
                onClick={() => {
                  setValue('email', d.email)
                  setValue('password', d.password)
                }}
              >
                {d.label}
              </button>
            ))}
          </div>
        </div>

        <p className="mt-6 text-center text-sm text-muted">
          No account?{' '}
          <Link to="/register" className="text-primary-soft hover:underline">
            Register
          </Link>
        </p>
      </Card>
    </div>
  )
}
