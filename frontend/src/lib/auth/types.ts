export interface User {
  id: string
  email: string
  firstName: string | null
  lastName: string | null
  displayName: string | null
  profilePictureUrl: string | null
  timeZone: string | null
  language: string | null
  phoneNumber: string | null
  emailVerified: boolean
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface AuthSession {
  user: User
  accessToken: string
  refreshToken?: string
  expiresAt: number
}

export interface AuthState {
  user: User | null
  loading: boolean
  error: string | null
}
