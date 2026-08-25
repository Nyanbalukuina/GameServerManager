type GamePortAccessFieldsProps = {
  allowLocalSubnet: boolean
  allowTailscale: boolean
  allowAnyRemoteAddress: boolean
  customRemoteAddresses: string
  error?: string
  onChange: (
    field: 'allowLocalSubnet' | 'allowTailscale' | 'allowAnyRemoteAddress' | 'customRemoteAddresses',
    value: boolean | string,
  ) => void
}

export function GamePortAccessFields({
  allowLocalSubnet,
  allowTailscale,
  allowAnyRemoteAddress,
  customRemoteAddresses,
  error,
  onChange,
}: GamePortAccessFieldsProps) {
  return (
    <fieldset>
      <legend>プレイヤーの接続範囲</legend>
      <label className="check-field">
        <input type="checkbox" checked={allowLocalSubnet} disabled={allowAnyRemoteAddress} onChange={(event) => onChange('allowLocalSubnet', event.target.checked)} />
        同一LANを許可
      </label>
      <label className="check-field">
        <input type="checkbox" checked={allowTailscale} disabled={allowAnyRemoteAddress} onChange={(event) => onChange('allowTailscale', event.target.checked)} />
        Tailscaleを許可（100.64.0.0/10）
      </label>
      <label className="field" htmlFor="customRemoteAddresses">
        接続元を手動指定（任意）
        <textarea
          id="customRemoteAddresses"
          value={customRemoteAddresses}
          disabled={allowAnyRemoteAddress}
          placeholder={'例: 10.8.0.0/24\n100.80.0.20'}
          onChange={(event) => onChange('customRemoteAddresses', event.target.value)}
        />
      </label>
      <p className="error">{error}</p>
      <label className="check-field">
        <input type="checkbox" checked={allowAnyRemoteAddress} onChange={(event) => onChange('allowAnyRemoteAddress', event.target.checked)} />
        すべての接続元を許可（上級者向け）
      </label>
      {allowAnyRemoteAddress && <p className="notice">サーバーPCへ到達可能なすべての端末からゲームポートへの通信を許可します。</p>}
    </fieldset>
  )
}
