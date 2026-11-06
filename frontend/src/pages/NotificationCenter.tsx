import { useEffect, useState } from 'react'
import {
  Bell,
  Check,
  CheckCheck,
  Clock,
  Filter,
  RefreshCw,
  Settings,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { notificationApi } from '../api'
import type { NotificationCategory, NotificationItem, PageResponse } from '../types'

const CATEGORIES: { label: string; value: NotificationCategory | 'ALL' }[] = [
  { label: 'All Categories', value: 'ALL' },
  { label: 'Work & Tasks', value: 'WORK' },
  { label: 'Projects', value: 'PROJECT' },
  { label: 'Documents', value: 'DOCUMENT' },
  { label: 'Meetings', value: 'COMMUNICATION' },
  { label: 'Finance & Billing', value: 'FINANCE' },
  { label: 'Organization', value: 'ORGANIZATION' },
  { label: 'Clients', value: 'CLIENT' },
  { label: 'System & Security', value: 'SYSTEM' },
]

export function NotificationCenter() {
  const [notifications, setNotifications] = useState<NotificationItem[]>([])
  const [pageData, setPageData] = useState<PageResponse<NotificationItem> | null>(null)
  const [readFilter, setReadFilter] = useState<'all' | 'unread' | 'read'>('all')
  const [selectedCategory, setSelectedCategory] = useState<NotificationCategory | 'ALL'>('ALL')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [markingAll, setMarkingAll] = useState(false)

  const loadNotifications = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await notificationApi.list(readFilter, page, 15)
      setPageData(res)
      setNotifications(res.content || [])
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load notifications'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadNotifications()
  }, [readFilter, page])

  const handleMarkRead = async (id: string) => {
    try {
      await notificationApi.markRead(id)
      setNotifications((prev) =>
        prev.map((item) => (item.id === id ? { ...item, readFlag: true } : item))
      )
    } catch (err: unknown) {
      console.error('Mark read failed', err)
    }
  }

  const handleMarkAllRead = async () => {
    setMarkingAll(true)
    try {
      await notificationApi.markAllRead()
      setNotifications((prev) => prev.map((item) => ({ ...item, readFlag: true })))
    } catch (err: unknown) {
      console.error('Mark all read failed', err)
    } finally {
      setMarkingAll(false)
    }
  }

  // Filter client-side by category if category selected
  const displayedNotifications =
    selectedCategory === 'ALL'
      ? notifications
      : notifications.filter((n) => n.category === selectedCategory)

  return (
    <div style={{ padding: '8px 0' }}>
      <main className="page-content" style={{ padding: '8px 0 64px' }}>
        {/* Header toolbar */}
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
              Notifications
            </h1>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginTop: '4px' }}>
              Stay updated on projects, tasks, reviews, meetings, and finance alerts.
            </p>
          </div>

          <div style={{ display: 'flex', gap: '10px' }}>
            <button
              className="btn btn-secondary"
              type="button"
              onClick={handleMarkAllRead}
              disabled={markingAll || notifications.every((n) => n.readFlag)}
            >
              <CheckCheck size={16} />
              <span>Mark all read</span>
            </button>
            <Link className="btn btn-secondary" to="/app/settings/notifications">
              <Settings size={16} />
              <span>Preferences</span>
            </Link>
            <button
              className="btn btn-secondary btn-icon"
              type="button"
              onClick={loadNotifications}
              title="Refresh"
            >
              <RefreshCw size={16} className={loading ? 'spinner' : ''} />
            </button>
          </div>
        </div>

        {/* Filters */}
        <div
          style={{
            background: 'var(--bg-secondary)',
            border: '1px solid var(--border)',
            borderRadius: '16px',
            padding: '16px 20px',
            marginBottom: '24px',
            display: 'flex',
            flexWrap: 'wrap',
            gap: '16px',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          {/* Read status pills */}
          <div style={{ display: 'flex', gap: '6px' }}>
            {(['all', 'unread', 'read'] as const).map((status) => (
              <button
                key={status}
                type="button"
                className={`btn btn-sm ${
                  readFilter === status ? 'btn-primary' : 'btn-ghost'
                }`}
                onClick={() => {
                  setReadFilter(status)
                  setPage(0)
                }}
                style={{ textTransform: 'capitalize' }}
              >
                {status}
              </button>
            ))}
          </div>

          {/* Category selector */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Filter size={15} style={{ color: 'var(--text-muted)' }} />
            <select
              className="input"
              style={{
                width: 'auto',
                minWidth: '180px',
                padding: '6px 12px',
                fontSize: '0.84rem',
              }}
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value as NotificationCategory | 'ALL')}
            >
              {CATEGORIES.map((cat) => (
                <option key={cat.value} value={cat.value}>
                  {cat.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Content State */}
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
            <button className="btn btn-secondary btn-sm" type="button" onClick={loadNotifications}>
              Retry
            </button>
          </div>
        ) : loading && notifications.length === 0 ? (
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
            <p>Loading your notifications...</p>
          </div>
        ) : displayedNotifications.length === 0 ? (
          <div
            style={{
              padding: '72px 24px',
              textAlign: 'center',
              background: 'var(--bg-secondary)',
              borderRadius: '16px',
              border: '1px solid var(--border)',
            }}
          >
            <Bell size={40} style={{ margin: '0 auto 16px', color: 'var(--text-muted)', opacity: 0.5 }} />
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)' }}>
              No notifications found
            </h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginTop: '6px' }}>
              {readFilter === 'unread'
                ? "You're all caught up! No unread notifications."
                : 'No notification records matching your current filter.'}
            </p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {displayedNotifications.map((n) => {
              const isUrgent = n.priority === 'URGENT'
              const isHigh = n.priority === 'HIGH'
              return (
                <div
                  key={n.id}
                  style={{
                    padding: '20px',
                    borderRadius: '16px',
                    background: n.readFlag ? 'var(--bg-secondary)' : 'rgba(99, 102, 241, 0.08)',
                    border: `1px solid ${
                      n.readFlag ? 'var(--border)' : 'rgba(99, 102, 241, 0.35)'
                    }`,
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '10px',
                    transition: 'border-color var(--transition-fast)',
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
                          background: isUrgent
                            ? 'rgba(239, 68, 68, 0.2)'
                            : isHigh
                            ? 'rgba(245, 158, 11, 0.2)'
                            : 'rgba(99, 102, 241, 0.15)',
                          color: isUrgent ? '#fca5a5' : isHigh ? '#fcd34d' : '#a5b4fc',
                        }}
                      >
                        {n.category}
                      </span>
                      {n.priority && n.priority !== 'MEDIUM' && (
                        <span
                          className="badge"
                          style={{
                            background: isUrgent ? '#dc2626' : '#d97706',
                            color: 'white',
                          }}
                        >
                          {n.priority}
                        </span>
                      )}
                      {!n.readFlag && (
                        <span
                          style={{
                            width: '8px',
                            height: '8px',
                            borderRadius: '50%',
                            background: 'var(--accent-primary)',
                          }}
                        />
                      )}
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--text-muted)', fontSize: '0.78rem' }}>
                      <Clock size={14} />
                      <span>{new Date(n.createdAt).toLocaleString()}</span>
                    </div>
                  </div>

                  <div>
                    <h3
                      style={{
                        fontSize: '1rem',
                        fontWeight: 700,
                        color: n.readFlag ? 'var(--text-primary)' : '#ffffff',
                        marginBottom: '4px',
                      }}
                    >
                      {n.title}
                    </h3>
                    <p
                      style={{
                        fontSize: '0.88rem',
                        color: 'var(--text-secondary)',
                        lineHeight: 1.45,
                      }}
                    >
                      {n.message}
                    </p>
                  </div>

                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      marginTop: '6px',
                      paddingTop: '8px',
                      borderTop: '1px solid rgba(148, 163, 184, 0.08)',
                    }}
                  >
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                      Event Type: <code>{n.eventType || 'NOTIFICATION'}</code>
                    </span>

                    {!n.readFlag ? (
                      <button
                        className="btn btn-secondary btn-sm"
                        type="button"
                        onClick={() => handleMarkRead(n.id)}
                      >
                        <Check size={14} />
                        <span>Mark as read</span>
                      </button>
                    ) : (
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                        Read
                      </span>
                    )}
                  </div>
                </div>
              )
            })}

            {/* Pagination Controls */}
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
      </main>
    </div>
  )
}
