import api from '@/lib/api'
import type { ApiKey, NotificationItem, PageResponse, WebhookEndpoint, AuditLog, ReportSummary } from '@/types'

export const notificationsApi = {
  list: (params?: { page?: number; size?: number }) =>
    api.get<PageResponse<NotificationItem>>('/notifications', { params }).then((r) => r.data),
  markRead: (id: string) => api.put(`/notifications/${id}/read`).then((r) => r.data),
  markAllRead: () => api.put('/notifications/read-all').then((r) => r.data).catch(() => null),
}

export const developerApi = {
  apiKeys: () => api.get<ApiKey[]>('/developer/api-keys').then((r) => r.data),
  createApiKey: (name: string) => api.post<ApiKey>('/developer/api-keys', { name }).then((r) => r.data),
  deleteApiKey: (id: string) => api.delete(`/developer/api-keys/${id}`).then((r) => r.data),
  webhooks: () => api.get<WebhookEndpoint[]>('/developer/webhooks').then((r) => r.data),
  createWebhook: (payload: { url: string; events: string[] }) =>
    api.post<WebhookEndpoint>('/developer/webhooks', payload).then((r) => r.data),
  deleteWebhook: (id: string) => api.delete(`/developer/webhooks/${id}`).then((r) => r.data),
}

export const reportsApi = {
  list: () => api.get<ReportSummary[]>('/reports').then((r) => r.data),
}

export const auditApi = {
  list: (params?: { page?: number; size?: number; search?: string }) =>
    api.get<PageResponse<AuditLog>>('/audit-logs', { params }).then((r) => r.data),
}

export const settingsApi = {
  get: () => api.get('/settings').then((r) => r.data),
  update: (payload: Record<string, unknown>) => api.put('/settings', payload).then((r) => r.data),
}
