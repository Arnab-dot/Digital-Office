import { Building2 } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../app/providers/AuthProvider'
import { Alert, Button } from '../../components/ui'

export function RegisterPage() {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const { register } = useAuth()

  const handleSubmit = async () => {
    setError(null)
    setLoading(true)

    try {
      await register()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Registration failed. Please try again.')
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
            <h1>Create your account</h1>
            <p>Get started with Digital Office OS</p>
          </div>

          {error && (
            <Alert variant="error" onClose={() => setError(null)}>
              {error}
            </Alert>
          )}

          <div className="auth-form">
            <Button type="button" onClick={() => void handleSubmit()} loading={loading} style={{ width: '100%' }}>
              Register with Keycloak
            </Button>
          </div>

          <div className="auth-footer">
            <p>
              Already have an account?{' '}
              <Link to="/sign-in" className="auth-link">
                Sign in
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
            <strong>Authentication:</strong> Account registration is handled securely by Keycloak.
          </p>
        </div>
      </div>
    </div>
  )
}
