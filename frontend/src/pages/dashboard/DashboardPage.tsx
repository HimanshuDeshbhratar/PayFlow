import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { DollarSign, CheckCircle2, ShieldBan, Store } from 'lucide-react'
import { format, subDays } from 'date-fns'
import { dashboardApi } from '@/api/dashboard'
import {
  PageHeader,
  MetricCard,
  ChartCard,
  DateRangePicker,
  FraudAlertCard,
  TransactionRow,
  SkeletonCard,
  EmptyState,
  Card,
} from '@/components/ui'
import { VolumeBarChart, StatusDonutChart } from '@/components/charts/Charts'
import { useAuth } from '@/hooks/useAuth'
import { formatCurrency } from '@/lib/utils'

export function DashboardPage() {
  const { user } = useAuth()
  const [range, setRange] = useState({
    from: format(subDays(new Date(), 30), 'yyyy-MM-dd'),
    to: format(new Date(), 'yyyy-MM-dd'),
  })

  const { data, isLoading, isError } = useQuery({
    queryKey: ['dashboard', range],
    queryFn: () => dashboardApi.get(range),
    retry: 1,
  })

  const kpis = data?.kpis
  const firstName = useMemo(() => user?.fullName?.split(' ')[0] ?? 'there', [user])

  return (
    <div>
      <PageHeader
        title={`Welcome back, ${firstName}`}
        description="Here’s what’s happening across your payment stack."
        actions={<DateRangePicker from={range.from} to={range.to} onChange={setRange} />}
      />

      {isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      ) : isError || !data ? (
        <EmptyState
          title="Dashboard unavailable"
          description="Could not reach the API. Start the backend on :8080 or check VITE_API_BASE_URL."
        />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <MetricCard
              label="Total Volume"
              value={(kpis?.totalVolumeCents ?? 0) / 100}
              prefix="$"
              changePercent={kpis?.volumeChangePercent}
              icon={<DollarSign className="h-4 w-4" />}
            />
            <MetricCard
              label="Successful Payments"
              value={kpis?.successfulPayments ?? 0}
              changePercent={kpis?.successChangePercent}
              icon={<CheckCircle2 className="h-4 w-4" />}
            />
            <MetricCard
              label="Blocked (Fraud)"
              value={kpis?.blockedFraud ?? 0}
              changePercent={kpis?.blockedChangePercent}
              icon={<ShieldBan className="h-4 w-4" />}
            />
            <MetricCard
              label="Active Merchants"
              value={kpis?.activeMerchants ?? 0}
              changePercent={kpis?.merchantsChangePercent}
              icon={<Store className="h-4 w-4" />}
            />
          </div>

          <div className="mt-6 grid gap-4 xl:grid-cols-3">
            <ChartCard title="Transaction Volume" description="Daily processed volume" className="xl:col-span-2">
              <VolumeBarChart
                data={
                  (data.volumeSeries?.length
                    ? data.volumeSeries
                    : (data as { volumeChart?: { label?: string; date?: string; value?: number; amountCents?: number }[] })
                        .volumeChart?.map((p) => ({
                          label: p.label || p.date || '',
                          value: p.value ?? (p.amountCents ?? 0) / 100,
                        })) || [{ label: '—', value: 0 }])
                }
              />
            </ChartCard>
            <ChartCard title="Payment Status">
              <div className="flex h-full flex-col">
                <div className="min-h-[180px] flex-1">
                  <StatusDonutChart
                    data={
                      data.statusBreakdown?.length
                        ? data.statusBreakdown
                        : [{ status: 'PENDING', count: 1 }]
                    }
                  />
                </div>
                <div className="mt-2 flex flex-wrap gap-3 text-xs text-muted">
                  {(data.statusBreakdown || []).slice(0, 4).map((s) => (
                    <span key={s.status}>
                      {s.status}: <strong className="text-foreground">{s.count}</strong>
                    </span>
                  ))}
                </div>
              </div>
            </ChartCard>
          </div>

          <div className="mt-6 grid gap-4 xl:grid-cols-3">
            <Card className="p-0 xl:col-span-2">
              <div className="border-b border-border px-5 py-4">
                <h3 className="font-semibold">Recent Transactions</h3>
              </div>
              <div>
                {(data.recentTransactions || []).length === 0 ? (
                  <EmptyState title="No recent transactions" description="New payments will appear here." />
                ) : (
                  <>
                    <div className="hidden border-b border-border px-4 py-2 text-xs uppercase tracking-wider text-muted sm:grid sm:grid-cols-6">
                      <span>ID</span>
                      <span>Customer</span>
                      <span>Amount</span>
                      <span>Status</span>
                      <span>Risk</span>
                      <span className="text-right">Time</span>
                    </div>
                    {data.recentTransactions.map((tx) => (
                      <TransactionRow key={tx.id} tx={tx} />
                    ))}
                  </>
                )}
              </div>
            </Card>

            <div className="space-y-3">
              <h3 className="font-semibold">Fraud Alerts</h3>
              {(data.fraudAlerts || []).length === 0 ? (
                <EmptyState title="All clear" description="No open fraud alerts." />
              ) : (
                data.fraudAlerts.slice(0, 5).map((a) => <FraudAlertCard key={a.id} alert={a} />)
              )}
              {kpis && (
                <p className="text-xs text-muted">
                  Volume snapshot: {formatCurrency(kpis.totalVolumeCents)} across selected range.
                </p>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  )
}
