export const TOKEN_KEY = 'merchant_token'
export const USER_KEY = 'merchant_user'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY)
}

export function isDemoMode() {
  return getToken() === 'demo-token'
}

export function setAuth(token: string, user: unknown) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(USER_KEY)
}

export function getStoredUser<T>() {
  const rawUser = localStorage.getItem(USER_KEY) || sessionStorage.getItem(USER_KEY)
  if (!rawUser) {
    return null
  }
  try {
    return JSON.parse(rawUser) as T
  } catch {
    return null
  }
}
