import { motion, useMotionValue, useSpring, useTransform, animate } from 'framer-motion'
import { useEffect } from 'react'
import { TrendingDown, TrendingUp } from 'lucide-react'
import { Card } from './Card'
import { cn, formatNumber } from '@/lib/utils'

interface MetricCardProps {
  label: string
  value: number | string
  prefix?: string
  suffix?: string
  changePercent?: number
  icon?: React.ReactNode
  className?: string
  formatAsCurrency?: boolean
}

function CountUp({ value }: { value: number }) {
  const mv = useMotionValue(0)
  const spring = useSpring(mv, { stiffness: 80, damping: 20 })
  const display = useTransform(spring, (v) => formatNumber(Math.round(v)))

  useEffect(() => {
    const controls = animate(mv, value, { duration: 1.1, ease: 'easeOut' })
    return controls.stop
  }, [mv, value])

  return <motion.span>{display}</motion.span>
}

export function MetricCard({
  label,
  value,
  prefix,
  suffix,
  changePercent,
  icon,
  className,
}: MetricCardProps) {
  const positive = (changePercent ?? 0) >= 0
  return (
    <Card className={cn('relative overflow-hidden', className)}>
      <div className="absolute -right-6 -top-6 h-24 w-24 rounded-full bg-primary/10 blur-2xl" />
      <div className="flex items-start justify-between">
        <p className="text-sm text-muted">{label}</p>
        {icon && <div className="rounded-xl bg-primary/15 p-2 text-primary-soft">{icon}</div>}
      </div>
      <div className="mt-3 flex items-end gap-2">
        <p className="text-2xl font-bold tracking-tight md:text-3xl">
          {prefix}
          {typeof value === 'number' ? <CountUp value={value} /> : value}
          {suffix}
        </p>
      </div>
      {changePercent != null && (
        <div className={cn('mt-2 flex items-center gap-1 text-xs font-medium', positive ? 'text-success' : 'text-danger')}>
          {positive ? <TrendingUp className="h-3.5 w-3.5" /> : <TrendingDown className="h-3.5 w-3.5" />}
          {positive ? '+' : ''}
          {changePercent.toFixed(1)}%
          <span className="text-muted font-normal">vs last period</span>
        </div>
      )}
    </Card>
  )
}
