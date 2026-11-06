import {
  ArrowLeft,
  Check,
  CircleAlert,
  Copy,
  LogOut,
  Mic,
  MicOff,
  Video,
  VideoOff,
} from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { ApiError, meetingsApi } from '../api'
import { JitsiEmbed } from '../components/JitsiEmbed'
import { WebRtcConference } from '../components/conference/WebRtcConference'
import type { JoinMeetingResponse, Meeting, MeetingParticipant } from '../types'
import { formatMeetingTime, statusLabel } from '../utils'

export function MeetingRoom() {
  const { meetingId } = useParams()
  const navigate = useNavigate()
  const leavingRef = useRef(false)

  const [meeting, setMeeting] = useState<Meeting | null>(null)
  const [participants, setParticipants] = useState<MeetingParticipant[]>([])
  const [connection, setConnection] = useState<JoinMeetingResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [joining, setJoining] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [copiedLink, setCopiedLink] = useState(false)

  // Conference mode: 'WEBRTC' (built-in Google Meet style) vs 'JITSI' (external)
  const [conferenceEngine, setConferenceEngine] = useState<'WEBRTC' | 'JITSI'>('WEBRTC')

  // Pre-join preview state
  const [previewStream, setPreviewStream] = useState<MediaStream | null>(null)
  const [previewMic, setPreviewMic] = useState(true)
  const [previewCam, setPreviewCam] = useState(true)
  const previewVideoRef = useRef<HTMLVideoElement | null>(null)

  // 1. Load meeting details
  useEffect(() => {
    if (!meetingId) return
    let cancelled = false
    void (async () => {
      try {
        const loadedMeeting = await meetingsApi.get(meetingId)
        if (cancelled) return
        setMeeting(loadedMeeting)
        const parts = await meetingsApi.participants(meetingId).catch(() => [])
        if (!cancelled) setParticipants(parts)
      } catch (requestError) {
        if (!cancelled) {
          setError(requestError instanceof ApiError ? requestError.message : 'Unable to load this meeting.')
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [meetingId])

  // 2. Pre-join Camera Preview
  useEffect(() => {
    if (connection) {
      // Stop preview when joining the actual conference
      if (previewStream) {
        previewStream.getTracks().forEach((t) => t.stop())
        setPreviewStream(null)
      }
      return
    }

    let active = true
    const startPreview = async () => {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: true,
          audio: true,
        })
        if (!active) {
          stream.getTracks().forEach((t) => t.stop())
          return
        }
        setPreviewStream(stream)
        if (previewVideoRef.current) {
          previewVideoRef.current.srcObject = stream
        }
      } catch {
        // Fallback gracefully if no webcam found
      }
    }

    void startPreview()

    return () => {
      active = false
      if (previewStream) {
        previewStream.getTracks().forEach((t) => t.stop())
      }
    }
  }, [connection])

  const togglePreviewMic = () => {
    if (previewStream) {
      const audioTrack = previewStream.getAudioTracks()[0]
      if (audioTrack) {
        audioTrack.enabled = !previewMic
      }
    }
    setPreviewMic(!previewMic)
  }

  const togglePreviewCam = () => {
    if (previewStream) {
      const videoTrack = previewStream.getVideoTracks()[0]
      if (videoTrack) {
        videoTrack.enabled = !previewCam
      }
    }
    setPreviewCam(!previewCam)
  }

  const handleJoin = async () => {
    if (!meetingId) return
    setJoining(true)
    setError(null)
    try {
      const joined = await meetingsApi.join(meetingId)
      setConnection(joined)
    } catch (requestError) {
      setError(requestError instanceof ApiError ? requestError.message : 'Unable to join this meeting.')
    } finally {
      setJoining(false)
    }
  }

  const handleLeave = useCallback(async () => {
    if (!meetingId || leavingRef.current) return
    leavingRef.current = true
    setConnection(null)
    try {
      await meetingsApi.leave(meetingId)
    } catch {
      // ignore
    } finally {
      navigate('/app/meetings', { replace: true })
    }
  }, [meetingId, navigate])

  const handleEndMeeting = async () => {
    if (!meetingId || !window.confirm('End this meeting for everyone?')) return
    leavingRef.current = true
    setConnection(null)
    setError(null)
    try {
      await meetingsApi.end(meetingId)
      navigate('/app/meetings', { replace: true })
    } catch (requestError) {
      leavingRef.current = false
      setError(requestError instanceof ApiError ? requestError.message : 'Unable to end this meeting.')
    }
  }

  const copyMeetingLink = async () => {
    try {
      await navigator.clipboard.writeText(window.location.href)
      setCopiedLink(true)
      setTimeout(() => setCopiedLink(false), 2500)
    } catch {
      setError('Unable to copy the meeting link. Copy it from your browser address bar instead.')
    }
  }

  if (!meetingId) return <Navigate to="/app/meetings" replace />

  if (loading) {
    return (
      <div className="meeting-room" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '80vh' }}>
        <div className="loading-state">
          <span className="spinner" aria-label="Loading meeting" />
          <p style={{ marginTop: '12px', color: 'var(--text-secondary)' }}>Preparing meeting room...</p>
        </div>
      </div>
    )
  }

  if (!meeting) {
    return (
      <div className="meeting-room">
        <div className="prejoin">
          <div className="prejoin__card">
            <span className="prejoin__icon">
              <CircleAlert size={28} aria-hidden="true" />
            </span>
            <h1>Meeting unavailable</h1>
            <p>{error ?? 'This meeting could not be found or has already expired.'}</p>
            <button className="btn btn-primary" type="button" onClick={() => navigate('/app/meetings')}>
              Back to meetings
            </button>
          </div>
        </div>
      </div>
    )
  }

  const canJoin = meeting.status === 'ACTIVE' || meeting.status === 'SCHEDULED'
  const isHost = connection?.role === 'ORGANIZER' || connection?.role === 'CO_HOST'

  // Render Full Screen In-Call Conference when joined
  if (connection) {
    if (conferenceEngine === 'WEBRTC') {
      return (
        <WebRtcConference
          meeting={meeting}
          connection={connection}
          onLeave={handleLeave}
          onEndMeeting={handleEndMeeting}
          isHost={isHost}
        />
      )
    }

    // Jitsi Fallback
    return (
      <div className="meeting-room" style={{ height: '100vh', display: 'flex', flexDirection: 'column' }}>
        <header className="meeting-room__header">
          <div className="meeting-room__title">
            <button className="btn btn-ghost btn-icon" type="button" onClick={handleLeave} aria-label="Leave meeting">
              <ArrowLeft size={19} aria-hidden="true" />
            </button>
            <div>
              <h1>{meeting.title}</h1>
              <p>Connected via Jitsi Meet as {connection.displayName}</p>
            </div>
          </div>
          <div className="meeting-room__header-actions">
            <button className="btn btn-danger" type="button" onClick={handleLeave}>
              <LogOut size={16} aria-hidden="true" /> Leave
            </button>
          </div>
        </header>
        <div style={{ flex: 1 }}>
          <JitsiEmbed connection={connection} onConferenceLeft={handleLeave} onError={(err) => setError(err)} />
        </div>
      </div>
    )
  }

  // Pre-join Lobby Screen (Google Meet style)
  return (
    <div
      style={{
        minHeight: '100vh',
        background: 'var(--bg-primary, #0b0f19)',
        color: '#f8fafc',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      {/* Top Bar */}
      <header
        style={{
          padding: '16px 28px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <button
            type="button"
            className="btn btn-ghost btn-icon"
            onClick={() => navigate('/app/meetings')}
            aria-label="Back to meetings"
          >
            <ArrowLeft size={18} />
          </button>
          <span style={{ fontWeight: 700, fontSize: '1.1rem' }}>Digital Office Meet</span>
        </div>

        <button
          type="button"
          onClick={copyMeetingLink}
          className="btn btn-secondary btn-sm"
          style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
        >
          {copiedLink ? <Check size={14} /> : <Copy size={14} />}
          {copiedLink ? 'Copied link!' : 'Copy invite link'}
        </button>
      </header>

      {/* Lobby Content */}
      <main
        style={{
          flex: 1,
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          padding: '32px 20px',
        }}
      >
        <div
          style={{
            maxWidth: '920px',
            width: '100%',
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))',
            gap: '32px',
            alignItems: 'center',
          }}
        >
          {/* Left: Camera & Microphone Preview Tile */}
          <div
            style={{
              background: '#1a1d26',
              borderRadius: '16px',
              overflow: 'hidden',
              boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.5)',
              border: '1px solid rgba(255, 255, 255, 0.1)',
              position: 'relative',
              aspectRatio: '16/10',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'center',
              alignItems: 'center',
            }}
          >
            {previewCam ? (
              <video
                ref={previewVideoRef}
                autoPlay
                playsInline
                muted
                style={{
                  width: '100%',
                  height: '100%',
                  objectFit: 'cover',
                  transform: 'scaleX(-1)',
                }}
              />
            ) : (
              <div
                style={{
                  width: '90px',
                  height: '90px',
                  borderRadius: '50%',
                  background: '#4f46e5',
                  color: '#fff',
                  display: 'flex',
                  justifyContent: 'center',
                  alignItems: 'center',
                  fontSize: '2rem',
                  fontWeight: 700,
                }}
              >
                DO
              </div>
            )}

            {/* In-Preview Controls */}
            <div
              style={{
                position: 'absolute',
                bottom: '16px',
                left: '50%',
                transform: 'translateX(-50%)',
                display: 'flex',
                gap: '12px',
                background: 'rgba(0, 0, 0, 0.6)',
                backdropFilter: 'blur(8px)',
                padding: '8px 14px',
                borderRadius: '30px',
              }}
            >
              <button
                type="button"
                onClick={togglePreviewMic}
                title={previewMic ? 'Mute microphone' : 'Unmute microphone'}
                style={{
                  width: '42px',
                  height: '42px',
                  borderRadius: '50%',
                  border: 'none',
                  background: previewMic ? 'rgba(255, 255, 255, 0.15)' : '#ef4444',
                  color: '#fff',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  cursor: 'pointer',
                }}
              >
                {previewMic ? <Mic size={18} /> : <MicOff size={18} />}
              </button>

              <button
                type="button"
                onClick={togglePreviewCam}
                title={previewCam ? 'Turn off camera' : 'Turn on camera'}
                style={{
                  width: '42px',
                  height: '42px',
                  borderRadius: '50%',
                  border: 'none',
                  background: previewCam ? 'rgba(255, 255, 255, 0.15)' : '#ef4444',
                  color: '#fff',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  cursor: 'pointer',
                }}
              >
                {previewCam ? <Video size={18} /> : <VideoOff size={18} />}
              </button>
            </div>
          </div>

          {/* Right: Meeting Info & Join Action */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
            <div>
              <span
                style={{
                  display: 'inline-block',
                  background: 'rgba(99, 102, 241, 0.15)',
                  color: '#818cf8',
                  padding: '3px 10px',
                  borderRadius: '6px',
                  fontSize: '0.8rem',
                  fontWeight: 600,
                  marginBottom: '8px',
                }}
              >
                {meeting.meetingType} • {statusLabel(meeting.status)}
              </span>
              <h1 style={{ fontSize: '1.9rem', fontWeight: 800, margin: '0 0 6px', color: '#f1f5f9' }}>
                {meeting.title}
              </h1>
              <p style={{ margin: 0, color: '#94a3b8', fontSize: '0.9rem' }}>
                {meeting.description || 'Secure video room with peer-to-peer HD video and audio.'}
              </p>
            </div>

            <div
              style={{
                background: 'rgba(255, 255, 255, 0.03)',
                border: '1px solid rgba(255, 255, 255, 0.08)',
                borderRadius: '10px',
                padding: '14px 16px',
                display: 'flex',
                flexDirection: 'column',
                gap: '8px',
                fontSize: '0.85rem',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: '#94a3b8' }}>Room Code:</span>
                <code style={{ color: '#38bdf8' }}>{meeting.roomCode}</code>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: '#94a3b8' }}>Capacity:</span>
                <span>Up to {meeting.maxParticipants} participants</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: '#94a3b8' }}>Registered:</span>
                <span>{participants.length} member{participants.length === 1 ? '' : 's'}</span>
              </div>
              {meeting.scheduledStart && (
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: '#94a3b8' }}>Time:</span>
                  <span>{formatMeetingTime(meeting)}</span>
                </div>
              )}
            </div>

            {/* Conference Engine Selector */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
              <label style={{ fontSize: '0.78rem', color: '#94a3b8', fontWeight: 600 }}>
                Conference Engine
              </label>
              <div style={{ display: 'flex', gap: '10px' }}>
                <button
                  type="button"
                  onClick={() => setConferenceEngine('WEBRTC')}
                  style={{
                    flex: 1,
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: conferenceEngine === 'WEBRTC' ? '2px solid #6366f1' : '1px solid rgba(255,255,255,0.1)',
                    background: conferenceEngine === 'WEBRTC' ? 'rgba(99, 102, 241, 0.1)' : 'transparent',
                    color: '#fff',
                    fontSize: '0.82rem',
                    fontWeight: 600,
                    cursor: 'pointer',
                  }}
                >
                  🚀 In-App Meet (WebRTC)
                </button>
                <button
                  type="button"
                  onClick={() => setConferenceEngine('JITSI')}
                  style={{
                    flex: 1,
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: conferenceEngine === 'JITSI' ? '2px solid #6366f1' : '1px solid rgba(255,255,255,0.1)',
                    background: conferenceEngine === 'JITSI' ? 'rgba(99, 102, 241, 0.1)' : 'transparent',
                    color: '#fff',
                    fontSize: '0.82rem',
                    fontWeight: 600,
                    cursor: 'pointer',
                  }}
                >
                  🌐 External Jitsi
                </button>
              </div>
            </div>

            {error && <div className="inline-alert" role="alert">{error}</div>}

            <div style={{ display: 'flex', gap: '12px', marginTop: '6px' }}>
              <button
                className="btn btn-secondary"
                type="button"
                onClick={() => navigate('/app/meetings')}
                style={{ flex: 1, padding: '12px' }}
              >
                Back to dashboard
              </button>
              <button
                className="btn btn-primary"
                type="button"
                onClick={handleJoin}
                disabled={!canJoin || joining}
                style={{
                  flex: 2,
                  padding: '12px',
                  fontSize: '0.95rem',
                  fontWeight: 700,
                  display: 'flex',
                  justifyContent: 'center',
                  alignItems: 'center',
                  gap: '8px',
                }}
              >
                {joining ? <span className="spinner" /> : <Video size={18} />}
                {joining ? 'Entering room…' : canJoin ? 'Join meeting now' : 'Meeting closed'}
              </button>
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}
