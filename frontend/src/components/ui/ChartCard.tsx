import { Card, CardDescription, CardHeader, CardTitle } from './Card'
import { cn } from '@/lib/utils'

interface ChartCardProps {
  title: string
  description?: string
  actions?: React.ReactNode
  children: React.ReactNode
  className?: string
}

export function ChartCard({ title, description, actions, children, className }: ChartCardProps) {
  return (
    <Card className={cn('flex flex-col', className)}>
      <CardHeader>
        <div>
          <CardTitle>{title}</CardTitle>
          {description && <CardDescription className="mt-1">{description}</CardDescription>}
        </div>
        {actions}
      </CardHeader>
      <div className="min-h-[220px] flex-1">{children}</div>
    </Card>
  )
}
