import type { CsrfToken } from '../types/security'

let csrfToken: CsrfToken | null = null

export async function initializeCsrf(): Promise<void> {
  const response = await fetch('/api/security/csrf', { credentials: 'same-origin' })
  if (!response.ok) {
    throw new Error('セキュリティトークンを取得できませんでした')
  }
  csrfToken = await response.json() as CsrfToken
}

export async function apiFetch(input: RequestInfo | URL, init: RequestInit = {}): Promise<Response> {
  const method = (init.method ?? 'GET').toUpperCase()
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method) && csrfToken === null) {
    await initializeCsrf()
  }

  const headers = new Headers(init.headers)
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method) && csrfToken !== null) {
    headers.set(csrfToken.headerName, csrfToken.token)
  }

  const response = await fetch(input, {
    ...init,
    headers,
    credentials: 'same-origin',
  })
  if (response.status === 403 && !['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    csrfToken = null
  }
  return response
}
