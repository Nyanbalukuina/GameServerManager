import type {
  ServerConstructionPlan,
} from '../../types/palworldConstruction'
import type {
  DemoConstructionReport,
  ServerConstructionReport,
  ServerPreflightReport,
} from '../../types/serverOperations'
import { ConstructionProgress } from '../common/ConstructionProgress'
import { PreflightResults } from '../common/PreflightResults'
import { AppLink } from '../common/AppLink'

type PalworldConstructionPlanProps = {
  plan: ServerConstructionPlan
  preflight: ServerPreflightReport | null
  preflightError: string | null
  isChecking: boolean
  isConstructing: boolean
  constructionReport: DemoConstructionReport | null
  realReport: ServerConstructionReport | null
  constructionError: string | null
  onCheckEnvironment: () => Promise<void>
  onRunDemoConstruction: () => Promise<void>
  onRunRealConstruction: () => Promise<void>
  onReturnToForm: () => void
}

export function PalworldConstructionPlan({
  plan,
  preflight,
  preflightError,
  isChecking,
  isConstructing,
  constructionReport,
  realReport,
  constructionError,
  onCheckEnvironment,
  onRunDemoConstruction,
  onRunRealConstruction,
  onReturnToForm,
}: PalworldConstructionPlanProps) {
  return (
    <>
      <header>
        <p className="eyebrow">Game Server Manager</p>
        <h1>構築計画</h1>
        <p>以下は確認用の計画です。まだインストールやファイル作成は行いません。</p>
      </header>

      <section className="card">
        <h2>{plan.serverName}</h2>
        <dl>
          <dt>ゲーム</dt>
          <dd>Palworld</dd>
          <dt>Palworldサーバーのインストール先</dt>
          <dd>{plan.installPath}</dd>
          <dt>SteamCMDの保存先</dt>
          <dd>{plan.steamCmdPath}</dd>
          <dt>ゲームポート</dt>
          <dd>{plan.gamePort}</dd>
          <dt>ゲームポートの接続元</dt>
          <dd>{formatGamePortAccess(plan)}</dd>
          <dt>RCONポート</dt>
          <dd>{plan.rconPort}</dd>
          <dt>最大プレイヤー数</dt>
          <dd>{plan.maxPlayers}</dd>
          <dt>サーバーパスワード</dt>
          <dd>{plan.serverPasswordConfigured ? '設定あり' : '設定なし'}</dd>
          <dt>管理者パスワード</dt>
          <dd>{plan.adminPasswordConfigured ? '設定あり' : '設定なし'}</dd>
          <dt>自動運転</dt>
          <dd>{plan.automationEnabled ? '有効' : '無効'}</dd>
          <dt>毎日の停止時刻</dt>
          <dd>{plan.shutdownTime}</dd>
          <dt>毎日の起動時刻</dt>
          <dd>{plan.startupTime}</dd>
          <dt>停止後のバックアップ</dt>
          <dd>{plan.backupAfterShutdown ? '有効' : '無効'}</dd>
          <dt>バックアップ保持数</dt>
          <dd>{plan.backupRetentionCount}個</dd>
        </dl>
      </section>

      <section className="card">
        <h2>実Palworldサーバー構築</h2>
        <p>SteamCMDとPalworld Dedicated Serverを導入し、設定保存と起動確認まで実行します。ダウンロードには時間がかかります。</p>
        <button
          type="button"
          onClick={() => void onRunRealConstruction()}
          disabled={!preflight?.canProceed || isConstructing || realReport?.completed === true}
          aria-busy={isConstructing}
        >
          {isConstructing ? '構築中...' : 'Palworldサーバーを構築して起動'}
        </button>
        {!preflight && <p className="notice">先に事前検証を実行してください。</p>}
        {preflight && !preflight.canProceed && (
          <p className="notice">事前検証のエラーを解消すると実構築できます。</p>
        )}
      </section>

      <section className="card">
        <h2>構築前の事前検証</h2>
        <p>Windowsのパス、空き容量、ポート、SteamCMDの状態を確認します。</p>
        <button
          type="button"
          onClick={() => void onCheckEnvironment()}
          disabled={isChecking}
          aria-busy={isChecking}
        >
          {isChecking ? '検証中...' : '事前検証を実行'}
        </button>

        {preflightError && <p className="error request-error">{preflightError}</p>}
        {preflight && <PreflightResults report={preflight} />}
      </section>

      <section className="card">
        <h2>デモサーバー作成</h2>
        <p>本物のSteamCMDやゲームは導入せず、一時領域にサーバー構成・Palworld設定・自動運転設定を作成します。</p>
        <button
          type="button"
          onClick={() => void onRunDemoConstruction()}
          disabled={!preflight || isConstructing}
          aria-busy={isConstructing}
        >
          {isConstructing ? 'デモサーバー作成中...' : 'デモサーバーを作成'}
        </button>
        {!preflight && <p className="notice">先に事前検証を実行してください。</p>}
        {preflight && !preflight.canProceed && (
          <p className="notice">
            実構築には修正が必要ですが、デモ構築は一時領域で実行できます。
          </p>
        )}
        {constructionError && <p className="error request-error">{constructionError}</p>}
      </section>

      {constructionReport && <ConstructionProgress report={constructionReport} />}
      {realReport && <ConstructionProgress report={realReport} />}
      {(constructionReport?.completed || realReport?.completed) && (
        <AppLink className="management-link" href="/servers/palworld">Palworld管理画面を開く</AppLink>
      )}

      <p className="notice">事前検証ではフォルダー作成や設定変更を行いません。</p>
      <button type="button" className="secondary" onClick={onReturnToForm}>
        入力画面へ戻る
      </button>
    </>
  )
}

function formatGamePortAccess(plan: ServerConstructionPlan): string {
  if (plan.gamePortAccess.allowAny) return 'すべての接続元'
  const scopes = [
    plan.gamePortAccess.localSubnet ? '同一LAN' : null,
    plan.gamePortAccess.tailscale ? 'Tailscale' : null,
    ...plan.gamePortAccess.customRemoteAddresses,
  ].filter((scope): scope is string => scope !== null)
  return scopes.join('、')
}
