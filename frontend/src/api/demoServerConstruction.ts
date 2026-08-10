import type {
  DemoConstructionReport,
  NewServerRequest,
  ValidationErrors,
} from '../types/serverConstruction'

type DemoConstructionResult =
  | { ok: true; report: DemoConstructionReport }
  | { ok: false; errors: ValidationErrors }

export async function runDemoServerConstruction(
  request: NewServerRequest,
): Promise<DemoConstructionResult> {
  try {
    const response = await fetch('/api/server-constructions/demo', {
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
      return { ok: true, report: await response.json() }
    }

    if (response.status === 400) {
      return { ok: false, errors: (await response.json()).errors }
    }

    return {
      ok: false,
      errors: { request: 'デモ構築を実行できませんでした' },
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
