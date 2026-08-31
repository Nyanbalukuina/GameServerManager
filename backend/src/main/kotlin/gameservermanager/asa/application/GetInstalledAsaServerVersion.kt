package gameservermanager.asa.application

import gameservermanager.shared.steamcmd.InstalledSteamServerVersion
import org.springframework.stereotype.Service

@Service
class GetInstalledAsaServerVersion(
    private val versionProvider: AsaServerVersionProvider,
) {
    fun execute(command: Command): InstalledSteamServerVersion {
        return InstalledSteamServerVersion(
            appId = InstallAsaServer.ASA_SERVER_APP_ID,
            buildId = versionProvider.currentBuildId(command.steamCmdPath, command.installPath),
        )
    }

    data class Command(val steamCmdPath: String, val installPath: String)
}
