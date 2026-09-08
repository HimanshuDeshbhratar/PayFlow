import { useQuery } from '@tanstack/react-query'
import { ShieldAlert, ShieldBan, AlertTriangle, DollarSign } from 'lucide-react'
import { fraudApi } from '@/api/fraud'
import {
  PageHeader,
  MetricCard,
  ChartCard,
  FraudAlertCard,
  DataTable,
  Badge,
  SkeletonCard,
  EmptyState,
  type Column,
} from '@/components/ui'
import { TrendLineChart } from '@/components/charts/Charts'
import type { FraudRule } from '@/types'
import { formatCurrency } from '@/lib/utils'

export function RiskFraudPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['fraud-dashboard'],
    queryFn: () => fraudApi.dashboard(),
    retry: 1,
  })

  const ruleColumns: Column<FraudRule>[] = [
    { key: 'code', header: 'Code', render: (r) => <span className="font-mono text-xs">{r.code}</span> },
    { key: 'name', header: 'Rule', render: (r) => r.name },
    { key: 'weight', header: 'Weight', render: (r) => <Badge variant="primary">+{r.weight}</Badge> },
    {
      key: 'enabled',
      header: 'Status',
      render: (r) => <Badge variant={r.enabled ? 'success' : 'muted'}>{r.enabled ? 'Enabled' : 'Disabled'}</Badge>,
    },
  ]

  if (isLoading) {
    return (
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <SkeletonCard key={i} />
        ))}
      </div>
    )
  }

  if (isError || !data) {
    return <EmptyState title="Risk data unavailable" description="Connect to the backend fraud APIs to load live metrics." />
  }

  return (
    <div>
      <PageHeader title="Risk & Fraud" description="Monitor blocked attempts, open alerts, and scoring rules." />
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard label="Fraud rate" value={data.fraudRatePercent} suffix="%" icon={<ShieldAlert className="h-4 w-4" />} />
        <MetricCard label="Blocked" value={data.blockedCount} icon={<ShieldBan className="h-4 w-4" />} />
        <MetricCard label="High risk" value={data.highRiskCount} icon={<AlertTriangle className="h-4 w-4" />} />
        <MetricCard
          label="Prevented loss"
          value={(data.preventedLossCents || 0) / 100}
          prefix="$"
          icon={<DollarSign className="h-4 w-4" />}
        />
      </div>

      <div className="mt-6 grid gap-4 lg:grid-cols-2">
        <ChartCard title="Fraud trend">
          <TrendLineChart data={data.trendSeries?.length ? data.trendSeries : [{ label: '—', value: 0 }]} />
        </ChartCard>
        <div className="space-y-3">
          <h3 className="font-semibold">Open alerts</h3>
          {(data.alerts || []).length === 0 ? (
            <EmptyState title="No alerts" description="Fraud alerts will appear here." />
          ) : (
            data.alerts.slice(0, 4).map((a) => <FraudAlertCard key={a.id} alert={a} />)
          )}
        </div>
      </div>

      <div className="mt-6">
        <h3 className="mb-3 font-semibold">Fraud rules</h3>
        <DataTable columns={ruleColumns} data={data.rules || []} keyExtractor={(r) => r.id} emptyTitle="No rules" />
        <p className="mt-2 text-xs text-muted">
          Prevented loss snapshot: {formatCurrency(data.preventedLossCents || 0)}
        </p>
      </div>
    </div>
  )
}
