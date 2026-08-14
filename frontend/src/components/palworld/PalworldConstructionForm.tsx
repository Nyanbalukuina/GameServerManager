import type { FormEvent } from 'react'
import type {
  NewServerRequest,
  ValidationErrors,
} from '../../types/palworldConstruction'
import { FormField } from '../common/FormField'

type PalworldConstructionFormProps = {
  form: NewServerRequest
  errors: ValidationErrors
  isSubmitting: boolean
  onFieldChange: (field: keyof NewServerRequest, value: string | boolean) => void
  onSubmit: () => Promise<void>
}

export function PalworldConstructionForm({
  form,
  errors,
  isSubmitting,
  onFieldChange,
  onSubmit,
}: PalworldConstructionFormProps) {
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
          <FormField
            id="serverName"
            label="サーバー名"
            value={form.serverName}
            error={errors.serverName}
            onChange={(value) => onFieldChange('serverName', value)}
          />
          <FormField
            id="installPath"
            label="Palworldサーバーのインストール先"
            value={form.installPath}
            error={errors.installPath}
            onChange={(value) => onFieldChange('installPath', value)}
          />
          <FormField
            id="steamCmdPath"
            label="SteamCMDの保存先"
            value={form.steamCmdPath}
            error={errors.steamCmdPath}
            onChange={(value) => onFieldChange('steamCmdPath', value)}
          />
        </section>

        <section className="card form-section">
          <h2>接続設定</h2>
          <FormField
            id="gamePort"
            label="ゲームポート"
            type="number"
            value={form.gamePort}
            error={errors.gamePort}
            onChange={(value) => onFieldChange('gamePort', value)}
          />
          <FormField
            id="rconPort"
            label="RCONポート"
            type="number"
            value={form.rconPort}
            error={errors.rconPort}
            onChange={(value) => onFieldChange('rconPort', value)}
          />
          <FormField
            id="maxPlayers"
            label="最大プレイヤー数"
            type="number"
            value={form.maxPlayers}
            error={errors.maxPlayers}
            onChange={(value) => onFieldChange('maxPlayers', value)}
          />
          <fieldset>
            <legend>プレイヤーの接続範囲</legend>
            <label className="automation-toggle">
              <input
                type="checkbox"
                checked={form.allowLocalSubnet}
                disabled={form.allowAnyRemoteAddress}
                onChange={(event) => onFieldChange('allowLocalSubnet', event.target.checked)}
              />
              同一LANを許可
            </label>
            <label className="automation-toggle">
              <input
                type="checkbox"
                checked={form.allowTailscale}
                disabled={form.allowAnyRemoteAddress}
                onChange={(event) => onFieldChange('allowTailscale', event.target.checked)}
              />
              Tailscaleを許可（100.64.0.0/10）
            </label>
            <label htmlFor="customRemoteAddresses">接続元を手動指定（任意）</label>
            <textarea
              id="customRemoteAddresses"
              value={form.customRemoteAddresses}
              disabled={form.allowAnyRemoteAddress}
              placeholder={'例: 10.8.0.0/24\n100.80.0.20'}
              onChange={(event) => onFieldChange('customRemoteAddresses', event.target.value)}
            />
            <p className="error">{errors.customRemoteAddresses}</p>
            <label className="automation-toggle">
              <input
                type="checkbox"
                checked={form.allowAnyRemoteAddress}
                onChange={(event) => onFieldChange('allowAnyRemoteAddress', event.target.checked)}
              />
              すべての接続元を許可（上級者向け）
            </label>
            {form.allowAnyRemoteAddress && (
              <p className="notice">サーバーPCへ到達可能なすべての端末からゲームポートへの通信を許可します。</p>
            )}
          </fieldset>
        </section>

        <section className="card form-section">
          <h2>パスワード</h2>
          <FormField
            id="serverPassword"
            label="サーバーパスワード（任意）"
            type="password"
            value={form.serverPassword}
            error={errors.serverPassword}
            onChange={(value) => onFieldChange('serverPassword', value)}
          />
          <FormField
            id="adminPassword"
            label="管理者パスワード"
            type="password"
            value={form.adminPassword}
            error={errors.adminPassword}
            onChange={(value) => onFieldChange('adminPassword', value)}
          />
        </section>

        <section className="card form-section">
          <h2>自動運転</h2>
          <label className="automation-toggle">
            <input
              type="checkbox"
              checked={form.automationEnabled}
              onChange={(event) => onFieldChange('automationEnabled', event.target.checked)}
            />
            自動運転を有効にする
          </label>
          <label htmlFor="shutdownTime">毎日の停止時刻</label>
          <input
            id="shutdownTime"
            type="time"
            value={form.shutdownTime}
            onChange={(event) => onFieldChange('shutdownTime', event.target.value)}
          />
          <p className="error">{errors.shutdownTime}</p>
          <label htmlFor="startupTime">毎日の起動時刻</label>
          <input
            id="startupTime"
            type="time"
            value={form.startupTime}
            onChange={(event) => onFieldChange('startupTime', event.target.value)}
          />
          <p className="error">{errors.startupTime}</p>
          <label className="automation-toggle">
            <input
              type="checkbox"
              checked={form.backupAfterShutdown}
              onChange={(event) => onFieldChange('backupAfterShutdown', event.target.checked)}
            />
            停止後にバックアップする
          </label>
          <p className="notice">バックアップは最新3個を保持し、古いものから自動削除します。</p>
        </section>

        {errors.request && <p className="error request-error">{errors.request}</p>}
        <button type="submit" disabled={isSubmitting} aria-busy={isSubmitting}>
          {isSubmitting ? '確認中...' : '構築計画を確認'}
        </button>
      </form>
    </>
  )
}
