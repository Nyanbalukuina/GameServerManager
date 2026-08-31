package gameservermanager.palworld.application

import gameservermanager.palworld.domain.PalworldServerVersion
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class CheckPalworldServerVersion(
    private val versionProvider: PalworldServerVersionProvider,
) {
    fun execute(command: Command): PalworldServerVersion {
        val current = versionProvider.currentBuildId(command.steamCmdPath, command.installPath)
        val latest = versionProvider.latestBuildId(command.steamCmdPath, command.installPath)
        val updateAvailable = current != latest
        return PalworldServerVersion(
            currentBuildId = current,
            latestBuildId = latest,
            updateAvailable = updateAvailable,
            checkedAt = Clock.systemUTC().instant(),
            message = if (updateAvailable) "新しいサーバーバージョンを利用できます" else "サーバーは最新です",
        )
    }

    data class Command(val steamCmdPath: String, val installPath: String)
}
