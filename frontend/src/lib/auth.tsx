import {createContext, type ReactNode, useCallback, useContext, useEffect, useMemo, useState} from 'react'
import {get, post, setUnauthorizedHandler, tokenStore} from './api'
import type {User} from './types'

interface AuthContextValue {
  user: User | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string, displayName: string) => Promise<void>
  logout: () => void
}

interface AuthResponse {
  token: string
  user: User
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  const logout = useCallback(() => {
    tokenStore.clear()
    setUser(null)
  }, [])

  useEffect(() => {
    setUnauthorizedHandler(logout)
    if (!tokenStore.get()) {
      setLoading(false)
      return
    }
    get<User>('/auth/me')
      .then(setUser)
      .catch(() => tokenStore.clear())
      .finally(() => setLoading(false))
  }, [logout])

  const accept = (res: AuthResponse) => {
    tokenStore.set(res.token)
    setUser(res.user)
  }

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      loading,
      logout,
      login: async (email, password) => accept(await post<AuthResponse>('/auth/login', { email, password })),
      register: async (email, password, displayName) =>
        accept(await post<AuthResponse>('/auth/register', { email, password, displayName })),
    }),
    [user, loading, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
