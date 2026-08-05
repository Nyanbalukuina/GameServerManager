import { useState, type FormEvent } from 'react'
import { createServerConstructionPlan } from './api/serverConstructionPlans'
import type {
  NewServerRequest,
  ServerConstructionPlan,
  ValidationErrors,
} from './types/serverConstruction'
import './App.css'

const initialForm: NewServerRequest = {
  serverName: '',
  installPath: '',
  steamCmdPath: '',
  gamePort: '8211',
  rconPort: '25575',
  maxPlayers: '3',
  serverPassword: '',
  adminPassword: '',
}

function App() {
  const [form, setForm] = useState(initialForm)
  const [plan, setPlan] = useState<ServerConstructionPlan | null>(null)
  const [errors, setErrors] = useState<ValidationErrors>({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  const updateField = (field: keyof NewServerRequest, value: string) => {
    setForm((current) => ({ ...current, [field]: value }))
    setErrors((current) => ({ ...current, [field]: undefined }))
  }

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
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

  if (plan) {
    return (
      <main>
        <header>
          <p className="eyebrow">Game Server Manager</p>
          <h1>構築計画</h1>
          <p>以下は確認用の計画です。まだインストールやファイル作成は行いません。</p>
        </header>

        <section className="card">
          <h2>{plan.serverName}</h2>
          <dl>
            <dt>ゲーム</dt>
            <dd>Palworld</dd>
            <dt>インストール先</dt>
            <dd>{plan.installPath}</dd>
            <dt>SteamCMDの保存先</dt>
            <dd>{plan.steamCmdPath}</dd>
            <dt>ゲームポート</dt>
            <dd>{plan.gamePort}</dd>
            <dt>RCONポート</dt>
            <dd>{plan.rconPort}</dd>
            <dt>最大プレイヤー数</dt>
            <dd>{plan.maxPlayers}</dd>
            <dt>サーバーパスワード</dt>
            <dd>{plan.serverPasswordConfigured ? '設定あり' : '設定なし'}</dd>
            <dt>管理者パスワード</dt>
            <dd>{plan.adminPasswordConfigured ? '設定あり' : '設定なし'}</dd>
          </dl>
        </section>

        <p className="notice">実際の構築処理は次のフェーズで実装します。</p>
        <button type="button" className="secondary" onClick={() => setPlan(null)}>
          入力画面へ戻る
        </button>
      </main>
    )
  }

  return (
    <main>
      <header>
        <p className="eyebrow">Game Server Manager</p>
        <h1>新規Palworldサーバー構築</h1>
        <p>入力内容を確認するだけで、まだインストールやファイル作成は行いません。</p>
      </header>

      <form onSubmit={submit}>
        <section className="card form-section">
          <h2>基本設定</h2>
          <Field
            id="serverName"
            label="サーバー名"
            value={form.serverName}
            error={errors.serverName}
            onChange={(value) => updateField('serverName', value)}
          />
          <Field
            id="installPath"
            label="インストール先"
            placeholder="C:\GameServers\Palworld"
            value={form.installPath}
            error={errors.installPath}
            onChange={(value) => updateField('installPath', value)}
          />
          <Field
            id="steamCmdPath"
            label="SteamCMDの保存先"
            placeholder="C:\GameServers\SteamCMD"
            value={form.steamCmdPath}
            error={errors.steamCmdPath}
            onChange={(value) => updateField('steamCmdPath', value)}
          />
        </section>

        <section className="card form-section">
          <h2>接続設定</h2>
          <Field
            id="gamePort"
            label="ゲームポート"
            type="number"
            value={form.gamePort}
            error={errors.gamePort}
            onChange={(value) => updateField('gamePort', value)}
          />
          <Field
            id="rconPort"
            label="RCONポート"
            type="number"
            value={form.rconPort}
            error={errors.rconPort}
            onChange={(value) => updateField('rconPort', value)}
          />
          <Field
            id="maxPlayers"
            label="最大プレイヤー数"
            type="number"
            value={form.maxPlayers}
            error={errors.maxPlayers}
            onChange={(value) => updateField('maxPlayers', value)}
          />
        </section>

        <section className="card form-section">
          <h2>パスワード</h2>
          <Field
            id="serverPassword"
            label="サーバーパスワード（任意）"
            type="password"
            value={form.serverPassword}
            error={errors.serverPassword}
            onChange={(value) => updateField('serverPassword', value)}
          />
          <Field
            id="adminPassword"
            label="管理者パスワード"
            type="password"
            value={form.adminPassword}
            error={errors.adminPassword}
            onChange={(value) => updateField('adminPassword', value)}
          />
        </section>

        {errors.request && <p className="error request-error">{errors.request}</p>}
        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? '確認中...' : '構築計画を確認'}
        </button>
      </form>
    </main>
  )
}

type FieldProps = {
  id: string
  label: string
  value: string
  error?: string
  type?: 'text' | 'number' | 'password'
  placeholder?: string
  onChange: (value: string) => void
}

function Field({
  id,
  label,
  value,
  error,
  type = 'text',
  placeholder,
  onChange,
}: FieldProps) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <input
        id={id}
        type={type}
        value={value}
        placeholder={placeholder}
        autoComplete={type === 'password' ? 'new-password' : undefined}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `${id}-error` : undefined}
        onChange={(event) => onChange(event.target.value)}
      />
      <p id={`${id}-error`} className="error">
        {error}
      </p>
    </div>
  )
}

export default App

