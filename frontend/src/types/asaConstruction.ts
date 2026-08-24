import type { ServerConstructionReport } from './serverOperations'

export type AsaConstructionRequest = {
  serverName: string
  installPath: string
  steamCmdPath: string
  map: 'TheIsland_WP'
  gamePort: string
  queryPort: string
  rconPort: string
  maxPlayers: string
  serverPassword: string
  adminPassword: string
  allowLocalSubnet: boolean
  allowTailscale: boolean
  customRemoteAddresses: string
  allowAnyRemoteAddress: boolean
}

export type AsaConstructionErrors = Partial<Record<keyof AsaConstructionRequest | 'request', string>>

export type AsaConstructionResult =
  | { ok: true; report: ServerConstructionReport }
  | { ok: false; errors: AsaConstructionErrors }
