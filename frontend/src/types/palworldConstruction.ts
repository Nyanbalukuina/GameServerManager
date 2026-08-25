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
  allowLocalSubnet: boolean
  allowTailscale: boolean
  customRemoteAddresses: string
  allowAnyRemoteAddress: boolean
  serverDescription: string
  expRate: number
  palCaptureRate: number
  palSpawnRate: number
  enemyDropRate: number
  eggHatchingTime: number
  deathPenalty: 'None' | 'Item' | 'ItemAndEquipment' | 'All'
  pvpEnabled: boolean
  friendlyFireEnabled: boolean
  baseCampMaxNum: number
  baseCampWorkerMaxNum: number
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
  gamePortAccess: {
    localSubnet: boolean
    tailscale: boolean
    customRemoteAddresses: string[]
    allowAny: boolean
  }
}

export type ValidationErrors = Partial<Record<keyof NewServerRequest | 'request', string>>
