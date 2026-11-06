import { useEffect, useRef, useState } from 'react'
import type { JoinMeetingResponse } from '../types'

interface JitsiEmbedProps {
  connection: JoinMeetingResponse
  onConferenceLeft: () => void
  onError: (message: string) => void
}

interface JitsiInstance {
  dispose: () => void
  addEventListener: (event: string, listener: () => void) => void
}

interface JitsiOptions {
  roomName: string
  parentNode: HTMLElement
  width: string
  height: string
  userInfo: { displayName: string; email?: string }
  configOverwrite: Record<string, unknown>
}

interface JitsiConstructor {
  new (domain: string, options: JitsiOptions): JitsiInstance
}

declare global {
  interface Window {
    JitsiMeetExternalAPI?: JitsiConstructor
  }
}

const scriptLoads = new Map<string, Promise<JitsiConstructor>>()

function jitsiDomain(serverUrl: string): string {
  try {
    return new URL(serverUrl).host
  } catch {
    return serverUrl.replace(/^https?:\/\//, '').replace(/\/$/, '')
  }
}

function externalApiUrl(serverUrl: string): string {
  try {
    return `${new URL(serverUrl).origin}/external_api.js`
  } catch {
    return `https://${jitsiDomain(serverUrl)}/external_api.js`
  }
}

function loadJitsiApi(serverUrl: string): Promise<JitsiConstructor> {
  const domain = jitsiDomain(serverUrl)
  const existing = scriptLoads.get(domain)
  if (existing) return existing

  const promise = new Promise<JitsiConstructor>((resolve, reject) => {
    if (window.JitsiMeetExternalAPI) {
      resolve(window.JitsiMeetExternalAPI)
      return
    }

    const script = document.createElement('script')
    script.src = externalApiUrl(serverUrl)
    script.async = true
    script.onload = () => {
      if (window.JitsiMeetExternalAPI) resolve(window.JitsiMeetExternalAPI)
      else reject(new Error('The Jitsi external API was loaded, but did not initialise.'))
    }
    script.onerror = () => reject(new Error('Unable to load the Jitsi meeting client.'))
    document.head.appendChild(script)
  })
  scriptLoads.set(domain, promise)
  return promise
}

export function JitsiEmbed({ connection, onConferenceLeft, onError }: JitsiEmbedProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let disposed = false
    let instance: JitsiInstance | undefined
    void loadJitsiApi(connection.jitsiServerUrl)
      .then((ExternalApi) => {
        if (disposed || !containerRef.current) return
        instance = new ExternalApi(jitsiDomain(connection.jitsiServerUrl), {
          roomName: connection.roomCode,
          parentNode: containerRef.current,
          width: '100%',
          height: '100%',
          userInfo: {
            displayName: connection.displayName,
            email: connection.email || undefined,
          },
          configOverwrite: {
            prejoinPageEnabled: false,
            disableDeepLinking: true,
          },
        })
        instance.addEventListener('videoConferenceLeft', onConferenceLeft)
        setLoading(false)
      })
      .catch((error: unknown) => {
        if (!disposed) {
          onError(error instanceof Error ? error.message : 'Unable to start the video meeting.')
          setLoading(false)
        }
      })

    return () => {
      disposed = true
      instance?.dispose()
    }
  }, [connection, onConferenceLeft, onError])

  return (
    <div className="jitsi-container" ref={containerRef} aria-label="Video meeting">
      {loading && <div className="loading-state"><span className="spinner" aria-label="Loading video meeting" /></div>}
    </div>
  )
}
