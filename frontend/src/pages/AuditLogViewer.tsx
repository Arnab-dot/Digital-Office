import { useEffect, useState } from 'react'
import {
  CheckCircle,
  ChevronDown,
  ChevronUp,
  Filter,
  Lock,
  RefreshCw,
  Shield,
  ShieldAlert,
  XCircle,
} from 'lucide-react'
import { auditApi, organizationApi } from '../api'
import { useWorkspace } from '../app/providers/WorkspaceProvider'
import type { AuditLogItem, Organization, PageResponse } from '../types'

export function AuditLogViewer() {
  const { currentOrganization } = useWorkspace()
  const [organizations, setOrganizations] = useState<Organization[]>([])
  const [orgId, setOrgId] = useState<string>(currentOrganization?.id || '')
  const [actionFilter, setActionFilter] = useState('')
  const [actorFilter, setActorFilter] = useState('')
  const [page, setPage] = useState(0)

  useEffect(() => {
    organizationApi.list().then((list) => {
      setOrganizations(list)
      if (!orgId && list.length > 0) {
        setOrgId(currentOrganization?.id || list[0].id)
      }
    }).catch(() => {})
  }, [currentOrganization?.id])

  const [auditLogs, setAuditLogs] = useState<AuditLogItem[]>([])
  const [pageData, setPageData] = useState<PageResponse<AuditLogItem> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [expandedLogId, setExpandedLogId] = useState<string | null>(null)

  const loadLogs = async () => {
    if (!orgId.trim()) {
      setError('Organization ID is required.')
      return
    }
    setLoading(true)
    setError(null)
    try {
      const res = await auditApi.getOrgAuditLogs(
        orgId.trim(),
        actionFilter.trim() || undefined,
        actorFilter.trim() || undefined,
        page,
        20
      )
      setPageData(res)
      setAuditLogs(res.content || [])
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to fetch audit logs'
      setError(msg)
      setAuditLogs([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadLogs()
  }, [orgId, page])

  const handleFilterSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setPage(0)
    loadLogs()
  }

  return (
    <div style={{ padding: '0 0 64px' }}>
      {/* Header banner */}
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
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
              <span
                className="badge"
                style={{
                  background: 'rgba(239, 68, 68, 0.15)',
                  color: '#f87171',
                  border: '1px solid rgba(239, 68, 68, 0.3)',
                  padding: '4px 10px',
                }}
              >
                <Shield size={12} />
                Strict Admin Access (OWNER / ADMIN Only)
              </span>
            </div>
            <h1 style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--text-primary)' }}>
              Compliance & Security Audit Trail
            </h1>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginTop: '4px' }}>
              Immutable audit log of administrative, financial, role, and membership modifications.
            </p>
          </div>

          <div style={{ display: 'flex', gap: '10px' }}>
            <button
              className="btn btn-secondary btn-icon"
              type="button"
              onClick={loadLogs}
              title="Refresh logs"
            >
              <RefreshCw size={16} className={loading ? 'spinner' : ''} />
            </button>
          </div>
        </div>

        {/* Filter Bar */}
        <form
          onSubmit={handleFilterSubmit}
          style={{
            background: 'var(--bg-secondary)',
            border: '1px solid var(--border)',
            borderRadius: '16px',
            padding: '20px',
            marginBottom: '24px',
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr)) 120px',
            gap: '16px',
            alignItems: 'end',
          }}
        >
          <div className="input-group">
            <label>Organization</label>
            {organizations.length > 0 ? (
              <select
                className="input"
                value={orgId}
                onChange={(e) => setOrgId(e.target.value)}
                required
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
                value={orgId}
                onChange={(e) => setOrgId(e.target.value)}
                placeholder="Organization UUID"
                required
              />
            )}
          </div>

          <div className="input-group">
            <label>Action Type</label>
            <input
              type="text"
              className="input"
              value={actionFilter}
              onChange={(e) => setActionFilter(e.target.value)}
              placeholder="e.g. ROLE_CHANGED, PAYMENT"
            />
          </div>

          <div className="input-group">
            <label>Actor UUID</label>
            <input
              type="text"
              className="input"
              value={actorFilter}
              onChange={(e) => setActorFilter(e.target.value)}
              placeholder="Actor UUID"
            />
          </div>

          <button className="btn btn-primary" type="submit" style={{ height: '42px' }}>
            <Filter size={15} />
            <span>Filter</span>
          </button>
        </form>

        {/* Error / Forbidden State */}
        {error && (
          <div
            style={{
              padding: '20px 24px',
              borderRadius: '16px',
              background: 'rgba(239, 68, 68, 0.1)',
              border: '1px solid rgba(239, 68, 68, 0.25)',
              color: '#fca5a5',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              marginBottom: '24px',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <ShieldAlert size={22} style={{ flexShrink: 0 }} />
              <div>
                <div style={{ fontWeight: 700 }}>Authorization Notice</div>
                <div style={{ fontSize: '0.85rem' }}>{error}</div>
              </div>
            </div>
            <button className="btn btn-secondary btn-sm" type="button" onClick={loadLogs}>
              Retry
            </button>
          </div>
        )}

        {/* Table Content */}
        {loading && auditLogs.length === 0 ? (
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
            <p>Querying immutable audit vault...</p>
          </div>
        ) : auditLogs.length === 0 ? (
          <div
            style={{
              padding: '72px 24px',
              textAlign: 'center',
              background: 'var(--bg-secondary)',
              borderRadius: '16px',
              border: '1px solid var(--border)',
            }}
          >
            <Lock size={40} style={{ margin: '0 auto 16px', color: 'var(--text-muted)', opacity: 0.5 }} />
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)' }}>
              No audit records found
            </h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginTop: '6px' }}>
              No recorded administrative actions matched the query criteria.
            </p>
          </div>
        ) : (
          <div
            style={{
              background: 'var(--bg-secondary)',
              border: '1px solid var(--border)',
              borderRadius: '16px',
              overflow: 'hidden',
            }}
          >
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.84rem' }}>
                <thead>
                  <tr
                    style={{
                      borderBottom: '1px solid var(--border)',
                      background: 'var(--bg-tertiary)',
                      color: 'var(--text-secondary)',
                    }}
                  >
                    <th style={{ padding: '14px 18px', fontWeight: 700 }}>Timestamp</th>
                    <th style={{ padding: '14px 18px', fontWeight: 700 }}>Actor</th>
                    <th style={{ padding: '14px 18px', fontWeight: 700 }}>Action</th>
                    <th style={{ padding: '14px 18px', fontWeight: 700 }}>Resource</th>
                    <th style={{ padding: '14px 18px', fontWeight: 700 }}>Service</th>
                    <th style={{ padding: '14px 18px', fontWeight: 700 }}>Outcome</th>
                    <th style={{ padding: '14px 18px', fontWeight: 700, textAlign: 'right' }}>Metadata</th>
                  </tr>
                </thead>
                <tbody>
                  {auditLogs.map((log) => {
                    const isSuccess = log.outcome?.toUpperCase() === 'SUCCESS'
                    const isExpanded = expandedLogId === log.id
                    return (
                      <tr
                        key={log.id}
                        style={{
                          borderBottom: '1px solid rgba(148, 163, 184, 0.08)',
                          transition: 'background var(--transition-fast)',
                        }}
                      >
                        <td style={{ padding: '14px 18px', whiteSpace: 'nowrap', color: 'var(--text-muted)' }}>
                          {new Date(log.createdAt).toLocaleString()}
                        </td>
                        <td style={{ padding: '14px 18px', fontFamily: 'monospace', fontSize: '0.78rem' }}>
                          {log.actorId ? log.actorId.substring(0, 8) + '...' : 'SYSTEM'}
                        </td>
                        <td style={{ padding: '14px 18px', fontWeight: 600, color: 'var(--text-primary)' }}>
                          {log.action}
                        </td>
                        <td style={{ padding: '14px 18px' }}>
                          <span
                            className="badge"
                            style={{
                              background: 'rgba(148, 163, 184, 0.1)',
                              color: 'var(--text-secondary)',
                              textTransform: 'none',
                            }}
                          >
                            {log.resourceType}: {log.resourceId ? log.resourceId.substring(0, 8) + '...' : '-'}
                          </span>
                        </td>
                        <td style={{ padding: '14px 18px', color: 'var(--text-secondary)' }}>
                          {log.sourceService || 'platform'}
                        </td>
                        <td style={{ padding: '14px 18px' }}>
                          <span
                            className="badge"
                            style={{
                              background: isSuccess ? 'rgba(16, 185, 129, 0.15)' : 'rgba(239, 68, 68, 0.15)',
                              color: isSuccess ? '#6ee7b7' : '#fca5a5',
                            }}
                          >
                            {isSuccess ? <CheckCircle size={12} /> : <XCircle size={12} />}
                            {log.outcome}
                          </span>
                        </td>
                        <td style={{ padding: '14px 18px', textAlign: 'right' }}>
                          {log.metadata ? (
                            <button
                              className="btn btn-ghost btn-sm"
                              type="button"
                              onClick={() => setExpandedLogId(isExpanded ? null : log.id)}
                              style={{ padding: '4px 8px', fontSize: '0.74rem' }}
                            >
                              {isExpanded ? <ChevronUp size={13} /> : <ChevronDown size={13} />}
                              <span>{isExpanded ? 'Close' : 'Inspect'}</span>
                            </button>
                          ) : (
                            <span style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>-</span>
                          )}
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>

            {/* Expandable inspect modal or drawer */}
            {expandedLogId && (
              <div
                style={{
                  padding: '20px',
                  background: 'var(--bg-tertiary)',
                  borderTop: '1px solid var(--border)',
                }}
              >
                {(() => {
                  const log = auditLogs.find((l) => l.id === expandedLogId)
                  if (!log) return null
                  return (
                    <div>
                      <div
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          marginBottom: '10px',
                        }}
                      >
                        <span style={{ fontWeight: 700, fontSize: '0.85rem' }}>
                          Payload Inspection — Log ID: <code>{log.id}</code>
                        </span>
                        {log.correlationId && (
                          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                            Correlation ID: <code>{log.correlationId}</code>
                          </span>
                        )}
                      </div>
                      <pre
                        style={{
                          margin: 0,
                          padding: '12px',
                          borderRadius: '8px',
                          background: 'rgba(0, 0, 0, 0.3)',
                          fontSize: '0.78rem',
                          fontFamily: 'monospace',
                          color: '#e2e8f0',
                          overflowX: 'auto',
                        }}
                      >
                        {(() => {
                          try {
                            return JSON.stringify(JSON.parse(log.metadata || '{}'), null, 2)
                          } catch {
                            return log.metadata
                          }
                        })()}
                      </pre>
                    </div>
                  )
                })()}
              </div>
            )}

            {/* Pagination */}
            {pageData && pageData.totalPages > 1 && (
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '12px',
                  padding: '16px',
                  borderTop: '1px solid var(--border)',
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
