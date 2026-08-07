import type {
  PreflightStatus,
  ServerPreflightReport,
} from '../types/serverConstruction'

type PreflightResultsProps = {
  report: ServerPreflightReport
}

export function PreflightResults({ report }: PreflightResultsProps) {
  return (
    <div className="preflight-results">
      <p className={report.canProceed ? 'summary pass' : 'summary error-status'}>
        {report.canProceed
          ? '構築を進められる環境です'
          : '修正が必要な項目があります'}
      </p>
      <ul className="check-list">
        {report.checks.map((check) => (
          <li key={check.id} className={`check ${check.status.toLowerCase()}`}>
            <span className="status">{statusLabel(check.status)}</span>
            <span>
              <strong>{check.label}</strong>
              <small>{check.message}</small>
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}

function statusLabel(status: PreflightStatus) {
  switch (status) {
    case 'PASS':
      return '成功'
    case 'WARNING':
      return '警告'
    case 'ERROR':
      return 'エラー'
  }
}
