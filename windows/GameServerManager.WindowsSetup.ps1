param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('Plan', 'Install', 'Uninstall', 'Status')]
    [string]$Action,
    [ValidateRange(1, 65535)]
    [int]$ManagementPort = 8080,
    [string]$StorageRoot = 'C:\GameServerManager',
    [string]$JarPath = '',
    [string]$JavawPath = ''
)

$ErrorActionPreference = 'Stop'
$taskName = 'GameServerManager'
$firewallHelperTaskName = 'GameServerManager-FirewallHelper'
$managementRule = 'GameServerManager-Management-TCP'
$managementAllowedRemoteAddresses = @('LocalSubnet', '100.64.0.0/10')

function Test-Administrator {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = [Security.Principal.WindowsPrincipal]::new($identity)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function ConvertTo-SingleQuotedLiteral([string]$Value) {
    return "'" + $Value.Replace("'", "''") + "'"
}

function Resolve-SetupPaths {
    $script:resolvedStorageRoot = [System.IO.Path]::GetFullPath($StorageRoot)
    $effectiveJarPath = if ($JarPath) { $JarPath } else { Join-Path $PSScriptRoot 'game-server-manager.jar' }
    $script:resolvedJarPath = [System.IO.Path]::GetFullPath($effectiveJarPath)
    if ($JavawPath) {
        $script:resolvedJavawPath = [System.IO.Path]::GetFullPath($JavawPath)
    } else {
        $script:resolvedJavawPath = (Get-Command 'javaw.exe' -ErrorAction Stop).Source
    }
    $script:launcherPath = Join-Path $resolvedStorageRoot 'config\start-game-server-manager.ps1'
    $script:installedJarPath = Join-Path $resolvedStorageRoot 'app\game-server-manager.jar'
    $script:firewallHelperSourcePath = Join-Path $PSScriptRoot 'GameServerManager.FirewallHelper.ps1'
    $script:firewallHelperDirectory = Join-Path $env:ProgramFiles 'GameServerManager'
    $script:firewallHelperPath = Join-Path $firewallHelperDirectory 'GameServerManager.FirewallHelper.ps1'
}

function Get-SetupStatus {
    $gameRules = @(Get-NetFirewallRule -DisplayName 'GameServerManager-Game-*' -ErrorAction SilentlyContinue)
    return [ordered]@{
        taskRegistered = $null -ne (Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue)
        firewallHelperTaskRegistered = $null -ne (Get-ScheduledTask -TaskName $firewallHelperTaskName -ErrorAction SilentlyContinue)
        managementFirewallRuleRegistered = $null -ne (Get-NetFirewallRule -DisplayName $managementRule -ErrorAction SilentlyContinue)
        gameFirewallRuleCount = $gameRules.Count
        launcherPath = $launcherPath
        jarPath = $installedJarPath
        firewallHelperPath = $firewallHelperPath
        javawPath = $resolvedJavawPath
        storageRoot = $resolvedStorageRoot
        managementPort = $ManagementPort
    }
}

function Install-Integration {
    if (-not (Test-Administrator)) { throw 'Installは管理者権限のPowerShellで実行してください' }
    if (-not (Test-Path -LiteralPath $resolvedJarPath -PathType Leaf)) { throw "実行可能JARが見つかりません: $resolvedJarPath" }
    if ([System.IO.Path]::GetExtension($resolvedJarPath) -ne '.jar') { throw 'JarPathには.jarファイルを指定してください' }
    if (-not (Test-Path -LiteralPath $resolvedJavawPath -PathType Leaf)) { throw "javaw.exeが見つかりません: $resolvedJavawPath" }
    if (-not (Test-Path -LiteralPath $firewallHelperSourcePath -PathType Leaf)) { throw "Firewall補助スクリプトが見つかりません: $firewallHelperSourcePath" }

    New-Item -ItemType Directory -Path (Split-Path -Parent $launcherPath) -Force | Out-Null
    New-Item -ItemType Directory -Path (Split-Path -Parent $installedJarPath) -Force | Out-Null
    New-Item -ItemType Directory -Path $firewallHelperDirectory -Force | Out-Null
    Copy-Item -LiteralPath $resolvedJarPath -Destination $installedJarPath -Force
    Copy-Item -LiteralPath $firewallHelperSourcePath -Destination $firewallHelperPath -Force
    @(
        "`$env:GAME_SERVER_MANAGER_ROOT = " + (ConvertTo-SingleQuotedLiteral $resolvedStorageRoot)
        "`$env:GAME_SERVER_MANAGER_ADDRESS = '0.0.0.0'"
        "`$env:GAME_SERVER_MANAGER_PORT = '$ManagementPort'"
        "& " + (ConvertTo-SingleQuotedLiteral $resolvedJavawPath) + " -jar " + (ConvertTo-SingleQuotedLiteral $installedJarPath)
    ) | Set-Content -LiteralPath $launcherPath -Encoding UTF8

    Get-NetFirewallRule -DisplayName $managementRule -ErrorAction SilentlyContinue | Remove-NetFirewallRule
    New-NetFirewallRule -DisplayName $managementRule -Direction Inbound -Action Allow -Protocol TCP -LocalPort $ManagementPort -RemoteAddress $managementAllowedRemoteAddresses | Out-Null
    Get-NetFirewallRule -DisplayName 'GameServerManager-Palworld-Game-UDP' -ErrorAction SilentlyContinue | Remove-NetFirewallRule

    $userId = [Security.Principal.WindowsIdentity]::GetCurrent().Name
    $managerAction = New-ScheduledTaskAction -Execute 'powershell.exe' -Argument "-NoProfile -NonInteractive -ExecutionPolicy Bypass -File `"$launcherPath`""
    $managerTrigger = New-ScheduledTaskTrigger -AtStartup
    $managerPrincipal = New-ScheduledTaskPrincipal -UserId $userId -LogonType S4U -RunLevel Limited
    Register-ScheduledTask -TaskName $taskName -Action $managerAction -Trigger $managerTrigger -Principal $managerPrincipal -Settings (New-ScheduledTaskSettingsSet -StartWhenAvailable -ExecutionTimeLimit ([TimeSpan]::Zero)) -Force | Out-Null

    $helperAction = New-ScheduledTaskAction -Execute 'powershell.exe' -Argument "-NoProfile -NonInteractive -ExecutionPolicy Bypass -File `"$firewallHelperPath`" -StorageRoot `"$resolvedStorageRoot`""
    $helperPrincipal = New-ScheduledTaskPrincipal -UserId $userId -LogonType S4U -RunLevel Highest
    Register-ScheduledTask -TaskName $firewallHelperTaskName -Action $helperAction -Principal $helperPrincipal -Settings (New-ScheduledTaskSettingsSet -ExecutionTimeLimit ([TimeSpan]::FromMinutes(1))) -Force | Out-Null
}

function Uninstall-Integration {
    if (-not (Test-Administrator)) { throw 'Uninstallは管理者権限のPowerShellで実行してください' }
    Unregister-ScheduledTask -TaskName $taskName -Confirm:$false -ErrorAction SilentlyContinue
    Unregister-ScheduledTask -TaskName $firewallHelperTaskName -Confirm:$false -ErrorAction SilentlyContinue
    Get-NetFirewallRule -DisplayName $managementRule -ErrorAction SilentlyContinue | Remove-NetFirewallRule
    Get-NetFirewallRule -DisplayName 'GameServerManager-Palworld-Game-UDP' -ErrorAction SilentlyContinue | Remove-NetFirewallRule
    Get-NetFirewallRule -DisplayName 'GameServerManager-Game-*' -ErrorAction SilentlyContinue | Remove-NetFirewallRule
    Remove-Item -LiteralPath $launcherPath -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $installedJarPath -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $firewallHelperPath -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $firewallHelperDirectory -Force -ErrorAction SilentlyContinue
}

Resolve-SetupPaths
switch ($Action) {
    'Plan' {
        [ordered]@{
            operations = @('ADD_MANAGEMENT_FIREWALL_RULE', 'REGISTER_FIREWALL_HELPER', 'REGISTER_APPLICATION_STARTUP')
            taskName = $taskName
            firewallHelperTaskName = $firewallHelperTaskName
            managementRule = $managementRule
            managementAllowedRemoteAddresses = $managementAllowedRemoteAddresses
            launcherPath = $launcherPath
            sourceJarPath = $resolvedJarPath
            installedJarPath = $installedJarPath
            firewallHelperPath = $firewallHelperPath
            javawPath = $resolvedJavawPath
            storageRoot = $resolvedStorageRoot
            managementPort = $ManagementPort
        } | ConvertTo-Json -Depth 4
    }
    'Install' { Install-Integration; Get-SetupStatus | ConvertTo-Json -Depth 4 }
    'Uninstall' { Uninstall-Integration; Get-SetupStatus | ConvertTo-Json -Depth 4 }
    'Status' { Get-SetupStatus | ConvertTo-Json -Depth 4 }
}
