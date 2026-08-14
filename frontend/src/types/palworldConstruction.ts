export type NewServerRequest = {
  serverName: string
  installPath: string
  steamCmdPath: string
  gamePort: string
  rconPort: string
  maxPlayers: string
  serverPassword: string
  adminPassword: string
  automationEnabled: boolean
  shutdownTime: string
  startupTime: string
  backupAfterShutdown: boolean
  allowLocalSubnet: boolean
  allowTailscale: boolean
  customRemoteAddresses: string
  allowAnyRemoteAddress: boolean
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
  automationEnabled: boolean
  shutdownTime: string
  startupTime: string
  backupAfterShutdown: boolean
  backupRetentionCount: number
  gamePortAccess: {
    localSubnet: boolean
    tailscale: boolean
    customRemoteAddresses: string[]
    allowAny: boolean
  }
}

export type ValidationErrors = Partial<Record<keyof NewServerRequest | 'request', string>>
