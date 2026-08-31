package gameservermanager.palworld.application

import gameservermanager.shared.construction.SteamCmdInstallCommand

interface SteamCmdInstaller {
    fun install(command: SteamCmdInstallCommand): SteamCmdInstallResult
}

data class SteamCmdInstallResult(
    val workspacePath: String,
    val steamCmdExecutablePath: String,
    val gameExecutablePath: String,
)
