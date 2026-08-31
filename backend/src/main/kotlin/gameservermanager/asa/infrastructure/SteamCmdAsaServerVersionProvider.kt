package gameservermanager.asa.infrastructure

import gameservermanager.asa.application.AsaServerVersionProvider
import gameservermanager.asa.application.InstallAsaServer
import gameservermanager.shared.steamcmd.SteamCmdServerVersionReader
import org.springframework.stereotype.Component

@Component
class SteamCmdAsaServerVersionProvider(
    private val reader: SteamCmdServerVersionReader,
) : AsaServerVersionProvider {
    override fun currentBuildId(steamCmdPath: String, installPath: String): String {
        return reader.currentBuildId("ASA", InstallAsaServer.ASA_SERVER_APP_ID, steamCmdPath, installPath)
    }

    override fun latestBuildId(steamCmdPath: String, installPath: String): String {
        return reader.latestBuildId("ASA", InstallAsaServer.ASA_SERVER_APP_ID, steamCmdPath, installPath)
    }
}
