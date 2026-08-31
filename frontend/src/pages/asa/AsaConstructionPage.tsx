import { useEffect, useState } from 'react'
import { constructAsaServer, constructDemoAsaServer, runAsaPreflight } from '../../api/asaConstruction'
import { getStorageConfiguration } from '../../api/storageConfiguration'
import { AppLink } from '../../components/common/AppLink'
import { FormField } from '../../components/common/FormField'
import { GamePortAccessFields } from '../../components/common/GamePortAccessFields'
import { PreflightResults } from '../../components/common/PreflightResults'
import type { AsaConstructionErrors, AsaConstructionRequest } from '../../types/asaConstruction'
import type { ServerPreflightReport } from '../../types/serverOperations'
import type { ConstructionExecution, ConstructionExecutionResult } from '../../types/constructionExecution'
import '../../styles/serverConstruction.css'

const initialRequest: AsaConstructionRequest = {
  serverName: 'GSM ASA Server',
  installPath: '',
  steamCmdPath: '',
  map: 'TheIsland_WP',
  gamePort: '7777',
  queryPort: '27015',
  rconPort: '27020',
  maxPlayers: '20',
  serverPassword: '',
  adminPassword: '',
  allowLocalSubnet: true,
  allowTailscale: true,
  customRemoteAddresses: '',
  allowAnyRemoteAddress: false,
  pveEnabled: true,
  xpMultiplier: '1',
  tamingSpeedMultiplier: '1',
  harvestAmountMultiplier: '1',
  eggHatchSpeedMultiplier: '1',
  babyMatureSpeedMultiplier: '1',
}

export function AsaConstructionPage({ demoEnabled, onStartConstruction }: { demoEnabled: boolean; onStartConstruction: (execution: ConstructionExecution) => void }) {
  const [request, setRequest] = useState(initialRequest)
  const [kind, setKind] = useState<'DEMO' | 'REAL'>(demoEnabled ? 'DEMO' : 'REAL')
  const [errors, setErrors] = useState<AsaConstructionErrors>({})
  const [preflight, setPreflight] = useState<ServerPreflightReport | null>(null)
  const [checking, setChecking] = useState(false)
  const [reviewing, setReviewing] = useState(false)

  useEffect(() => {
    getStorageConfiguration()
      .then((configuration) => setRequest((current) => ({
        ...current,
        installPath: configuration.asaInstallPath,
        steamCmdPath: configuration.steamCmdPath,
      })))
      .catch((error: unknown) => setErrors({
        request: error instanceof Error ? error.message : '保存先を取得できませんでした',
      }))
  }, [])

  const set = <K extends keyof AsaConstructionRequest>(key: K, value: AsaConstructionRequest[K]) => {
    setRequest((current) => ({ ...current, [key]: value }))
    setErrors((current) => ({ ...current, [key]: undefined }))
    setPreflight(null)
  }

  const check = async () => {
    setChecking(true)
    setErrors({})
    try {
      setPreflight(await runAsaPreflight(request))
      setReviewing(true)
    } catch (error) {
      setErrors({ request: error instanceof Error ? error.message : '事前検証に失敗しました' })
    } finally {
      setChecking(false)
    }
  }

  const construct = () => {
    setErrors({})
    const result: Promise<ConstructionExecutionResult> = kind === 'DEMO'
      ? constructDemoAsaServer(request)
          .then((report) => ({ ok: true as const, report }))
          .catch((error: unknown) => ({ ok: false as const, message: error instanceof Error ? error.message : 'ARK: Survival Ascendedデモサーバーを構築できませんでした' }))
      : constructAsaServer(request).then((response) => response.ok
          ? { ok: true as const, report: response.report }
          : { ok: false as const, message: response.errors.request ?? 'ARK: Survival Ascendedサーバーを構築できませんでした' })
    onStartConstruction({ game: 'ASA', demo: kind === 'DEMO', result })
  }

  if (reviewing && preflight) {
    return (
      <main className="asa-construction-page compact-page">
        <button className="back-link link-button" type="button" onClick={() => setReviewing(false)}>入力画面へ戻る</button>
        <header className="page-header"><p className="eyebrow">ARK: Survival Ascended</p><h1>構築内容の確認</h1><p>入力内容と事前検証結果を確認してからサーバー構築を開始します。</p></header>
        <section className="card">
          <div className="section-heading"><div><p className="section-number">01</p><h2>基本設定</h2></div><p>ARK本体とSteamCMDの保存先です。</p></div>
          <dl><dt>インストール先</dt><dd>{request.installPath}</dd><dt>SteamCMDの保存先</dt><dd>{request.steamCmdPath}</dd></dl>
        </section>

        <section className="card">
          <div className="section-heading"><div><p className="section-number">02</p><h2>ゲーム設定</h2></div><p>ファイルごとに保存される設定を確認できます。</p></div>
          <div className="settings-preview-grid three-column">
            <section><h3>起動設定</h3><p className="setting-source">INIファイルではなく起動時に指定</p><dl><dt>マップ</dt><dd>{mapLabel(request.map)}</dd><dt>最大プレイヤー数</dt><dd>{request.maxPlayers}</dd></dl></section>
            <section><h3>GameUserSettings.ini</h3><dl><dt>サーバー名</dt><dd>{request.serverName}</dd><dt>ゲームモード</dt><dd>{request.pveEnabled ? 'PvE' : 'PvP'}</dd><dt>経験値倍率</dt><dd>{request.xpMultiplier}</dd><dt>テイム速度</dt><dd>{request.tamingSpeedMultiplier}</dd><dt>採取量倍率</dt><dd>{request.harvestAmountMultiplier}</dd></dl></section>
            <section><h3>Game.ini</h3><dl><dt>孵化速度</dt><dd>{request.eggHatchSpeedMultiplier}</dd><dt>赤ちゃんの成熟速度</dt><dd>{request.babyMatureSpeedMultiplier}</dd></dl></section>
          </div>
        </section>

        <section className="card"><div className="section-heading"><div><p className="section-number">03</p><h2>接続設定</h2></div></div><dl><dt>ゲームポート</dt><dd>{request.gamePort} / Peer {Number(request.gamePort) + 1}</dd><dt>Queryポート</dt><dd>{request.queryPort}</dd><dt>RCONポート</dt><dd>{request.rconPort}</dd><dt>接続元</dt><dd>{accessLabel(request)}</dd></dl></section>
        <section className="card"><div className="section-heading"><div><p className="section-number">04</p><h2>パスワード</h2></div><p>GameUserSettings.iniへ保存する内容です。</p></div><dl><dt>サーバーパスワード</dt><dd>{request.serverPassword || '未設定'}</dd><dt>管理者パスワード</dt><dd>{request.adminPassword || '未設定'}</dd></dl></section>

        <section className="card action-card">
          <div className="section-heading"><div><p className="section-number">05</p><h2>事前検証結果</h2></div><p>保存先、空き容量、SteamCMD、使用ポートの確認結果です。</p></div>
          <PreflightResults report={preflight} />
        </section>

        <section className="card action-card">
          <div className="section-heading"><div><p className="section-number">06</p><h2>サーバー構築</h2></div><p>{kind === 'DEMO' ? '実環境を変更せず、設定と操作の流れを確認します。' : 'ARKサーバーをインストールし、設定・Firewall・起動まで実行します。'}</p></div>
          {demoEnabled && <label className="field" htmlFor="asaConstructionKind">構築の種類<select id="asaConstructionKind" value={kind} onChange={(event) => setKind(event.target.value as 'DEMO' | 'REAL')}><option value="DEMO">デモ構築</option><option value="REAL">実サーバー構築</option></select></label>}
          <button type="button" disabled={kind === 'REAL' && !preflight.canProceed} onClick={construct}>{kind === 'DEMO' ? 'デモサーバーを構築' : 'ARKサーバーを構築して起動'}</button>
          {kind === 'REAL' && !preflight.canProceed && <p className="notice">事前検証のエラーを解消してから実構築を開始してください。</p>}
          {kind === 'DEMO' && !preflight.canProceed && <p className="notice">実構築には修正が必要ですが、デモ構築は実行できます。</p>}
          {errors.request && <p className="error request-error" role="alert">{errors.request}</p>}
        </section>
      </main>
    )
  }

  return (
    <main className="asa-construction-page compact-page">
      <AppLink className="back-link" href="/servers/new">ゲーム選択へ戻る</AppLink>
      <header className="page-header"><p className="eyebrow">ARK: Survival Ascended</p><h1>新規ARKサーバー構築</h1><p>必要な設定を入力し、確認画面で事前検証結果を確認してから構築します。</p></header>

      <section className="card form-section">
        <div className="section-heading"><div><p className="section-number">01</p><h2>基本設定</h2></div><p>ARK本体とSteamCMDの保存先を指定します。</p></div>
        <FormField id="asaInstallPath" label="ARKサーバーのインストール先" value={request.installPath} error={errors.installPath} onChange={(value) => set('installPath', value)} />
        <FormField id="asaSteamCmdPath" label="SteamCMDの保存先" value={request.steamCmdPath} error={errors.steamCmdPath} onChange={(value) => set('steamCmdPath', value)} />
      </section>

      <section className="card form-section">
        <div className="section-heading"><div><p className="section-number">02</p><h2>ゲーム設定</h2></div><p>起動引数とINIファイルの役割ごとに設定します。</p></div>
        <div className="settings-editor-group startup-settings"><h3>起動設定</h3><p className="setting-source">マップと最大人数はINIではなく、ARKサーバーの起動時に指定されます。</p><div className="form-grid"><label className="field" htmlFor="asaMap">マップ<select id="asaMap" value={request.map} onChange={(event) => set('map', event.target.value as 'TheIsland_WP')}><option value="TheIsland_WP">The Island</option></select><span className="error" aria-hidden="true" /></label><FormField id="asaMaxPlayers" label="最大プレイヤー数" type="number" value={request.maxPlayers} error={errors.maxPlayers} onChange={(value) => set('maxPlayers', value)} /></div></div>
        <div className="settings-editor-group"><h3><span>1</span>GameUserSettings.ini</h3><FormField id="asaServerName" label="サーバー名" value={request.serverName} error={errors.serverName} onChange={(value) => set('serverName', value)} /><label className="field" htmlFor="asaGameMode">ゲームモード<select id="asaGameMode" value={request.pveEnabled ? 'PVE' : 'PVP'} onChange={(event) => set('pveEnabled', event.target.value === 'PVE')}><option value="PVE">PvE</option><option value="PVP">PvP</option></select></label><div className="form-grid"><FormField id="asaXpMultiplier" label="経験値倍率" type="number" value={request.xpMultiplier} error={errors.xpMultiplier} onChange={(value) => set('xpMultiplier', value)} /><FormField id="asaTamingSpeedMultiplier" label="テイム速度" type="number" value={request.tamingSpeedMultiplier} error={errors.tamingSpeedMultiplier} onChange={(value) => set('tamingSpeedMultiplier', value)} /><FormField id="asaHarvestAmountMultiplier" label="採取量倍率" type="number" value={request.harvestAmountMultiplier} error={errors.harvestAmountMultiplier} onChange={(value) => set('harvestAmountMultiplier', value)} /></div></div>
        <div className="settings-editor-group"><h3><span>2</span>Game.ini</h3><div className="form-grid"><FormField id="asaEggHatchSpeedMultiplier" label="孵化速度" type="number" value={request.eggHatchSpeedMultiplier} error={errors.eggHatchSpeedMultiplier} onChange={(value) => set('eggHatchSpeedMultiplier', value)} /><FormField id="asaBabyMatureSpeedMultiplier" label="赤ちゃんの成熟速度" type="number" value={request.babyMatureSpeedMultiplier} error={errors.babyMatureSpeedMultiplier} onChange={(value) => set('babyMatureSpeedMultiplier', value)} /></div></div>
        <p className="notice">各倍率の標準値は1です。0.1から100の範囲で指定できます。</p>
      </section>

      <section className="card form-section">
        <div className="section-heading"><div><p className="section-number">03</p><h2>接続設定</h2></div><p>使用するポートと接続を許可するネットワークを指定します。</p></div>
        <div className="form-grid"><FormField id="asaGamePort" label="ゲームポート" type="number" value={request.gamePort} error={errors.gamePort} onChange={(value) => set('gamePort', value)} /><FormField id="asaPeerPort" label="Peerポート（自動）" type="number" value={request.gamePort.trim() === '' ? '' : String(Number(request.gamePort) + 1)} disabled onChange={() => undefined} /><FormField id="asaQueryPort" label="Queryポート" type="number" value={request.queryPort} error={errors.queryPort} onChange={(value) => set('queryPort', value)} /><FormField id="asaRconPort" label="RCONポート" type="number" value={request.rconPort} error={errors.rconPort} onChange={(value) => set('rconPort', value)} /></div>
        <GamePortAccessFields allowLocalSubnet={request.allowLocalSubnet} allowTailscale={request.allowTailscale} allowAnyRemoteAddress={request.allowAnyRemoteAddress} customRemoteAddresses={request.customRemoteAddresses} error={errors.customRemoteAddresses} onChange={(field, value) => set(field, value as never)} />
      </section>

      <section className="card form-section">
        <div className="section-heading"><div><p className="section-number">04</p><h2>パスワード</h2></div><p>GameUserSettings.iniへ保存される機密設定です。</p></div>
        <div className="form-grid">
          <FormField id="asaServerPassword" label="サーバーパスワード（任意）" value={request.serverPassword} error={errors.serverPassword} onChange={(value) => set('serverPassword', value)} />
          <FormField id="asaAdminPassword" label="管理者パスワード" value={request.adminPassword} error={errors.adminPassword} onChange={(value) => set('adminPassword', value)} />
        </div>
      </section>

      <div className="construction-review-action"><button type="button" disabled={checking} aria-busy={checking} onClick={() => void check()}>{checking ? '事前検証中...' : 'この設定でサーバー構築'}</button><p>クリックすると自動で事前検証を行い、確認画面へ進みます。</p>{errors.request && <p className="error request-error" role="alert">{errors.request}</p>}</div>
    </main>
  )
}

function mapLabel(map: AsaConstructionRequest['map']) {
  return map === 'TheIsland_WP' ? 'The Island' : map
}

function accessLabel(request: AsaConstructionRequest) {
  const labels = []
  if (request.allowLocalSubnet) labels.push('同一LAN')
  if (request.allowTailscale) labels.push('Tailscale')
  if (request.customRemoteAddresses.trim()) labels.push('手動指定')
  if (request.allowAnyRemoteAddress) labels.push('すべての接続元')
  return labels.join('、') || 'なし'
}
