import { ArrowLeft } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { clientApi } from '../../api'
import { useWorkspace } from '../../app/providers/WorkspaceProvider'
import { Alert, Button, Card, Input, TextArea } from '../../components/ui'

export function CreateClientPage() {
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [industry, setIndustry] = useState('')
  const [website, setWebsite] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [address, setAddress] = useState('')
  const [city, setCity] = useState('')
  const [country, setCountry] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()
  const { currentOrganization } = useWorkspace()

  const orgId = currentOrganization?.id

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (!orgId) {
      setError('Please select or create an organization first.')
      return
    }
    setError(null)
    setLoading(true)

    try {
      const client = await clientApi.create(orgId, {
        name,
        description: description || undefined,
        industry: industry || undefined,
        website: website || undefined,
        email: email || undefined,
        phone: phone || undefined,
        address: address || undefined,
        city: city || undefined,
        country: country || undefined,
      })

      navigate(`/app/clients/${client.id}`)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create client')
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <div className="page-header">
        <div className="page-header__top">
          <Button variant="ghost" onClick={() => navigate('/app/clients')}>
            <ArrowLeft size={18} />
            Back to Clients
          </Button>
        </div>
        <h1 className="page-header__title">Add New Client</h1>
        <p className="page-header__subtitle">
          Create a new client record to track relationships and projects
        </p>
      </div>

      <Card style={{ maxWidth: '800px' }}>
        {error && <Alert variant="error" onClose={() => setError(null)}>{error}</Alert>}

        <form onSubmit={handleSubmit} className="stack-lg">
          <Input
            label="Client Name"
            placeholder="Acme Corporation"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            autoFocus
          />

          <TextArea
            label="Description"
            placeholder="Brief description of the client..."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={3}
          />

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-md)' }}>
            <Input
              label="Industry"
              placeholder="Technology"
              value={industry}
              onChange={(e) => setIndustry(e.target.value)}
            />

            <Input
              type="url"
              label="Website"
              placeholder="https://example.com"
              value={website}
              onChange={(e) => setWebsite(e.target.value)}
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-md)' }}>
            <Input
              type="email"
              label="Email"
              placeholder="contact@acme.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />

            <Input
              type="tel"
              label="Phone"
              placeholder="+1 (555) 123-4567"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
            />
          </div>

          <Input
            label="Address"
            placeholder="123 Main Street"
            value={address}
            onChange={(e) => setAddress(e.target.value)}
          />

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-md)' }}>
            <Input
              label="City"
              placeholder="San Francisco"
              value={city}
              onChange={(e) => setCity(e.target.value)}
            />

            <Input
              label="Country"
              placeholder="United States"
              value={country}
              onChange={(e) => setCountry(e.target.value)}
            />
          </div>

          <div style={{ display: 'flex', gap: 'var(--space-md)', justifyContent: 'flex-end', marginTop: 'var(--space-lg)' }}>
            <Button type="button" variant="secondary" onClick={() => navigate('/app/clients')}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" loading={loading}>
              Add Client
            </Button>
          </div>
        </form>
      </Card>
    </>
  )
}
