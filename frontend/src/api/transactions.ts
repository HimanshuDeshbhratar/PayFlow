import api from '@/lib/api'
import type { PageResponse, Transaction, TransactionDetail } from '@/types'

export interface TransactionFilters {
  page?: number
  size?: number
  status?: string
  riskLevel?: string
  search?: string
  from?: string
  to?: string
}

export const transactionsApi = {
  list: (params?: TransactionFilters) =>
    api.get<PageResponse<Transaction>>('/transactions', { params }).then((r) => r.data),
  get: (id: string) => api.get<TransactionDetail>(`/transactions/${id}`).then((r) => r.data),
  markSafe: (id: string) => api.post(`/transactions/${id}/mark-safe`).then((r) => r.data),
  block: (id: string) => api.post(`/transactions/${id}/block`).then((r) => r.data),
}
