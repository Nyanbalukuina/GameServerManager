import { useEffect, useState } from 'react'
import { AppLink } from '../components/common/AppLink'
import { ConstructionInProgress } from '../components/common/ConstructionInProgress'
import { ConstructionProgress } from '../components/common/ConstructionProgress'
import type { ConstructionExecution, ConstructionExecutionResult } from '../types/constructionExecution'

export function ConstructionExecutionPage({ execution }: { execution: ConstructionExecution | null }) {
  const [result, setResult] = useState<ConstructionExecutionResult | null>(null)

  useEffect(() => {
    if (!execution) return
    let active = true
    void execution.result.then((latest) => {
      if (active) setResult(latest)
    })
    return () => { active = false }
  }, [execution])

  useEffect(() => {
    if (!execution || result !== null) return
    const warn = (event: BeforeUnloadEvent) => event.preventDefault()
    window.addEventListener('beforeunload', warn)
    return () => window.removeEventListener('beforeunload', warn)
  }, [execution, result])

  if (!execution) {
    return <main className="compact-page"><section className="card"><h1>構築情報がありません</h1><p>構築内容の確認画面からもう一度開始してください。</p><AppLink className="management-link" href="/servers/new">ゲーム選択へ戻る</AppLink></section></main>
  }

  const gameName = execution.game === 'ASA' ? 'ARK: Survival Ascended' : 'Palworld'
  const managementPath = execution.game === 'ASA' ? '/servers/asa' : '/servers/palworld'
  return (
    <main className="compact-page construction-execution-page">
      <header className="page-header"><p className="eyebrow">{gameName}</p><h1>サーバー構築</h1><p>構築工程と現在の状態を確認できます。</p></header>
      {result === null && <ConstructionInProgress demo={execution.demo} game={execution.game} />}
      {result?.ok === false && <>
        <ConstructionInProgress demo={execution.demo} game={execution.game} active={false} />
        <p className="error request-error" role="alert">{result.message}</p>
        <AppLink className="management-link" href={execution.game === 'ASA' ? '/servers/new/asa' : '/servers/new/palworld'}>構築内容を見直す</AppLink>
      </>}
      {result?.ok === true && <><ConstructionProgress report={result.report} game={gameName} /><AppLink className="management-link" href={managementPath}>{execution.game === 'ASA' ? 'ARK' : 'Palworld'}管理画面を開く</AppLink></>}
    </main>
  )
}
