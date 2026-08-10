import type { DemoConstructionReport, ServerConstructionReport } from '../../types/serverOperations'

type ConstructionProgressProps = {
  report: DemoConstructionReport | ServerConstructionReport
}

export function ConstructionProgress({ report }: ConstructionProgressProps) {
  const demo = report.mode === 'DEMO'
  return (
    <section className="card">
      <h2>{demo ? 'デモサーバー作成結果' : 'Palworldサーバー構築結果'}</h2>
      <p className={report.completed ? 'summary pass' : 'summary error-status'}>
        {report.completed
          ? demo ? 'デモサーバーを作成しました' : 'Palworldサーバーを構築して起動しました'
          : demo ? 'デモサーバー作成に失敗しました' : 'Palworldサーバー構築に失敗しました'}
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
