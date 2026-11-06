import { Building2 } from 'lucide-react'
import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../../app/providers/AuthProvider'
import { Alert, Button } from '../../components/ui'

export function SignInPage() {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const { signIn } = useAuth()
  const location = useLocation()
  const message = (location.state as { message?: string })?.message

  const handleSubmit = async () => {
    setError(null)
    setLoading(true)

    try {
      await signIn()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Sign-in failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-container">
        <div className="auth-brand">
          <Building2 size={32} />
          <span>Digital Office OS</span>
        </div>

        <div className="auth-card">
          <div className="auth-header">
            <h1>Welcome back</h1>
            <p>Sign in to your workspace</p>
          </div>

          {message && (
            <Alert variant="success">
              {message}
            </Alert>
          )}

          {error && (
            <Alert variant="error" onClose={() => setError(null)}>
              {error}
            </Alert>
          )}

          <div className="auth-form">
            <Button type="button" onClick={() => void handleSubmit()} loading={loading} style={{ width: '100%' }}>
              Continue with Keycloak
            </Button>
          </div>

          <div className="auth-footer">
            <p>
              Don't have an account?{' '}
              <Link to="/register" className="auth-link">
                Create account
              </Link>
            </p>
            <p>
              <Link to="/" className="auth-link">
                ← Back to home
              </Link>
            </p>
          </div>
        </div>

        <div className="auth-notice">
          <p>
            <strong>Authentication:</strong> You will be redirected to Keycloak to sign in securely.
          </p>
        </div>
      </div>
    </div>
  )
}
