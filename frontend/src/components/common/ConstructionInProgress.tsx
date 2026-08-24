import { useEffect, useState } from 'react'

type ConstructionInProgressProps = {
  demo: boolean
  game?: 'PALWORLD' | 'ASA'
}

export function ConstructionInProgress({ demo, game = 'PALWORLD' }: ConstructionInProgressProps) {
  const [elapsedSeconds, setElapsedSeconds] = useState(0)

  useEffect(() => {
    const timer = window.setInterval(() => setElapsedSeconds((current) => current + 1), 1000)
    return () => window.clearInterval(timer)
  }, [])

  const steps = demo
    ? ['入力内容の確認', 'デモ用SteamCMDの準備', 'デモサーバーの配置', '設定と管理データの保存']
    : game === 'ASA'
      ? ['SteamCMDの準備・ダウンロード', 'ARK: Survival Ascended Dedicated Serverのダウンロード', 'GameUserSettings.iniの保存', 'Windows Firewallの設定', 'サーバー起動とポート待受確認', '管理対象への登録']
      : ['SteamCMDの準備・ダウンロード', 'Palworld Dedicated Serverのダウンロード', 'サーバー設定の保存', '自動運転設定の保存', 'Windows Firewallの設定', 'サーバー起動とポート待受確認', '管理対象への登録']

  return (
    <section className="card construction-running" role="status" aria-live="polite">
      <div className="construction-running-heading">
        <span className="spinner" aria-hidden="true" />
        <div>
          <h2>{demo ? `${game === 'ASA' ? 'ARK: Survival Ascended' : 'Palworld'}デモサーバーを作成しています` : `${game === 'ASA' ? 'ARK: Survival Ascended' : 'Palworld'}サーバーを構築しています`}</h2>
          <p>処理は継続中です。完了するまでこの画面を閉じないでください。</p>
        </div>
      </div>
      <p><strong>経過時間:</strong> {formatElapsed(elapsedSeconds)}</p>
      <ol className="pending-step-list">
        {steps.map((step) => <li key={step}>{step}</li>)}
      </ol>
      {!demo && <p className="notice">通信速度やSteam側の応答によっては数分以上かかります。</p>}
    </section>
  )
}

function formatElapsed(seconds: number): string {
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return `${minutes}分${remainingSeconds.toString().padStart(2, '0')}秒`
}
