import type { FormEvent } from 'react'
import type {
  NewServerRequest,
  ValidationErrors,
} from '../types/serverConstruction'

type ServerConstructionFormProps = {
  form: NewServerRequest
  errors: ValidationErrors
  isSubmitting: boolean
  onFieldChange: (field: keyof NewServerRequest, value: string) => void
  onSubmit: () => Promise<void>
}

export function ServerConstructionForm({
  form,
  errors,
  isSubmitting,
  onFieldChange,
  onSubmit,
}: ServerConstructionFormProps) {
  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    void onSubmit()
  }

  return (
    <>
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
            onChange={(value) => onFieldChange('serverName', value)}
          />
          <Field
            id="installPath"
            label="インストール先"
            value={form.installPath}
            error={errors.installPath}
            onChange={(value) => onFieldChange('installPath', value)}
          />
          <Field
            id="steamCmdPath"
            label="SteamCMDの保存先"
            value={form.steamCmdPath}
            error={errors.steamCmdPath}
            onChange={(value) => onFieldChange('steamCmdPath', value)}
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
            onChange={(value) => onFieldChange('gamePort', value)}
          />
          <Field
            id="rconPort"
            label="RCONポート"
            type="number"
            value={form.rconPort}
            error={errors.rconPort}
            onChange={(value) => onFieldChange('rconPort', value)}
          />
          <Field
            id="maxPlayers"
            label="最大プレイヤー数"
            type="number"
            value={form.maxPlayers}
            error={errors.maxPlayers}
            onChange={(value) => onFieldChange('maxPlayers', value)}
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
            onChange={(value) => onFieldChange('serverPassword', value)}
          />
          <Field
            id="adminPassword"
            label="管理者パスワード"
            type="password"
            value={form.adminPassword}
            error={errors.adminPassword}
            onChange={(value) => onFieldChange('adminPassword', value)}
          />
        </section>

        {errors.request && <p className="error request-error">{errors.request}</p>}
        <button type="submit" disabled={isSubmitting} aria-busy={isSubmitting}>
          {isSubmitting ? '確認中...' : '構築計画を確認'}
        </button>
      </form>
    </>
  )
}

type FieldProps = {
  id: string
  label: string
  value: string
  error?: string
  type?: 'text' | 'number' | 'password'
  onChange: (value: string) => void
}

function Field({
  id,
  label,
  value,
  error,
  type = 'text',
  onChange,
}: FieldProps) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <input
        id={id}
        type={type}
        value={value}
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
