import { useEffect, useState } from 'react'
import { getConstructionProgress } from '../../api/constructionProgress'
import type { ConstructionProgressSnapshot, ConstructionProgressStepStatus } from '../../types/serverOperations'

type ConstructionInProgressProps = {
  demo: boolean
  game?: 'PALWORLD' | 'ASA'
  active?: boolean
}

export function ConstructionInProgress({ demo, game = 'PALWORLD', active = true }: ConstructionInProgressProps) {
  const [elapsedSeconds, setElapsedSeconds] = useState(0)
  const [progress, setProgress] = useState<ConstructionProgressSnapshot | null>(null)

  useEffect(() => {
    const timer = window.setInterval(() => setElapsedSeconds((current) => current + 1), 1000)
    return () => window.clearInterval(timer)
  }, [])

  useEffect(() => {
    if (demo) return
    let disposed = false
    const refresh = async () => {
      try {
        const latest = await getConstructionProgress(game)
        if (!disposed && latest) setProgress(latest)
      } catch {}
    }
    void refresh()
    if (!active) return () => { disposed = true }
    const timer = window.setInterval(() => void refresh(), 1000)
    return () => {
      disposed = true
      window.clearInterval(timer)
    }
  }, [active, demo, game])

  const labels = demo
    ? ['入力内容の確認', 'デモ用SteamCMDの準備', 'デモサーバーの配置', '設定と管理データの保存']
    : game === 'ASA'
      ? ['SteamCMDの準備・ダウンロード', 'ARK: Survival Ascended Dedicated Serverのダウンロード', 'GameUserSettings.iniの保存', 'Windows Firewallの設定', 'サーバー起動とポート待受確認', '管理対象への登録']
      : ['SteamCMDの準備・ダウンロード', 'Palworld Dedicated Serverのダウンロード', 'サーバー設定の保存', '自動運転設定の保存', 'Windows Firewallの設定', 'サーバー起動とポート待受確認', '管理対象への登録']
  const steps = demo
    ? labels.map((label) => ({ id: label, label, status: 'PENDING' as ConstructionProgressStepStatus, message: '' }))
    : progress?.steps ?? labels.map((label) => ({ id: label, label, status: 'PENDING' as ConstructionProgressStepStatus, message: '待機中' }))
  const failed = progress?.status === 'ERROR'

  return (
    <section className="card construction-running" role="status" aria-live="polite">
      <div className="construction-running-heading">
        {active && <span className="spinner" aria-hidden="true" />}
        <div>
          <h2>{failed ? 'サーバー構築が途中で停止しました' : demo ? `${game === 'ASA' ? 'ARK: Survival Ascended' : 'Palworld'}デモサーバーを作成しています` : `${game === 'ASA' ? 'ARK: Survival Ascended' : 'Palworld'}サーバーを構築しています`}</h2>
          <p>{failed ? '完了した工程と失敗した工程を確認してください。' : '処理は継続中です。完了するまでこの画面を閉じないでください。'}</p>
        </div>
      </div>
      <p><strong>経過時間:</strong> {formatElapsed(elapsedSeconds)}</p>
      <ol className="pending-step-list">
        {steps.map((step) => <li key={step.id} className={`progress-${step.status.toLowerCase()}`}>
          <span className="progress-icon" aria-hidden="true">{icon(step.status)}</span>
          <span><strong>{step.label}</strong>{step.message && <small>{step.message}</small>}</span>
        </li>)}
      </ol>
      {!demo && <p className="notice">通信速度やSteam側の応答によっては数分以上かかります。</p>}
    </section>
  )
}

function icon(status: ConstructionProgressStepStatus): string {
  return { PENDING: '○', RUNNING: '↻', COMPLETED: '✓', ERROR: '✕' }[status]
}

function formatElapsed(seconds: number): string {
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return `${minutes}分${remainingSeconds.toString().padStart(2, '0')}秒`
}
