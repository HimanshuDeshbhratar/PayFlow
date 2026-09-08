import api from '@/lib/api'
import type { Customer, PageResponse } from '@/types'

export const customersApi = {
  list: (params?: { page?: number; size?: number; search?: string }) =>
    api.get<PageResponse<Customer>>('/customers', { params }).then((r) => r.data),
  get: (id: string) => api.get<Customer>(`/customers/${id}`).then((r) => r.data),
  block: (id: string, reason?: string) =>
    api.post(`/customers/${id}/block`, { reason }).then((r) => r.data),
}
