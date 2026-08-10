import type { DemoConstructionReport } from '../types/serverConstruction'

type ConstructionProgressProps = {
  report: DemoConstructionReport
}

export function ConstructionProgress({ report }: ConstructionProgressProps) {
  return (
    <section className="card">
      <h2>デモ構築結果</h2>
      <p className={report.completed ? 'summary pass' : 'summary error-status'}>
        {report.completed ? 'デモ構築が完了しました' : 'デモ構築に失敗しました'}
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
