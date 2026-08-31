import type { ConstructionProgressSnapshot } from '../types/serverOperations'
import { apiFetch } from './http'

export async function getConstructionProgress(game: 'ASA' | 'PALWORLD'): Promise<ConstructionProgressSnapshot | null> {
  const path = game === 'ASA' ? '/api/asa/constructions/progress' : '/api/palworld/constructions/progress'
  const response = await apiFetch(path)
  if (!response.ok) throw new Error('構築状況を取得できませんでした')
  if (response.status === 204) return null
  const text = await response.text()
  return text ? JSON.parse(text) as ConstructionProgressSnapshot : null
}
