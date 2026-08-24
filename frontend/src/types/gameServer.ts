export type GameServerRegistration = {
  game: 'PALWORLD' | 'ASA' | 'MINECRAFT'
  serverId: string
  mode: 'DEMO' | 'REAL'
  state: 'STOPPED' | 'RUNNING'
  serverName: string
  installPath: string
  workspacePath: string
  gamePort: number
  rconPort: number
  peerPort?: number
  queryPort?: number
  map?: string
  maxPlayers?: number
  createdAt: string
  gamePortAccess: {
    localSubnet: boolean
    tailscale: boolean
    customRemoteAddresses: string[]
    allowAny: boolean
  }
}

export type PalworldSettings = {
  serverName: string
  serverDescription: string
  maxPlayers: number
  serverPassword: string
  adminPassword: string
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

export type UpdatePalworldSettings = PalworldSettings
