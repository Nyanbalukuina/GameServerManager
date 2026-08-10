import { apiFetch } from './http'
import type { StorageConfiguration } from '../types/storageConfiguration'

export async function getStorageConfiguration(): Promise<StorageConfiguration> {
  const response = await apiFetch('/api/configuration/storage')
  if (!response.ok) {
    throw new Error('保存先の設定を取得できませんでした')
  }
  return await response.json() as StorageConfiguration
}
