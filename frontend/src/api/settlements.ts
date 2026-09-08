import api from '@/lib/api'
import type { LedgerAccount, LedgerEntry, PageResponse, Settlement } from '@/types'

export const settlementsApi = {
  list: (params?: { page?: number; size?: number; status?: string }) =>
    api.get<PageResponse<Settlement>>('/settlements', { params }).then((r) => r.data),
}

export const ledgerApi = {
  accounts: (params?: { page?: number; size?: number }) =>
    api.get<PageResponse<LedgerAccount>>('/ledger/accounts', { params }).then((r) => r.data),
  entries: (accountId: string, params?: { page?: number; size?: number }) =>
    api
      .get<PageResponse<LedgerEntry>>(`/ledger/accounts/${accountId}/entries`, { params })
      .then((r) => r.data),
}
