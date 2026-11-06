import {
  Check,
  Copy,
  Hand,
  Mic,
  MicOff,
  Monitor,
  MonitorOff,
  Pin,
  PinOff,
  Send,
  Users,
  Video,
  VideoOff,
  X,
} from 'lucide-react'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { meetingsApi } from '../../api'
import type { ChatMessage, JoinMeetingResponse, Meeting } from '../../types'
import { initials } from '../../utils'

interface WebRtcConferenceProps {
  meeting: Meeting
  connection: JoinMeetingResponse
  onLeave: () => void
  onEndMeeting?: () => void
  isHost: boolean
}

interface RemotePeer {
  peerId: string
  displayName: string
  stream: MediaStream
  isMuted: boolean
  isVideoOff: boolean
  isHandRaised: boolean
  isSpeaking: boolean
}

const RTC_CONFIG: RTCConfiguration = {
  iceServers: [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:stun1.l.google.com:19302' },
    { urls: 'stun:stun2.l.google.com:19302' },
  ],
}

/**
 * Creates a synthetic fallback MediaStream with an animated avatar canvas
 * and silent audio track so WebRTC connections work even on machines without physical webcams.
 */
function createSyntheticMediaStream(displayName: string): MediaStream {
  const canvas = document.createElement('canvas')
  canvas.width = 640
  canvas.height = 360
  const ctx = canvas.getContext('2d')
  if (ctx) {
    let frame = 0
    const draw = () => {
      frame++
      // Gradient background
      const grad = ctx.createLinearGradient(0, 0, canvas.width, canvas.height)
      grad.addColorStop(0, '#1e1b4b')
      grad.addColorStop(1, '#0f172a')
      ctx.fillStyle = grad
      ctx.fillRect(0, 0, canvas.width, canvas.height)

      // Center Avatar Circle
      ctx.beginPath()
      ctx.arc(canvas.width / 2, canvas.height / 2, 60, 0, Math.PI * 2)
      ctx.fillStyle = '#6366f1'
      ctx.fill()
      ctx.lineWidth = 4
      ctx.strokeStyle = '#818cf8'
      ctx.stroke()

      // Initials Text
      ctx.fillStyle = '#ffffff'
      ctx.font = 'bold 36px system-ui, -apple-system, sans-serif'
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillText(initials(displayName), canvas.width / 2, canvas.height / 2)

      // Small animated indicator
      ctx.beginPath()
      ctx.arc(canvas.width / 2 + 40, canvas.height / 2 + 40, 10, 0, Math.PI * 2)
      ctx.fillStyle = frame % 60 < 30 ? '#10b981' : '#059669'
      ctx.fill()

      requestAnimationFrame(draw)
    }
    draw()
  }

  const videoStream = canvas.captureStream(24)

  // Web Audio silent track
  const audioCtx = new (window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext)()
  const osc = audioCtx.createOscillator()
  const gain = audioCtx.createGain()
  gain.gain.value = 0
  osc.connect(gain)
  const dest = audioCtx.createMediaStreamDestination()
  gain.connect(dest)
  osc.start()

  const combined = new MediaStream()
  videoStream.getVideoTracks().forEach((t) => combined.addTrack(t))
  dest.stream.getAudioTracks().forEach((t) => combined.addTrack(t))
  return combined
}

export function WebRtcConference({
  meeting,
  connection,
  onLeave,
  onEndMeeting,
  isHost,
}: WebRtcConferenceProps) {
  const myPeerId = useMemo(
    () => `peer-${connection.displayName.replace(/\s+/g, '-').toLowerCase()}-${Math.random().toString(36).substring(2, 7)}`,
    [connection.displayName]
  )

  // Local media state
  const [localStream, setLocalStream] = useState<MediaStream | null>(null)
  const [isMuted, setIsMuted] = useState(false)
  const [isVideoOff, setIsVideoOff] = useState(false)
  const [isScreenSharing, setIsScreenSharing] = useState(false)
  const [isHandRaised, setIsHandRaised] = useState(false)
  const [isLocalSpeaking, setIsLocalSpeaking] = useState(false)

  // Remote peers
  const [remotePeers, setRemotePeers] = useState<Map<string, RemotePeer>>(new Map())
  const [pinnedPeerId, setPinnedPeerId] = useState<string | null>(null)

  // UI drawers
  const [activeDrawer, setActiveDrawer] = useState<'none' | 'chat' | 'participants' | 'info'>('none')
  const [copiedLink, setCopiedLink] = useState(false)
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([])
  const [chatDraft, setChatDraft] = useState('')
  const [sendingChat, setSendingChat] = useState(false)

  // References
  const localVideoRef = useRef<HTMLVideoElement | null>(null)
  const peerConnections = useRef<Map<string, RTCPeerConnection>>(new Map())
  const remoteStreams = useRef<Map<string, MediaStream>>(new Map())
  const sinceRef = useRef<number>(Date.now() - 3000)
  const localStreamRef = useRef<MediaStream | null>(null)
  const originalVideoTrack = useRef<MediaStreamTrack | null>(null)
  const pollingRef = useRef<number | null>(null)

  // 1. Initialize Local Media (Camera + Microphone)
  useEffect(() => {
    let mounted = true

    const setupLocalMedia = async () => {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: { width: { ideal: 1280 }, height: { ideal: 720 }, frameRate: { ideal: 30 } },
          audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true },
        })

        if (!mounted) {
          stream.getTracks().forEach((t) => t.stop())
          return
        }

        localStreamRef.current = stream
        setLocalStream(stream)
        originalVideoTrack.current = stream.getVideoTracks()[0] || null

        if (localVideoRef.current) {
          localVideoRef.current.srcObject = stream
        }
      } catch (err) {
        console.warn('Real webcam not found or access denied, falling back to synthetic stream:', err)
        const fallback = createSyntheticMediaStream(connection.displayName)
        if (!mounted) return
        localStreamRef.current = fallback
        setLocalStream(fallback)
        originalVideoTrack.current = fallback.getVideoTracks()[0] || null

        if (localVideoRef.current) {
          localVideoRef.current.srcObject = fallback
        }
      }
    }

    void setupLocalMedia()

    return () => {
      mounted = false
      if (localStreamRef.current) {
        localStreamRef.current.getTracks().forEach((t) => t.stop())
      }
      peerConnections.current.forEach((pc) => pc.close())
      peerConnections.current.clear()
    }
  }, [connection.displayName])

  // Bind local stream to video element
  useEffect(() => {
    if (localVideoRef.current && localStream) {
      localVideoRef.current.srcObject = localStream
    }
  }, [localStream])

  // 2. Audio Level Monitor for speaking indicator
  useEffect(() => {
    if (!localStream) return
    const audioTrack = localStream.getAudioTracks()[0]
    if (!audioTrack) return

    let audioContext: AudioContext | null = null
    try {
      audioContext = new (window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext)()
      const analyser = audioContext.createAnalyser()
      analyser.fftSize = 256
      const source = audioContext.createMediaStreamSource(localStream)
      source.connect(analyser)

      const dataArray = new Uint8Array(analyser.frequencyBinCount)
      let animId = 0

      const checkVolume = () => {
        if (!isMuted && audioTrack.enabled) {
          analyser.getByteFrequencyData(dataArray)
          let sum = 0
          for (let i = 0; i < dataArray.length; i++) sum += dataArray[i]
          const avg = sum / dataArray.length
          setIsLocalSpeaking(avg > 18)
        } else {
          setIsLocalSpeaking(false)
        }
        animId = requestAnimationFrame(checkVolume)
      }
      checkVolume()

      return () => {
        cancelAnimationFrame(animId)
        if (audioContext && audioContext.state !== 'closed') {
          void audioContext.close()
        }
      }
    } catch {
      // AudioContext not supported or restricted
    }
  }, [localStream, isMuted])

  // Helper: Create & Configure RTCPeerConnection for a remote peer
  const getOrCreatePeerConnection = useCallback(
    (targetPeerId: string, remoteDisplayName: string) => {
      let pc = peerConnections.current.get(targetPeerId)
      if (pc) return pc

      pc = new RTCPeerConnection(RTC_CONFIG)
      peerConnections.current.set(targetPeerId, pc)

      // Attach local tracks
      if (localStreamRef.current) {
        localStreamRef.current.getTracks().forEach((track) => {
          pc?.addTrack(track, localStreamRef.current!)
        })
      }

      // Handle ICE Candidates
      pc.onicecandidate = (event) => {
        if (event.candidate) {
          void meetingsApi.sendSignal(meeting.id, {
            type: 'candidate',
            senderPeerId: myPeerId,
            senderName: connection.displayName,
            targetPeerId,
            payload: event.candidate,
          }).catch(() => {})
        }
      }

      // Handle Remote Tracks
      pc.ontrack = (event) => {
        const stream = event.streams[0] || new MediaStream([event.track])
        remoteStreams.current.set(targetPeerId, stream)

        setRemotePeers((prev) => {
          const next = new Map(prev)
          const existing = next.get(targetPeerId) || {
            peerId: targetPeerId,
            displayName: remoteDisplayName,
            stream,
            isMuted: false,
            isVideoOff: false,
            isHandRaised: false,
            isSpeaking: false,
          }
          existing.stream = stream
          next.set(targetPeerId, existing)
          return next
        })
      }

      pc.onconnectionstatechange = () => {
        if (pc?.connectionState === 'disconnected' || pc?.connectionState === 'failed' || pc?.connectionState === 'closed') {
          setRemotePeers((prev) => {
            const next = new Map(prev)
            next.delete(targetPeerId)
            return next
          })
          peerConnections.current.delete(targetPeerId)
        }
      }

      return pc
    },
    [connection.displayName, meeting.id, myPeerId]
  )

  // 3. Signaling Loop: Poll for offers, answers, candidates, and presence
  useEffect(() => {
    let active = true

    // Send initial join announce
    const announceJoin = async () => {
      try {
        await meetingsApi.sendSignal(meeting.id, {
          type: 'join',
          senderPeerId: myPeerId,
          senderName: connection.displayName,
          payload: {
            isMuted,
            isVideoOff,
            isHandRaised,
          },
        })
      } catch (err) {
        console.warn('Failed to announce join to room:', err)
      }
    }
    void announceJoin()

    const pollSignals = async () => {
      if (!active) return
      try {
        const signals = await meetingsApi.getSignals(meeting.id, myPeerId, sinceRef.current)
        for (const s of signals) {
          if (s.timestamp > sinceRef.current) {
            sinceRef.current = s.timestamp
          }

          if (s.senderPeerId === myPeerId) continue

          switch (s.type) {
            case 'join': {
              // A new peer joined. Initiate offer as existing peer.
              const pc = getOrCreatePeerConnection(s.senderPeerId, s.senderName)
              const offer = await pc.createOffer()
              await pc.setLocalDescription(offer)
              await meetingsApi.sendSignal(meeting.id, {
                type: 'offer',
                senderPeerId: myPeerId,
                senderName: connection.displayName,
                targetPeerId: s.senderPeerId,
                payload: offer,
              })
              break
            }

            case 'offer': {
              // Received offer from peer. Set remote desc and reply with answer.
              const pc = getOrCreatePeerConnection(s.senderPeerId, s.senderName)
              await pc.setRemoteDescription(new RTCSessionDescription(s.payload))
              const answer = await pc.createAnswer()
              await pc.setLocalDescription(answer)
              await meetingsApi.sendSignal(meeting.id, {
                type: 'answer',
                senderPeerId: myPeerId,
                senderName: connection.displayName,
                targetPeerId: s.senderPeerId,
                payload: answer,
              })
              break
            }

            case 'answer': {
              // Received answer to our offer.
              const pc = peerConnections.current.get(s.senderPeerId)
              if (pc && pc.signalingState !== 'stable') {
                await pc.setRemoteDescription(new RTCSessionDescription(s.payload))
              }
              break
            }

            case 'candidate': {
              // Received ICE candidate.
              const pc = peerConnections.current.get(s.senderPeerId)
              if (pc && s.payload) {
                try {
                  await pc.addIceCandidate(new RTCIceCandidate(s.payload))
                } catch (candidateErr) {
                  console.warn('Failed adding ICE candidate:', candidateErr)
                }
              }
              break
            }

            case 'state': {
              // Remote peer updated their mute / video / hand state
              setRemotePeers((prev) => {
                const next = new Map(prev)
                const p = next.get(s.senderPeerId)
                if (p) {
                  next.set(s.senderPeerId, {
                    ...p,
                    isMuted: s.payload?.isMuted ?? p.isMuted,
                    isVideoOff: s.payload?.isVideoOff ?? p.isVideoOff,
                    isHandRaised: s.payload?.isHandRaised ?? p.isHandRaised,
                  })
                }
                return next
              })
              break
            }

            case 'leave': {
              const pc = peerConnections.current.get(s.senderPeerId)
              if (pc) pc.close()
              peerConnections.current.delete(s.senderPeerId)
              setRemotePeers((prev) => {
                const next = new Map(prev)
                next.delete(s.senderPeerId)
                return next
              })
              break
            }
          }
        }
      } catch {
        // Silently continue polling
      }

      if (active) {
        pollingRef.current = window.setTimeout(pollSignals, 800)
      }
    }

    pollingRef.current = window.setTimeout(pollSignals, 500)

    return () => {
      active = false
      if (pollingRef.current) clearTimeout(pollingRef.current)
      void meetingsApi.sendSignal(meeting.id, {
        type: 'leave',
        senderPeerId: myPeerId,
        senderName: connection.displayName,
      }).catch(() => {})
    }
  }, [connection.displayName, getOrCreatePeerConnection, isHandRaised, isMuted, isVideoOff, meeting.id, myPeerId])

  // 4. Toggle Microphone
  const toggleMute = () => {
    if (!localStream) return
    const next = !isMuted
    localStream.getAudioTracks().forEach((track) => {
      track.enabled = !next
    })
    setIsMuted(next)
    void meetingsApi.sendSignal(meeting.id, {
      type: 'state',
      senderPeerId: myPeerId,
      senderName: connection.displayName,
      payload: { isMuted: next },
    }).catch(() => {})
  }

  // 5. Toggle Camera
  const toggleVideo = () => {
    if (!localStream) return
    const next = !isVideoOff
    localStream.getVideoTracks().forEach((track) => {
      track.enabled = !next
    })
    setIsVideoOff(next)
    void meetingsApi.sendSignal(meeting.id, {
      type: 'state',
      senderPeerId: myPeerId,
      senderName: connection.displayName,
      payload: { isVideoOff: next },
    }).catch(() => {})
  }

  // 6. Toggle Screen Sharing
  const toggleScreenShare = async () => {
    if (!localStream) return

    if (isScreenSharing) {
      // Revert back to webcam
      if (originalVideoTrack.current) {
        peerConnections.current.forEach((pc) => {
          const sender = pc.getSenders().find((s) => s.track && s.track.kind === 'video')
          if (sender && originalVideoTrack.current) {
            void sender.replaceTrack(originalVideoTrack.current)
          }
        })

        // Reconnect local preview
        const currentTracks = localStream.getVideoTracks()
        currentTracks.forEach((t) => {
          localStream.removeTrack(t)
          t.stop()
        })
        localStream.addTrack(originalVideoTrack.current)
        if (localVideoRef.current) {
          localVideoRef.current.srcObject = localStream
        }
      }
      setIsScreenSharing(false)
    } else {
      // Prompt screen share
      try {
        const displayStream = await navigator.mediaDevices.getDisplayMedia({
          video: { cursor: 'always' } as MediaTrackConstraints,
          audio: false,
        })
        const screenTrack = displayStream.getVideoTracks()[0]
        if (!screenTrack) return

        screenTrack.onended = () => {
          void toggleScreenShare()
        }

        peerConnections.current.forEach((pc) => {
          const sender = pc.getSenders().find((s) => s.track && s.track.kind === 'video')
          if (sender) {
            void sender.replaceTrack(screenTrack)
          }
        })

        const currentTracks = localStream.getVideoTracks()
        currentTracks.forEach((t) => localStream.removeTrack(t))
        localStream.addTrack(screenTrack)
        if (localVideoRef.current) {
          localVideoRef.current.srcObject = localStream
        }

        setIsScreenSharing(true)
      } catch (err) {
        console.warn('Screen share cancelled or failed:', err)
      }
    }
  }

  // 7. Toggle Hand Raise
  const toggleHandRaise = () => {
    const next = !isHandRaised
    setIsHandRaised(next)
    void meetingsApi.sendSignal(meeting.id, {
      type: 'state',
      senderPeerId: myPeerId,
      senderName: connection.displayName,
      payload: { isHandRaised: next },
    }).catch(() => {})
  }

  // 8. Copy invite link
  const copyInviteLink = async () => {
    const url = `${window.location.origin}/app/meetings/${meeting.id}`
    try {
      await navigator.clipboard.writeText(url)
      setCopiedLink(true)
      setTimeout(() => setCopiedLink(false), 2500)
    } catch {
      // Fallback
    }
  }

  // Load in-call chat
  const loadChat = useCallback(async () => {
    try {
      const msgs = await meetingsApi.chat(meeting.id)
      setChatMessages(msgs)
    } catch {
      // ignore
    }
  }, [meeting.id])

  useEffect(() => {
    if (activeDrawer === 'chat') {
      void loadChat()
      const t = setInterval(loadChat, 3000)
      return () => clearInterval(t)
    }
  }, [activeDrawer, loadChat])

  const sendChatMessage = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!chatDraft.trim()) return
    setSendingChat(true)
    try {
      const sent = await meetingsApi.sendMessage(meeting.id, chatDraft.trim())
      setChatMessages((prev) => [...prev, sent])
      setChatDraft('')
    } catch (err) {
      console.warn('Failed to send chat message:', err)
    } finally {
      setSendingChat(false)
    }
  }

  // Calculate layout class based on total participants (remote + self)
  const peerList = Array.from(remotePeers.values())
  const totalInCall = peerList.length + 1

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 50,
        background: '#12141a',
        color: '#f8fafc',
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
        fontFamily: 'Inter, system-ui, -apple-system, sans-serif',
      }}
    >
      {/* Top Header Bar */}
      <header
        style={{
          height: '56px',
          padding: '0 20px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          background: 'rgba(18, 20, 26, 0.95)',
          borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
          zIndex: 10,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div
            style={{
              width: '10px',
              height: '10px',
              borderRadius: '50%',
              background: '#22c55e',
              boxShadow: '0 0 8px #22c55e',
            }}
          />
          <h1 style={{ fontSize: '1.05rem', fontWeight: 700, margin: 0, color: '#f1f5f9' }}>
            {meeting.title}
          </h1>
          <span
            style={{
              fontSize: '0.8rem',
              color: '#94a3b8',
              background: 'rgba(255, 255, 255, 0.06)',
              padding: '2px 8px',
              borderRadius: '4px',
            }}
          >
            {meeting.roomCode}
          </span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <button
            type="button"
            onClick={copyInviteLink}
            style={{
              background: copiedLink ? '#059669' : 'rgba(255, 255, 255, 0.08)',
              border: 'none',
              borderRadius: '6px',
              padding: '6px 12px',
              color: '#fff',
              fontSize: '0.82rem',
              fontWeight: 500,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              transition: 'all 0.15s ease',
            }}
          >
            {copiedLink ? <Check size={14} /> : <Copy size={14} />}
            {copiedLink ? 'Copied link!' : 'Copy invite link'}
          </button>
        </div>
      </header>

      {/* Main View Area: Video Grid + Optional Side Drawer */}
      <div style={{ flex: 1, display: 'flex', overflow: 'hidden', position: 'relative' }}>
        {/* Video Grid */}
        <div
          style={{
            flex: 1,
            padding: '16px',
            overflowY: 'auto',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'center',
            alignItems: 'center',
          }}
        >
          <div
            style={{
              width: '100%',
              maxWidth: pinnedPeerId ? '1400px' : totalInCall === 1 ? '850px' : '1280px',
              height: '100%',
              display: 'grid',
              gap: '14px',
              gridTemplateColumns:
                pinnedPeerId
                  ? '3fr 1fr'
                  : totalInCall === 1
                  ? '1fr'
                  : totalInCall === 2
                  ? 'repeat(auto-fit, minmax(420px, 1fr))'
                  : 'repeat(auto-fit, minmax(340px, 1fr))',
              alignContent: 'center',
              alignItems: 'center',
            }}
          >
            {/* Local Video Tile ("You") */}
            <div
              style={{
                position: 'relative',
                background: '#1e222d',
                borderRadius: '12px',
                overflow: 'hidden',
                aspectRatio: '16/9',
                border: isLocalSpeaking ? '2px solid #22c55e' : '1px solid rgba(255, 255, 255, 0.1)',
                boxShadow: isLocalSpeaking ? '0 0 16px rgba(34, 197, 94, 0.4)' : 'none',
                transition: 'border 0.2s ease, box-shadow 0.2s ease',
              }}
            >
              <video
                ref={localVideoRef}
                autoPlay
                playsInline
                muted
                style={{
                  width: '100%',
                  height: '100%',
                  objectFit: 'cover',
                  transform: isScreenSharing ? 'none' : 'scaleX(-1)', // Mirror webcam, but not screen
                  display: isVideoOff ? 'none' : 'block',
                }}
              />

              {/* Camera Off Avatar Fallback */}
              {isVideoOff && (
                <div
                  style={{
                    width: '100%',
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'center',
                    alignItems: 'center',
                    background: '#1e222d',
                  }}
                >
                  <div
                    style={{
                      width: '84px',
                      height: '84px',
                      borderRadius: '50%',
                      background: '#4f46e5',
                      color: '#fff',
                      display: 'flex',
                      justifyContent: 'center',
                      alignItems: 'center',
                      fontSize: '1.8rem',
                      fontWeight: 700,
                      border: isLocalSpeaking ? '3px solid #22c55e' : '2px solid rgba(255, 255, 255, 0.2)',
                    }}
                  >
                    {initials(connection.displayName)}
                  </div>
                </div>
              )}

              {/* Name & Status Pill */}
              <div
                style={{
                  position: 'absolute',
                  bottom: '10px',
                  left: '10px',
                  background: 'rgba(0, 0, 0, 0.65)',
                  backdropFilter: 'blur(4px)',
                  padding: '4px 10px',
                  borderRadius: '6px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  fontSize: '0.8rem',
                  fontWeight: 600,
                  color: '#fff',
                }}
              >
                {isMuted ? <MicOff size={13} color="#ef4444" /> : <Mic size={13} color="#22c55e" />}
                <span>{connection.displayName} (You)</span>
                {isHost && (
                  <span style={{ background: '#3b82f6', fontSize: '0.65rem', padding: '1px 5px', borderRadius: '4px' }}>
                    Host
                  </span>
                )}
              </div>

              {/* Hand Raised Icon */}
              {isHandRaised && (
                <div
                  style={{
                    position: 'absolute',
                    top: '10px',
                    right: '10px',
                    background: '#eab308',
                    color: '#000',
                    borderRadius: '50%',
                    padding: '6px',
                    display: 'flex',
                    boxShadow: '0 2px 8px rgba(0,0,0,0.5)',
                  }}
                >
                  <Hand size={18} />
                </div>
              )}
            </div>

            {/* Remote Peer Video Tiles */}
            {peerList.map((peer) => (
              <RemoteVideoTile
                key={peer.peerId}
                peer={peer}
                isPinned={pinnedPeerId === peer.peerId}
                onTogglePin={() => setPinnedPeerId(pinnedPeerId === peer.peerId ? null : peer.peerId)}
              />
            ))}
          </div>
        </div>

        {/* Side Panel Drawer (Chat, Participants, Meeting Info) */}
        {activeDrawer !== 'none' && (
          <aside
            style={{
              width: '360px',
              background: '#1a1d26',
              borderLeft: '1px solid rgba(255, 255, 255, 0.08)',
              display: 'flex',
              flexDirection: 'column',
              zIndex: 20,
            }}
          >
            {/* Drawer Header */}
            <div
              style={{
                height: '56px',
                padding: '0 16px',
                borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
              }}
            >
              <h3 style={{ margin: 0, fontSize: '0.95rem', fontWeight: 700, color: '#f8fafc' }}>
                {activeDrawer === 'chat' && 'In-Call Messages'}
                {activeDrawer === 'participants' && `People in Call (${totalInCall})`}
                {activeDrawer === 'info' && 'Meeting Information'}
              </h3>
              <button
                type="button"
                onClick={() => setActiveDrawer('none')}
                style={{ background: 'transparent', border: 'none', color: '#94a3b8', cursor: 'pointer', padding: '4px' }}
              >
                <X size={18} />
              </button>
            </div>

            {/* Drawer Body */}
            <div style={{ flex: 1, overflowY: 'auto', padding: '16px' }}>
              {activeDrawer === 'chat' && (
                <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
                  <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                    {chatMessages.length === 0 ? (
                      <p style={{ color: '#64748b', fontSize: '0.85rem', textAlign: 'center', marginTop: '32px' }}>
                        No messages yet. Send a note to everyone in the room!
                      </p>
                    ) : (
                      chatMessages.map((m) => (
                        <div key={m.id} style={{ background: 'rgba(255,255,255,0.04)', borderRadius: '8px', padding: '10px' }}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.76rem', color: '#94a3b8', marginBottom: '4px' }}>
                            <strong style={{ color: '#cbd5e1' }}>{m.senderName}</strong>
                            <span>{new Date(m.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                          </div>
                          <div style={{ fontSize: '0.86rem', color: '#f1f5f9' }}>{m.content}</div>
                        </div>
                      ))
                    )}
                  </div>

                  <form onSubmit={sendChatMessage} style={{ marginTop: '12px', display: 'flex', gap: '8px' }}>
                    <input
                      type="text"
                      className="input"
                      placeholder="Send a message to everyone..."
                      value={chatDraft}
                      onChange={(e) => setChatDraft(e.target.value)}
                      style={{ fontSize: '0.84rem' }}
                    />
                    <button
                      type="submit"
                      disabled={sendingChat || !chatDraft.trim()}
                      style={{
                        background: '#4f46e5',
                        border: 'none',
                        color: '#fff',
                        borderRadius: '8px',
                        padding: '0 14px',
                        cursor: 'pointer',
                      }}
                    >
                      <Send size={15} />
                    </button>
                  </form>
                </div>
              )}

              {activeDrawer === 'participants' && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  {/* Self */}
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '8px 12px',
                      background: 'rgba(255, 255, 255, 0.04)',
                      borderRadius: '8px',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                      <div
                        style={{
                          width: '32px',
                          height: '32px',
                          borderRadius: '50%',
                          background: '#4f46e5',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          fontWeight: 600,
                          fontSize: '0.85rem',
                        }}
                      >
                        {initials(connection.displayName)}
                      </div>
                      <div>
                        <div style={{ fontSize: '0.86rem', fontWeight: 600 }}>{connection.displayName} (You)</div>
                        <div style={{ fontSize: '0.72rem', color: '#94a3b8' }}>{isHost ? 'Host' : 'Participant'}</div>
                      </div>
                    </div>
                    {isMuted ? <MicOff size={15} color="#ef4444" /> : <Mic size={15} color="#22c55e" />}
                  </div>

                  {/* Remote Peers */}
                  {peerList.map((peer) => (
                    <div
                      key={peer.peerId}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '8px 12px',
                        background: 'rgba(255, 255, 255, 0.02)',
                        borderRadius: '8px',
                        border: '1px solid rgba(255, 255, 255, 0.05)',
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                        <div
                          style={{
                            width: '32px',
                            height: '32px',
                            borderRadius: '50%',
                            background: '#0284c7',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            fontWeight: 600,
                            fontSize: '0.85rem',
                          }}
                        >
                          {initials(peer.displayName)}
                        </div>
                        <div>
                          <div style={{ fontSize: '0.86rem', fontWeight: 600 }}>{peer.displayName}</div>
                          <div style={{ fontSize: '0.72rem', color: '#94a3b8' }}>Connected</div>
                        </div>
                      </div>
                      {peer.isMuted ? <MicOff size={15} color="#ef4444" /> : <Mic size={15} color="#22c55e" />}
                    </div>
                  ))}
                </div>
              )}

              {activeDrawer === 'info' && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '14px', fontSize: '0.86rem' }}>
                  <div>
                    <span style={{ color: '#94a3b8', fontSize: '0.75rem', display: 'block' }}>Meeting Name</span>
                    <strong>{meeting.title}</strong>
                  </div>
                  <div>
                    <span style={{ color: '#94a3b8', fontSize: '0.75rem', display: 'block' }}>Meeting Code</span>
                    <code>{meeting.roomCode}</code>
                  </div>
                  <div>
                    <span style={{ color: '#94a3b8', fontSize: '0.75rem', display: 'block' }}>Invite Link</span>
                    <div style={{ display: 'flex', gap: '8px', marginTop: '4px' }}>
                      <input
                        type="text"
                        readOnly
                        value={`${window.location.origin}/app/meetings/${meeting.id}`}
                        className="input"
                        style={{ fontSize: '0.8rem', background: 'rgba(0,0,0,0.3)' }}
                      />
                      <button
                        type="button"
                        onClick={copyInviteLink}
                        style={{
                          background: '#4f46e5',
                          border: 'none',
                          borderRadius: '6px',
                          padding: '0 12px',
                          color: '#fff',
                          cursor: 'pointer',
                        }}
                      >
                        {copiedLink ? <Check size={14} /> : <Copy size={14} />}
                      </button>
                    </div>
                  </div>
                  <div style={{ borderTop: '1px solid rgba(255,255,255,0.08)', paddingTop: '12px' }}>
                    <p style={{ color: '#94a3b8', fontSize: '0.8rem' }}>
                      Share this link with any member of your organization. When they click the link, they will join the room with audio and video.
                    </p>
                  </div>
                </div>
              )}
            </div>
          </aside>
        )}
      </div>

      {/* Google Meet Bottom Controls Bar */}
      <footer
        style={{
          height: '76px',
          background: '#12141a',
          borderTop: '1px solid rgba(255, 255, 255, 0.08)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          padding: '0 24px',
          zIndex: 10,
        }}
      >
        {/* Left: Time & Room Code */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '0.85rem', color: '#94a3b8' }}>
          <span>{new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
          <span>•</span>
          <span>{meeting.roomCode}</span>
        </div>

        {/* Center: In-Call Action Pills */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          {/* Mic Button */}
          <button
            type="button"
            onClick={toggleMute}
            title={isMuted ? 'Unmute microphone' : 'Mute microphone'}
            style={{
              width: '46px',
              height: '46px',
              borderRadius: '50%',
              border: 'none',
              background: isMuted ? '#ef4444' : 'rgba(255, 255, 255, 0.12)',
              color: '#fff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
              transition: 'background 0.15s ease',
            }}
          >
            {isMuted ? <MicOff size={20} /> : <Mic size={20} />}
          </button>

          {/* Camera Button */}
          <button
            type="button"
            onClick={toggleVideo}
            title={isVideoOff ? 'Turn on camera' : 'Turn off camera'}
            style={{
              width: '46px',
              height: '46px',
              borderRadius: '50%',
              border: 'none',
              background: isVideoOff ? '#ef4444' : 'rgba(255, 255, 255, 0.12)',
              color: '#fff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
              transition: 'background 0.15s ease',
            }}
          >
            {isVideoOff ? <VideoOff size={20} /> : <Video size={20} />}
          </button>

          {/* Screen Share Button */}
          <button
            type="button"
            onClick={toggleScreenShare}
            title={isScreenSharing ? 'Stop sharing screen' : 'Share screen'}
            style={{
              width: '46px',
              height: '46px',
              borderRadius: '50%',
              border: 'none',
              background: isScreenSharing ? '#3b82f6' : 'rgba(255, 255, 255, 0.12)',
              color: '#fff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
              transition: 'background 0.15s ease',
            }}
          >
            {isScreenSharing ? <MonitorOff size={20} /> : <Monitor size={20} />}
          </button>

          {/* Hand Raise Button */}
          <button
            type="button"
            onClick={toggleHandRaise}
            title={isHandRaised ? 'Lower hand' : 'Raise hand'}
            style={{
              width: '46px',
              height: '46px',
              borderRadius: '50%',
              border: 'none',
              background: isHandRaised ? '#eab308' : 'rgba(255, 255, 255, 0.12)',
              color: isHandRaised ? '#000' : '#fff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
              transition: 'background 0.15s ease',
            }}
          >
            <Hand size={20} />
          </button>

          {/* Leave Call Button */}
          <button
            type="button"
            onClick={onLeave}
            title="Leave call"
            style={{
              height: '46px',
              padding: '0 22px',
              borderRadius: '23px',
              border: 'none',
              background: '#ef4444',
              color: '#fff',
              fontWeight: 600,
              fontSize: '0.88rem',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              cursor: 'pointer',
              marginLeft: '8px',
            }}
          >
            Leave call
          </button>

          {/* End Call for All (Host only) */}
          {isHost && onEndMeeting && (
            <button
              type="button"
              onClick={onEndMeeting}
              title="End call for everyone"
              style={{
                height: '46px',
                padding: '0 16px',
                borderRadius: '23px',
                border: '1px solid rgba(239, 68, 68, 0.4)',
                background: 'rgba(239, 68, 68, 0.1)',
                color: '#f87171',
                fontWeight: 600,
                fontSize: '0.84rem',
                cursor: 'pointer',
              }}
            >
              End for all
            </button>
          )}
        </div>

        {/* Right: Drawer Toggles */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          {/* Meeting Info */}
          <button
            type="button"
            onClick={() => setActiveDrawer(activeDrawer === 'info' ? 'none' : 'info')}
            title="Meeting details"
            style={{
              width: '40px',
              height: '40px',
              borderRadius: '50%',
              border: 'none',
              background: activeDrawer === 'info' ? '#4f46e5' : 'transparent',
              color: activeDrawer === 'info' ? '#fff' : '#94a3b8',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
            }}
          >
            ℹ️
          </button>

          {/* People */}
          <button
            type="button"
            onClick={() => setActiveDrawer(activeDrawer === 'participants' ? 'none' : 'participants')}
            title="Show participants"
            style={{
              height: '40px',
              padding: '0 12px',
              borderRadius: '20px',
              border: 'none',
              background: activeDrawer === 'participants' ? '#4f46e5' : 'rgba(255, 255, 255, 0.08)',
              color: activeDrawer === 'participants' ? '#fff' : '#cbd5e1',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              fontSize: '0.82rem',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            <Users size={16} />
            <span>{totalInCall}</span>
          </button>

          {/* Chat */}
          <button
            type="button"
            onClick={() => setActiveDrawer(activeDrawer === 'chat' ? 'none' : 'chat')}
            title="In-call chat"
            style={{
              height: '40px',
              padding: '0 12px',
              borderRadius: '20px',
              border: 'none',
              background: activeDrawer === 'chat' ? '#4f46e5' : 'rgba(255, 255, 255, 0.08)',
              color: activeDrawer === 'chat' ? '#fff' : '#cbd5e1',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              fontSize: '0.82rem',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            <span>💬 Chat</span>
          </button>
        </div>
      </footer>
    </div>
  )
}

/**
 * Single remote video tile with automatic track playback
 */
function RemoteVideoTile({
  peer,
  isPinned,
  onTogglePin,
}: {
  peer: RemotePeer
  isPinned: boolean
  onTogglePin: () => void
}) {
  const videoRef = useRef<HTMLVideoElement | null>(null)

  useEffect(() => {
    if (videoRef.current && peer.stream) {
      videoRef.current.srcObject = peer.stream
    }
  }, [peer.stream])

  return (
    <div
      style={{
        position: 'relative',
        background: '#1e222d',
        borderRadius: '12px',
        overflow: 'hidden',
        aspectRatio: '16/9',
        border: peer.isSpeaking ? '2px solid #22c55e' : '1px solid rgba(255, 255, 255, 0.1)',
        boxShadow: peer.isSpeaking ? '0 0 16px rgba(34, 197, 94, 0.4)' : 'none',
        transition: 'border 0.2s ease, box-shadow 0.2s ease',
      }}
    >
      <video
        ref={videoRef}
        autoPlay
        playsInline
        style={{
          width: '100%',
          height: '100%',
          objectFit: 'cover',
          display: peer.isVideoOff ? 'none' : 'block',
        }}
      />

      {/* Fallback avatar when camera is off */}
      {peer.isVideoOff && (
        <div
          style={{
            width: '100%',
            height: '100%',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'center',
            alignItems: 'center',
            background: '#1e222d',
          }}
        >
          <div
            style={{
              width: '84px',
              height: '84px',
              borderRadius: '50%',
              background: '#0284c7',
              color: '#fff',
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
              fontSize: '1.8rem',
              fontWeight: 700,
              border: peer.isSpeaking ? '3px solid #22c55e' : '2px solid rgba(255, 255, 255, 0.2)',
            }}
          >
            {initials(peer.displayName)}
          </div>
        </div>
      )}

      {/* Name Pill */}
      <div
        style={{
          position: 'absolute',
          bottom: '10px',
          left: '10px',
          background: 'rgba(0, 0, 0, 0.65)',
          backdropFilter: 'blur(4px)',
          padding: '4px 10px',
          borderRadius: '6px',
          display: 'flex',
          alignItems: 'center',
          gap: '6px',
          fontSize: '0.8rem',
          fontWeight: 600,
          color: '#fff',
        }}
      >
        {peer.isMuted ? <MicOff size={13} color="#ef4444" /> : <Mic size={13} color="#22c55e" />}
        <span>{peer.displayName}</span>
      </div>

      {/* Hand Raised Icon */}
      {peer.isHandRaised && (
        <div
          style={{
            position: 'absolute',
            top: '10px',
            right: '10px',
            background: '#eab308',
            color: '#000',
            borderRadius: '50%',
            padding: '6px',
            display: 'flex',
            boxShadow: '0 2px 8px rgba(0,0,0,0.5)',
          }}
        >
          <Hand size={18} />
        </div>
      )}

      {/* Pin toggle button */}
      <button
        type="button"
        onClick={onTogglePin}
        title={isPinned ? 'Unpin video' : 'Pin to spotlight'}
        style={{
          position: 'absolute',
          top: '10px',
          left: '10px',
          background: 'rgba(0, 0, 0, 0.5)',
          border: 'none',
          color: isPinned ? '#38bdf8' : '#cbd5e1',
          borderRadius: '50%',
          width: '28px',
          height: '28px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          cursor: 'pointer',
        }}
      >
        {isPinned ? <PinOff size={14} /> : <Pin size={14} />}
      </button>
    </div>
  )
}
