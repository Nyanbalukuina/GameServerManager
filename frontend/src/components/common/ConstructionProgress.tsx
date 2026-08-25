import type { DemoConstructionReport, ServerConstructionReport } from '../../types/serverOperations'

type ConstructionProgressProps = {
  report: DemoConstructionReport | ServerConstructionReport
  game?: 'Palworld' | 'ARK: Survival Ascended'
}

export function ConstructionProgress({ report, game = 'Palworld' }: ConstructionProgressProps) {
  const demo = report.mode === 'DEMO'
  return (
    <section className="card">
      <h2>{demo ? `${game}デモサーバー作成結果` : `${game}サーバー構築結果`}</h2>
      <p className={report.completed ? 'summary pass' : 'summary error-status'}>
        {report.completed
          ? demo ? `${game}デモサーバーを作成しました` : `${game}サーバーを構築して起動しました`
          : demo ? `${game}デモサーバー作成に失敗しました` : `${game}サーバー構築に失敗しました`}
      </p>
      <ol className="step-list">
        {report.steps.map((step) => (
          <li key={step.id} className={`step ${step.status.toLowerCase()}`}>
            <strong>{step.label}</strong>
            <span>{step.message}</span>
          </li>
        ))}
      </ol>
      <p className="workspace-path">
        <strong>{demo ? '一時データ:' : 'インストール先:'}</strong>{' '}
        {demo ? report.workspacePath : report.installPath}
      </p>
    </section>
  )
}
