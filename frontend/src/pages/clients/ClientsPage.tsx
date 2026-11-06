import { Building2, Plus, Search } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { clientApi } from '../../api'
import { useWorkspace } from '../../app/providers/WorkspaceProvider'
import { Badge, Button, Card, EmptyState, Input, LoadingSpinner } from '../../components/ui'
import type { Client } from '../../types'

export function ClientsPage() {
  const [clients, setClients] = useState<Client[]>([])
  const [loading, setLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState('')
  const navigate = useNavigate()
  const { currentOrganization } = useWorkspace()

  const orgId = currentOrganization?.id

  useEffect(() => {
    if (!orgId) {
      setClients([])
      setLoading(false)
      return
    }

    const loadClients = async () => {
      setLoading(true)
      try {
        const response = await clientApi.list(orgId)
        const clientList = Array.isArray(response)
          ? response
          : Array.isArray((response as any)?.content)
          ? (response as any).content
          : []
        setClients(clientList)
      } catch (error) {
        console.error('Failed to load clients:', error)
        setClients([])
      } finally {
        setLoading(false)
      }
    }
    void loadClients()
  }, [orgId])

  const filteredClients = (clients || []).filter((client) =>
    (client?.name || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
    (client?.industry || '').toLowerCase().includes(searchQuery.toLowerCase())
  )

  if (loading) return <LoadingSpinner label="Loading clients" />

  return (
    <>
      <div className="page-header">
        <div className="page-header__top">
          <h1 className="page-header__title">Clients</h1>
          <Button variant="primary" onClick={() => navigate('/app/clients/new')}>
            <Plus size={18} />
            Add Client
          </Button>
        </div>
        <p className="page-header__subtitle">
          Manage your client relationships and information
        </p>
      </div>

      <div style={{ marginBottom: 'var(--space-lg)' }}>
        <div style={{ position: 'relative', maxWidth: '400px' }}>
          <Search
            size={18}
            style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }}
          />
          <Input
            type="search"
            placeholder="Search clients..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{ paddingLeft: '40px' }}
          />
        </div>
      </div>

      {filteredClients.length === 0 ? (
        <EmptyState
          icon={Building2}
          title={searchQuery ? 'No clients found' : 'No clients yet'}
          description={searchQuery ? 'Try a different search term' : 'Add your first client to get started.'}
          action={
            !searchQuery && (
              <Button variant="primary" onClick={() => navigate('/app/clients/new')}>
                Add Client
              </Button>
            )
          }
        />
      ) : (
        <div className="grid-list">
          {filteredClients.map((client) => (
            <Card key={client.id} className="clickable-card" onClick={() => navigate(`/app/clients/${client.id}`)}>
              <div className="client-card">
                <div className="client-card__header">
                  <h3>{client.name}</h3>
                  <Badge variant={client.status === 'ACTIVE' ? 'success' : 'default'}>
                    {client.status}
                  </Badge>
                </div>
                {client.description && <p className="client-card__description">{client.description}</p>}
                <div className="client-card__meta">
                  {client.industry && <span><Building2 size={14} /> {client.industry}</span>}
                  {client.city && <span>{client.city}, {client.country}</span>}
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
    </>
  )
}
