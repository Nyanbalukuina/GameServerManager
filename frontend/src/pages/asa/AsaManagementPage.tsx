import { useEffect, useState } from 'react'
import { checkAsaServerVersion, deleteAsaServer, getAsaGameplaySettings, getAsaServer, getAsaServerStatus, getInstalledAsaServerVersion, restartAsaServer, startAsaServer, stopAsaServer, updateAsaGameplaySettings, updateAsaServer } from '../../api/asaServer'
import { AppLink } from '../../components/common/AppLink'
import type { AsaServerStatus } from '../../types/asaServer'
import type { GameServerRegistration } from '../../types/gameServer'
import type { AsaGameplaySettings } from '../../types/asaGameplaySettings'
import type { AsaServerVersion } from '../../types/asaServerVersion'
import type { InstalledSteamServerVersion } from '../../types/installedSteamServerVersion'
import '../../styles/serverConstruction.css'

export function AsaManagementPage() {
  const [server, setServer] = useState<GameServerRegistration | null>(null)
  const [status, setStatus] = useState<AsaServerStatus | null>(null)
  const [adminPassword, setAdminPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [settings, setSettings] = useState<AsaGameplaySettings | null>(null)
  const [settingsMessage, setSettingsMessage] = useState<string | null>(null)
  const [version, setVersion] = useState<AsaServerVersion | null>(null)
  const [versionAction, setVersionAction] = useState<'CHECK' | 'UPDATE' | null>(null)
  const [installedVersion, setInstalledVersion] = useState<InstalledSteamServerVersion | null>(null)
  const [versionError, setVersionError] = useState<string | null>(null)
  const [updateCheckError, setUpdateCheckError] = useState<string | null>(null)

  useEffect(() => {
    Promise.all([getAsaServer(), getAsaServerStatus(), getAsaGameplaySettings()])
      .then(([registration, current, gameplaySettings]) => {
        setServer(registration); setStatus(current); setSettings(gameplaySettings)
        if (registration.mode === 'REAL') {
          void getInstalledAsaServerVersion()
            .then(setInstalledVersion)
            .catch((reason: unknown) => setVersionError(reason instanceof Error ? reason.message : '現在のBuild IDを取得できませんでした'))
          void checkAsaServerVersion()
            .then((checked) => {
              setVersion(checked); setInstalledVersion({ appId: 2430930, buildId: checked.currentBuildId })
            })
            .catch(() => setUpdateCheckError('最新バージョンの配布状況を確認できませんでした'))
        }
      })
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
  const checkVersion = async () => {
    setBusy(true); setVersionAction('CHECK'); setError(null); setUpdateCheckError(null)
    try {
      const checked = await checkAsaServerVersion()
      setVersion(checked); setInstalledVersion({ appId: 2430930, buildId: checked.currentBuildId }); setVersionError(null); setUpdateCheckError(null)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '最新サーバーバージョンを取得できませんでした')
    } finally { setBusy(false); setVersionAction(null) }
  }
  const applyUpdate = async () => {
    setBusy(true); setVersionAction('UPDATE'); setError(null)
    try {
      const updated = await updateAsaServer()
      setVersion(updated); setInstalledVersion({ appId: 2430930, buildId: updated.currentBuildId }); setVersionError(null)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '最新サーバーバージョンを適用できませんでした')
    } finally { setBusy(false); setVersionAction(null) }
  }
  const saveSettings = async () => {
    if (!settings) return
    setBusy(true); setError(null); setSettingsMessage(null)
    try {
      const updated = await updateAsaGameplaySettings(settings)
      setSettings(updated)
      setServer((current) => current && ({ ...current, serverName: updated.serverName, maxPlayers: updated.maxPlayers }))
      setSettingsMessage('設定を保存しました。次回起動時から反映されます。')
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'ARK設定を保存できませんでした')
    } finally { setBusy(false) }
  }
  const remove = async () => {
    setBusy(true); setError(null)
    try {
      await deleteAsaServer(confirmation)
      window.history.pushState({}, '', '/servers/new'); window.dispatchEvent(new PopStateEvent('popstate'))
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'ARK: Survival Ascendedサーバーを削除できませんでした'); setBusy(false)
    }
  }
  return <main className="compact-page">
    <AppLink className="back-link" href="/servers/new">ゲーム選択へ戻る</AppLink>
    <header className="page-header"><p className="eyebrow">ARK: Survival Ascended</p><h1>サーバー管理</h1><p>現在状態を確認し、安全な停止・再起動を行います。</p></header>
    {error && <p className="error request-error" role="alert">{error}</p>}
    {!server && !error && <p>読み込み中...</p>}
    {server && status && <section className="card server-overview">
      <div className="server-overview-heading"><div><p className="section-number">SERVER</p><h2>{server.serverName}</h2></div><span className={`status-badge ${running ? 'running' : 'stopped'}`}>{stateLabel(status.state)}</span></div>
      <dl><dt>種類</dt><dd>{server.mode === 'DEMO' ? 'デモ' : '実サーバー'}</dd><dt>状態</dt><dd>{stateLabel(status.state)}</dd><dt>マップ</dt><dd>{server.map}</dd><dt>ゲームポート</dt><dd>{server.gamePort} / Peer {server.peerPort}</dd><dt>Queryポート</dt><dd>{server.queryPort}</dd><dt>RCONポート</dt><dd>{server.rconPort}</dd><dt>ログ</dt><dd>{status.logPath ?? '未作成'}</dd></dl>
      {server.mode === 'REAL' && <label className="field">管理者パスワード（停止・再起動時に使用）<input value={adminPassword} onChange={(event) => setAdminPassword(event.target.value)} /></label>}
      <div className="button-row management-actions"><button type="button" disabled={busy || running} onClick={() => void operate('START')}>起動</button><button type="button" disabled={busy || !running || (server.mode === 'REAL' && adminPassword.length < 8)} onClick={() => void operate('STOP')}>保存して停止</button><button type="button" disabled={busy || !running || (server.mode === 'REAL' && adminPassword.length < 8)} onClick={() => void operate('RESTART')}>保存して再起動</button></div>
      <p className="notice">{server.mode === 'DEMO' ? 'デモ操作のため、実際のARK: Survival AscendedプロセスやRCONは操作しません。' : '停止と再起動では、RCONでSaveWorldを実行してからARK: Survival Ascendedを終了します。'}</p>
    </section>}
    {server && status && <section className="card form-section">
      <div className="section-heading"><div><p className="section-number">UPDATE</p><h2>サーバーバージョン</h2></div><p>Steamで公開されているASAサーバーのBuild IDと比較します。</p></div>
      {version?.updateAvailable && <p className="notice" role="status">最新バージョンが配布されています。</p>}
      <dl><dt>Steam App ID</dt><dd>2430930</dd><dt>現在のBuild ID</dt><dd>{server.mode === 'DEMO' ? 'デモのため取得不可' : installedVersion?.buildId ?? (versionError ? '取得失敗' : '読み込み中...')}</dd><dt>最新のBuild ID</dt><dd>{server.mode === 'DEMO' ? 'デモのため取得不可' : version?.latestBuildId ?? '確認中...'}</dd>{version && <><dt>確認結果</dt><dd>{version.message}</dd></>}</dl>
      {versionError && <p className="error" role="alert">{versionError}</p>}
      {updateCheckError && <p className="notice">{updateCheckError}必要に応じて再確認してください。</p>}
      <div className="button-row management-actions">
        <button type="button" disabled={busy || server.mode === 'DEMO'} onClick={() => void checkVersion()}>{versionAction === 'CHECK' ? '更新状況を確認中...' : '更新状況を再確認'}</button>
        <button type="button" disabled={busy || server.mode === 'DEMO' || running || !version?.updateAvailable} onClick={() => void applyUpdate()}>{versionAction === 'UPDATE' ? '最新バージョンを適用中...' : '最新バージョンを適用'}</button>
      </div>
      {server.mode === 'DEMO' && <p className="notice">デモサーバーでは画面のみ確認できます。Build IDの取得と更新は実行しません。</p>}
      {server.mode === 'REAL' && running && <p className="notice">更新を適用するにはサーバーを停止してください。</p>}
      {server.mode === 'REAL' && !version && !updateCheckError && <p className="notice">最新バージョンの配布状況を確認しています。</p>}
    </section>}
    {server && status && settings && <section className="card form-section">
      <div className="section-heading"><div><p className="section-number">SETTINGS 00</p><h2>起動設定</h2></div><p>INIではなくARKサーバーの起動時に適用されます。</p></div>
      <label className="field" htmlFor="asaManagedMaxPlayers">最大プレイヤー数<input id="asaManagedMaxPlayers" type="number" min="1" max="70" value={settings.maxPlayers} onChange={(event) => setSettings({ ...settings, maxPlayers: Number(event.target.value) })} /></label>
    </section>}
    {server && status && settings && <section className="card form-section">
      <div className="section-heading"><div><p className="section-number">SETTINGS 01</p><h2>GameUserSettings.ini</h2></div><p>現在の基本設定をファイルから読み込んで表示しています。</p></div>
      <label className="field" htmlFor="asaManagedServerName">サーバー名<input id="asaManagedServerName" value={settings.serverName} onChange={(event) => setSettings({ ...settings, serverName: event.target.value })} /></label>
      <div className="form-grid">
        <label className="field" htmlFor="asaManagedGameMode">ゲームモード<select id="asaManagedGameMode" value={settings.pveEnabled ? 'PVE' : 'PVP'} onChange={(event) => setSettings({ ...settings, pveEnabled: event.target.value === 'PVE' })}><option value="PVE">PvE</option><option value="PVP">PvP</option></select></label>
        <label className="field" htmlFor="asaManagedXp">経験値倍率<input id="asaManagedXp" type="number" min="0.1" max="100" step="0.1" value={settings.xpMultiplier} onChange={(event) => setSettings({ ...settings, xpMultiplier: Number(event.target.value) })} /></label>
        <label className="field" htmlFor="asaManagedTaming">テイム速度<input id="asaManagedTaming" type="number" min="0.1" max="100" step="0.1" value={settings.tamingSpeedMultiplier} onChange={(event) => setSettings({ ...settings, tamingSpeedMultiplier: Number(event.target.value) })} /></label>
        <label className="field" htmlFor="asaManagedHarvest">採取量倍率<input id="asaManagedHarvest" type="number" min="0.1" max="100" step="0.1" value={settings.harvestAmountMultiplier} onChange={(event) => setSettings({ ...settings, harvestAmountMultiplier: Number(event.target.value) })} /></label>
      </div>
    </section>}
    {server && status && settings && <section className="card form-section">
      <div className="section-heading"><div><p className="section-number">SETTINGS 02</p><h2>Game.ini</h2></div><p>現在の孵化・成長設定をファイルから読み込んで表示しています。</p></div>
      <div className="form-grid">
        <label className="field" htmlFor="asaManagedEggHatch">孵化速度<input id="asaManagedEggHatch" type="number" min="0.1" max="100" step="0.1" value={settings.eggHatchSpeedMultiplier} onChange={(event) => setSettings({ ...settings, eggHatchSpeedMultiplier: Number(event.target.value) })} /></label>
        <label className="field" htmlFor="asaManagedBabyMature">赤ちゃんの成熟速度<input id="asaManagedBabyMature" type="number" min="0.1" max="100" step="0.1" value={settings.babyMatureSpeedMultiplier} onChange={(event) => setSettings({ ...settings, babyMatureSpeedMultiplier: Number(event.target.value) })} /></label>
      </div>
      <label className="automation-toggle" htmlFor="asaManagedUseSingleplayerSettings"><input id="asaManagedUseSingleplayerSettings" type="checkbox" checked={settings.useSingleplayerSettings} onChange={(event) => setSettings({ ...settings, useSingleplayerSettings: event.target.checked })} />少人数・シングル向け補正を有効にする</label>
      <p className="notice">有効にすると、表示中の倍率に加えてARKのシングルプレイヤー補正が適用されます。</p>
    </section>}
    {server && status && settings && <div className="card settings-save-panel">
      <button type="button" disabled={busy || running} onClick={() => void saveSettings()}>両方の設定を保存</button>
      {running && <p className="notice">設定を変更するにはサーバーを停止してください。</p>}
      {settingsMessage && <p className="success-message" role="status">{settingsMessage}</p>}
    </div>}
    {server && status && <section className="card danger-zone"><h2>ARK: Survival Ascendedサーバーを削除</h2><p>{server.mode === 'DEMO' ? 'ARK: Survival Ascendedのデモデータと管理登録を削除します。' : 'ARK: Survival Ascended本体、設定、Firewall規則、管理登録を削除します。GSM管理ルートのバックアップは残します。'}</p><label className="field">確認のためASAと入力してください<input value={confirmation} onChange={(event) => setConfirmation(event.target.value)} /></label><button type="button" disabled={busy || status.state === 'RUNNING' || confirmation !== 'ASA'} onClick={() => void remove()}>ARK: Survival Ascendedサーバーを削除</button>{status.state === 'RUNNING' && <p className="notice">削除する前にサーバーを停止してください。</p>}</section>}
  </main>
}

function stateLabel(state: AsaServerStatus['state']): string {
  return { NOT_STARTED: '未起動', STARTING: '起動中', RUNNING: '実行中', STOPPED: '停止中', FAILED: '異常終了' }[state]
}
