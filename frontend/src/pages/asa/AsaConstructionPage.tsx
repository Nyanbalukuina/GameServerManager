import { useEffect, useState } from 'react'
import { constructAsaServer, constructDemoAsaServer, runAsaPreflight } from '../../api/asaConstruction'
import { getStorageConfiguration } from '../../api/storageConfiguration'
import { AppLink } from '../../components/common/AppLink'
import { ConstructionInProgress } from '../../components/common/ConstructionInProgress'
import { ConstructionProgress } from '../../components/common/ConstructionProgress'
import { PreflightResults } from '../../components/common/PreflightResults'
import type { AsaConstructionErrors, AsaConstructionRequest } from '../../types/asaConstruction'
import type { DemoConstructionReport, ServerConstructionReport, ServerPreflightReport } from '../../types/serverOperations'
import '../../styles/serverConstruction.css'

const initialRequest: AsaConstructionRequest = {
  serverName: 'GSM ASA Server', installPath: '', steamCmdPath: '', map: 'TheIsland_WP',
  gamePort: '7777', queryPort: '27015', rconPort: '27020', maxPlayers: '20',
  serverPassword: '', adminPassword: '', allowLocalSubnet: true, allowTailscale: true,
  customRemoteAddresses: '', allowAnyRemoteAddress: false,
}

export function AsaConstructionPage() {
  const [request, setRequest] = useState(initialRequest)
  const [kind, setKind] = useState<'DEMO' | 'REAL'>('DEMO')
  const [errors, setErrors] = useState<AsaConstructionErrors>({})
  const [preflight, setPreflight] = useState<ServerPreflightReport | null>(null)
  const [report, setReport] = useState<DemoConstructionReport | ServerConstructionReport | null>(null)
  const [checking, setChecking] = useState(false)
  const [constructing, setConstructing] = useState(false)

  useEffect(() => {
    getStorageConfiguration()
      .then((configuration) => setRequest((current) => ({
        ...current, installPath: configuration.asaInstallPath, steamCmdPath: configuration.steamCmdPath,
      })))
      .catch((error: unknown) => setErrors({ request: error instanceof Error ? error.message : '保存先を取得できませんでした' }))
  }, [])

  useEffect(() => {
    if (!constructing) return
    const warn = (event: BeforeUnloadEvent) => event.preventDefault()
    window.addEventListener('beforeunload', warn)
    return () => window.removeEventListener('beforeunload', warn)
  }, [constructing])

  const set = <K extends keyof AsaConstructionRequest>(key: K, value: AsaConstructionRequest[K]) => {
    setRequest((current) => ({ ...current, [key]: value })); setPreflight(null); setReport(null)
  }

  const check = async () => {
    setChecking(true); setErrors({}); setReport(null)
    try { setPreflight(await runAsaPreflight(request)) }
    catch (error) { setErrors({ request: error instanceof Error ? error.message : '事前検証に失敗しました' }) }
    finally { setChecking(false) }
  }

  const construct = async () => {
    setConstructing(true); setErrors({})
    try {
      if (kind === 'DEMO') setReport(await constructDemoAsaServer(request))
      else {
        const result = await constructAsaServer(request)
        if (result.ok) setReport(result.report)
        else setErrors(result.errors)
      }
    } catch (error) { setErrors({ request: error instanceof Error ? error.message : 'ARK: Survival Ascendedサーバーを構築できませんでした' }) }
    finally { setConstructing(false) }
  }

  return <main>
    <AppLink className="back-link" href="/servers/new">ゲーム選択へ戻る</AppLink>
    <header><p className="eyebrow">ARK: Survival Ascended</p><h1>ARK: Survival Ascended専用サーバー構築</h1><p>入力後に構築可能か検証し、デモまたは実サーバーを構築します。</p></header>
    {constructing && <ConstructionInProgress demo={kind === 'DEMO'} game="ASA" />}
    <section className="card"><h2>構築方法</h2>
      <label className="field">構築の種類<select value={kind} onChange={(event) => { setKind(event.target.value as 'DEMO' | 'REAL'); setReport(null) }}><option value="DEMO">デモ構築（画面と設定を確認）</option><option value="REAL">実構築（SteamCMD・Firewall・ARK: Survival Ascendedを実行）</option></select></label>
      <p className="notice">デモ構築は一時領域だけを使用し、ダウンロード・Firewall変更・ゲーム起動を行いません。</p>
    </section>
    <section className="card"><h2>サーバー設定</h2>
      <label className="field">サーバー名<input value={request.serverName} onChange={(e) => set('serverName', e.target.value)} /></label>
      <label className="field">ARK: Survival Ascendedインストール先<input value={request.installPath} onChange={(e) => set('installPath', e.target.value)} /></label>
      <label className="field">SteamCMD保存先<input value={request.steamCmdPath} onChange={(e) => set('steamCmdPath', e.target.value)} /></label>
      <label className="field">マップ<select value={request.map} onChange={(e) => set('map', e.target.value as 'TheIsland_WP')}><option value="TheIsland_WP">The Island</option></select></label>
      <label className="field">ゲームポート<input type="number" min="1" max="65534" value={request.gamePort} onChange={(e) => set('gamePort', e.target.value)} /></label>
      <label className="field">Peerポート<input value={Number(request.gamePort) + 1 || ''} disabled /></label>
      <label className="field">Queryポート<input type="number" min="1" max="65535" value={request.queryPort} onChange={(e) => set('queryPort', e.target.value)} /></label>
      <label className="field">RCONポート<input type="number" min="1" max="65535" value={request.rconPort} onChange={(e) => set('rconPort', e.target.value)} /></label>
      <label className="field">最大プレイヤー数<input type="number" min="1" max="70" value={request.maxPlayers} onChange={(e) => set('maxPlayers', e.target.value)} /></label>
      <label className="field">サーバーパスワード<input value={request.serverPassword} onChange={(e) => set('serverPassword', e.target.value)} /></label>
      <label className="field">管理者パスワード<input value={request.adminPassword} onChange={(e) => set('adminPassword', e.target.value)} minLength={8} /></label>
      <label className="check-field"><input type="checkbox" checked={request.allowLocalSubnet} onChange={(e) => set('allowLocalSubnet', e.target.checked)} />同一LANからの接続を許可</label>
      <label className="check-field"><input type="checkbox" checked={request.allowTailscale} onChange={(e) => set('allowTailscale', e.target.checked)} />Tailscaleからの接続を許可</label>
      <label className="check-field"><input type="checkbox" checked={request.allowAnyRemoteAddress} onChange={(e) => set('allowAnyRemoteAddress', e.target.checked)} />すべての接続元を許可</label>
      <label className="field">追加の接続元アドレス<input value={request.customRemoteAddresses} onChange={(e) => set('customRemoteAddresses', e.target.value)} placeholder="例: 192.168.1.0/24" /></label>
    </section>
    <section className="card"><h2>構築前の事前検証</h2><p>保存先、書き込み権限、空き容量、SteamCMD、ARK: Survival Ascendedが使用する全ポートを確認します。</p><button type="button" disabled={checking || constructing} aria-busy={checking} onClick={() => void check()}>{checking ? '検証中...' : '事前検証を実行'}</button>{preflight && <PreflightResults report={preflight} />}</section>
    <section className="card"><h2>{kind === 'DEMO' ? 'ARK: Survival Ascendedデモサーバー構築' : 'ARK: Survival Ascended実サーバー構築'}</h2><button type="button" disabled={!preflight || (kind === 'REAL' && !preflight.canProceed) || constructing || report !== null} onClick={() => void construct()}>{constructing ? '構築中...' : kind === 'DEMO' ? 'デモサーバーを構築' : '実サーバーを構築して起動'}</button>{!preflight && <p className="notice">先に事前検証を実行してください。</p>}{kind === 'DEMO' && preflight && !preflight.canProceed && <p className="notice">実構築には修正が必要ですが、デモ構築は一時領域で実行できます。</p>}{errors.request && <p className="error request-error" role="alert">{errors.request}</p>}</section>
    {report && <><ConstructionProgress report={report} /><AppLink className="management-link" href="/servers/asa">ARK: Survival Ascended管理画面を開く</AppLink></>}
  </main>
}
