import { createContext, useCallback, useEffect, useMemo, useState } from 'react'
import { authApi } from '@/api/auth'
import { tokenStore } from '@/lib/api'
import type { User } from '@/types'

interface AuthContextValue {
  user: User | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (payload: { email: string; password: string; fullName: string; businessName?: string }) => Promise<void>
  logout: () => Promise<void>
  refreshUser: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  const refreshUser = useCallback(async () => {
    if (!tokenStore.getAccess()) {
      setUser(null)
      return
    }
    try {
      const me = await authApi.me()
      setUser(me)
    } catch {
      tokenStore.clear()
      setUser(null)
    }
  }, [])

  useEffect(() => {
    void (async () => {
      await refreshUser()
      setLoading(false)
    })()
  }, [refreshUser])

  const login = useCallback(async (email: string, password: string) => {
    const res = await authApi.login({ email, password })
    tokenStore.set(res.accessToken, res.refreshToken)
    setUser(res.user)
  }, [])

  const register = useCallback(
    async (payload: { email: string; password: string; fullName: string; businessName?: string }) => {
      const res = await authApi.register(payload)
      tokenStore.set(res.accessToken, res.refreshToken)
      setUser(res.user)
    },
    [],
  )

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } finally {
      tokenStore.clear()
      setUser(null)
    }
  }, [])

  const value = useMemo(
    () => ({ user, loading, login, register, logout, refreshUser }),
    [user, loading, login, register, logout, refreshUser],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
