import { apiFetch, initializeCsrf } from './http'
import type { AuthenticationStatus } from '../types/authentication'

export async function getAuthenticationStatus(): Promise<AuthenticationStatus> {
  const response = await fetch('/api/auth/status', { credentials: 'same-origin' })
  if (!response.ok) {
    throw new Error('認証状態を確認できませんでした')
  }
  return response.json() as Promise<AuthenticationStatus>
}

export async function setupAdministrator(
  password: string,
  passwordConfirmation: string,
): Promise<string | null> {
  await initializeCsrf()
  const response = await apiFetch('/api/auth/setup', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ password, passwordConfirmation }),
  })
  if (response.ok) {
    return null
  }
  return readError(response, '管理者を設定できませんでした')
}

export async function login(password: string): Promise<string | null> {
  await initializeCsrf()
  const body = new URLSearchParams({ username: 'admin', password })
  const response = await apiFetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
  })
  if (response.ok) {
    await initializeCsrf()
    return null
  }
  return response.status === 401
    ? 'ユーザー名またはパスワードが正しくありません'
    : 'ログインできませんでした'
}

export async function logout(): Promise<void> {
  await apiFetch('/api/auth/logout', { method: 'POST' })
}

async function readError(response: Response, fallback: string): Promise<string> {
  const body = await response.json().catch(() => null) as { errors?: Record<string, string> } | null
  return body?.errors?.request ?? body?.errors?.password ?? fallback
}
