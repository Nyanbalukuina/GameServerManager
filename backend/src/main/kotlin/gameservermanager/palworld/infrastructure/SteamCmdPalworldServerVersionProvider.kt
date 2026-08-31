package gameservermanager.palworld.infrastructure

import gameservermanager.palworld.application.InstallPalworldServer
import gameservermanager.palworld.application.PalworldServerVersionProvider
import gameservermanager.shared.steamcmd.SteamCmdServerVersionReader
import org.springframework.stereotype.Component

@Component
class SteamCmdPalworldServerVersionProvider(
    private val reader: SteamCmdServerVersionReader,
) : PalworldServerVersionProvider {
    override fun currentBuildId(steamCmdPath: String, installPath: String): String {
        return reader.currentBuildId("Palworld", InstallPalworldServer.PALWORLD_SERVER_APP_ID, steamCmdPath, installPath)
    }

    override fun latestBuildId(steamCmdPath: String, installPath: String): String {
        return reader.latestBuildId("Palworld", InstallPalworldServer.PALWORLD_SERVER_APP_ID, steamCmdPath, installPath)
    }
}
