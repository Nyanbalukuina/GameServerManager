import type { GameServerRegistration } from '../types/gameServer'
import type { AsaServerStatus } from '../types/asaServer'
import type { AsaGameplaySettings } from '../types/asaGameplaySettings'
import { apiFetch } from './http'

export async function getAsaServer(): Promise<GameServerRegistration> {
  return request<GameServerRegistration>('/api/asa/server')
}

export async function getAsaServerStatus(): Promise<AsaServerStatus> {
  return request<AsaServerStatus>('/api/asa/server/status')
}

export async function startAsaServer(): Promise<AsaServerStatus> {
  return request<AsaServerStatus>('/api/asa/server/start', { method: 'POST' })
}

export async function stopAsaServer(adminPassword: string): Promise<AsaServerStatus> {
  return operate('/api/asa/server/stop', adminPassword)
}

export async function restartAsaServer(adminPassword: string): Promise<AsaServerStatus> {
  return operate('/api/asa/server/restart', adminPassword)
}

export async function deleteAsaServer(confirmation: string): Promise<void> {
  const response = await apiFetch(`/api/asa/server?confirmation=${encodeURIComponent(confirmation)}`, { method: 'DELETE' })
  if (!response.ok) {
    const body = await response.json().catch(() => ({})) as { message?: string; errors?: Record<string, string> }
    throw new Error(Object.values(body.errors ?? {})[0] ?? body.message ?? 'ARK: Survival Ascendedサーバーを削除できませんでした')
  }
}

export function getAsaGameplaySettings(): Promise<AsaGameplaySettings> {
  return request<AsaGameplaySettings>('/api/asa/server/gameplay-settings')
}

export function updateAsaGameplaySettings(settings: AsaGameplaySettings): Promise<AsaGameplaySettings> {
  return request<AsaGameplaySettings>('/api/asa/server/gameplay-settings', {
    method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(settings),
  })
}

async function operate(path: string, adminPassword: string): Promise<AsaServerStatus> {
  return request<AsaServerStatus>(path, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ adminPassword }),
  })
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await apiFetch(path, init)
  if (!response.ok) {
    const body = await response.json().catch(() => ({})) as { message?: string; errors?: Record<string, string> }
    throw new Error(Object.values(body.errors ?? {})[0] ?? body.message ?? 'ARK: Survival Ascendedサーバーを操作できませんでした')
  }
  return await response.json() as T
}
