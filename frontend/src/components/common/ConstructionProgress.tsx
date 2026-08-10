import type { DemoConstructionReport } from '../../types/serverOperations'

type ConstructionProgressProps = {
  report: DemoConstructionReport
}

export function ConstructionProgress({ report }: ConstructionProgressProps) {
  return (
    <section className="card">
      <h2>デモサーバー作成結果</h2>
      <p className={report.completed ? 'summary pass' : 'summary error-status'}>
        {report.completed ? 'デモサーバーを作成しました' : 'デモサーバー作成に失敗しました'}
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
        <strong>一時データ:</strong> {report.workspacePath}
      </p>
    </section>
  )
}
