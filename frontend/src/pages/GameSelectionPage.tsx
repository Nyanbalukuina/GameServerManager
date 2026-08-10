import { AppLink } from '../components/AppLink'
import '../styles/serverConstruction.css'

export function GameSelectionPage() {
  return (
    <main>
      <header>
        <p className="eyebrow">Game Server Manager</p>
        <h1>新規ゲームサーバー構築</h1>
        <p>構築するゲームを選択してください。</p>
      </header>

      <section className="card game-list">
        <AppLink className="game-card" href="/servers/new/palworld">
          <strong>Palworld</strong>
          <span>新しいPalworld専用サーバーを構築します</span>
        </AppLink>
        <div className="game-card unavailable" aria-disabled="true">
          <strong>ARK</strong>
          <span>今後対応予定です</span>
        </div>
      </section>
    </main>
  )
}
