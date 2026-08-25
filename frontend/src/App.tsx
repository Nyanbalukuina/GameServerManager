import { useEffect, useState } from 'react'
import { GameSelectionPage } from './pages/GameSelectionPage'
import { PalworldConstructionPage } from './pages/palworld/PalworldConstructionPage'
import { PalworldManagementPage } from './pages/palworld/PalworldManagementPage'
import { AsaConstructionPage } from './pages/asa/AsaConstructionPage'
import { AsaManagementPage } from './pages/asa/AsaManagementPage'
import { getFeatureConfiguration } from './api/features'
import type { FeatureConfiguration } from './types/features'

function App() {
  const [path, setPath] = useState(window.location.pathname)
  const [features, setFeatures] = useState<FeatureConfiguration | null>(null)
  const [configurationError, setConfigurationError] = useState<string | null>(null)

  useEffect(() => {
    getFeatureConfiguration()
      .then(setFeatures)
      .catch((error: unknown) => setConfigurationError(error instanceof Error ? error.message : 'アプリ設定を取得できませんでした'))
    const updatePath = () => setPath(window.location.pathname)
    window.addEventListener('popstate', updatePath)

    if (window.location.pathname === '/') {
      window.history.replaceState({}, '', '/servers/new')
      updatePath()
    }

    return () => {
      window.removeEventListener('popstate', updatePath)
    }
  }, [])

  if (configurationError) {
    return <main><p role="alert">{configurationError}</p></main>
  }
  if (features === null) {
    return <main><p>読み込み中...</p></main>
  }

  const page = (() => {
    switch (path) {
    case '/':
    case '/servers/new':
      return <GameSelectionPage />
    case '/servers/new/palworld':
      return <PalworldConstructionPage demoEnabled={features.demoEnabled} />
    case '/servers/new/asa':
      return <AsaConstructionPage demoEnabled={features.demoEnabled} />
    case '/servers/asa':
      return <AsaManagementPage />
    case '/servers/palworld':
      return <PalworldManagementPage />
    default:
      return <GameSelectionPage />
    }
  })()

  return page
}

export default App
