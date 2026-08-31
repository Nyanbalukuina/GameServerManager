import { apiFetch } from './http'
import type { GameServerRegistration, PalworldSettings, UpdatePalworldSettings } from '../types/gameServer'
import type { PalworldServerVersion } from '../types/palworldServerVersion'
import type { PalworldServerStatus } from '../types/palworldServer'
import type { InstalledSteamServerVersion } from '../types/installedSteamServerVersion'

export async function getGameServers(): Promise<GameServerRegistration[]> {
  const response = await apiFetch('/api/servers')
  if (!response.ok) throw new Error('サーバー登録を取得できませんでした')
  return await response.json() as GameServerRegistration[]
}

export async function getPalworldServer(): Promise<GameServerRegistration> {
  const response = await apiFetch('/api/servers/palworld')
  if (!response.ok) throw new Error('Palworldサーバーを取得できませんでした')
  return await response.json() as GameServerRegistration
}

export async function getPalworldServerStatus(): Promise<PalworldServerStatus> {
  const response = await apiFetch('/api/palworld/server/status')
  if (!response.ok) throw new Error('Palworldサーバーの状態を取得できませんでした')
  return await response.json() as PalworldServerStatus
}

export async function operateDemoPalworldServer(
  action: 'START' | 'STOP' | 'RESTART',
): Promise<GameServerRegistration> {
  const response = await apiFetch('/api/servers/palworld/demo-operation', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ action }),
  })
  if (!response.ok) throw new Error('デモサーバーを操作できませんでした')
  return await response.json() as GameServerRegistration
}

export async function getDemoPalworldSettings(): Promise<PalworldSettings> {
  const response = await apiFetch('/api/servers/palworld/demo-settings')
  if (!response.ok) throw new Error('Palworld設定を取得できませんでした')
  return await response.json() as PalworldSettings
}

export async function updateDemoPalworldSettings(settings: UpdatePalworldSettings): Promise<PalworldSettings> {
  const response = await apiFetch('/api/servers/palworld/demo-settings', {
    method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(settings),
  })
  if (!response.ok) {
    const body = await response.json() as { errors?: Record<string, string> }
    throw new Error(Object.values(body.errors ?? {})[0] ?? 'Palworld設定を保存できませんでした')
  }
  return await response.json() as PalworldSettings
}

export async function deleteDemoPalworldServer(confirmation: string): Promise<void> {
  const response = await apiFetch(
    `/api/servers/palworld/demo?confirmation=${encodeURIComponent(confirmation)}`,
    { method: 'DELETE' },
  )
  if (!response.ok) {
    const body = await response.json() as { errors?: { request?: string } }
    throw new Error(body.errors?.request ?? 'デモサーバーを削除できませんでした')
  }
}

export async function checkPalworldServerVersion(): Promise<PalworldServerVersion> {
  return requestPalworldVersion('/api/palworld/server/version/check')
}

export async function getInstalledPalworldServerVersion(): Promise<InstalledSteamServerVersion> {
  const response = await apiFetch('/api/palworld/server/version/current')
  if (!response.ok) throw new Error('現在のPalworldサーバーバージョンを取得できませんでした')
  return await response.json() as InstalledSteamServerVersion
}

export async function updatePalworldServerVersion(): Promise<PalworldServerVersion> {
  return requestPalworldVersion('/api/palworld/server/version/update')
}

async function requestPalworldVersion(path: string): Promise<PalworldServerVersion> {
  const response = await apiFetch(path, { method: 'POST' })
  if (!response.ok) {
    const body = await response.json().catch(() => ({})) as { message?: string; errors?: Record<string, string> }
    throw new Error(Object.values(body.errors ?? {})[0] ?? body.message ?? 'Palworldサーバーバージョンを操作できませんでした')
  }
  return await response.json() as PalworldServerVersion
}
