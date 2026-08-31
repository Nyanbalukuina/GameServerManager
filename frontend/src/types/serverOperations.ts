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

export type ConstructionProgressStepStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'ERROR'

export type ConstructionProgressSnapshot = {
  game: 'ASA' | 'PALWORLD'
  status: 'RUNNING' | 'COMPLETED' | 'ERROR'
  steps: Array<{
    id: string
    label: string
    status: ConstructionProgressStepStatus
    message: string
  }>
  updatedAt: string
}

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

export type ServerConstructionReport = {
  completed: boolean
  mode: 'REAL'
  installPath: string
  steps: ConstructionStep[]
}
