import { AuthStorage } from './storage'
import type { User } from './types'

const issuer = (import.meta.env.VITE_KEYCLOAK_ISSUER ?? 'http://localhost:8180/realms/digital-office').replace(/\/$/, '')
const clientId = import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'digital-office-frontend'
const redirectUri = `${window.location.origin}/sign-in`
const verifierKey = 'digital-office.pkce-verifier'
const stateKey = 'digital-office.oidc-state'

interface TokenResponse {
  access_token: string
  refresh_token?: string
  expires_in: number
}

interface JwtClaims {
  sub: string
  email?: string
  given_name?: string
  family_name?: string
  name?: string
  email_verified?: boolean
}

function encode(value: Uint8Array): string {
  let binary = ''
  value.forEach((byte) => {
    binary += String.fromCharCode(byte)
  })
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

function decodeClaims(token: string): JwtClaims {
  const payload = token.split('.')[1]
  if (!payload) throw new Error('Keycloak returned an invalid access token.')
  return JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/'))) as JwtClaims
}

async function createChallenge(verifier: string): Promise<string> {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier))
  return encode(new Uint8Array(digest))
}

export async function beginLogin(register = false): Promise<void> {
  const verifier = encode(crypto.getRandomValues(new Uint8Array(32)))
  const state = encode(crypto.getRandomValues(new Uint8Array(16)))
  sessionStorage.setItem(verifierKey, verifier)
  sessionStorage.setItem(stateKey, state)

  const params = new URLSearchParams({
    client_id: clientId,
    redirect_uri: redirectUri,
    response_type: 'code',
    scope: 'openid profile email',
    code_challenge: await createChallenge(verifier),
    code_challenge_method: 'S256',
    state,
  })
  if (register) params.set('kc_action', 'register')
  window.location.assign(`${issuer}/protocol/openid-connect/auth?${params}`)
}

export async function completeLogin(): Promise<User | null> {
  const params = new URLSearchParams(window.location.search)
  const code = params.get('code')
  if (!code) return null
  const expectedState = sessionStorage.getItem(stateKey)
  if (!expectedState || params.get('state') !== expectedState) {
    throw new Error('Keycloak login state validation failed.')
  }
  const verifier = sessionStorage.getItem(verifierKey)
  if (!verifier) throw new Error('Keycloak login session expired. Please try again.')

  const response = await fetch(`${issuer}/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'authorization_code',
      client_id: clientId,
      redirect_uri: redirectUri,
      code,
      code_verifier: verifier,
    }),
  })
  if (!response.ok) throw new Error('Keycloak could not complete the login.')

  const tokens = (await response.json()) as TokenResponse
  const claims = decodeClaims(tokens.access_token)
  const now = new Date().toISOString()
  const user: User = {
    id: claims.sub,
    email: claims.email ?? '',
    firstName: claims.given_name ?? null,
    lastName: claims.family_name ?? null,
    displayName: claims.name ?? claims.email ?? null,
    profilePictureUrl: null,
    timeZone: 'UTC',
    language: 'en',
    phoneNumber: null,
    emailVerified: claims.email_verified ?? false,
    active: true,
    createdAt: now,
    updatedAt: now,
  }

  AuthStorage.setAccessToken(tokens.access_token, true)
  if (tokens.refresh_token) AuthStorage.setRefreshToken(tokens.refresh_token)
  AuthStorage.setUser(JSON.stringify(user), true)
  sessionStorage.removeItem(verifierKey)
  sessionStorage.removeItem(stateKey)
  window.history.replaceState({}, document.title, window.location.pathname)
  return user
}

export async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = AuthStorage.getRefreshToken()
  if (!refreshToken) return null

  try {
    const response = await fetch(`${issuer}/protocol/openid-connect/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        grant_type: 'refresh_token',
        client_id: clientId,
        refresh_token: refreshToken,
      }),
    })

    if (!response.ok) {
      AuthStorage.clear()
      return null
    }

    const tokens = (await response.json()) as TokenResponse
    AuthStorage.setAccessToken(tokens.access_token, true)
    if (tokens.refresh_token) {
      AuthStorage.setRefreshToken(tokens.refresh_token)
    }
    return tokens.access_token
  } catch {
    return null
  }
}
