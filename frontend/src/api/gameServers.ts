import { apiFetch } from './http'
import type { GameServerRegistration } from '../types/gameServer'

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
