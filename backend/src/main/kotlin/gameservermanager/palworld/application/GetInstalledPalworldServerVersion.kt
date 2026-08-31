package gameservermanager.palworld.application

import gameservermanager.shared.steamcmd.InstalledSteamServerVersion
import org.springframework.stereotype.Service

@Service
class GetInstalledPalworldServerVersion(
    private val versionProvider: PalworldServerVersionProvider,
) {
    fun execute(command: Command): InstalledSteamServerVersion {
        return InstalledSteamServerVersion(
            appId = InstallPalworldServer.PALWORLD_SERVER_APP_ID,
            buildId = versionProvider.currentBuildId(command.steamCmdPath, command.installPath),
        )
    }

    data class Command(val steamCmdPath: String, val installPath: String)
}
