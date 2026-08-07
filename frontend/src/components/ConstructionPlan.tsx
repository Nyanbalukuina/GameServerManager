import type {
  ServerConstructionPlan,
  ServerPreflightReport,
} from '../types/serverConstruction'
import { PreflightResults } from './PreflightResults'

type ConstructionPlanProps = {
  plan: ServerConstructionPlan
  preflight: ServerPreflightReport | null
  preflightError: string | null
  isChecking: boolean
  onCheckEnvironment: () => Promise<void>
  onReturnToForm: () => void
}

export function ConstructionPlan({
  plan,
  preflight,
  preflightError,
  isChecking,
  onCheckEnvironment,
  onReturnToForm,
}: ConstructionPlanProps) {
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
          <dt>インストール先</dt>
          <dd>{plan.installPath}</dd>
          <dt>SteamCMDの保存先</dt>
          <dd>{plan.steamCmdPath}</dd>
          <dt>ゲームポート</dt>
          <dd>{plan.gamePort}</dd>
          <dt>RCONポート</dt>
          <dd>{plan.rconPort}</dd>
          <dt>最大プレイヤー数</dt>
          <dd>{plan.maxPlayers}</dd>
          <dt>サーバーパスワード</dt>
          <dd>{plan.serverPasswordConfigured ? '設定あり' : '設定なし'}</dd>
          <dt>管理者パスワード</dt>
          <dd>{plan.adminPasswordConfigured ? '設定あり' : '設定なし'}</dd>
        </dl>
      </section>

      <section className="card">
        <h2>構築前の事前検証</h2>
        <p>Windowsのパス、空き容量、ポート、SteamCMDの状態を確認します。</p>
        <button type="button" onClick={() => void onCheckEnvironment()} disabled={isChecking}>
          {isChecking ? '検証中...' : '事前検証を実行'}
        </button>

        {preflightError && <p className="error request-error">{preflightError}</p>}
        {preflight && <PreflightResults report={preflight} />}
      </section>

      <p className="notice">検証ではフォルダー作成や設定変更を行いません。</p>
      <button type="button" className="secondary" onClick={onReturnToForm}>
        入力画面へ戻る
      </button>
    </>
  )
}
