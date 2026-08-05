import type {
  ServerConstructionPlan,
  ServerPreflightReport,
} from '../types/serverConstruction'

type PreflightResult =
  | { ok: true; report: ServerPreflightReport }
  | { ok: false; message: string }

export async function runServerPreflight(
  plan: ServerConstructionPlan,
): Promise<PreflightResult> {
  try {
    const response = await fetch('/api/server-construction-preflight', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        installPath: plan.installPath,
        steamCmdPath: plan.steamCmdPath,
        gamePort: plan.gamePort,
        rconPort: plan.rconPort,
      }),
    })

    if (response.ok) {
      return { ok: true, report: await response.json() }
    }

    return { ok: false, message: '事前検証を実行できませんでした' }
  } catch {
    return { ok: false, message: 'バックエンドへ接続できません' }
  }
}

