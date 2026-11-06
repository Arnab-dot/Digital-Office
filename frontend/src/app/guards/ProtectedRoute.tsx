import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../providers/AuthProvider'
import { LoadingSpinner } from '../../components/ui'

interface ProtectedRouteProps {
  children: React.ReactNode
}

export function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { isAuthenticated, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return <LoadingSpinner label="Loading session" />
  }

  if (!isAuthenticated) {
    // Redirect to sign-in, preserving the intended destination
    return <Navigate to="/sign-in" state={{ from: location }} replace />
  }

  return <>{children}</>
}
