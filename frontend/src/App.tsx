import { useEffect, useState } from 'react'
import { GameSelectionPage } from './pages/GameSelectionPage'
import { ServerConstructionPage } from './pages/ServerConstructionPage'

function App() {
  const [path, setPath] = useState(window.location.pathname)

  useEffect(() => {
    const updatePath = () => setPath(window.location.pathname)
    window.addEventListener('popstate', updatePath)

    if (window.location.pathname === '/') {
      window.history.replaceState({}, '', '/servers/new')
      updatePath()
    }

    return () => window.removeEventListener('popstate', updatePath)
  }, [])

  switch (path) {
    case '/':
    case '/servers/new':
      return <GameSelectionPage />
    case '/servers/new/palworld':
      return <ServerConstructionPage />
    default:
      return <GameSelectionPage />
  }
}

export default App
