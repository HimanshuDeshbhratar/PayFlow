import api from '@/lib/api'
import type { FraudAlert, FraudDashboard, FraudRule, PageResponse } from '@/types'

export const fraudApi = {
  dashboard: () => api.get<FraudDashboard>('/fraud/dashboard').then((r) => r.data).catch(async () => {
    const [alerts, rules] = await Promise.all([
      api.get<PageResponse<FraudAlert>>('/fraud/alerts', { params: { size: 20 } }).then((r) => r.data),
      api.get<FraudRule[]>('/fraud/rules').then((r) => r.data).catch(() => [] as FraudRule[]),
    ])
    return {
      fraudRatePercent: 0,
      blockedCount: 0,
      highRiskCount: alerts.content.filter((a) => a.severity === 'HIGH' || a.severity === 'CRITICAL').length,
      preventedLossCents: 0,
      alerts: alerts.content,
      rules: Array.isArray(rules) ? rules : [],
      trendSeries: [],
    } satisfies FraudDashboard
  }),
  alerts: (params?: { page?: number; size?: number; status?: string }) =>
    api.get<PageResponse<FraudAlert>>('/fraud/alerts', { params }).then((r) => r.data),
  rules: () => api.get<FraudRule[]>('/fraud/rules').then((r) => r.data),
  createRule: (payload: Partial<FraudRule>) => api.post<FraudRule>('/fraud/rules', payload).then((r) => r.data),
  updateAlert: (id: string, status: string) =>
    api.patch(`/fraud/alerts/${id}`, { status }).then((r) => r.data),
}
