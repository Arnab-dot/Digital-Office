import { useEffect, useState } from 'react'
import {
  Bell,
  CheckCircle2,
  Moon,
  Save,
  Sliders,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { notificationApi } from '../api'

const CATEGORY_KEYS = [
  { key: 'WORK', label: 'Work & Tasks', desc: 'Assignments, status updates, comments and mentions' },
  { key: 'PROJECT', label: 'Projects', desc: 'Milestone completions, memberships, role changes' },
  { key: 'DOCUMENT', label: 'Documents', desc: 'Document reviews, approvals, signatures and revisions' },
  { key: 'COMMUNICATION', label: 'Meetings', desc: 'Invitations, schedules, meeting starts and transcripts' },
  { key: 'FINANCE', label: 'Finance & Billing', desc: 'Invoices, payment receipts, subscription renewals and usage alerts' },
  { key: 'ORGANIZATION', label: 'Organization', desc: 'Member joins, role changes, organization settings' },
  { key: 'CLIENT', label: 'Clients & CRM', desc: 'Client account creation, updates and interactions' },
  { key: 'SYSTEM', label: 'System & Security', desc: 'Security alerts, audit notifications and system maintenance' },
]

export function NotificationPreferences() {
  const [inAppEnabled, setInAppEnabled] = useState(true)
  const [emailEnabled, setEmailEnabled] = useState(true)
  const [realTimeEnabled, setRealTimeEnabled] = useState(true)
  const [quietHoursStart, setQuietHoursStart] = useState<string>('22:00')
  const [quietHoursEnd, setQuietHoursEnd] = useState<string>('08:00')
  const [timeZone, setTimeZone] = useState('UTC')
  const [digestFrequency, setDigestFrequency] = useState('INSTANT')
  const [categorySettings, setCategorySettings] = useState<Record<string, boolean>>({
    WORK: true,
    PROJECT: true,
    DOCUMENT: true,
    COMMUNICATION: true,
    FINANCE: true,
    ORGANIZATION: true,
    CLIENT: true,
    SYSTEM: true,
  })

  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [savedSuccess, setSavedSuccess] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    async function loadPrefs() {
      setLoading(true)
      try {
        const p = await notificationApi.getPreferences()
        setInAppEnabled(p.inAppEnabled)
        setEmailEnabled(p.emailEnabled)
        setRealTimeEnabled(p.realTimeEnabled)
        setQuietHoursStart(p.quietHoursStart || '22:00')
        setQuietHoursEnd(p.quietHoursEnd || '08:00')
        setTimeZone(p.timeZone || 'UTC')
        setDigestFrequency(p.digestFrequency || 'INSTANT')

        if (p.categoryPreferences) {
          try {
            const parsed = JSON.parse(p.categoryPreferences)
            setCategorySettings((prev) => ({ ...prev, ...parsed }))
          } catch {
            // Keep default category toggles
          }
        }
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : 'Failed to load preferences'
        setError(msg)
      } finally {
        setLoading(false)
      }
    }
    loadPrefs()
  }, [])

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setError(null)
    setSavedSuccess(false)
    try {
      await notificationApi.updatePreferences({
        inAppEnabled,
        emailEnabled,
        realTimeEnabled,
        categoryPreferences: JSON.stringify(categorySettings),
        quietHoursStart: quietHoursStart.trim() || null,
        quietHoursEnd: quietHoursEnd.trim() || null,
        timeZone: timeZone.trim() || 'UTC',
        digestFrequency,
      })
      setSavedSuccess(true)
      setTimeout(() => setSavedSuccess(false), 3000)
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to save preferences'
      setError(msg)
    } finally {
      setSaving(false)
    }
  }

  const toggleCategory = (catKey: string) => {
    setCategorySettings((prev) => ({
      ...prev,
      [catKey]: !prev[catKey],
    }))
  }

  return (
    <div style={{ padding: '8px 0' }}>
      <main className="page-content" style={{ padding: '8px 0 64px' }}>
        <div style={{ maxWidth: '820px', margin: '0 auto' }}>
          {/* Header */}
          <div style={{ marginBottom: '32px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
              <Link to="/notifications" style={{ color: 'var(--accent-primary)', fontSize: '0.85rem' }}>
                &larr; Back to notifications
              </Link>
            </div>
            <h1 style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--text-primary)' }}>
              Notification Preferences
            </h1>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginTop: '4px' }}>
              Control when, where, and how you receive alerts across the Digital Office OS.
            </p>
          </div>

          {error && (
            <div
              style={{
                padding: '16px 20px',
                borderRadius: '12px',
                background: 'rgba(239, 68, 68, 0.1)',
                border: '1px solid rgba(239, 68, 68, 0.25)',
                color: '#fca5a5',
                marginBottom: '24px',
              }}
            >
              {error}
            </div>
          )}

          {savedSuccess && (
            <div
              style={{
                padding: '16px 20px',
                borderRadius: '12px',
                background: 'rgba(16, 185, 129, 0.1)',
                border: '1px solid rgba(16, 185, 129, 0.25)',
                color: '#6ee7b7',
                marginBottom: '24px',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
              }}
            >
              <CheckCircle2 size={18} />
              <span>Preferences saved successfully!</span>
            </div>
          )}

          {loading ? (
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
              <p>Loading notification preferences...</p>
            </div>
          ) : (
            <form onSubmit={handleSave} style={{ display: 'flex', flexDirection: 'column', gap: '28px' }}>
              {/* Delivery Channels */}
              <div
                style={{
                  background: 'var(--bg-secondary)',
                  border: '1px solid var(--border)',
                  borderRadius: '16px',
                  padding: '24px',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
                  <Bell size={20} style={{ color: 'var(--accent-primary)' }} />
                  <div>
                    <h2 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                      Delivery Channels
                    </h2>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.8rem' }}>
                      Choose your preferred communication channels for alerts.
                    </p>
                  </div>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                  {/* In-App Toggle */}
                  <label
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '12px 16px',
                      borderRadius: '10px',
                      background: 'var(--bg-tertiary)',
                      cursor: 'pointer',
                    }}
                  >
                    <div>
                      <div style={{ fontWeight: 600, fontSize: '0.88rem', color: 'var(--text-primary)' }}>
                        In-App Notification Center
                      </div>
                      <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                        Persist notifications to your global notification bell and center
                      </div>
                    </div>
                    <input
                      type="checkbox"
                      checked={inAppEnabled}
                      onChange={(e) => setInAppEnabled(e.target.checked)}
                      style={{ width: '18px', height: '18px', cursor: 'pointer' }}
                    />
                  </label>

                  {/* Real-Time WebSocket Toggle */}
                  <label
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '12px 16px',
                      borderRadius: '10px',
                      background: 'var(--bg-tertiary)',
                      cursor: 'pointer',
                    }}
                  >
                    <div>
                      <div style={{ fontWeight: 600, fontSize: '0.88rem', color: 'var(--text-primary)' }}>
                        Real-Time Instant Push (WebSocket)
                      </div>
                      <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                        Deliver instant badge updates and popups while you are active on the site
                      </div>
                    </div>
                    <input
                      type="checkbox"
                      checked={realTimeEnabled}
                      onChange={(e) => setRealTimeEnabled(e.target.checked)}
                      style={{ width: '18px', height: '18px', cursor: 'pointer' }}
                    />
                  </label>

                  {/* Email Toggle */}
                  <label
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '12px 16px',
                      borderRadius: '10px',
                      background: 'var(--bg-tertiary)',
                      cursor: 'pointer',
                    }}
                  >
                    <div>
                      <div style={{ fontWeight: 600, fontSize: '0.88rem', color: 'var(--text-primary)' }}>
                        Email Notifications
                      </div>
                      <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                        Send HTML emails for meeting invites, task assignments, reviews and billing
                      </div>
                    </div>
                    <input
                      type="checkbox"
                      checked={emailEnabled}
                      onChange={(e) => setEmailEnabled(e.target.checked)}
                      style={{ width: '18px', height: '18px', cursor: 'pointer' }}
                    />
                  </label>
                </div>
              </div>

              {/* Quiet Hours & Schedule */}
              <div
                style={{
                  background: 'var(--bg-secondary)',
                  border: '1px solid var(--border)',
                  borderRadius: '16px',
                  padding: '24px',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
                  <Moon size={20} style={{ color: 'var(--accent-primary)' }} />
                  <div>
                    <h2 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                      Quiet Hours & Time Zone
                    </h2>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.8rem' }}>
                      Suppress non-urgent push and email notifications during designated hours.
                    </p>
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '16px' }}>
                  <div className="input-group">
                    <label>Quiet Hours Start</label>
                    <input
                      type="time"
                      className="input"
                      value={quietHoursStart}
                      onChange={(e) => setQuietHoursStart(e.target.value)}
                    />
                  </div>

                  <div className="input-group">
                    <label>Quiet Hours End</label>
                    <input
                      type="time"
                      className="input"
                      value={quietHoursEnd}
                      onChange={(e) => setQuietHoursEnd(e.target.value)}
                    />
                  </div>

                  <div className="input-group">
                    <label>Time Zone</label>
                    <select
                      className="input"
                      value={timeZone}
                      onChange={(e) => setTimeZone(e.target.value)}
                    >
                      <option value="UTC">UTC</option>
                      <option value="America/New_York">America/New_York (EST/EDT)</option>
                      <option value="America/Los_Angeles">America/Los_Angeles (PST/PDT)</option>
                      <option value="America/Chicago">America/Chicago (CST/CDT)</option>
                      <option value="Europe/London">Europe/London (GMT/BST)</option>
                      <option value="Europe/Paris">Europe/Paris (CET/CEST)</option>
                      <option value="Asia/Kolkata">Asia/Kolkata (IST)</option>
                      <option value="Asia/Tokyo">Asia/Tokyo (JST)</option>
                      <option value="Australia/Sydney">Australia/Sydney (AEST)</option>
                    </select>
                  </div>
                </div>

                {/* Digest Frequency */}
                <div style={{ marginTop: '20px' }}>
                  <label style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-secondary)', display: 'block', marginBottom: '8px' }}>
                    Digest Frequency
                  </label>
                  <div style={{ display: 'flex', gap: '10px' }}>
                    {[
                      { val: 'INSTANT', label: 'Instant' },
                      { val: 'DAILY', label: 'Daily Digest' },
                      { val: 'WEEKLY', label: 'Weekly Summary' },
                      { val: 'NEVER', label: 'Never' },
                    ].map((d) => (
                      <button
                        key={d.val}
                        type="button"
                        className={`btn btn-sm ${digestFrequency === d.val ? 'btn-primary' : 'btn-secondary'}`}
                        onClick={() => setDigestFrequency(d.val)}
                      >
                        {d.label}
                      </button>
                    ))}
                  </div>
                </div>
              </div>

              {/* Category Subscriptions */}
              <div
                style={{
                  background: 'var(--bg-secondary)',
                  border: '1px solid var(--border)',
                  borderRadius: '16px',
                  padding: '24px',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
                  <Sliders size={20} style={{ color: 'var(--accent-primary)' }} />
                  <div>
                    <h2 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                      Category Subscriptions
                    </h2>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.8rem' }}>
                      Choose which functional areas you wish to receive notifications from.
                    </p>
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                  {CATEGORY_KEYS.map((cat) => {
                    const enabled = categorySettings[cat.key] !== false
                    return (
                      <div
                        key={cat.key}
                        onClick={() => toggleCategory(cat.key)}
                        style={{
                          padding: '14px',
                          borderRadius: '12px',
                          background: enabled ? 'rgba(99, 102, 241, 0.08)' : 'var(--bg-tertiary)',
                          border: `1px solid ${enabled ? 'rgba(99, 102, 241, 0.3)' : 'var(--border)'}`,
                          cursor: 'pointer',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          gap: '12px',
                          transition: 'all var(--transition-fast)',
                        }}
                      >
                        <div>
                          <div style={{ fontWeight: 600, fontSize: '0.86rem', color: 'var(--text-primary)' }}>
                            {cat.label}
                          </div>
                          <div style={{ fontSize: '0.74rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                            {cat.desc}
                          </div>
                        </div>
                        <input
                          type="checkbox"
                          checked={enabled}
                          onChange={() => {}}
                          style={{ width: '16px', height: '16px', cursor: 'pointer' }}
                        />
                      </div>
                    )
                  })}
                </div>
              </div>

              {/* Actions */}
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
                <Link to="/notifications" className="btn btn-secondary">
                  Cancel
                </Link>
                <button className="btn btn-primary" type="submit" disabled={saving}>
                  <Save size={16} />
                  <span>{saving ? 'Saving...' : 'Save Preferences'}</span>
                </button>
              </div>
            </form>
          )}
        </div>
      </main>
    </div>
  )
}
