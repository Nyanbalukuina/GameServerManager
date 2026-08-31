import { useEffect, useState } from 'react'
import { checkPalworldServerVersion, deleteDemoPalworldServer, getDemoPalworldSettings, getInstalledPalworldServerVersion, getPalworldServer, getPalworldServerStatus, operateDemoPalworldServer, updateDemoPalworldSettings, updatePalworldServerVersion } from '../../api/gameServers'
import { AppLink } from '../../components/common/AppLink'
import type { GameServerRegistration, PalworldSettings, UpdatePalworldSettings } from '../../types/gameServer'
import type { PalworldServerVersion } from '../../types/palworldServerVersion'
import type { PalworldServerStatus } from '../../types/palworldServer'
import type { InstalledSteamServerVersion } from '../../types/installedSteamServerVersion'
import '../../styles/serverConstruction.css'

export function PalworldManagementPage() {
  const [server, setServer] = useState<GameServerRegistration | null>(null)
  const [settings, setSettings] = useState<PalworldSettings | null>(null)
  const [draft, setDraft] = useState<UpdatePalworldSettings | null>(null)
  const [editing, setEditing] = useState(false)
  const [confirmation, setConfirmation] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [version, setVersion] = useState<PalworldServerVersion | null>(null)
  const [versionAction, setVersionAction] = useState<'CHECK' | 'UPDATE' | null>(null)
  const [serverStatus, setServerStatus] = useState<PalworldServerStatus | null>(null)
  const [installedVersion, setInstalledVersion] = useState<InstalledSteamServerVersion | null>(null)
  const [versionError, setVersionError] = useState<string | null>(null)
  const [updateCheckError, setUpdateCheckError] = useState<string | null>(null)

  useEffect(() => {
    getPalworldServer()
      .then(async (loadedServer) => {
        setServer(loadedServer)
        if (loadedServer.mode === 'DEMO') setSettings(await getDemoPalworldSettings())
        else {
          setServerStatus(await getPalworldServerStatus())
          void getInstalledPalworldServerVersion()
            .then(setInstalledVersion)
            .catch((reason: unknown) => setVersionError(reason instanceof Error ? reason.message : '現在のBuild IDを取得できませんでした'))
          void checkPalworldServerVersion()
            .then((checked) => {
              setVersion(checked); setInstalledVersion({ appId: 2394010, buildId: checked.currentBuildId })
            })
            .catch(() => setUpdateCheckError('最新バージョンの配布状況を確認できませんでした'))
        }
      })
      .catch((reason: unknown) => setError(reason instanceof Error ? reason.message : 'Palworldサーバーを取得できませんでした'))
  }, [])

  const operate = async (action: 'START' | 'STOP' | 'RESTART') => {
    setBusy(true); setError(null); setNotice(null)
    try {
      setServer(await operateDemoPalworldServer(action)); setEditing(false)
      setNotice(action === 'STOP' ? 'サーバーを停止しました' : action === 'START' ? 'サーバーを起動しました' : 'サーバーを再起動しました')
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'デモサーバーを操作できませんでした')
    } finally { setBusy(false) }
  }

  const beginEditing = () => {
    if (!settings) return
    setDraft({ ...settings }); setEditing(true); setNotice(null)
  }

  const save = async () => {
    if (!draft) return
    setBusy(true); setError(null)
    try {
      const updated = await updateDemoPalworldSettings(draft)
      setSettings(updated); setServer((current) => current ? { ...current, serverName: updated.serverName } : current)
      setEditing(false); setDraft(null); setNotice('設定を保存し、変更前のデモ設定をバックアップしました')
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Palworld設定を保存できませんでした')
    } finally { setBusy(false) }
  }

  const remove = async () => {
    setBusy(true); setError(null)
    try {
      await deleteDemoPalworldServer(confirmation)
      window.history.pushState({}, '', '/servers/new'); window.dispatchEvent(new PopStateEvent('popstate'))
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'デモサーバーを削除できませんでした'); setBusy(false)
    }
  }

  const checkVersion = async () => {
    setBusy(true); setVersionAction('CHECK'); setError(null); setNotice(null); setUpdateCheckError(null)
    try {
      const checked = await checkPalworldServerVersion()
      setVersion(checked); setInstalledVersion({ appId: 2394010, buildId: checked.currentBuildId }); setVersionError(null); setUpdateCheckError(null)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '最新サーバーバージョンを取得できませんでした')
    } finally { setBusy(false); setVersionAction(null) }
  }

  const applyUpdate = async () => {
    setBusy(true); setVersionAction('UPDATE'); setError(null); setNotice(null)
    try {
      const updated = await updatePalworldServerVersion()
      setVersion(updated); setInstalledVersion({ appId: 2394010, buildId: updated.currentBuildId }); setVersionError(null); setNotice(updated.message)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '最新サーバーバージョンを適用できませんでした')
    } finally { setBusy(false); setVersionAction(null) }
  }

  const running = server?.mode === 'REAL' ? serverStatus?.state === 'RUNNING' : server?.state === 'RUNNING'

  return <main className="compact-page">
    <AppLink className="back-link" href="/servers/new">ゲーム選択へ戻る</AppLink>
    <header className="page-header"><p className="eyebrow">Palworld</p><h1>サーバー管理</h1><p>現在状態を確認し、サーバー操作とワールド設定を管理します。</p></header>
    {error && <p className="error request-error" role="alert">{error}</p>}
    {notice && <p className="success-notice" role="status">{notice}</p>}
    {!server && !error && <p>読み込み中...</p>}
    {server && <>
      <section className="card server-overview">
        <div className="server-overview-heading"><div><p className="section-number">SERVER</p><h2>{server.serverName}</h2></div><span className={`status-badge ${running ? 'running' : 'stopped'}`}>{running ? '起動中' : '停止中'}</span></div>
        <dl><dt>種類</dt><dd>{server.mode === 'DEMO' ? 'デモ' : '実サーバー'}</dd><dt>状態</dt><dd>{running ? '起動中' : '停止中'}</dd><dt>ゲームポート</dt><dd>{server.gamePort}</dd><dt>RCONポート</dt><dd>{server.rconPort}</dd><dt>ゲームポートの接続元</dt><dd>{formatGamePortAccess(server)}</dd><dt>{server.mode === 'DEMO' ? 'デモデータ' : 'インストール先'}</dt><dd>{server.mode === 'DEMO' ? server.workspacePath : server.installPath}</dd></dl>
        <div className="button-row management-actions"><button type="button" disabled={busy || server.state === 'RUNNING'} onClick={() => void operate('START')}>起動</button><button type="button" disabled={busy || server.state === 'STOPPED'} onClick={() => void operate('STOP')}>保存して停止</button><button type="button" disabled={busy || server.state === 'STOPPED'} onClick={() => void operate('RESTART')}>保存して再起動</button></div>
        <p className="notice">デモ操作のため、実際のゲームプロセスは操作しません。</p>
      </section>
      <section className="card form-section">
        <div className="section-heading"><div><p className="section-number">UPDATE</p><h2>サーバーバージョン</h2></div><p>Steamで公開されているPalworldサーバーのBuild IDと比較します。</p></div>
        {version?.updateAvailable && <p className="notice" role="status">最新バージョンが配布されています。</p>}
        <dl><dt>Steam App ID</dt><dd>2394010</dd><dt>現在のBuild ID</dt><dd>{server.mode === 'DEMO' ? 'デモのため取得不可' : installedVersion?.buildId ?? (versionError ? '取得失敗' : '読み込み中...')}</dd><dt>最新のBuild ID</dt><dd>{server.mode === 'DEMO' ? 'デモのため取得不可' : version?.latestBuildId ?? '確認中...'}</dd>{version && <><dt>確認結果</dt><dd>{version.message}</dd></>}</dl>
        {versionError && <p className="error" role="alert">{versionError}</p>}
        {updateCheckError && <p className="notice">{updateCheckError}必要に応じて再確認してください。</p>}
        <div className="button-row management-actions">
          <button type="button" disabled={busy || server.mode === 'DEMO'} onClick={() => void checkVersion()}>{versionAction === 'CHECK' ? '更新状況を確認中...' : '更新状況を再確認'}</button>
          <button type="button" disabled={busy || server.mode === 'DEMO' || running || !version?.updateAvailable} onClick={() => void applyUpdate()}>{versionAction === 'UPDATE' ? '最新バージョンを適用中...' : '最新バージョンを適用'}</button>
        </div>
        {server.mode === 'DEMO' && <p className="notice">デモサーバーでは画面のみ確認できます。Build IDの取得と更新は実行しません。</p>}
        {server.mode === 'REAL' && running && <p className="notice">更新を適用するにはサーバーを停止してください。</p>}
        {server.mode === 'REAL' && !version && !updateCheckError && <p className="notice">最新バージョンの配布状況を確認しています。</p>}
      </section>
      {settings && <section className="card"><h2>ワールド設定</h2>{editing && draft ? <SettingsForm draft={draft} busy={busy} onChange={setDraft} onSave={() => void save()} onCancel={() => setEditing(false)} /> : <><SettingsPreview settings={settings} /><button type="button" disabled={busy || server.state !== 'STOPPED'} onClick={beginEditing}>設定を編集</button>{server.state === 'RUNNING' && <p className="notice">設定を編集するにはサーバーを停止してください。</p>}</>}</section>}
      <section className="card danger-zone"><h2>デモサーバーを削除</h2><p>一時領域のデモデータとPalworldの登録を削除します。</p><label htmlFor="deleteConfirmation">確認のためPALWORLDと入力してください</label><input id="deleteConfirmation" value={confirmation} onChange={(event) => setConfirmation(event.target.value)} /><button type="button" disabled={busy || confirmation !== 'PALWORLD'} onClick={() => void remove()}>デモサーバーを削除</button></section>
    </>}
  </main>
}

function SettingsPreview({ settings }: { settings: PalworldSettings }) {
  return <dl><dt>サーバー名</dt><dd>{settings.serverName}</dd><dt>サーバー説明</dt><dd>{settings.serverDescription || '未設定'}</dd><dt>最大プレイヤー数</dt><dd>{settings.maxPlayers}</dd><dt>サーバーパスワード</dt><dd>{settings.serverPassword || '未設定'}</dd><dt>管理者パスワード</dt><dd>{settings.adminPassword || '未設定'}</dd><dt>経験値倍率</dt><dd>{settings.expRate}</dd><dt>捕獲率</dt><dd>{settings.palCaptureRate}</dd><dt>パル出現倍率</dt><dd>{settings.palSpawnRate}</dd><dt>ドロップ倍率</dt><dd>{settings.enemyDropRate}</dd><dt>タマゴ孵化時間</dt><dd>{settings.eggHatchingTime}時間</dd><dt>デスペナルティ</dt><dd>{deathPenaltyLabel(settings.deathPenalty)}</dd><dt>PvP</dt><dd>{enabled(settings.pvpEnabled)}</dd><dt>フレンドリーファイア</dt><dd>{enabled(settings.friendlyFireEnabled)}</dd><dt>ワールド全体の拠点数</dt><dd>{settings.baseCampMaxNum}</dd><dt>拠点作業パル数</dt><dd>{settings.baseCampWorkerMaxNum}</dd></dl>
}

function SettingsForm({ draft, busy, onChange, onSave, onCancel }: { draft: UpdatePalworldSettings; busy: boolean; onChange: (draft: UpdatePalworldSettings) => void; onSave: () => void; onCancel: () => void }) {
  const text = (key: 'serverName' | 'serverDescription' | 'serverPassword' | 'adminPassword') => (event: React.ChangeEvent<HTMLInputElement>) => onChange({ ...draft, [key]: event.target.value })
  const number = (key: 'maxPlayers' | 'expRate' | 'palCaptureRate' | 'palSpawnRate' | 'enemyDropRate' | 'eggHatchingTime' | 'baseCampMaxNum' | 'baseCampWorkerMaxNum') => (event: React.ChangeEvent<HTMLInputElement>) => onChange({ ...draft, [key]: Number(event.target.value) })
  return <form onSubmit={(event) => { event.preventDefault(); onSave() }}>
    <label className="field">サーバー名<input required maxLength={100} value={draft.serverName} onChange={text('serverName')} /></label><label className="field">サーバー説明<input maxLength={500} value={draft.serverDescription} onChange={text('serverDescription')} /></label><label className="field">最大プレイヤー数<input type="number" min={1} max={32} value={draft.maxPlayers} onChange={number('maxPlayers')} /></label>
    <label className="field">サーバーパスワード<input maxLength={100} value={draft.serverPassword} onChange={text('serverPassword')} /></label><label className="field">管理者パスワード<input maxLength={100} value={draft.adminPassword} onChange={text('adminPassword')} /></label>
    <label className="field">経験値倍率<input type="number" min={0.1} max={5} step={0.1} value={draft.expRate} onChange={number('expRate')} /></label><label className="field">捕獲率<input type="number" min={0.1} max={5} step={0.1} value={draft.palCaptureRate} onChange={number('palCaptureRate')} /></label><label className="field">パル出現倍率<input type="number" min={0.1} max={3} step={0.1} value={draft.palSpawnRate} onChange={number('palSpawnRate')} /></label><label className="field">ドロップ倍率<input type="number" min={0.1} max={5} step={0.1} value={draft.enemyDropRate} onChange={number('enemyDropRate')} /></label><label className="field">タマゴ孵化時間<input type="number" min={0} max={240} step={0.5} value={draft.eggHatchingTime} onChange={number('eggHatchingTime')} /></label>
    <label className="field">デスペナルティ<select value={draft.deathPenalty} onChange={(event) => onChange({ ...draft, deathPenalty: event.target.value as PalworldSettings['deathPenalty'] })}><option value="None">なし</option><option value="Item">アイテム</option><option value="ItemAndEquipment">アイテムと装備</option><option value="All">すべて</option></select></label><label className="check-field"><input type="checkbox" checked={draft.pvpEnabled} onChange={(event) => onChange({ ...draft, pvpEnabled: event.target.checked })} />PvPを有効にする</label><label className="check-field"><input type="checkbox" checked={draft.friendlyFireEnabled} onChange={(event) => onChange({ ...draft, friendlyFireEnabled: event.target.checked })} />フレンドリーファイアを有効にする</label>
    <label className="field">ワールド全体の拠点数<input type="number" min={1} max={128} value={draft.baseCampMaxNum} onChange={number('baseCampMaxNum')} /></label><label className="field">拠点作業パル数<input type="number" min={1} max={50} value={draft.baseCampWorkerMaxNum} onChange={number('baseCampWorkerMaxNum')} /></label><div className="button-row"><button type="submit" disabled={busy}>設定を保存</button><button className="secondary" type="button" disabled={busy} onClick={onCancel}>キャンセル</button></div>
  </form>
}

function enabled(value: boolean): string { return value ? '有効' : '無効' }
function deathPenaltyLabel(value: PalworldSettings['deathPenalty']): string { return { None: 'なし', Item: 'アイテム', ItemAndEquipment: 'アイテムと装備', All: 'すべて' }[value] }
function formatGamePortAccess(server: GameServerRegistration): string { if (server.gamePortAccess.allowAny) return 'すべての接続元'; return [server.gamePortAccess.localSubnet ? '同一LAN' : null, server.gamePortAccess.tailscale ? 'Tailscale' : null, ...server.gamePortAccess.customRemoteAddresses].filter((value): value is string => value !== null).join('、') }
