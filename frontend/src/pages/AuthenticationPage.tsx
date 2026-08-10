import { useState, type FormEvent } from 'react'
import { login, setupAdministrator } from '../api/authentication'
import type { AuthenticationStatus } from '../types/authentication'

type AuthenticationPageProps = {
  status: AuthenticationStatus
  onAuthenticated: () => Promise<void>
}

export function AuthenticationPage({ status, onAuthenticated }: AuthenticationPageProps) {
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    const message = status.configured
      ? await login(password)
      : await setupAdministrator(password, confirmation)
    if (message === null) {
      setPassword('')
      setConfirmation('')
      await onAuthenticated()
    } else {
      setError(message)
    }
    setSubmitting(false)
  }

  if (!status.configured && !status.setupAllowed) {
    return (
      <main className="auth-page">
        <section className="card auth-card">
          <h1>初回設定が必要です</h1>
          <p>サーバーPC上で http://127.0.0.1:8080 を開いて管理者を設定してください。</p>
        </section>
      </main>
    )
  }

  const setup = !status.configured
  return (
    <main className="auth-page">
      <form className="card auth-card" onSubmit={submit}>
        <p className="eyebrow">Game Server Manager</p>
        <h1>{setup ? '初回管理者設定' : 'ログイン'}</h1>
        <label htmlFor="loginPassword">パスワード</label>
        <input
          id="loginPassword"
          type="password"
          autoComplete={setup ? 'new-password' : 'current-password'}
          minLength={setup ? 12 : undefined}
          maxLength={128}
          required
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />
        {setup && (
          <>
            <label htmlFor="passwordConfirmation">確認用パスワード</label>
            <input
              id="passwordConfirmation"
              type="password"
              autoComplete="new-password"
              minLength={12}
              maxLength={128}
              required
              value={confirmation}
              onChange={(event) => setConfirmation(event.target.value)}
            />
          </>
        )}
        {error && <p className="error-message" role="alert">{error}</p>}
        <button type="submit" disabled={submitting} aria-busy={submitting}>
          {submitting ? '処理中...' : setup ? '管理者を設定' : 'ログイン'}
        </button>
      </form>
    </main>
  )
}
