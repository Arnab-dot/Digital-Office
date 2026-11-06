import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { AuthStorage } from '../../lib/auth/storage'
import type { User, AuthState } from '../../lib/auth/types'
import { beginLogin, completeLogin } from '../../lib/auth/keycloak'

interface AuthContextValue extends AuthState {
  signIn: () => Promise<void>
  register: () => Promise<void>
  signOut: () => void
  isAuthenticated: boolean
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}

interface AuthProviderProps {
  children: ReactNode
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    void (async () => {
      try {
        const callbackUser = await completeLogin()
        if (callbackUser) setUser(callbackUser)
        else {
          const storedUser = AuthStorage.getUser()
          const token = AuthStorage.getAccessToken()
          if (storedUser && token) setUser(JSON.parse(storedUser) as User)
        }
      } catch (err) {
        AuthStorage.clear()
        setError(err instanceof Error ? err.message : 'Authentication failed')
      } finally {
        setLoading(false)
      }
    })()
  }, [])

  const signIn = () => beginLogin()
  const register = () => beginLogin(true)

  const signOut = () => {
    AuthStorage.clear()
    setUser(null)
    setError(null)
  }

  const value: AuthContextValue = {
    user,
    loading,
    error,
    signIn,
    register,
    signOut,
    isAuthenticated: !!user,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
