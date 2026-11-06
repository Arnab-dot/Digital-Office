import { CalendarClock, Video, X, Zap } from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { ApiError, meetingsApi, organizationApi, projectApi } from '../api'
import { useWorkspace } from '../app/providers/WorkspaceProvider'
import type { CreateMeetingInput, MeetingType, Organization, Project } from '../types'
import { toLocalDateTimeInput } from '../utils'

interface CreateMeetingModalProps {
  onClose: () => void
  onCreated: (meetingId: string) => void
}

interface MeetingFormState {
  title: string
  description: string
  type: MeetingType
  maxParticipants: number
  scheduledStart: string
  scheduledEnd: string
  organizationId: string
  projectId: string
}

const isUuid = (val: string) =>
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(val.trim())

export function CreateMeetingModal({ onClose, onCreated }: CreateMeetingModalProps) {
  const { currentOrganization } = useWorkspace()
  const [organizations, setOrganizations] = useState<Organization[]>([])
  const [projects, setProjects] = useState<Project[]>([])

  const [form, setForm] = useState<MeetingFormState>(() => {
    const now = new Date()
    const start = new Date(now.getTime() + 30 * 60_000)
    const end = new Date(start.getTime() + 60 * 60_000)
    return {
      title: '',
      description: '',
      type: 'INSTANT',
      maxParticipants: 50,
      scheduledStart: toLocalDateTimeInput(start),
      scheduledEnd: toLocalDateTimeInput(end),
      organizationId: currentOrganization?.id || '',
      projectId: '',
    }
  })
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    organizationApi.list().then(setOrganizations).catch(() => {})
    projectApi.list(0, 50).then((res) => {
      setProjects(Array.isArray(res) ? res : [])
    }).catch(() => {})
  }, [])

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !submitting) onClose()
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onClose, submitting])

  const update = <K extends keyof MeetingFormState>(key: K, value: MeetingFormState[K]) => {
    setForm((current) => ({ ...current, [key]: value }))
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)

    if (!form.title.trim()) {
      setError('Give the meeting a title before creating it.')
      return
    }
    if (form.type === 'SCHEDULED' && (!form.scheduledStart || !form.scheduledEnd)) {
      setError('Scheduled meetings need a start and end time.')
      return
    }
    if (form.type === 'SCHEDULED' && new Date(form.scheduledEnd) <= new Date(form.scheduledStart)) {
      setError('The end time must be after the start time.')
      return
    }

    const input: CreateMeetingInput = {
      title: form.title.trim(),
      description: form.description.trim() || undefined,
      meetingType: form.type,
      maxParticipants: form.maxParticipants || 50,
      organizationId: isUuid(form.organizationId) ? form.organizationId.trim() : undefined,
      projectId: isUuid(form.projectId) ? form.projectId.trim() : undefined,
      scheduledStart: form.type === 'SCHEDULED' ? new Date(form.scheduledStart).toISOString() : undefined,
      scheduledEnd: form.type === 'SCHEDULED' ? new Date(form.scheduledEnd).toISOString() : undefined,
    }

    setSubmitting(true)
    try {
      const meeting = await meetingsApi.create(input)
      onCreated(meeting.id)
    } catch (requestError) {
      setError(requestError instanceof ApiError ? requestError.message : 'Unable to create the meeting.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="modal-overlay" role="presentation" onMouseDown={(event) => {
      if (event.target === event.currentTarget && !submitting) onClose()
    }}>
      <section className="modal" role="dialog" aria-modal="true" aria-labelledby="new-meeting-title">
        <div className="modal__heading">
          <div>
            <h2 id="new-meeting-title">Start a meeting</h2>
            <p>Set up a room for a live HD video conversation or a scheduled session.</p>
          </div>
          <button className="btn btn-ghost btn-icon" type="button" onClick={onClose} disabled={submitting} aria-label="Close dialog">
            <X size={19} aria-hidden="true" />
          </button>
        </div>

        <form className="meeting-form" onSubmit={handleSubmit}>
          <div className="input-group">
            <label htmlFor="meeting-title">Meeting title *</label>
            <input id="meeting-title" className="input" value={form.title} maxLength={300} autoFocus placeholder="e.g. Design Sync & Review" onChange={(event) => update('title', event.target.value)} required />
          </div>

          <div className="input-group">
            <label htmlFor="meeting-description">Description <span>(optional)</span></label>
            <textarea id="meeting-description" className="input" value={form.description} maxLength={2000} placeholder="Let attendees know what this session is about." onChange={(event) => update('description', event.target.value)} />
          </div>

          <div className="input-group">
            <label>Meeting type</label>
            <div className="meeting-type-control">
              <button type="button" aria-pressed={form.type === 'INSTANT'} onClick={() => update('type', 'INSTANT')}>
                <Zap size={17} aria-hidden="true" /> Start now (Instant)
              </button>
              <button type="button" aria-pressed={form.type === 'SCHEDULED'} onClick={() => update('type', 'SCHEDULED')}>
                <CalendarClock size={17} aria-hidden="true" /> Schedule for later
              </button>
            </div>
          </div>

          {form.type === 'SCHEDULED' && (
            <div className="field-row">
              <div className="input-group">
                <label htmlFor="meeting-start">Start time</label>
                <input id="meeting-start" className="input" type="datetime-local" value={form.scheduledStart} onChange={(event) => update('scheduledStart', event.target.value)} required />
              </div>
              <div className="input-group">
                <label htmlFor="meeting-end">End time</label>
                <input id="meeting-end" className="input" type="datetime-local" value={form.scheduledEnd} onChange={(event) => update('scheduledEnd', event.target.value)} required />
              </div>
            </div>
          )}

          <div className="field-row">
            <div className="input-group">
              <label htmlFor="meeting-capacity">Participant capacity</label>
              <input id="meeting-capacity" className="input" type="number" min="2" max="100" value={form.maxParticipants} onChange={(event) => update('maxParticipants', Number(event.target.value))} required />
            </div>
            <div className="input-group">
              <label htmlFor="meeting-org">Organization</label>
              {organizations.length > 0 ? (
                <select id="meeting-org" className="input" value={form.organizationId} onChange={(event) => update('organizationId', event.target.value)}>
                  <option value="">No organization (Personal)</option>
                  {organizations.map((o) => (
                    <option key={o.id} value={o.id}>{o.name}</option>
                  ))}
                </select>
              ) : (
                <input id="meeting-org" className="input" value={form.organizationId} placeholder="Optional Organization UUID" onChange={(event) => update('organizationId', event.target.value)} />
              )}
            </div>
          </div>

          {projects.length > 0 && (
            <div className="input-group">
              <label htmlFor="meeting-project">Link to Project <span>(optional)</span></label>
              <select id="meeting-project" className="input" value={form.projectId} onChange={(event) => update('projectId', event.target.value)}>
                <option value="">None (Independent Meeting)</option>
                {projects.map((p) => (
                  <option key={p.id} value={p.id}>{p.name}</option>
                ))}
              </select>
            </div>
          )}

          {error && <div className="inline-alert" role="alert">{error}</div>}

          <div className="modal-actions">
            <button className="btn btn-ghost" type="button" onClick={onClose} disabled={submitting}>Cancel</button>
            <button className="btn btn-primary" type="submit" disabled={submitting}>
              {submitting ? <span className="spinner" aria-label="Creating meeting" /> : <Video size={17} aria-hidden="true" />}
              {submitting ? 'Setting up…' : form.type === 'INSTANT' ? 'Start call now' : 'Schedule meeting'}
            </button>
          </div>
        </form>
      </section>
    </div>
  )
}
