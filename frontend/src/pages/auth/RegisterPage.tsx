import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { useAuth } from '@/hooks/useAuth'
import { Button, Card, Input, useToast } from '@/components/ui'
import { getErrorMessage } from '@/lib/api'

const schema = z.object({
  fullName: z.string().min(2),
  businessName: z.string().min(2).optional().or(z.literal('')),
  email: z.string().email(),
  password: z.string().min(8),
})

type FormValues = z.infer<typeof schema>

export function RegisterPage() {
  const { register: registerUser } = useAuth()
  const navigate = useNavigate()
  const { toast } = useToast()
  const [loading, setLoading] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) })

  const onSubmit = async (values: FormValues) => {
    setLoading(true)
    try {
      await registerUser({
        email: values.email,
        password: values.password,
        fullName: values.fullName,
        businessName: values.businessName || undefined,
      })
      toast({ type: 'success', title: 'Account created' })
      navigate('/dashboard')
    } catch (e) {
      toast({ type: 'error', title: 'Registration failed', description: getErrorMessage(e) })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="relative flex min-h-screen items-center justify-center px-4 py-12">
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_rgba(34,211,238,0.12),_transparent_50%)]" />
      <Card className="relative z-10 w-full max-w-md p-8">
        <Link to="/" className="mb-6 flex items-center gap-2">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-primary font-bold">P</div>
          <span className="font-bold">PayFlow</span>
        </Link>
        <h1 className="text-2xl font-bold">Create account</h1>
        <p className="mt-1 text-sm text-muted">Start processing simulated payments in minutes.</p>

        <form className="mt-6 space-y-4" onSubmit={handleSubmit(onSubmit)}>
          <Input label="Full name" error={errors.fullName?.message} {...register('fullName')} />
          <Input label="Business name" error={errors.businessName?.message} {...register('businessName')} />
          <Input label="Email" type="email" error={errors.email?.message} {...register('email')} />
          <Input label="Password" type="password" error={errors.password?.message} {...register('password')} />
          <Button type="submit" className="w-full" loading={loading}>
            Create account
          </Button>
        </form>

        <p className="mt-6 text-center text-sm text-muted">
          Already have an account?{' '}
          <Link to="/login" className="text-primary-soft hover:underline">
            Sign in
          </Link>
        </p>
      </Card>
    </div>
  )
}
