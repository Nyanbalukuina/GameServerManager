import { useEffect, useState } from 'react'
import { deleteAsaServer, getAsaServer, getAsaServerStatus, restartAsaServer, startAsaServer, stopAsaServer } from '../../api/asaServer'
import { AppLink } from '../../components/common/AppLink'
import type { AsaServerStatus } from '../../types/asaServer'
import type { GameServerRegistration } from '../../types/gameServer'
import '../../styles/serverConstruction.css'

export function AsaManagementPage() {
  const [server, setServer] = useState<GameServerRegistration | null>(null)
  const [status, setStatus] = useState<AsaServerStatus | null>(null)
  const [adminPassword, setAdminPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    Promise.all([getAsaServer(), getAsaServerStatus()])
      .then(([registration, current]) => { setServer(registration); setStatus(current) })
      .catch((reason: unknown) => setError(reason instanceof Error ? reason.message : 'ARK: Survival Ascendedサーバーを取得できませんでした'))
  }, [])

  const operate = async (action: 'START' | 'STOP' | 'RESTART') => {
    setBusy(true); setError(null)
    try {
      const current = action === 'START' ? await startAsaServer() : action === 'STOP' ? await stopAsaServer(adminPassword) : await restartAsaServer(adminPassword)
      setStatus(current)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'ARK: Survival Ascendedサーバーを操作できませんでした')
    } finally { setBusy(false) }
  }

  const running = status?.state === 'RUNNING'
  const remove = async () => {
    setBusy(true); setError(null)
    try {
      await deleteAsaServer(confirmation)
      window.history.pushState({}, '', '/servers/new'); window.dispatchEvent(new PopStateEvent('popstate'))
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'ARK: Survival Ascendedサーバーを削除できませんでした'); setBusy(false)
    }
  }
  return <main>
    <AppLink className="back-link" href="/servers/new">ゲーム選択へ戻る</AppLink>
    <header><p className="eyebrow">ARK: Survival Ascended</p><h1>ARK: Survival Ascendedサーバー管理</h1><p>現在状態を確認し、安全な停止・再起動を行います。</p></header>
    {error && <p className="error request-error" role="alert">{error}</p>}
    {!server && !error && <p>読み込み中...</p>}
    {server && status && <section className="card">
      <h2>{server.serverName}</h2>
      <dl><dt>種類</dt><dd>{server.mode === 'DEMO' ? 'デモ' : '実サーバー'}</dd><dt>状態</dt><dd>{stateLabel(status.state)}</dd><dt>マップ</dt><dd>{server.map}</dd><dt>ゲームポート</dt><dd>{server.gamePort} / Peer {server.peerPort}</dd><dt>Queryポート</dt><dd>{server.queryPort}</dd><dt>RCONポート</dt><dd>{server.rconPort}</dd><dt>ログ</dt><dd>{status.logPath ?? '未作成'}</dd></dl>
      {server.mode === 'REAL' && <label className="field">管理者パスワード（停止・再起動時に使用）<input value={adminPassword} onChange={(event) => setAdminPassword(event.target.value)} /></label>}
      <div className="button-row"><button type="button" disabled={busy || running} onClick={() => void operate('START')}>起動</button><button type="button" disabled={busy || !running || (server.mode === 'REAL' && adminPassword.length < 8)} onClick={() => void operate('STOP')}>保存して停止</button><button type="button" disabled={busy || !running || (server.mode === 'REAL' && adminPassword.length < 8)} onClick={() => void operate('RESTART')}>保存して再起動</button></div>
      <p className="notice">{server.mode === 'DEMO' ? 'デモ操作のため、実際のARK: Survival AscendedプロセスやRCONは操作しません。' : '停止と再起動では、RCONでSaveWorldを実行してからARK: Survival Ascendedを終了します。'}</p>
    </section>}
    {server && status && <section className="card danger-zone"><h2>ARK: Survival Ascendedサーバーを削除</h2><p>{server.mode === 'DEMO' ? 'ARK: Survival Ascendedのデモデータと管理登録を削除します。' : 'ARK: Survival Ascended本体、設定、Firewall規則、管理登録を削除します。GSM管理ルートのバックアップは残します。'}</p><label className="field">確認のためASAと入力してください<input value={confirmation} onChange={(event) => setConfirmation(event.target.value)} /></label><button type="button" disabled={busy || status.state === 'RUNNING' || confirmation !== 'ASA'} onClick={() => void remove()}>ARK: Survival Ascendedサーバーを削除</button>{status.state === 'RUNNING' && <p className="notice">削除する前にサーバーを停止してください。</p>}</section>}
  </main>
}

function stateLabel(state: AsaServerStatus['state']): string {
  return { NOT_STARTED: '未起動', STARTING: '起動中', RUNNING: '実行中', STOPPED: '停止中', FAILED: '異常終了' }[state]
}
