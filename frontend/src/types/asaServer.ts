export type AsaServerStatus = {
  state: 'NOT_STARTED' | 'STARTING' | 'RUNNING' | 'STOPPED' | 'FAILED'
  processId: number | null
  gamePort: number | null
  peerPort: number | null
  queryPort: number | null
  logPath: string | null
  message: string
}
