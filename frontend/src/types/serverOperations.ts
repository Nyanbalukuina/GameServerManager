export type PreflightStatus = 'PASS' | 'WARNING' | 'ERROR'

export type PreflightCheck = {
  id: string
  label: string
  status: PreflightStatus
  message: string
}

export type ServerPreflightReport = {
  canProceed: boolean
  checks: PreflightCheck[]
}

export type ConstructionStepStatus = 'COMPLETED' | 'ERROR'

export type ConstructionStep = {
  id: string
  label: string
  status: ConstructionStepStatus
  message: string
}

export type DemoConstructionReport = {
  completed: boolean
  mode: 'DEMO'
  workspacePath: string
  steps: ConstructionStep[]
}
