import { Bell, CheckCheck, Plus, Settings, Video } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { notificationApi } from '../api'
import type { NotificationItem } from '../types'

interface AppHeaderProps {
  onCreateMeeting?: () => void
}

export function AppHeader({ onCreateMeeting }: AppHeaderProps) {
  const location = useLocation()
  const isFinance = location.pathname.startsWith('/app/finance') || location.pathname.startsWith('/finance')
  const isMeetings = location.pathname.startsWith('/app/meetings') || location.pathname.startsWith('/meetings')
  const isActivity = location.pathname.startsWith('/app/activity') || location.pathname.startsWith('/activity')
  const isAudit = location.pathname.startsWith('/app/audit-logs') || location.pathname.startsWith('/audit-logs')
  const isNotifications = location.pathname.startsWith('/app/notifications') || location.pathname.startsWith('/notifications')

  const [unreadCount, setUnreadCount] = useState<number>(0)
  const [dropdownOpen, setDropdownOpen] = useState(false)
  const [recentNotifications, setRecentNotifications] = useState<NotificationItem[]>([])
  const [loadingNotifications, setLoadingNotifications] = useState(false)
  const dropdownRef = useRef<HTMLDivElement>(null)

  const fetchUnread = async () => {
    try {
      const res = await notificationApi.unreadCount()
      setUnreadCount(res.unreadCount || 0)
    } catch {
      // silently ignore unauthenticated or network failures in background poll
    }
  }

  const fetchRecent = async () => {
    setLoadingNotifications(true)
    try {
      const res = await notificationApi.list('all', 0, 5)
      setRecentNotifications(res.content || [])
    } catch {
      // silently ignore
    } finally {
      setLoadingNotifications(false)
    }
  }

  useEffect(() => {
    fetchUnread()
    const timer = setInterval(fetchUnread, 15000)
    return () => clearInterval(timer)
  }, [])

  useEffect(() => {
    if (dropdownOpen) {
      fetchRecent()
      fetchUnread()
    }
  }, [dropdownOpen])

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setDropdownOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const handleMarkAllRead = async () => {
    try {
      await notificationApi.markAllRead()
      setUnreadCount(0)
      setRecentNotifications((prev) =>
        prev.map((n) => ({ ...n, readFlag: true }))
      )
    } catch (e) {
      console.error('Failed to mark all as read', e)
    }
  }

  const handleMarkItemRead = async (id: string, e: React.MouseEvent) => {
    e.stopPropagation()
    try {
      await notificationApi.markRead(id)
      setUnreadCount((c) => Math.max(0, c - 1))
      setRecentNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, readFlag: true } : n))
      )
    } catch (err) {
      console.error('Failed to mark read', err)
    }
  }

  return (
    <header className="app-header">
      <div className="app-header__inner">
        <Link className="brand" to="/app" aria-label="Digital Office">
          <span className="brand__mark">
            <Video size={18} aria-hidden="true" />
          </span>
          Digital Office
        </Link>

        <nav className="header-nav" aria-label="Main navigation">
          <Link
            className={`header-nav__link ${isMeetings ? 'header-nav__link--active' : ''}`}
            to="/app/meetings"
          >
            Meetings
          </Link>
          <Link
            className={`header-nav__link ${isFinance ? 'header-nav__link--active' : ''}`}
            to="/app/finance"
          >
            Finance & Billing
          </Link>
          <Link
            className={`header-nav__link ${isActivity ? 'header-nav__link--active' : ''}`}
            to="/app/activity"
          >
            Activity
          </Link>
          <Link
            className={`header-nav__link ${isAudit ? 'header-nav__link--active' : ''}`}
            to="/app/audit-logs"
          >
            Audit Logs
          </Link>
        </nav>

        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          {/* Notification Bell Dropdown */}
          <div style={{ position: 'relative' }} ref={dropdownRef}>
            <button
              className={`btn btn-secondary btn-icon ${isNotifications ? 'btn--active' : ''}`}
              type="button"
              aria-label="Notifications"
              onClick={() => setDropdownOpen((prev) => !prev)}
              style={{ position: 'relative' }}
            >
              <Bell size={18} aria-hidden="true" />
              {unreadCount > 0 && (
                <span
                  style={{
                    position: 'absolute',
                    top: '-4px',
                    right: '-4px',
                    backgroundColor: '#ef4444',
                    color: 'white',
                    borderRadius: '9999px',
                    fontSize: '0.65rem',
                    fontWeight: 700,
                    minWidth: '18px',
                    height: '18px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    padding: '0 4px',
                    border: '2px solid var(--bg-primary)',
                  }}
                >
                  {unreadCount > 99 ? '99+' : unreadCount}
                </span>
              )}
            </button>

            {dropdownOpen && (
              <div
                style={{
                  position: 'absolute',
                  right: 0,
                  top: 'calc(100% + 8px)',
                  width: '380px',
                  backgroundColor: '#111827',
                  border: '1px solid var(--border)',
                  borderRadius: '16px',
                  boxShadow: 'var(--shadow-lg)',
                  zIndex: 100,
                  overflow: 'hidden',
                  display: 'flex',
                  flexDirection: 'column',
                }}
              >
                <div
                  style={{
                    padding: '12px 16px',
                    borderBottom: '1px solid var(--border)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span style={{ fontWeight: 700, fontSize: '0.9rem' }}>Notifications</span>
                    {unreadCount > 0 && (
                      <span
                        className="badge"
                        style={{ background: 'rgba(99,102,241,0.2)', color: '#818cf8' }}
                      >
                        {unreadCount} new
                      </span>
                    )}
                  </div>
                  {unreadCount > 0 && (
                    <button
                      className="btn btn-ghost btn-sm"
                      type="button"
                      onClick={handleMarkAllRead}
                      style={{ padding: '4px 8px', fontSize: '0.75rem' }}
                    >
                      <CheckCheck size={14} />
                      Mark all read
                    </button>
                  )}
                </div>

                <div style={{ maxHeight: '320px', overflowY: 'auto' }}>
                  {loadingNotifications ? (
                    <div style={{ padding: '24px', textAlign: 'center', color: 'var(--text-muted)' }}>
                      <div className="spinner" style={{ margin: '0 auto 8px' }} />
                      Loading...
                    </div>
                  ) : recentNotifications.length === 0 ? (
                    <div style={{ padding: '32px 16px', textAlign: 'center', color: 'var(--text-muted)' }}>
                      <Bell size={28} style={{ margin: '0 auto 8px', opacity: 0.4 }} />
                      <p style={{ fontSize: '0.85rem' }}>No notifications right now</p>
                    </div>
                  ) : (
                    recentNotifications.map((n) => (
                      <div
                        key={n.id}
                        style={{
                          padding: '12px 16px',
                          borderBottom: '1px solid rgba(148, 163, 184, 0.08)',
                          backgroundColor: n.readFlag ? 'transparent' : 'rgba(99, 102, 241, 0.06)',
                          display: 'flex',
                          flexDirection: 'column',
                          gap: '4px',
                          cursor: 'pointer',
                          transition: 'background var(--transition-fast)',
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                          <span
                            className="badge"
                            style={{
                              fontSize: '0.6rem',
                              padding: '2px 6px',
                              background:
                                n.priority === 'URGENT'
                                  ? 'rgba(239, 68, 68, 0.2)'
                                  : n.priority === 'HIGH'
                                  ? 'rgba(245, 158, 11, 0.2)'
                                  : 'rgba(99, 102, 241, 0.15)',
                              color:
                                n.priority === 'URGENT'
                                  ? '#fca5a5'
                                  : n.priority === 'HIGH'
                                  ? '#fcd34d'
                                  : '#a5b4fc',
                            }}
                          >
                            {n.category}
                          </span>
                          <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
                            {new Date(n.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                          </span>
                        </div>
                        <div style={{ fontWeight: 600, fontSize: '0.82rem', color: 'var(--text-primary)' }}>
                          {n.title}
                        </div>
                        <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', lineHeight: 1.3 }}>
                          {n.message}
                        </div>
                        {!n.readFlag && (
                          <div style={{ alignSelf: 'flex-end', marginTop: '4px' }}>
                            <button
                              className="btn btn-ghost btn-sm"
                              type="button"
                              onClick={(e) => handleMarkItemRead(n.id, e)}
                              style={{ padding: '2px 6px', fontSize: '0.7rem' }}
                            >
                              Mark read
                            </button>
                          </div>
                        )}
                      </div>
                    ))
                  )}
                </div>

                <div
                  style={{
                    padding: '8px 12px',
                    borderTop: '1px solid var(--border)',
                    backgroundColor: 'var(--bg-secondary)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                  }}
                >
                  <Link
                    to="/app/notifications"
                    className="btn btn-ghost btn-sm"
                    onClick={() => setDropdownOpen(false)}
                    style={{ fontSize: '0.78rem' }}
                  >
                    View all
                  </Link>
                  <div style={{ display: 'flex', gap: '4px' }}>
                    <Link
                      to="/app/settings/notifications"
                      className="btn btn-ghost btn-sm"
                      title="Preferences"
                      onClick={() => setDropdownOpen(false)}
                      style={{ padding: '6px' }}
                    >
                      <Settings size={15} />
                    </Link>
                  </div>
                </div>
              </div>
            )}
          </div>

          {onCreateMeeting && (
            <button className="btn btn-primary" type="button" onClick={onCreateMeeting}>
              <Plus size={17} aria-hidden="true" />
              <span className="btn--label">New meeting</span>
            </button>
          )}
        </div>
      </div>
    </header>
  )
}

