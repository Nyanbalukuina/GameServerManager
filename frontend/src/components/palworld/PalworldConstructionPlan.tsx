import { useState } from 'react'
import type { NewServerRequest, ServerConstructionPlan } from '../../types/palworldConstruction'
import type { DemoConstructionReport, ServerConstructionReport, ServerPreflightReport } from '../../types/serverOperations'
import { AppLink } from '../common/AppLink'
import { ConstructionInProgress } from '../common/ConstructionInProgress'
import { ConstructionProgress } from '../common/ConstructionProgress'
import { PreflightResults } from '../common/PreflightResults'

type Props = {
  plan: ServerConstructionPlan
  preflight: ServerPreflightReport | null
  isConstructing: boolean
  constructionKind: 'REAL' | 'DEMO' | null
  constructionReport: DemoConstructionReport | null
  realReport: ServerConstructionReport | null
  constructionError: string | null
  onRunDemoConstruction: () => Promise<void>
  onRunRealConstruction: () => Promise<void>
  onReturnToForm: () => void
  demoEnabled: boolean
  serverPassword: string
  adminPassword: string
  worldSettings: NewServerRequest
}

export function PalworldConstructionPlan({ plan, preflight, isConstructing, constructionKind, constructionReport, realReport, constructionError, onRunDemoConstruction, onRunRealConstruction, onReturnToForm, demoEnabled, serverPassword, adminPassword, worldSettings }: Props) {
  const [kind, setKind] = useState<'DEMO' | 'REAL'>(demoEnabled ? 'DEMO' : 'REAL')
  const report = constructionReport ?? realReport
  const runConstruction = () => kind === 'DEMO' ? onRunDemoConstruction() : onRunRealConstruction()

  return <>
    <button className="back-link link-button" type="button" disabled={isConstructing} onClick={onReturnToForm}>入力画面へ戻る</button>
    <header className="page-header"><p className="eyebrow">Palworld</p><h1>構築内容の確認</h1><p>入力内容と事前検証結果を確認してからサーバー構築を開始します。</p></header>
    {(isConstructing || (constructionError !== null && constructionKind === 'REAL')) && constructionKind && <ConstructionInProgress demo={constructionKind === 'DEMO'} active={isConstructing} />}

    <section className="card">
      <div className="section-heading"><div><p className="section-number">01</p><h2>基本設定</h2></div><p>Palworld本体とSteamCMDの保存先です。</p></div>
      <dl><dt>インストール先</dt><dd>{plan.installPath}</dd><dt>SteamCMDの保存先</dt><dd>{plan.steamCmdPath}</dd></dl>
    </section>

    <section className="card">
      <div className="section-heading"><div><p className="section-number">02</p><h2>ゲーム設定</h2></div><p>PalWorldSettings.iniへ保存する内容です。</p></div>
      <section className="settings-preview"><h3>PalWorldSettings.ini</h3><dl>
          <dt>サーバー名</dt><dd>{plan.serverName}</dd><dt>最大プレイヤー数</dt><dd>{plan.maxPlayers}</dd><dt>サーバー説明</dt><dd>{worldSettings.serverDescription || '未設定'}</dd>
          <dt>経験値倍率</dt><dd>{worldSettings.expRate}</dd><dt>捕獲率</dt><dd>{worldSettings.palCaptureRate}</dd><dt>パル出現倍率</dt><dd>{worldSettings.palSpawnRate}</dd><dt>ドロップ倍率</dt><dd>{worldSettings.enemyDropRate}</dd>
          <dt>タマゴ孵化時間</dt><dd>{worldSettings.eggHatchingTime}時間</dd><dt>デスペナルティ</dt><dd>{formatDeathPenalty(worldSettings.deathPenalty)}</dd><dt>ワールド全体の拠点数</dt><dd>{worldSettings.baseCampMaxNum}</dd><dt>拠点作業パル数</dt><dd>{worldSettings.baseCampWorkerMaxNum}</dd>
          <dt>PvP</dt><dd>{worldSettings.pvpEnabled ? '有効' : '無効'}</dd><dt>フレンドリーファイア</dt><dd>{worldSettings.friendlyFireEnabled ? '有効' : '無効'}</dd>
      </dl></section>
    </section>

    <section className="card"><div className="section-heading"><div><p className="section-number">03</p><h2>自動運転</h2></div><p>停止・起動スケジュールとして保存する内容です。</p></div><dl><dt>状態</dt><dd>{plan.automationEnabled ? '有効' : '無効'}</dd>{plan.automationEnabled && <><dt>停止時刻</dt><dd>{plan.shutdownTime}</dd><dt>起動時刻</dt><dd>{plan.startupTime}</dd></>}</dl></section>

    <section className="card"><div className="section-heading"><div><p className="section-number">04</p><h2>接続設定</h2></div></div><dl><dt>ゲームポート</dt><dd>{plan.gamePort}</dd><dt>RCONポート</dt><dd>{plan.rconPort}</dd><dt>接続元</dt><dd>{formatGamePortAccess(plan)}</dd></dl></section>
    <section className="card"><div className="section-heading"><div><p className="section-number">05</p><h2>パスワード</h2></div><p>PalWorldSettings.iniへ保存する内容です。</p></div><dl><dt>サーバーパスワード</dt><dd>{serverPassword || '未設定'}</dd><dt>管理者パスワード</dt><dd>{adminPassword || '未設定'}</dd></dl></section>

    <section className="card action-card">
      <div className="section-heading"><div><p className="section-number">06</p><h2>事前検証結果</h2></div><p>保存先、空き容量、SteamCMD、使用ポートの確認結果です。</p></div>
      {preflight && <PreflightResults report={preflight} />}
    </section>

    <section className="card action-card">
      <div className="section-heading"><div><p className="section-number">07</p><h2>サーバー構築</h2></div><p>{kind === 'DEMO' ? '実環境を変更せず、設定と操作の流れを確認します。' : 'Palworldサーバーをインストールし、設定・Firewall・起動まで実行します。'}</p></div>
      {demoEnabled && <label className="field" htmlFor="palworldConstructionKind">構築の種類<select id="palworldConstructionKind" value={kind} onChange={(event) => setKind(event.target.value as 'DEMO' | 'REAL')}><option value="DEMO">デモ構築</option><option value="REAL">実サーバー構築</option></select></label>}
      <button type="button" disabled={(kind === 'REAL' && !preflight?.canProceed) || isConstructing || report !== null} onClick={() => void runConstruction()}>{isConstructing ? '構築中...' : kind === 'DEMO' ? 'デモサーバーを構築' : 'Palworldサーバーを構築して起動'}</button>
      {kind === 'REAL' && !preflight?.canProceed && <p className="notice">事前検証のエラーを解消してから実構築を開始してください。</p>}
      {kind === 'DEMO' && preflight && !preflight.canProceed && <p className="notice">実構築には修正が必要ですが、デモ構築は実行できます。</p>}
      {constructionError && <p className="error request-error" role="alert">{constructionError}</p>}
    </section>

    {report && <><ConstructionProgress report={report} /><AppLink className="management-link" href="/servers/palworld">Palworld管理画面を開く</AppLink></>}
  </>
}

function formatDeathPenalty(value: NewServerRequest['deathPenalty']): string {
  return { None: 'なし', Item: 'アイテム', ItemAndEquipment: 'アイテムと装備', All: 'すべて' }[value]
}

function formatGamePortAccess(plan: ServerConstructionPlan): string {
  if (plan.gamePortAccess.allowAny) return 'すべての接続元'
  return [plan.gamePortAccess.localSubnet ? '同一LAN' : null, plan.gamePortAccess.tailscale ? 'Tailscale' : null, ...plan.gamePortAccess.customRemoteAddresses].filter((scope): scope is string => scope !== null).join('、')
}
