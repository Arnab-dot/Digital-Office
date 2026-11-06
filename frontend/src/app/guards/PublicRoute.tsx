import { Navigate } from 'react-router-dom'
import { useAuth } from '../providers/AuthProvider'

interface PublicRouteProps {
  children: React.ReactNode
}

export function PublicRoute({ children }: PublicRouteProps) {
  const { isAuthenticated, loading } = useAuth()

  if (loading) {
    return null
  }

  if (isAuthenticated) {
    // Already signed in, redirect to app
    return <Navigate to="/app" replace />
  }

  return <>{children}</>
}
