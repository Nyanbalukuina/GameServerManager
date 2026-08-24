import { apiFetch } from './http'
import type { GameServerRegistration, PalworldSettings, UpdatePalworldSettings } from '../types/gameServer'

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
