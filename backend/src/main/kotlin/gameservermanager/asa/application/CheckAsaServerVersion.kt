package gameservermanager.asa.application

import gameservermanager.asa.domain.AsaServerVersion
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class CheckAsaServerVersion(
    private val versionProvider: AsaServerVersionProvider,
) {
    fun execute(command: Command): AsaServerVersion {
        val current = versionProvider.currentBuildId(command.steamCmdPath, command.installPath)
        val latest = versionProvider.latestBuildId(command.steamCmdPath, command.installPath)
        val updateAvailable = current != latest
        return AsaServerVersion(
            currentBuildId = current,
            latestBuildId = latest,
            updateAvailable = updateAvailable,
            checkedAt = Clock.systemUTC().instant(),
            message = if (updateAvailable) "新しいサーバーバージョンを利用できます" else "サーバーは最新です",
        )
    }

    data class Command(
        val steamCmdPath: String,
        val installPath: String,
    )
}
