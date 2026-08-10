import type {
  NewServerRequest,
  ServerConstructionPlan,
  ValidationErrors,
} from '../types/palworldConstruction'
import { apiFetch } from './http'

type CreatePlanResult =
  | { ok: true; plan: ServerConstructionPlan }
  | { ok: false; errors: ValidationErrors }

export async function createServerConstructionPlan(
  request: NewServerRequest,
): Promise<CreatePlanResult> {
  try {
    const response = await apiFetch('/api/server-construction-plans', {
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
      return { ok: true, plan: await response.json() }
    }

    if (response.status === 400) {
      return { ok: false, errors: (await response.json()).errors }
    }

    return {
      ok: false,
      errors: { request: 'サーバーとの通信に失敗しました' },
    }
  } catch {
    return {
      ok: false,
      errors: { request: 'バックエンドへ接続できません' },
    }
  }
}

function toNumber(value: string): number | null {
  if (value.trim() === '') {
    return null
  }

  const number = Number(value)
  return Number.isFinite(number) ? number : null
}
