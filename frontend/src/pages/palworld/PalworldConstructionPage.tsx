import { useEffect, useState } from 'react'
import { runDemoServerConstruction } from '../../api/palworldDemoConstruction'
import { runServerPreflight } from '../../api/serverPreflight'
import { createServerConstructionPlan } from '../../api/palworldConstructionPlans'
import { getStorageConfiguration } from '../../api/storageConfiguration'
import { getGameServers } from '../../api/gameServers'
import { constructPalworldServer } from '../../api/palworldConstruction'
import { AppLink } from '../../components/common/AppLink'
import { PalworldConstructionPlan } from '../../components/palworld/PalworldConstructionPlan'
import { PalworldConstructionForm } from '../../components/palworld/PalworldConstructionForm'
import type {
  NewServerRequest,
  ServerConstructionPlan as ServerConstructionPlanType,
  ValidationErrors,
} from '../../types/palworldConstruction'
import type {
  DemoConstructionReport,
  ServerConstructionReport,
  ServerPreflightReport,
} from '../../types/serverOperations'
import '../../styles/serverConstruction.css'

const initialForm: NewServerRequest = {
  serverName: '',
  installPath: '',
  steamCmdPath: '',
  gamePort: '8211',
  rconPort: '25575',
  maxPlayers: '3',
  serverPassword: '',
  adminPassword: '',
  automationEnabled: false,
  shutdownTime: '04:00',
  startupTime: '09:00',
  allowLocalSubnet: true,
  allowTailscale: true,
  customRemoteAddresses: '',
  allowAnyRemoteAddress: false,
  serverDescription: '',
  expRate: 1,
  palCaptureRate: 1,
  palSpawnRate: 1,
  enemyDropRate: 1,
  eggHatchingTime: 2,
  deathPenalty: 'All',
  pvpEnabled: false,
  friendlyFireEnabled: false,
  baseCampMaxNum: 128,
  baseCampWorkerMaxNum: 15,
}

export function PalworldConstructionPage({ demoEnabled }: { demoEnabled: boolean }) {
  const [form               , setForm               ] = useState(initialForm)
  const [storageError       , setStorageError       ] = useState<string | null>(null)
  const [plan               , setPlan               ] = useState<ServerConstructionPlanType | null>(null)
  const [preflight          , setPreflight          ] = useState<ServerPreflightReport | null>(null)
  const [errors             , setErrors             ] = useState<ValidationErrors>({})
  const [isSubmitting       , setIsSubmitting       ] = useState(false)
  const [isConstructing     , setIsConstructing     ] = useState(false)
  const [constructionKind   , setConstructionKind   ] = useState<'REAL' | 'DEMO' | null>(null)
  const [constructionReport , setConstructionReport ] = useState<DemoConstructionReport | null>(null)
  const [constructionError  , setConstructionError  ] = useState<string | null>(null)
  const [realReport         , setRealReport          ] = useState<ServerConstructionReport | null>(null)
  const [alreadyCreated     , setAlreadyCreated     ] = useState<boolean | null>(null)

  useEffect(() => {
    const loadStorageConfiguration = async () => {
      try {
        const configuration = await getStorageConfiguration()
        setForm((current) => ({
          ...current,
          installPath: configuration.palworldInstallPath,
          steamCmdPath: configuration.steamCmdPath,
        }))
      } catch (error) {
        setStorageError(error instanceof Error ? error.message : '保存先の設定を取得できませんでした')
      }
    }
    void loadStorageConfiguration()
    void getGameServers().then((servers) => {
      setAlreadyCreated(servers.some((server) => server.game === 'PALWORLD'))
    }).catch((error: unknown) => {
      setStorageError(error instanceof Error ? error.message : 'サーバー登録を取得できませんでした')
    })
  }, [])

  useEffect(() => {
    if (!isConstructing) return
    const warnBeforeLeaving = (event: BeforeUnloadEvent) => event.preventDefault()
    window.addEventListener('beforeunload', warnBeforeLeaving)
    return () => window.removeEventListener('beforeunload', warnBeforeLeaving)
  }, [isConstructing])

  const updateField = (field: keyof NewServerRequest, value: string | number | boolean) => {
    setForm((current) => ({ ...current, [field]: value }))
    setErrors((current) => ({ ...current, [field]: undefined }))
  }

  const submit = async () => {
    setIsSubmitting(true)
    setErrors({})

    const result = await createServerConstructionPlan(form)
    if (result.ok) {
      const preflightResult = await runServerPreflight(result.plan)
      if (preflightResult.ok) {
        setPreflight(preflightResult.report)
        setPlan(result.plan)
      } else {
        setErrors({ request: preflightResult.message })
      }
    } else {
      setErrors(result.errors)
    }

    setIsSubmitting(false)
  }

  const runDemoConstruction = async () => {
    setIsConstructing(true)
    setConstructionKind('DEMO')
    setConstructionError(null)

    const result = await runDemoServerConstruction(form)
    if (result.ok) {
      setConstructionReport(result.report)
    } else {
      setConstructionError(result.errors.request ?? 'デモ構築を実行できませんでした')
    }

    setIsConstructing(false)
    setConstructionKind(null)
  }

  const runRealConstruction = async () => {
    setIsConstructing(true)
    setConstructionKind('REAL')
    setConstructionError(null)
    const result = await constructPalworldServer(form)
    if (result.ok) {
      setRealReport(result.report)
    } else {
      setConstructionError(result.errors.request ?? 'Palworldサーバーを構築できませんでした')
    }
    setIsConstructing(false)
  }

  const returnToForm = () => {
    setPlan(null)
    setPreflight(null)
    setConstructionReport(null)
    setRealReport(null)
    setConstructionError(null)
  }

  return (
    <main className="compact-page">
      <AppLink className="back-link" href="/servers/new">
        ゲーム選択へ戻る
      </AppLink>

      {alreadyCreated === null ? (
        <p>読み込み中...</p>
      ) : alreadyCreated ? (
        <section className="card">
          <h1>Palworldサーバーは作成済みです</h1>
          <p>1タイトルにつき作成できるサーバーは1つです。</p>
          <AppLink className="management-link" href="/servers/palworld">Palworld管理画面を開く</AppLink>
        </section>
      ) : plan ? (
        <PalworldConstructionPlan
          plan={plan}
          preflight={preflight}
          isConstructing={isConstructing}
          constructionKind={constructionKind}
          constructionReport={constructionReport}
          realReport={realReport}
          constructionError={constructionError}
          onRunDemoConstruction={runDemoConstruction}
          onRunRealConstruction={runRealConstruction}
          onReturnToForm={returnToForm}
          demoEnabled={demoEnabled}
          serverPassword={form.serverPassword}
          adminPassword={form.adminPassword}
          worldSettings={form}
        />
      ) : (
        <>
          {storageError && <p role="alert">{storageError}</p>}
          <PalworldConstructionForm
            form={form}
            errors={errors}
            isSubmitting={isSubmitting}
            onFieldChange={updateField}
            onSubmit={submit}
          />
        </>
      )}
    </main>
  )
}
