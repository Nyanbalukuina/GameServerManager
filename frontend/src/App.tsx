import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { GameSelectionPage } from './pages/GameSelectionPage'
import { ServerConstructionPage } from './pages/ServerConstructionPage'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/servers/new" replace />} />
        <Route path="/servers/new" element={<GameSelectionPage />} />
        <Route path="/servers/new/palworld" element={<ServerConstructionPage />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
