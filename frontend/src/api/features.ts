import type { FeatureConfiguration } from '../types/features'
import { apiFetch } from './http'

export async function getFeatureConfiguration(): Promise<FeatureConfiguration> {
  const response = await apiFetch('/api/configuration/features')
  if (!response.ok) throw new Error('機能設定を取得できませんでした')
  return await response.json() as FeatureConfiguration
}
