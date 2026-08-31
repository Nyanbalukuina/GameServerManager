import type { DemoConstructionReport, ServerConstructionReport } from './serverOperations'

export type ConstructionExecutionResult =
  | { ok: true; report: DemoConstructionReport | ServerConstructionReport }
  | { ok: false; message: string }

export type ConstructionExecution = {
  game: 'ASA' | 'PALWORLD'
  demo: boolean
  result: Promise<ConstructionExecutionResult>
}
