param(
    [Parameter(Mandatory = $true)]
    [string]$StorageRoot
)

$ErrorActionPreference = 'Stop'
$exchangeDirectory = Join-Path ([System.IO.Path]::GetFullPath($StorageRoot)) 'config\firewall-helper'
$requestPath = Join-Path $exchangeDirectory 'request.json'
$responsePath = Join-Path $exchangeDirectory 'response.json'

function Test-RemoteAddress([string]$Value) {
    if ($Value -eq 'LocalSubnet' -or $Value -eq 'Any') {
        return $true
    }
    if ($Value -notmatch '^(?<ip>(?:\d{1,3}\.){3}\d{1,3})(?:/(?<prefix>\d{1,2}))?$') {
        return $false
    }
    $octets = $Matches.ip.Split('.')
    if ($octets | Where-Object { [int]$_ -gt 255 }) {
        return $false
    }
    return -not $Matches.prefix -or [int]$Matches.prefix -le 32
}

function Write-Response([string]$RequestId, [bool]$Success, [string]$Message) {
    New-Item -ItemType Directory -Path $exchangeDirectory -Force | Out-Null
    $temporaryPath = Join-Path $exchangeDirectory ('.response-' + [guid]::NewGuid().ToString('N') + '.tmp')
    [ordered]@{
        requestId = $RequestId
        success = $Success
        message = $Message
    } | ConvertTo-Json | Set-Content -LiteralPath $temporaryPath -Encoding UTF8
    Move-Item -LiteralPath $temporaryPath -Destination $responsePath -Force
}

$requestId = ''
try {
    if (-not (Test-Path -LiteralPath $requestPath -PathType Leaf)) {
        throw 'Firewall操作要求が見つかりません'
    }
    $request = Get-Content -LiteralPath $requestPath -Raw | ConvertFrom-Json
    $requestId = [string]$request.requestId
    if ($requestId -notmatch '^[0-9a-fA-F-]{36}$') {
        throw '要求IDが不正です'
    }
    if ($request.operation -notin @('APPLY', 'REMOVE')) {
        throw '許可されていないFirewall操作です'
    }
    if ($request.ruleName -notmatch '^GameServerManager-Game-[A-Za-z0-9-]{1,64}-[A-Za-z0-9-]{1,64}-UDP-\d{1,5}$') {
        throw 'Firewall規則名が不正です'
    }
    $localPort = [int]$request.localPort
    if ($localPort -lt 1 -or $localPort -gt 65535 -or -not $request.ruleName.EndsWith("-UDP-$localPort")) {
        throw 'ゲームポートが不正です'
    }
    $remoteAddresses = @($request.remoteAddresses)
    if ($remoteAddresses.Count -eq 0) {
        throw '接続元を1つ以上指定してください'
    }
    foreach ($address in $remoteAddresses) {
        if (-not (Test-RemoteAddress ([string]$address))) {
            throw "接続元が不正です: $address"
        }
    }
    if ($remoteAddresses -contains 'Any' -and $remoteAddresses.Count -ne 1) {
        throw 'Anyはほかの接続元と同時に指定できません'
    }

    Get-NetFirewallRule -DisplayName $request.ruleName -ErrorAction SilentlyContinue | Remove-NetFirewallRule
    if ($request.operation -eq 'APPLY') {
        New-NetFirewallRule `
            -DisplayName $request.ruleName `
            -Direction Inbound `
            -Action Allow `
            -Protocol UDP `
            -LocalPort $localPort `
            -RemoteAddress $remoteAddresses | Out-Null
        Write-Response $requestId $true 'Firewall規則を追加しました'
    } else {
        Write-Response $requestId $true 'Firewall規則を削除しました'
    }
} catch {
    Write-Response $requestId $false $_.Exception.Message
    exit 1
} finally {
    Remove-Item -LiteralPath $requestPath -Force -ErrorAction SilentlyContinue
}
