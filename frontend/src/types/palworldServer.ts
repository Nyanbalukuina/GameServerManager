export type PalworldServerStatus = {
  state: 'NOT_STARTED' | 'STARTING' | 'RUNNING' | 'STOPPED' | 'FAILED'
  processId: number | null
  gamePort: number | null
  logPath: string | null
  message: string
}
