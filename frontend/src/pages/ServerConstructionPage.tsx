import { useState } from 'react'
import { Link } from 'react-router-dom'
import { runServerPreflight } from '../api/serverPreflight'
import { createServerConstructionPlan } from '../api/serverConstructionPlans'
import { ConstructionPlan } from '../components/ConstructionPlan'
import { ServerConstructionForm } from '../components/ServerConstructionForm'
import type {
  NewServerRequest,
  ServerConstructionPlan as ServerConstructionPlanType,
  ServerPreflightReport,
  ValidationErrors,
} from '../types/serverConstruction'
import '../styles/serverConstruction.css'

const initialForm: NewServerRequest = {
  serverName: '',
  installPath: 'C:\\GameServers\\Palworld',
  steamCmdPath: 'C:\\GameServers\\SteamCMD',
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
      setForm((current) => ({
        ...current,
        serverPassword: '',
        adminPassword: '',
      }))
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
    const result = await runServerPreflight(plan)
    if (result.ok) {
      setPreflight(result.report)
    } else {
      setPreflightError(result.message)
    }
    setIsChecking(false)
  }

  const returnToForm = () => {
    setPlan(null)
    setPreflight(null)
    setPreflightError(null)
  }

  return (
    <main>
      <Link className="back-link" to="/servers/new">
        ゲーム選択へ戻る
      </Link>

      {plan ? (
        <ConstructionPlan
          plan={plan}
          preflight={preflight}
          preflightError={preflightError}
          isChecking={isChecking}
          onCheckEnvironment={checkEnvironment}
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
