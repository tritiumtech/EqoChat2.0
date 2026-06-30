function trimTrailingSlash(value?: string): string {
  const normalized = String(value || '').trim()
  return normalized.endsWith('/') ? normalized.slice(0, -1) : normalized
}

function browserWebSocketBaseUrl(): string {
  const locationLike = typeof globalThis !== 'undefined'
    ? (globalThis as any).location
    : undefined
  if (!locationLike?.host) {
    return ''
  }
  const protocol = locationLike.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${locationLike.host}`
}

export function resolveApiBaseUrl(): string {
  const explicit = trimTrailingSlash(import.meta.env.VITE_API_BASE_URL)
  if (explicit) {
    return explicit
  }
  return import.meta.env.DEV ? 'http://localhost:8080' : ''
}

export function resolveWebSocketBaseUrl(): string {
  const explicit = trimTrailingSlash(import.meta.env.VITE_WS_URL)
  if (explicit) {
    return explicit
  }
  if (import.meta.env.DEV) {
    return 'ws://localhost:8080'
  }
  return browserWebSocketBaseUrl()
}

export function buildApiUrl(path: string): string {
  const baseUrl = resolveApiBaseUrl()
  return `${baseUrl}${path}`
}

export const API_BASE_URL = resolveApiBaseUrl()
export const WS_BASE_URL = resolveWebSocketBaseUrl()
