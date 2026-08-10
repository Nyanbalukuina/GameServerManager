import { useEffect, useState } from 'react'
import { GameSelectionPage } from './pages/GameSelectionPage'
import { PalworldConstructionPage } from './pages/palworld/PalworldConstructionPage'
import { PalworldManagementPage } from './pages/palworld/PalworldManagementPage'
import { AuthenticationPage } from './pages/AuthenticationPage'
import { getAuthenticationStatus, logout } from './api/authentication'
import type { AuthenticationStatus } from './types/authentication'

function App() {
  const [path, setPath] = useState(window.location.pathname)
  const [authentication, setAuthentication] = useState<AuthenticationStatus | null>(null)
  const [authenticationError, setAuthenticationError] = useState<string | null>(null)

  const refreshAuthentication = async () => {
    try {
      setAuthentication(await getAuthenticationStatus())
      setAuthenticationError(null)
    } catch (error) {
      setAuthenticationError(error instanceof Error ? error.message : '認証状態を確認できませんでした')
    }
  }

  useEffect(() => {
    void refreshAuthentication()
    const updatePath = () => setPath(window.location.pathname)
    const authenticationRequired = () => void refreshAuthentication()
    window.addEventListener('popstate', updatePath)
    window.addEventListener('game-server-manager:authentication-required', authenticationRequired)

    if (window.location.pathname === '/') {
      window.history.replaceState({}, '', '/servers/new')
      updatePath()
    }

    return () => {
      window.removeEventListener('popstate', updatePath)
      window.removeEventListener('game-server-manager:authentication-required', authenticationRequired)
    }
  }, [])

  if (authenticationError) {
    return <main><p role="alert">{authenticationError}</p></main>
  }
  if (authentication === null) {
    return <main><p>読み込み中...</p></main>
  }
  if (!authentication.configured || !authentication.authenticated) {
    return <AuthenticationPage status={authentication} onAuthenticated={refreshAuthentication} />
  }

  const page = (() => {
    switch (path) {
    case '/':
    case '/servers/new':
      return <GameSelectionPage />
    case '/servers/new/palworld':
      return <PalworldConstructionPage />
    case '/servers/palworld':
      return <PalworldManagementPage />
    default:
      return <GameSelectionPage />
    }
  })()

  const signOut = async () => {
    await logout()
    await refreshAuthentication()
  }

  return (
    <>
      <button className="logout-button" type="button" onClick={() => void signOut()}>
        ログアウト
      </button>
      {page}
    </>
  )
}

export default App
