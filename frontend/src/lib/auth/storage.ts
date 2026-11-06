const ACCESS_TOKEN_KEY = 'digital-office.access-token'
const REFRESH_TOKEN_KEY = 'digital-office.refresh-token'
const USER_KEY = 'digital-office.user'

export const AuthStorage = {
  getAccessToken(): string | null {
    return sessionStorage.getItem(ACCESS_TOKEN_KEY) ?? localStorage.getItem(ACCESS_TOKEN_KEY)
  },

  setAccessToken(token: string, remember = false): void {
    const storage = remember ? localStorage : sessionStorage
    storage.setItem(ACCESS_TOKEN_KEY, token)
  },

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY)
  },

  setRefreshToken(token: string): void {
    localStorage.setItem(REFRESH_TOKEN_KEY, token)
  },

  getUser(): string | null {
    return sessionStorage.getItem(USER_KEY) ?? localStorage.getItem(USER_KEY)
  },

  setUser(user: string, remember = false): void {
    const storage = remember ? localStorage : sessionStorage
    storage.setItem(USER_KEY, user)
  },

  clear(): void {
    sessionStorage.removeItem(ACCESS_TOKEN_KEY)
    sessionStorage.removeItem(USER_KEY)
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  },
}
