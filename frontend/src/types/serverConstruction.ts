export type NewServerRequest = {
  serverName: string
  installPath: string
  steamCmdPath: string
  gamePort: string
  rconPort: string
  maxPlayers: string
  serverPassword: string
  adminPassword: string
}

export type ServerConstructionPlan = {
  serverName: string
  installPath: string
  steamCmdPath: string
  gamePort: number
  rconPort: number
  maxPlayers: number
  serverPasswordConfigured: boolean
  adminPasswordConfigured: boolean
}

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

export type ValidationErrors = Partial<Record<keyof NewServerRequest | 'request', string>>
