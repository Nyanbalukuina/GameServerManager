param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('Plan', 'Install', 'Uninstall', 'Status')]
    [string]$Action,

    [ValidateRange(1, 65535)]
    [int]$ManagementPort = 8080,

    [ValidateRange(1, 65535)]
    [int]$GamePort = 8211,

    [string]$GameRemoteAddress = 'LocalSubnet,100.64.0.0/10',

    [string]$StorageRoot = 'C:\GameServerManager',

    [string]$JarPath = '',

    [string]$JavawPath = ''
)

$ErrorActionPreference = 'Stop'
$taskName = 'GameServerManager'
$managementRule = 'GameServerManager-Management-TCP'
$gameRule = 'GameServerManager-Palworld-Game-UDP'
$managementAllowedRemoteAddresses = @('LocalSubnet', '100.64.0.0/10')
$gameRemoteAddresses = @($GameRemoteAddress.Split(',') | ForEach-Object { $_.Trim() } | Where-Object { $_ })

function Test-RemoteAddress([string]$Value) {
    if ($Value -eq 'LocalSubnet' -or $Value -eq 'Any') {
        return $true
    }
    return $Value -match '^(?:\d{1,3}\.){3}\d{1,3}(?:/(?:[0-9]|[12][0-9]|3[0-2]))?$'
}

function Assert-GameRemoteAddresses {
    if (-not $gameRemoteAddresses -or $gameRemoteAddresses.Count -eq 0) {
        throw 'GameRemoteAddressを1つ以上指定してください'
    }
    foreach ($address in $gameRemoteAddresses) {
        if (-not (Test-RemoteAddress $address)) {
            throw "GameRemoteAddressが不正です: $address"
        }
    }
    if ($gameRemoteAddresses -contains 'Any' -and $gameRemoteAddresses.Count -ne 1) {
        throw 'AnyはほかのGameRemoteAddressと同時に指定できません'
    }
}

function Resolve-SetupPaths {
    $script:resolvedStorageRoot = [System.IO.Path]::GetFullPath($StorageRoot)
    $effectiveJarPath = if ($JarPath) { $JarPath } else { Join-Path $PSScriptRoot 'game-server-manager.jar' }
    $script:resolvedJarPath = [System.IO.Path]::GetFullPath($effectiveJarPath)
    if (-not $JavawPath) {
        $javaCommand = Get-Command 'javaw.exe' -ErrorAction Stop
        $script:resolvedJavawPath = $javaCommand.Source
    } else {
        $script:resolvedJavawPath = [System.IO.Path]::GetFullPath($JavawPath)
    }
    $script:launcherPath = Join-Path $resolvedStorageRoot 'config\start-game-server-manager.ps1'
    $script:installedJarPath = Join-Path $resolvedStorageRoot 'app\game-server-manager.jar'
}

function Test-Administrator {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = [Security.Principal.WindowsPrincipal]::new($identity)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function ConvertTo-SingleQuotedLiteral([string]$Value) {
    return "'" + $Value.Replace("'", "''") + "'"
}

function Get-SetupStatus {
    $task = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
    $management = Get-NetFirewallRule -DisplayName $managementRule -ErrorAction SilentlyContinue
    $game = Get-NetFirewallRule -DisplayName $gameRule -ErrorAction SilentlyContinue
    return [ordered]@{
        taskRegistered = $null -ne $task
        managementFirewallRuleRegistered = $null -ne $management
        gameFirewallRuleRegistered = $null -ne $game
        launcherPath = $launcherPath
        jarPath = $installedJarPath
        javawPath = $resolvedJavawPath
        storageRoot = $resolvedStorageRoot
        managementPort = $ManagementPort
        gamePort = $GamePort
        gameRemoteAddresses = $gameRemoteAddresses
    }
}

function Install-Integration {
    if (-not (Test-Administrator)) {
        throw 'Installは管理者権限のPowerShellで実行してください'
    }
    if (-not [System.IO.Path]::IsPathRooted($resolvedStorageRoot)) {
        throw 'StorageRootには絶対パスを指定してください'
    }
    if (-not (Test-Path -LiteralPath $resolvedJarPath -PathType Leaf)) {
        throw "実行可能JARが見つかりません: $resolvedJarPath"
    }
    if ([System.IO.Path]::GetExtension($resolvedJarPath) -ne '.jar') {
        throw 'JarPathには.jarファイルを指定してください'
    }
    if (-not (Test-Path -LiteralPath $resolvedJavawPath -PathType Leaf)) {
        throw "javaw.exeが見つかりません: $resolvedJavawPath"
    }
    Assert-GameRemoteAddresses

    $launcherDirectory = Split-Path -Parent $launcherPath
    $applicationDirectory = Split-Path -Parent $installedJarPath
    New-Item -ItemType Directory -Path $launcherDirectory -Force | Out-Null
    New-Item -ItemType Directory -Path $applicationDirectory -Force | Out-Null
    Copy-Item -LiteralPath $resolvedJarPath -Destination $installedJarPath -Force
    $launcher = @(
        "`$env:GAME_SERVER_MANAGER_ROOT = " + (ConvertTo-SingleQuotedLiteral $resolvedStorageRoot)
        "`$env:GAME_SERVER_MANAGER_ADDRESS = '0.0.0.0'"
        "`$env:GAME_SERVER_MANAGER_PORT = '$ManagementPort'"
        "& " + (ConvertTo-SingleQuotedLiteral $resolvedJavawPath) + " -jar " + (ConvertTo-SingleQuotedLiteral $installedJarPath)
    )
    Set-Content -LiteralPath $launcherPath -Value $launcher -Encoding UTF8

    Get-NetFirewallRule -DisplayName $managementRule -ErrorAction SilentlyContinue | Remove-NetFirewallRule
    New-NetFirewallRule `
        -DisplayName $managementRule `
        -Direction Inbound `
        -Action Allow `
        -Protocol TCP `
        -LocalPort $ManagementPort `
        -RemoteAddress $managementAllowedRemoteAddresses | Out-Null

    Get-NetFirewallRule -DisplayName $gameRule -ErrorAction SilentlyContinue | Remove-NetFirewallRule
    New-NetFirewallRule `
        -DisplayName $gameRule `
        -Direction Inbound `
        -Action Allow `
        -Protocol UDP `
        -LocalPort $GamePort `
        -RemoteAddress $gameRemoteAddresses | Out-Null

    $taskAction = New-ScheduledTaskAction `
        -Execute 'powershell.exe' `
        -Argument "-NoProfile -NonInteractive -ExecutionPolicy Bypass -File `"$launcherPath`""
    $trigger = New-ScheduledTaskTrigger -AtStartup
    $principal = New-ScheduledTaskPrincipal `
        -UserId ([Security.Principal.WindowsIdentity]::GetCurrent().Name) `
        -LogonType S4U `
        -RunLevel Limited
    $settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -ExecutionTimeLimit ([TimeSpan]::Zero)
    Register-ScheduledTask `
        -TaskName $taskName `
        -Action $taskAction `
        -Trigger $trigger `
        -Principal $principal `
        -Settings $settings `
        -Force | Out-Null
}

function Uninstall-Integration {
    if (-not (Test-Administrator)) {
        throw 'Uninstallは管理者権限のPowerShellで実行してください'
    }
    Unregister-ScheduledTask -TaskName $taskName -Confirm:$false -ErrorAction SilentlyContinue
    Get-NetFirewallRule -DisplayName $managementRule -ErrorAction SilentlyContinue | Remove-NetFirewallRule
    Get-NetFirewallRule -DisplayName $gameRule -ErrorAction SilentlyContinue | Remove-NetFirewallRule
    Remove-Item -LiteralPath $launcherPath -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $installedJarPath -Force -ErrorAction SilentlyContinue
}

Resolve-SetupPaths

switch ($Action) {
    'Plan' {
        Assert-GameRemoteAddresses
        [ordered]@{
            operations = @(
                'ADD_MANAGEMENT_FIREWALL_RULE',
                'ADD_PALWORLD_GAME_FIREWALL_RULE',
                'REGISTER_APPLICATION_STARTUP'
            )
            taskName = $taskName
            managementRule = $managementRule
            gameRule = $gameRule
            managementAllowedRemoteAddresses = $managementAllowedRemoteAddresses
            gameRemoteAddresses = $gameRemoteAddresses
            launcherPath = $launcherPath
            sourceJarPath = $resolvedJarPath
            installedJarPath = $installedJarPath
            javawPath = $resolvedJavawPath
            storageRoot = $resolvedStorageRoot
            managementPort = $ManagementPort
            gamePort = $GamePort
        } | ConvertTo-Json -Depth 4
    }
    'Install' {
        Install-Integration
        Get-SetupStatus | ConvertTo-Json -Depth 4
    }
    'Uninstall' {
        Uninstall-Integration
        Get-SetupStatus | ConvertTo-Json -Depth 4
    }
    'Status' {
        Get-SetupStatus | ConvertTo-Json -Depth 4
    }
}
