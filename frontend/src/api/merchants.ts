import api from '@/lib/api'
import type { Merchant, PageResponse } from '@/types'

export const merchantsApi = {
  list: (params?: { page?: number; size?: number; search?: string; status?: string }) =>
    api.get<PageResponse<Merchant>>('/merchants', { params }).then((r) => r.data),
  get: (id: string) => api.get<Merchant>(`/merchants/${id}`).then((r) => r.data),
}
