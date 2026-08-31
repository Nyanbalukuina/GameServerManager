package gameservermanager.palworld.application

import gameservermanager.palworld.domain.PalworldOperationHistoryEntry
import gameservermanager.palworld.domain.PalworldServerStatus
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class RestartPalworldServer(
    private val stopPalworldServer: StopPalworldServer,
    private val startPalworldServer: StartPalworldServer,
    private val historyStore: PalworldOperationHistoryStore,
) {
    fun execute(command: Command): PalworldServerStatus {
        return try {
            stopPalworldServer.execute(
                StopPalworldServer.Command(command.restApiPort, command.adminPassword),
            )
            val status = startPalworldServer.execute(
                StartPalworldServer.Command(
                    command.installPath,
                    command.gamePort,
                    command.maxPlayers,
                ),
            )
            historyStore.append(
                PalworldOperationHistoryEntry(
                    Clock.systemUTC().instant(),
                    "RESTART",
                    "COMPLETED",
                    "Palworldサーバーを再起動しました",
                ),
            )
            status
        } catch (exception: RuntimeException) {
            historyStore.append(
                PalworldOperationHistoryEntry(
                    Clock.systemUTC().instant(),
                    "RESTART",
                    "FAILED",
                    exception.message ?: "Palworldサーバーの再起動に失敗しました",
                ),
            )
            throw exception
        }
    }

    data class Command(
        val installPath: String,
        val gamePort: Int,
        val maxPlayers: Int,
        val restApiPort: Int,
        val adminPassword: String,
    )
}
