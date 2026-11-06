import { useEffect, useState } from 'react'
import {
  Activity,
  ChevronDown,
  ChevronUp,
  Clock,
  Filter,
  RefreshCw,
} from 'lucide-react'
import { activityApi, organizationApi } from '../api'
import { useWorkspace } from '../app/providers/WorkspaceProvider'
import type { ActivityFeedItem, Organization, PageResponse } from '../types'

const CATEGORIES = [
  'ALL',
  'ORGANIZATION',
  'CLIENT',
  'PROJECT',
  'WORK',
  'DOCUMENT',
  'COMMUNICATION',
  'FINANCE',
  'SYSTEM',
]

export function ActivityFeedPage() {
  const { currentOrganization } = useWorkspace()
  const [organizations, setOrganizations] = useState<Organization[]>([])
  const [feedScope, setFeedScope] = useState<'ORGANIZATION' | 'PROJECT'>('ORGANIZATION')
  const [orgId, setOrgId] = useState<string>(currentOrganization?.id || '')
  const [projectId, setProjectId] = useState<string>('')
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL')
  const [page, setPage] = useState(0)

  useEffect(() => {
    organizationApi.list().then((list) => {
      setOrganizations(list)
      if (!orgId && list.length > 0) {
        setOrgId(currentOrganization?.id || list[0].id)
      }
    }).catch(() => {})
  }, [currentOrganization?.id])

  const [activities, setActivities] = useState<ActivityFeedItem[]>([])
  const [pageData, setPageData] = useState<PageResponse<ActivityFeedItem> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [expandedId, setExpandedId] = useState<string | null>(null)

  const loadActivities = async () => {
    setLoading(true)
    setError(null)
    try {
      let res: PageResponse<ActivityFeedItem>
      if (feedScope === 'ORGANIZATION') {
        if (!orgId.trim()) {
          setError('Please provide a valid Organization ID')
          setLoading(false)
          return
        }
        res = await activityApi.getOrgActivity(orgId.trim(), selectedCategory, page, 20)
      } else {
        if (!projectId.trim()) {
          setError('Please provide a Project ID to inspect project-level activities')
          setLoading(false)
          return
        }
        res = await activityApi.getProjectActivity(projectId.trim(), selectedCategory, page, 20)
      }
      setPageData(res)
      setActivities(res.content || [])
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to fetch activity feed'
      setError(msg)
      setActivities([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadActivities()
  }, [feedScope, selectedCategory, page])

  const handleScopeChange = (scope: 'ORGANIZATION' | 'PROJECT') => {
    setFeedScope(scope)
    setPage(0)
  }

  return (
    <div style={{ padding: '0 0 64px' }}>
      {/* Page title and refresh */}
        <div
          style={{
            display: 'flex',
            flexWrap: 'wrap',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '16px',
            marginBottom: '28px',
          }}
        >
          <div>
            <h1 style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--text-primary)' }}>
              Activity Feed
            </h1>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginTop: '4px' }}>
              Chronological log of cross-service organization and project updates.
            </p>
          </div>

          <div style={{ display: 'flex', gap: '10px' }}>
            <button
              className="btn btn-secondary btn-icon"
              type="button"
              onClick={loadActivities}
              title="Refresh feed"
            >
              <RefreshCw size={16} className={loading ? 'spinner' : ''} />
            </button>
          </div>
        </div>

        {/* Controls Bar */}
        <div
          style={{
            background: 'var(--bg-secondary)',
            border: '1px solid var(--border)',
            borderRadius: '16px',
            padding: '20px',
            marginBottom: '24px',
            display: 'flex',
            flexDirection: 'column',
            gap: '16px',
          }}
        >
          <div
            style={{
              display: 'flex',
              flexWrap: 'wrap',
              gap: '16px',
              alignItems: 'center',
              justifyContent: 'space-between',
            }}
          >
            {/* Scope tabs */}
            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                type="button"
                className={`btn btn-sm ${
                  feedScope === 'ORGANIZATION' ? 'btn-primary' : 'btn-ghost'
                }`}
                onClick={() => handleScopeChange('ORGANIZATION')}
              >
                Organization Feed
              </button>
              <button
                type="button"
                className={`btn btn-sm ${
                  feedScope === 'PROJECT' ? 'btn-primary' : 'btn-ghost'
                }`}
                onClick={() => handleScopeChange('PROJECT')}
              >
                Project Feed
              </button>
            </div>

            {/* Category filter */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Filter size={15} style={{ color: 'var(--text-muted)' }} />
              <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Category:</span>
              <select
                className="input"
                style={{
                  width: 'auto',
                  padding: '6px 12px',
                  fontSize: '0.84rem',
                }}
                value={selectedCategory}
                onChange={(e) => {
                  setSelectedCategory(e.target.value)
                  setPage(0)
                }}
              >
                {CATEGORIES.map((c) => (
                  <option key={c} value={c}>
                    {c}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Scope Inputs */}
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '14px', alignItems: 'center' }}>
            {feedScope === 'ORGANIZATION' ? (
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flex: '1 1 320px' }}>
                <label style={{ fontSize: '0.78rem', fontWeight: 600, color: 'var(--text-secondary)', whiteSpace: 'nowrap' }}>
                  Organization:
                </label>
                {organizations.length > 0 ? (
                  <select
                    className="input"
                    style={{ fontSize: '0.82rem', padding: '6px 10px' }}
                    value={orgId}
                    onChange={(e) => {
                      setOrgId(e.target.value)
                      setPage(0)
                    }}
                  >
                    {organizations.map((org) => (
                      <option key={org.id} value={org.id}>
                        {org.name}
                      </option>
                    ))}
                  </select>
                ) : (
                  <input
                    type="text"
                    className="input"
                    style={{ fontSize: '0.82rem', padding: '6px 10px' }}
                    value={orgId}
                    onChange={(e) => setOrgId(e.target.value)}
                    placeholder="Organization UUID"
                  />
                )}
                <button className="btn btn-secondary btn-sm" type="button" onClick={loadActivities}>
                  Query
                </button>
              </div>
            ) : (
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flex: '1 1 320px' }}>
                <label style={{ fontSize: '0.78rem', fontWeight: 600, color: 'var(--text-secondary)', whiteSpace: 'nowrap' }}>
                  Project ID:
                </label>
                <input
                  type="text"
                  className="input"
                  style={{ fontSize: '0.82rem', padding: '6px 10px' }}
                  value={projectId}
                  onChange={(e) => setProjectId(e.target.value)}
                  placeholder="Enter Project UUID"
                />
                <button className="btn btn-secondary btn-sm" type="button" onClick={loadActivities}>
                  Query
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Content */}
        {error ? (
          <div
            style={{
              padding: '24px',
              borderRadius: '16px',
              background: 'rgba(239, 68, 68, 0.1)',
              border: '1px solid rgba(239, 68, 68, 0.25)',
              color: '#fca5a5',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
            }}
          >
            <span>{error}</span>
            <button className="btn btn-secondary btn-sm" type="button" onClick={loadActivities}>
              Retry
            </button>
          </div>
        ) : loading && activities.length === 0 ? (
          <div
            style={{
              padding: '64px',
              textAlign: 'center',
              color: 'var(--text-muted)',
              background: 'var(--bg-secondary)',
              borderRadius: '16px',
              border: '1px solid var(--border)',
            }}
          >
            <div className="spinner" style={{ margin: '0 auto 12px' }} />
            <p>Loading activity stream...</p>
          </div>
        ) : activities.length === 0 ? (
          <div
            style={{
              padding: '72px 24px',
              textAlign: 'center',
              background: 'var(--bg-secondary)',
              borderRadius: '16px',
              border: '1px solid var(--border)',
            }}
          >
            <Activity size={40} style={{ margin: '0 auto 16px', color: 'var(--text-muted)', opacity: 0.5 }} />
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)' }}>
              No activities found
            </h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginTop: '6px' }}>
              No events logged for this scope and filter yet.
            </p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {activities.map((item) => {
              const isExpanded = expandedId === item.id
              return (
                <div
                  key={item.id}
                  style={{
                    padding: '20px',
                    borderRadius: '16px',
                    background: 'var(--bg-secondary)',
                    border: '1px solid var(--border)',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '10px',
                  }}
                >
                  <div
                    style={{
                      display: 'flex',
                      flexWrap: 'wrap',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      gap: '8px',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                      <span
                        className="badge"
                        style={{
                          background: 'rgba(99, 102, 241, 0.15)',
                          color: '#a5b4fc',
                        }}
                      >
                        {item.category}
                      </span>
                      <span
                        style={{
                          fontSize: '0.78rem',
                          fontWeight: 700,
                          color: 'var(--text-primary)',
                        }}
                      >
                        {item.eventType}
                      </span>
                      {item.resourceType && (
                        <span
                          className="badge"
                          style={{
                            background: 'rgba(148, 163, 184, 0.1)',
                            color: 'var(--text-secondary)',
                            textTransform: 'none',
                          }}
                        >
                          {item.resourceType}: {item.resourceId ? item.resourceId.substring(0, 8) + '...' : 'Global'}
                        </span>
                      )}
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--text-muted)', fontSize: '0.78rem' }}>
                      <Clock size={14} />
                      <span>{new Date(item.createdAt).toLocaleString()}</span>
                    </div>
                  </div>

                  <p style={{ fontSize: '0.9rem', color: 'var(--text-primary)', lineHeight: 1.4 }}>
                    {item.description}
                  </p>

                  <div
                    style={{
                      display: 'flex',
                      flexWrap: 'wrap',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      paddingTop: '8px',
                      borderTop: '1px solid rgba(148, 163, 184, 0.08)',
                      gap: '8px',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '16px', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                      <span>Actor: <code>{item.actorId ? item.actorId.substring(0, 8) + '...' : 'System'}</code></span>
                      {item.correlationId && (
                        <span>Correlation: <code>{item.correlationId.substring(0, 8)}...</code></span>
                      )}
                    </div>

                    {item.metadata && (
                      <button
                        className="btn btn-ghost btn-sm"
                        type="button"
                        onClick={() => setExpandedId(isExpanded ? null : item.id)}
                        style={{ padding: '2px 8px', fontSize: '0.72rem' }}
                      >
                        {isExpanded ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                        <span>{isExpanded ? 'Hide Details' : 'View Metadata'}</span>
                      </button>
                    )}
                  </div>

                  {isExpanded && item.metadata && (
                    <div
                      style={{
                        marginTop: '8px',
                        padding: '12px',
                        borderRadius: '8px',
                        background: 'var(--bg-tertiary)',
                        fontSize: '0.75rem',
                        fontFamily: 'monospace',
                        color: 'var(--text-secondary)',
                        overflowX: 'auto',
                      }}
                    >
                      <pre style={{ margin: 0 }}>
                        {(() => {
                          try {
                            return JSON.stringify(JSON.parse(item.metadata), null, 2)
                          } catch {
                            return item.metadata
                          }
                        })()}
                      </pre>
                    </div>
                  )}
                </div>
              )
            })}

            {/* Pagination */}
            {pageData && pageData.totalPages > 1 && (
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '12px',
                  marginTop: '20px',
                }}
              >
                <button
                  className="btn btn-secondary btn-sm"
                  type="button"
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                >
                  Previous
                </button>
                <span style={{ fontSize: '0.82rem', color: 'var(--text-secondary)' }}>
                  Page {page + 1} of {pageData.totalPages}
                </span>
                <button
                  className="btn btn-secondary btn-sm"
                  type="button"
                  disabled={page + 1 >= pageData.totalPages}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Next
                </button>
              </div>
            )}
          </div>
        )}
    </div>
  )
}
