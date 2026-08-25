import type { AsaConstructionRequest, AsaConstructionResult } from '../types/asaConstruction'
import type { DemoConstructionReport, ServerPreflightReport } from '../types/serverOperations'
import { apiFetch } from './http'

export async function constructAsaServer(request: AsaConstructionRequest): Promise<AsaConstructionResult> {
  try {
    const response = await apiFetch('/api/asa/constructions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        ...request,
        gamePort: toNumber(request.gamePort),
        queryPort: toNumber(request.queryPort),
        rconPort: toNumber(request.rconPort),
        maxPlayers: toNumber(request.maxPlayers),
        xpMultiplier: toDecimal(request.xpMultiplier),
        tamingSpeedMultiplier: toDecimal(request.tamingSpeedMultiplier),
        harvestAmountMultiplier: toDecimal(request.harvestAmountMultiplier),
        eggHatchSpeedMultiplier: toDecimal(request.eggHatchSpeedMultiplier),
        babyMatureSpeedMultiplier: toDecimal(request.babyMatureSpeedMultiplier),
      }),
    })

    if (response.ok) return { ok: true, report: await response.json() }
    if (response.status === 400 || response.status === 409) {
      const body = await response.json()
      return { ok: false, errors: body.errors ?? { request: body.message ?? '入力内容を確認してください' } }
    }
    return { ok: false, errors: { request: 'ARK: Survival Ascendedサーバーを構築できませんでした' } }
  } catch {
    return { ok: false, errors: { request: 'バックエンドへ接続できません' } }
  }
}

export async function runAsaPreflight(request: AsaConstructionRequest): Promise<ServerPreflightReport> {
  const response = await apiFetch('/api/asa/preflight', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(numericRequest(request)),
  })
  if (!response.ok) throw new Error('ARK: Survival Ascendedの事前検証を実行できませんでした')
  return await response.json() as ServerPreflightReport
}

export async function constructDemoAsaServer(request: AsaConstructionRequest): Promise<DemoConstructionReport> {
  const response = await apiFetch('/api/asa/constructions/demo', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(numericRequest(request)),
  })
  if (!response.ok) {
    const body = await response.json().catch(() => ({})) as { message?: string; errors?: Record<string, string> }
    throw new Error(Object.values(body.errors ?? {})[0] ?? body.message ?? 'ARK: Survival Ascendedデモサーバーを構築できませんでした')
  }
  return await response.json() as DemoConstructionReport
}

function numericRequest(request: AsaConstructionRequest) {
  return {
    ...request, gamePort: toNumber(request.gamePort), queryPort: toNumber(request.queryPort),
    rconPort: toNumber(request.rconPort), maxPlayers: toNumber(request.maxPlayers),
    xpMultiplier: toDecimal(request.xpMultiplier),
    tamingSpeedMultiplier: toDecimal(request.tamingSpeedMultiplier),
    harvestAmountMultiplier: toDecimal(request.harvestAmountMultiplier),
    eggHatchSpeedMultiplier: toDecimal(request.eggHatchSpeedMultiplier),
    babyMatureSpeedMultiplier: toDecimal(request.babyMatureSpeedMultiplier),
  }
}

function toDecimal(value: string): number | null {
  if (value.trim() === '') return null
  const number = Number(value)
  return Number.isFinite(number) ? number : null
}

function toNumber(value: string): number | null {
  if (value.trim() === '') return null
  const number = Number(value)
  return Number.isInteger(number) ? number : null
}
