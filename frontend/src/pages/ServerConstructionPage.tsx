import { useState } from 'react'
import { runDemoServerConstruction } from '../api/demoServerConstruction'
import { runServerPreflight } from '../api/serverPreflight'
import { createServerConstructionPlan } from '../api/serverConstructionPlans'
import { AppLink } from '../components/AppLink'
import { ConstructionPlan } from '../components/ConstructionPlan'
import { ServerConstructionForm } from '../components/ServerConstructionForm'
import type {
  NewServerRequest,
  DemoConstructionReport,
  ServerConstructionPlan as ServerConstructionPlanType,
  ServerPreflightReport,
  ValidationErrors,
} from '../types/serverConstruction'
import '../styles/serverConstruction.css'

const initialForm: NewServerRequest = {
  serverName: '',
  installPath: 'C:\\GameServerManager\\servers\\palworld\\main\\runtime',
  steamCmdPath: 'C:\\GameServerManager\\tools\\steamcmd',
  gamePort: '8211',
  rconPort: '25575',
  maxPlayers: '3',
  serverPassword: '',
  adminPassword: '',
}

export function ServerConstructionPage() {
  const [form, setForm] = useState(initialForm)
  const [plan, setPlan] = useState<ServerConstructionPlanType | null>(null)
  const [preflight, setPreflight] = useState<ServerPreflightReport | null>(null)
  const [preflightError, setPreflightError] = useState<string | null>(null)
  const [errors, setErrors] = useState<ValidationErrors>({})
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isChecking, setIsChecking] = useState(false)
  const [isConstructing, setIsConstructing] = useState(false)
  const [constructionReport, setConstructionReport] = useState<DemoConstructionReport | null>(null)
  const [constructionError, setConstructionError] = useState<string | null>(null)

  const updateField = (field: keyof NewServerRequest, value: string) => {
    setForm((current) => ({ ...current, [field]: value }))
    setErrors((current) => ({ ...current, [field]: undefined }))
  }

  const submit = async () => {
    setIsSubmitting(true)
    setErrors({})

    const result = await createServerConstructionPlan(form)
    if (result.ok) {
      setPlan(result.plan)
    } else {
      setErrors(result.errors)
    }

    setIsSubmitting(false)
  }

  const checkEnvironment = async () => {
    if (!plan) {
      return
    }

    setIsChecking(true)
    setPreflightError(null)
    setConstructionReport(null)
    setConstructionError(null)
    const result = await runServerPreflight(plan)
    if (result.ok) {
      setPreflight(result.report)
    } else {
      setPreflightError(result.message)
    }
    setIsChecking(false)
  }

  const runDemoConstruction = async () => {
    setIsConstructing(true)
    setConstructionError(null)

    const result = await runDemoServerConstruction(form)
    if (result.ok) {
      setConstructionReport(result.report)
      setForm((current) => ({
        ...current,
        serverPassword: '',
        adminPassword: '',
      }))
    } else {
      setConstructionError(result.errors.request ?? 'デモ構築を実行できませんでした')
    }

    setIsConstructing(false)
  }

  const returnToForm = () => {
    setPlan(null)
    setPreflight(null)
    setPreflightError(null)
    setConstructionReport(null)
    setConstructionError(null)
  }

  return (
    <main>
      <AppLink className="back-link" href="/servers/new">
        ゲーム選択へ戻る
      </AppLink>

      {plan ? (
        <ConstructionPlan
          plan={plan}
          preflight={preflight}
          preflightError={preflightError}
          isChecking={isChecking}
          isConstructing={isConstructing}
          constructionReport={constructionReport}
          constructionError={constructionError}
          onCheckEnvironment={checkEnvironment}
          onRunDemoConstruction={runDemoConstruction}
          onReturnToForm={returnToForm}
        />
      ) : (
        <ServerConstructionForm
          form={form}
          errors={errors}
          isSubmitting={isSubmitting}
          onFieldChange={updateField}
          onSubmit={submit}
        />
      )}
    </main>
  )
}
