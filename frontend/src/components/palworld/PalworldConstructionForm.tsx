import type { FormEvent } from 'react'
import type { NewServerRequest, ValidationErrors } from '../../types/palworldConstruction'
import { FormField } from '../common/FormField'
import { GamePortAccessFields } from '../common/GamePortAccessFields'

type Props = {
  form: NewServerRequest
  errors: ValidationErrors
  isSubmitting: boolean
  onFieldChange: (field: keyof NewServerRequest, value: string | number | boolean) => void
  onSubmit: () => Promise<void>
}

export function PalworldConstructionForm({ form, errors, isSubmitting, onFieldChange, onSubmit }: Props) {
  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    void onSubmit()
  }

  return <>
    <header className="page-header"><p className="eyebrow">Palworld</p><h1>新規Palworldサーバー構築</h1><p>必要な設定を入力し、確認画面で事前検証結果を確認してから構築します。</p></header>
    <form onSubmit={submit}>
      <section className="card form-section">
        <div className="section-heading"><div><p className="section-number">01</p><h2>基本設定</h2></div><p>Palworld本体とSteamCMDの保存先を指定します。</p></div>
        <FormField id="installPath" label="Palworldサーバーのインストール先" value={form.installPath} error={errors.installPath} onChange={(value) => onFieldChange('installPath', value)} />
        <FormField id="steamCmdPath" label="SteamCMDの保存先" value={form.steamCmdPath} error={errors.steamCmdPath} onChange={(value) => onFieldChange('steamCmdPath', value)} />
      </section>

      <section className="card form-section">
        <div className="section-heading"><div><p className="section-number">02</p><h2>ゲーム設定</h2></div><p>設定ファイルと自動運転の役割ごとに設定します。</p></div>
        <div className="settings-editor-group">
          <h3>PalWorldSettings.ini</h3><p className="setting-source">サーバー名、参加人数、ワールド内の動作を設定します。</p>
          <div className="form-grid"><FormField id="serverName" label="サーバー名" value={form.serverName} error={errors.serverName} onChange={(value) => onFieldChange('serverName', value)} /><FormField id="maxPlayers" label="最大プレイヤー数" type="number" value={form.maxPlayers} error={errors.maxPlayers} onChange={(value) => onFieldChange('maxPlayers', value)} /></div>
          <label className="field" htmlFor="serverDescription">サーバー説明<input id="serverDescription" maxLength={500} value={form.serverDescription} onChange={(event) => onFieldChange('serverDescription', event.target.value)} /></label>
          <div className="form-grid">
            <NumberSetting id="expRate" label="経験値倍率" value={form.expRate} min={0.1} max={5} step={0.1} onChange={(value) => onFieldChange('expRate', value)} />
            <NumberSetting id="palCaptureRate" label="捕獲率" value={form.palCaptureRate} min={0.1} max={5} step={0.1} onChange={(value) => onFieldChange('palCaptureRate', value)} />
            <NumberSetting id="palSpawnRate" label="パル出現倍率" value={form.palSpawnRate} min={0.1} max={3} step={0.1} onChange={(value) => onFieldChange('palSpawnRate', value)} />
            <NumberSetting id="enemyDropRate" label="ドロップ倍率" value={form.enemyDropRate} min={0.1} max={5} step={0.1} onChange={(value) => onFieldChange('enemyDropRate', value)} />
            <NumberSetting id="eggHatchingTime" label="タマゴ孵化時間（時間）" value={form.eggHatchingTime} min={0} max={240} step={0.5} onChange={(value) => onFieldChange('eggHatchingTime', value)} />
            <label className="field" htmlFor="deathPenalty">デスペナルティ<select id="deathPenalty" value={form.deathPenalty} onChange={(event) => onFieldChange('deathPenalty', event.target.value)}><option value="None">なし</option><option value="Item">アイテム</option><option value="ItemAndEquipment">アイテムと装備</option><option value="All">すべて</option></select></label>
            <NumberSetting id="baseCampMaxNum" label="ワールド全体の拠点数" value={form.baseCampMaxNum} min={1} max={128} onChange={(value) => onFieldChange('baseCampMaxNum', value)} />
            <NumberSetting id="baseCampWorkerMaxNum" label="拠点作業パル数" value={form.baseCampWorkerMaxNum} min={1} max={50} onChange={(value) => onFieldChange('baseCampWorkerMaxNum', value)} />
          </div>
          <label className="check-field"><input type="checkbox" checked={form.pvpEnabled} onChange={(event) => onFieldChange('pvpEnabled', event.target.checked)} />PvPを有効にする</label>
          <label className="check-field"><input type="checkbox" checked={form.friendlyFireEnabled} onChange={(event) => onFieldChange('friendlyFireEnabled', event.target.checked)} />フレンドリーファイアを有効にする</label>
        </div>
        <div className="settings-editor-group">
          <h3>自動運転</h3><p className="setting-source">毎日の停止、バックアップ、再起動を設定します。</p>
          <label className="automation-toggle"><input type="checkbox" checked={form.automationEnabled} onChange={(event) => onFieldChange('automationEnabled', event.target.checked)} />自動運転を有効にする</label>
          <div className="form-grid"><TimeSetting id="shutdownTime" label="毎日の停止時刻" value={form.shutdownTime} error={errors.shutdownTime} onChange={(value) => onFieldChange('shutdownTime', value)} /><TimeSetting id="startupTime" label="毎日の起動時刻" value={form.startupTime} error={errors.startupTime} onChange={(value) => onFieldChange('startupTime', value)} /></div>
        </div>
      </section>

      <section className="card form-section">
        <div className="section-heading"><div><p className="section-number">03</p><h2>接続設定</h2></div><p>使用するポートと接続を許可するネットワークを指定します。</p></div>
        <div className="form-grid"><FormField id="gamePort" label="ゲームポート" type="number" value={form.gamePort} error={errors.gamePort} onChange={(value) => onFieldChange('gamePort', value)} /><FormField id="rconPort" label="RCONポート" type="number" value={form.rconPort} error={errors.rconPort} onChange={(value) => onFieldChange('rconPort', value)} /></div>
        <GamePortAccessFields allowLocalSubnet={form.allowLocalSubnet} allowTailscale={form.allowTailscale} allowAnyRemoteAddress={form.allowAnyRemoteAddress} customRemoteAddresses={form.customRemoteAddresses} error={errors.customRemoteAddresses} onChange={(field, value) => onFieldChange(field, value)} />
      </section>

      <section className="card form-section">
        <div className="section-heading"><div><p className="section-number">04</p><h2>パスワード</h2></div><p>PalWorldSettings.iniへ保存される参加用・管理用設定です。</p></div>
        <div className="form-grid"><FormField id="serverPassword" label="サーバーパスワード（任意）" value={form.serverPassword} error={errors.serverPassword} onChange={(value) => onFieldChange('serverPassword', value)} /><FormField id="adminPassword" label="管理者パスワード" value={form.adminPassword} error={errors.adminPassword} onChange={(value) => onFieldChange('adminPassword', value)} /></div>
      </section>

      <div className="construction-review-action"><button type="submit" disabled={isSubmitting} aria-busy={isSubmitting}>{isSubmitting ? '事前検証中...' : 'この設定でサーバー構築'}</button><p>クリックすると自動で事前検証を行い、確認画面へ進みます。</p>{errors.request && <p className="error request-error" role="alert">{errors.request}</p>}</div>
    </form>
  </>
}

function NumberSetting({ id, label, value, min, max, step = 1, onChange }: { id: string; label: string; value: number; min: number; max: number; step?: number; onChange: (value: number) => void }) {
  return <label className="field" htmlFor={id}>{label}<input id={id} type="number" min={min} max={max} step={step} value={value} onChange={(event) => onChange(Number(event.target.value))} /></label>
}

function TimeSetting({ id, label, value, error, onChange }: { id: string; label: string; value: string; error?: string; onChange: (value: string) => void }) {
  return <label className="field" htmlFor={id}>{label}<input id={id} type="time" value={value} onChange={(event) => onChange(event.target.value)} /><span className="error">{error}</span></label>
}
