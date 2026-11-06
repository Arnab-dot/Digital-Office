import { CalendarDays, CircleAlert, Clock3, LoaderCircle, Plus, RefreshCw, Users, Video } from 'lucide-react'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError, meetingsApi } from '../api'
import { CreateMeetingModal } from '../components/CreateMeetingModal'
import type { Meeting, MeetingStatus } from '../types'
import { formatMeetingTime, statusLabel } from '../utils'

type MeetingFilter = 'ALL' | 'ACTIVE' | 'SCHEDULED' | 'PAST'

const filterLabels: Record<MeetingFilter, string> = {
  ALL: 'All meetings',
  ACTIVE: 'In progress',
  SCHEDULED: 'Scheduled',
  PAST: 'Past',
}

function statusClass(status: MeetingStatus): string {
  return `badge badge--${status.toLowerCase()}`
}

function MeetingCard({ meeting, onOpen }: { meeting: Meeting; onOpen: () => void }) {
  return (
    <article className="meeting-card">
      <div className="meeting-card__top">
        <span className={statusClass(meeting.status)}>{statusLabel(meeting.status)}</span>
        {meeting.meetingType === 'INSTANT' && <span className="badge badge--instant">Instant</span>}
      </div>
      <div className="meeting-card__meta"><Clock3 size={15} aria-hidden="true" /> {formatMeetingTime(meeting)}</div>
      <h3>{meeting.title}</h3>
      <p className="meeting-card__description">{meeting.description || 'No description provided.'}</p>
      <div className="meeting-card__footer">
        <span className="meeting-capacity"><Users size={14} aria-hidden="true" /> Up to {meeting.maxParticipants}</span>
        <button className={meeting.status === 'ACTIVE' ? 'btn btn-primary btn-sm' : 'btn btn-secondary btn-sm'} type="button" onClick={onOpen}>
          {meeting.status === 'ACTIVE' ? 'Join now' : meeting.status === 'SCHEDULED' ? 'Open' : 'View'}
        </button>
      </div>
    </article>
  )
}

export function MeetingDashboard() {
  const navigate = useNavigate()
  const [meetings, setMeetings] = useState<Meeting[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filter, setFilter] = useState<MeetingFilter>('ALL')
  const [showCreateModal, setShowCreateModal] = useState(false)

  const refreshMeetings = useCallback(async () => {
    setError(null)
    setLoading(true)
    try {
      setMeetings(await meetingsApi.list())
    } catch (requestError) {
      setError(requestError instanceof ApiError ? requestError.message : 'Unable to load your meetings.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    void (async () => {
      try {
        const loadedMeetings = await meetingsApi.list()
        if (!cancelled) setMeetings(loadedMeetings)
      } catch (requestError) {
        if (!cancelled) setError(requestError instanceof ApiError ? requestError.message : 'Unable to load your meetings.')
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => { cancelled = true }
  }, [])

  const visibleMeetings = useMemo(() => meetings.filter((meeting) => {
    if (filter === 'PAST') return meeting.status === 'COMPLETED' || meeting.status === 'CANCELLED'
    return filter === 'ALL' || meeting.status === filter
  }), [filter, meetings])

  const activeCount = meetings.filter((meeting) => meeting.status === 'ACTIVE').length
  const scheduledCount = meetings.filter((meeting) => meeting.status === 'SCHEDULED').length
  const completedCount = meetings.filter((meeting) => meeting.status === 'COMPLETED').length

  return (
    <div className="meetings-page" style={{ padding: '8px 0' }}>
      <main className="page-content" style={{ padding: 0 }}>
        <section className="dashboard-hero" aria-labelledby="meetings-title">
          <div>
            <span className="eyebrow"><Video size={14} aria-hidden="true" /> Video conferencing</span>
            <h1 id="meetings-title">Meet face to face, wherever work happens.</h1>
            <p>Create a room in seconds, invite your team, and keep the conversation moving without leaving Digital Office.</p>
          </div>
          <div className="dashboard-actions">
            <button className="btn btn-secondary" type="button" onClick={() => void refreshMeetings()} disabled={loading}>
              <RefreshCw size={16} aria-hidden="true" /> Refresh
            </button>
            <button className="btn btn-primary" type="button" onClick={() => setShowCreateModal(true)}>
              <Plus size={17} aria-hidden="true" /> New meeting
            </button>
          </div>
        </section>

        <section className="meeting-overview" aria-label="Meeting overview">
          <div className="overview-card"><span className="overview-card__icon"><Video size={20} aria-hidden="true" /></span><div><span className="overview-card__label">Happening now</span><strong className="overview-card__value">{activeCount}</strong></div></div>
          <div className="overview-card"><span className="overview-card__icon"><CalendarDays size={20} aria-hidden="true" /></span><div><span className="overview-card__label">Coming up</span><strong className="overview-card__value">{scheduledCount}</strong></div></div>
          <div className="overview-card"><span className="overview-card__icon"><Clock3 size={20} aria-hidden="true" /></span><div><span className="overview-card__label">Completed</span><strong className="overview-card__value">{completedCount}</strong></div></div>
        </section>

        <section aria-labelledby="meeting-list-title">
          <div className="meeting-toolbar">
            <h2 id="meeting-list-title">{filterLabels[filter]}</h2>
            <div className="filter-tabs" role="tablist" aria-label="Filter meetings">
              {(Object.keys(filterLabels) as MeetingFilter[]).map((name) => (
                <button key={name} type="button" className={`filter-tab ${filter === name ? 'filter-tab--active' : ''}`} role="tab" aria-selected={filter === name} onClick={() => setFilter(name)}>{filterLabels[name]}</button>
              ))}
            </div>
          </div>

          {error && <div className="inline-alert" role="alert"><CircleAlert size={18} aria-hidden="true" /> <span>{error}</span></div>}

          {loading ? (
            <div className="loading-state"><LoaderCircle className="spinner" aria-label="Loading meetings" /></div>
          ) : visibleMeetings.length ? (
            <div className="meetings-grid">
              {visibleMeetings.map((meeting) => <MeetingCard key={meeting.id} meeting={meeting} onOpen={() => navigate(`/app/meetings/${meeting.id}`)} />)}
            </div>
          ) : (
            <div className="empty-state">
              <span className="empty-state__icon"><Video size={29} aria-hidden="true" /></span>
              <h3>{filter === 'ALL' ? 'No meetings yet' : `No ${filterLabels[filter].toLowerCase()} meetings`}</h3>
              <p>{filter === 'ALL' ? 'Create a private room or schedule time with your team to get started.' : 'Try another filter, or create a meeting for your team.'}</p>
              <button className="btn btn-primary" type="button" onClick={() => setShowCreateModal(true)}><Plus size={17} aria-hidden="true" /> New meeting</button>
            </div>
          )}
        </section>
      </main>

      {showCreateModal && <CreateMeetingModal onClose={() => setShowCreateModal(false)} onCreated={(meetingId) => navigate(`/app/meetings/${meetingId}`)} />}
    </div>
  )
}
