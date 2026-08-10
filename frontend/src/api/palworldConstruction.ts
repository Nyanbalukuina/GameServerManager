import type { NewServerRequest, ValidationErrors } from '../types/palworldConstruction'
import type { ServerConstructionReport } from '../types/serverOperations'
import { apiFetch } from './http'

type ConstructionResult =
  | { ok: true; report: ServerConstructionReport }
  | { ok: false; errors: ValidationErrors }

export async function constructPalworldServer(
  request: NewServerRequest,
): Promise<ConstructionResult> {
  try {
    const response = await apiFetch('/api/palworld/constructions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        ...request,
        gamePort: toNumber(request.gamePort),
        rconPort: toNumber(request.rconPort),
        maxPlayers: toNumber(request.maxPlayers),
      }),
    })

    if (response.ok) {
      return { ok: true, report: await response.json() as ServerConstructionReport }
    }

    if (response.status === 400 || response.status === 409) {
      return { ok: false, errors: (await response.json()).errors }
    }

    return { ok: false, errors: { request: 'Palworldサーバーを構築できませんでした' } }
  } catch {
    return { ok: false, errors: { request: 'バックエンドへ接続できません' } }
  }
}

function toNumber(value: string): number | null {
  if (value.trim() === '') return null
  const number = Number(value)
  return Number.isFinite(number) ? number : null
}
