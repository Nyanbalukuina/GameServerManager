import { useEffect, useState } from 'react'
import {
  deleteDemoPalworldServer,
  getPalworldServer,
  operateDemoPalworldServer,
} from '../../api/gameServers'
import { AppLink } from '../../components/common/AppLink'
import type { GameServerRegistration } from '../../types/gameServer'
import '../../styles/serverConstruction.css'

export function PalworldManagementPage() {
  const [server, setServer] = useState<GameServerRegistration | null>(null)
  const [confirmation, setConfirmation] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    getPalworldServer().then(setServer).catch((reason: unknown) => {
      setError(reason instanceof Error ? reason.message : 'Palworldサーバーを取得できませんでした')
    })
  }, [])

  const operate = async (action: 'START' | 'STOP' | 'RESTART') => {
    setBusy(true)
    setError(null)
    try {
      setServer(await operateDemoPalworldServer(action))
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'デモサーバーを操作できませんでした')
    } finally {
      setBusy(false)
    }
  }

  const remove = async () => {
    setBusy(true)
    setError(null)
    try {
      await deleteDemoPalworldServer(confirmation)
      window.history.pushState({}, '', '/servers/new')
      window.dispatchEvent(new PopStateEvent('popstate'))
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'デモサーバーを削除できませんでした')
      setBusy(false)
    }
  }

  return (
    <main>
      <AppLink className="back-link" href="/servers/new">ゲーム選択へ戻る</AppLink>
      <header>
        <p className="eyebrow">Game Server Manager</p>
        <h1>Palworldサーバー管理</h1>
        <p>登録されたPalworldサーバーの状態を確認します。</p>
      </header>
      {error && <p className="error request-error" role="alert">{error}</p>}
      {!server && !error && <p>読み込み中...</p>}
      {server && (
        <>
          <section className="card">
            <h2>{server.serverName}</h2>
            <dl>
              <dt>種類</dt><dd>{server.mode === 'DEMO' ? 'デモ' : '実サーバー'}</dd>
              <dt>状態</dt><dd>{server.state === 'RUNNING' ? '起動中' : '停止中'}</dd>
              <dt>ゲームポート</dt><dd>{server.gamePort}</dd>
              <dt>RCONポート</dt><dd>{server.rconPort}</dd>
              <dt>デモデータ</dt><dd>{server.workspacePath}</dd>
            </dl>
            {server.mode === 'DEMO' ? (
              <>
                <div className="button-row">
                  <button type="button" disabled={busy || server.state === 'RUNNING'} onClick={() => void operate('START')}>起動</button>
                  <button type="button" disabled={busy || server.state === 'STOPPED'} onClick={() => void operate('STOP')}>停止</button>
                  <button type="button" disabled={busy || server.state === 'STOPPED'} onClick={() => void operate('RESTART')}>再起動</button>
                </div>
                <p className="notice">デモ操作のため、実際のゲームプロセスは起動しません。</p>
              </>
            ) : (
              <>
                <p><strong>接続先:</strong> サーバーPCのIPアドレス:{server.gamePort}</p>
                <p className="notice">実サーバーの起動・停止操作は次の実装でこの画面へ接続します。</p>
              </>
            )}
          </section>
          {server.mode === 'DEMO' && (
            <section className="card danger-zone">
              <h2>デモサーバーを削除</h2>
              <p>一時領域のデモデータとPalworldの登録を削除します。</p>
              <label htmlFor="deleteConfirmation">確認のためPALWORLDと入力してください</label>
              <input id="deleteConfirmation" value={confirmation} onChange={(event) => setConfirmation(event.target.value)} />
              <button type="button" disabled={busy || confirmation !== 'PALWORLD'} onClick={() => void remove()}>デモサーバーを削除</button>
            </section>
          )}
        </>
      )}
    </main>
  )
}
