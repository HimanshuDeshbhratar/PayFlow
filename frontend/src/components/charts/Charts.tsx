import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  PieChart,
  Pie,
  Cell,
  LineChart,
  Line,
  AreaChart,
  Area,
} from 'recharts'
import type { ChartPoint, PaymentStatusBreakdown } from '@/types'

const tooltipStyle = {
  backgroundColor: 'rgba(15, 18, 32, 0.95)',
  border: '1px solid rgba(168, 85, 247, 0.35)',
  borderRadius: 12,
  color: '#f8fafc',
  fontSize: 12,
}

const STATUS_COLORS: Record<string, string> = {
  COMPLETED: '#22c55e',
  APPROVED: '#22c55e',
  BLOCKED: '#ef4444',
  PENDING: '#f59e0b',
  FAILED: '#f43f5e',
  PROCESSING: '#a855f7',
  FRAUD_SCREENING: '#22d3ee',
}

export function VolumeBarChart({ data }: { data: ChartPoint[] }) {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <BarChart data={data} margin={{ top: 8, right: 8, left: -12, bottom: 0 }}>
        <defs>
          <linearGradient id="barGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#A855F7" stopOpacity={1} />
            <stop offset="100%" stopColor="#7C3AED" stopOpacity={0.55} />
          </linearGradient>
        </defs>
        <CartesianGrid stroke="rgba(148,163,184,0.08)" vertical={false} />
        <XAxis dataKey="label" tick={{ fill: '#94a3b8', fontSize: 11 }} axisLine={false} tickLine={false} />
        <YAxis tick={{ fill: '#94a3b8', fontSize: 11 }} axisLine={false} tickLine={false} />
        <Tooltip contentStyle={tooltipStyle} cursor={{ fill: 'rgba(124,58,237,0.08)' }} />
        <Bar dataKey="value" fill="url(#barGrad)" radius={[8, 8, 4, 4]} maxBarSize={36} />
      </BarChart>
    </ResponsiveContainer>
  )
}

export function StatusDonutChart({ data }: { data: PaymentStatusBreakdown[] }) {
  const chartData = data.map((d) => ({ name: d.status, value: d.count }))
  return (
    <ResponsiveContainer width="100%" height="100%">
      <PieChart>
        <Pie data={chartData} dataKey="value" nameKey="name" innerRadius="62%" outerRadius="82%" paddingAngle={3}>
          {chartData.map((entry) => (
            <Cell key={entry.name} fill={STATUS_COLORS[entry.name] || '#6366f1'} />
          ))}
        </Pie>
        <Tooltip contentStyle={tooltipStyle} />
      </PieChart>
    </ResponsiveContainer>
  )
}

export function MiniAreaChart({ data }: { data: ChartPoint[] }) {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <AreaChart data={data}>
        <defs>
          <linearGradient id="areaGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#A855F7" stopOpacity={0.45} />
            <stop offset="100%" stopColor="#A855F7" stopOpacity={0} />
          </linearGradient>
        </defs>
        <Area type="monotone" dataKey="value" stroke="#A855F7" fill="url(#areaGrad)" strokeWidth={2} />
      </AreaChart>
    </ResponsiveContainer>
  )
}

export function TrendLineChart({ data }: { data: ChartPoint[] }) {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <LineChart data={data} margin={{ top: 8, right: 8, left: -12, bottom: 0 }}>
        <CartesianGrid stroke="rgba(148,163,184,0.08)" vertical={false} />
        <XAxis dataKey="label" tick={{ fill: '#94a3b8', fontSize: 11 }} axisLine={false} tickLine={false} />
        <YAxis tick={{ fill: '#94a3b8', fontSize: 11 }} axisLine={false} tickLine={false} />
        <Tooltip contentStyle={tooltipStyle} />
        <Line type="monotone" dataKey="value" stroke="#22d3ee" strokeWidth={2} dot={false} />
      </LineChart>
    </ResponsiveContainer>
  )
}
