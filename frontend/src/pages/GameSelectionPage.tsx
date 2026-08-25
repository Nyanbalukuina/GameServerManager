import { useEffect, useState } from 'react'
import { getGameServers } from '../api/gameServers'
import { AppLink } from '../components/common/AppLink'
import type { GameServerRegistration } from '../types/gameServer'
import '../styles/serverConstruction.css'

export function GameSelectionPage() {
  const [servers, setServers] = useState<GameServerRegistration[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    getGameServers().then(setServers).catch((reason: unknown) => {
      setError(reason instanceof Error ? reason.message : 'サーバー登録を取得できませんでした')
    })
  }, [])

  const palworld = servers?.find((server) => server.game === 'PALWORLD')
  const asa = servers?.find((server) => server.game === 'ASA')

  return (
    <main className="compact-page">
      <header>
        <p className="eyebrow">Game Server Manager</p>
        <h1>新規ゲームサーバー構築</h1>
        <p>構築するゲームを選択してください。</p>
      </header>

      <section className="card game-list">
        {error && <p className="error" role="alert">{error}</p>}
        {servers === null && !error && <p>読み込み中...</p>}
        {servers !== null && (palworld ? (
          <AppLink className="game-card" href="/servers/palworld">
            <strong>Palworld</strong>
            <span>作成済み・管理画面を開く</span>
          </AppLink>
        ) : (
          <AppLink className="game-card" href="/servers/new/palworld">
            <strong>Palworld</strong>
            <span>未作成・新しいPalworld専用サーバーを構築します</span>
          </AppLink>
        ))}
        {servers !== null && (asa ? (
          <AppLink className="game-card" href="/servers/asa"><strong>ARK: Survival Ascended</strong><span>作成済み・管理画面を開く</span></AppLink>
        ) : (
          <AppLink className="game-card" href="/servers/new/asa"><strong>ARK: Survival Ascended</strong><span>未作成・新しいARK: Survival Ascended専用サーバーを構築します</span></AppLink>
        ))}
        <div className="game-card unavailable" aria-disabled="true">
          <strong>Minecraft</strong>
          <span>未作成・今後対応予定です</span>
        </div>
      </section>
    </main>
  )
}
