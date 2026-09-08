import api from '@/lib/api'
import type {
  CheckoutSession,
  CreatePaymentRequest,
  PageResponse,
  Payment,
  ProcessCheckoutRequest,
} from '@/types'

export interface PaymentFilters {
  page?: number
  size?: number
  status?: string
  search?: string
  from?: string
  to?: string
}

export const paymentsApi = {
  list: (params?: PaymentFilters) =>
    api.get<PageResponse<Payment>>('/payments', { params }).then((r) => r.data),
  get: (id: string) => api.get<Payment>(`/payments/${id}`).then((r) => r.data),
  create: (payload: CreatePaymentRequest) => api.post<Payment>('/payments', payload).then((r) => r.data),
  refund: (id: string, amountCents?: number) =>
    api.post(`/payments/${id}/refund`, amountCents != null ? { amountCents } : {}).then((r) => r.data),
  checkout: (paymentId: string) =>
    api.get<CheckoutSession>(`/payments/${paymentId}/checkout`).then((r) => r.data),
  processCheckout: (paymentId: string, payload: ProcessCheckoutRequest) =>
    api.post<Payment>(`/payments/${paymentId}/checkout`, payload).then((r) => r.data),
}
