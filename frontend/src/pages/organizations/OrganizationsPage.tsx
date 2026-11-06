import { Building2, Plus } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { organizationApi } from '../../api'
import { useWorkspace } from '../../app/providers/WorkspaceProvider'
import { Button, Card, EmptyState, LoadingSpinner } from '../../components/ui'
import type { Organization } from '../../types'

export function OrganizationsPage() {
  const [organizations, setOrganizations] = useState<Organization[]>([])
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()
  const { setCurrentOrganization } = useWorkspace()

  useEffect(() => {
    const loadOrganizations = async () => {
      try {
        const data = await organizationApi.list()
        setOrganizations(data)
      } catch (error) {
        console.error('Failed to load organizations:', error)
      } finally {
        setLoading(false)
      }
    }
    void loadOrganizations()
  }, [])

  if (loading) return <LoadingSpinner label="Loading organizations" />

  return (
    <>
      <div className="page-header">
        <div className="page-header__top">
          <h1 className="page-header__title">Organizations</h1>
          <Button variant="primary" onClick={() => navigate('/app/organizations/new')}>
            <Plus size={18} />
            New Organization
          </Button>
        </div>
        <p className="page-header__subtitle">
          Manage your organizations and workspace memberships
        </p>
      </div>

      {organizations.length === 0 ? (
        <EmptyState
          icon={Building2}
          title="No organizations yet"
          description="Create your first organization to get started."
          action={
            <Button variant="primary" onClick={() => navigate('/app/organizations/new')}>
              Create Organization
            </Button>
          }
        />
      ) : (
        <div className="grid-list">
          {organizations.map((org) => (
            <Card
              key={org.id}
              className="clickable-card"
              onClick={() => {
                setCurrentOrganization({ id: org.id, name: org.name, membership: null })
                navigate(`/app/organizations/${org.id}`)
              }}
            >
              <div className="org-card">
                <div className="org-card__icon">
                  {org.logoUrl ? (
                    <img src={org.logoUrl} alt={org.name} />
                  ) : (
                    <span>{org.name.charAt(0)}</span>
                  )}
                </div>
                <div className="org-card__info">
                  <h3>{org.name}</h3>
                  {org.description && <p>{org.description}</p>}
                  <div className="org-card__meta">
                    {org.industry && <span>{org.industry}</span>}
                    {org.city && <span>{org.city}, {org.country}</span>}
                  </div>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
    </>
  )
}
