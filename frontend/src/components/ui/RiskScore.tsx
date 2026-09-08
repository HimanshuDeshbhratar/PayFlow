import { cn } from '@/lib/utils'

interface RiskScoreProps {
  score: number
  max?: number
  level?: string | null
  className?: string
  size?: number
}

export function RiskScore({ score, max = 100, level, className, size = 160 }: RiskScoreProps) {
  const pct = Math.min(100, Math.max(0, (score / max) * 100))
  const stroke = 10
  const radius = (size - stroke) / 2
  const circumference = 2 * Math.PI * radius
  const offset = circumference - (pct / 100) * circumference

  const color =
    pct >= 80 ? '#ef4444' : pct >= 60 ? '#f59e0b' : pct >= 40 ? '#a855f7' : '#22c55e'

  return (
    <div className={cn('relative mx-auto flex flex-col items-center', className)} style={{ width: size, height: size }}>
      <svg width={size} height={size} className="-rotate-90">
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke="rgba(148,163,184,0.12)"
          strokeWidth={stroke}
        />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke={color}
          strokeWidth={stroke}
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          style={{ filter: `drop-shadow(0 0 8px ${color}66)` }}
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <span className="text-3xl font-bold">{score}</span>
        <span className="text-xs text-muted">/{max}</span>
        {level && (
          <span className="mt-1 text-xs font-semibold uppercase tracking-wide" style={{ color }}>
            {level.replace('_', ' ')} Risk
          </span>
        )}
      </div>
    </div>
  )
}
