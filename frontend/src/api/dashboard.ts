import api from '@/lib/api'
import type { DashboardResponse } from '@/types'

export const dashboardApi = {
  get: (params?: { from?: string; to?: string }) =>
    api.get<DashboardResponse>('/dashboard', { params }).then((r) => r.data),
}
