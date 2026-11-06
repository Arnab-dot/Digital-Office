import { ArrowLeft, Building2 } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { organizationApi } from '../../api'
import { Alert, Button, Card, Input, TextArea } from '../../components/ui'

export function CreateOrganizationPage() {
  const [name, setName] = useState('')
  const [slug, setSlug] = useState('')
  const [slugModified, setSlugModified] = useState(false)
  const [description, setDescription] = useState('')
  const [website, setWebsite] = useState('')
  const [industry, setIndustry] = useState('')
  const [size, setSize] = useState('')
  const [city, setCity] = useState('')
  const [country, setCountry] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()

  const handleNameChange = (val: string) => {
    setName(val)
    if (!slugModified) {
      setSlug(val.toLowerCase().trim().replace(/[^a-z0-9-]+/g, '-').replace(/^-+|-+$/g, ''))
    }
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setLoading(true)

    try {
      const org = await organizationApi.create({
        name,
        slug: slug || undefined,
        description: description || undefined,
        website: website || undefined,
        industry: industry || undefined,
        size: size || undefined,
        city: city || undefined,
        country: country || undefined,
      })

      navigate('/app/organizations', { state: { message: `Organization "${org.name}" created successfully!` } })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create organization')
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <div className="page-header">
        <div className="page-header__top">
          <Button variant="ghost" onClick={() => navigate('/app/organizations')}>
            <ArrowLeft size={18} />
            Back to Organizations
          </Button>
        </div>
        <h1 className="page-header__title">Create New Organization</h1>
        <p className="page-header__subtitle">
          Set up a new organization workspace for your team
        </p>
      </div>

      <div
        style={{
          maxWidth: '800px',
          marginBottom: '20px',
          padding: '16px 20px',
          background: 'linear-gradient(135deg, rgba(99, 102, 241, 0.1), rgba(168, 85, 247, 0.08))',
          border: '1px solid rgba(99, 102, 241, 0.25)',
          borderRadius: '10px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          gap: '16px',
        }}
      >
        <div>
          <div style={{ fontWeight: 700, fontSize: '0.95rem', color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Building2 size={18} color="var(--color-primary)" />
            Corporate or Startup Entity Onboarding?
          </div>
          <p style={{ margin: '4px 0 0', fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
            Need statutory legal registration (CIN/EIN), tax IDs (GST/VAT), KYC document uploads, and tailored subscription tiers?
          </p>
        </div>
        <Button variant="primary" onClick={() => navigate('/app/onboarding')} style={{ whiteSpace: 'nowrap' }}>
          Open Onboarding Wizard &rarr;
        </Button>
      </div>

      <Card style={{ maxWidth: '800px' }}>
        {error && <Alert variant="error" onClose={() => setError(null)}>{error}</Alert>}

        <form onSubmit={handleSubmit} className="stack-lg">
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-md)' }}>
            <Input
              label="Organization Name"
              placeholder="My Company Inc."
              value={name}
              onChange={(e) => handleNameChange(e.target.value)}
              required
              autoFocus
            />

            <Input
              label="Workspace Slug / URL Identifier"
              placeholder="my-company"
              value={slug}
              onChange={(e) => {
                setSlugModified(true)
                setSlug(e.target.value.toLowerCase().replace(/[^a-z0-9-]+/g, ''))
              }}
              required
            />
          </div>

          <TextArea
            label="Description"
            placeholder="Brief description of your organization..."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={3}
          />

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-md)' }}>
            <Input
              type="url"
              label="Website"
              placeholder="https://mycompany.com"
              value={website}
              onChange={(e) => setWebsite(e.target.value)}
            />

            <Input
              label="Industry"
              placeholder="Technology"
              value={industry}
              onChange={(e) => setIndustry(e.target.value)}
            />
          </div>

          <Input
            label="Company Size"
            placeholder="1-10, 11-50, 51-200, etc."
            value={size}
            onChange={(e) => setSize(e.target.value)}
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
            <Button type="button" variant="secondary" onClick={() => navigate('/app/organizations')}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" loading={loading}>
              Create Organization
            </Button>
          </div>
        </form>
      </Card>
    </>
  )
}
