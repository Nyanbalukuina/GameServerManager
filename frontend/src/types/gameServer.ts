export type GameServerRegistration = {
  game: 'PALWORLD' | 'ARK' | 'MINECRAFT'
  serverId: string
  mode: 'DEMO' | 'REAL'
  state: 'STOPPED' | 'RUNNING'
  serverName: string
  installPath: string
  workspacePath: string
  gamePort: number
  rconPort: number
  createdAt: string
}
