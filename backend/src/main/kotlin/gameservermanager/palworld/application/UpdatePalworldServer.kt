package gameservermanager.palworld.application

import gameservermanager.palworld.domain.PalworldInstallationReport
import gameservermanager.palworld.domain.PalworldOperationHistoryEntry
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class UpdatePalworldServer(
    private val processManager: PalworldServerProcessManager,
    private val installPalworldServer: InstallPalworldServer,
    private val historyStore: PalworldOperationHistoryStore,
) {
    fun execute(command: Command): PalworldInstallationReport {
        require(processManager.current()?.alive != true) {
            "Palworldサーバーを停止してから更新してください"
        }

        return try {
            val report = installPalworldServer.execute(
                InstallPalworldServer.Command(command.steamCmdPath, command.installPath),
            )
            historyStore.append(
                PalworldOperationHistoryEntry(
                    Clock.systemUTC().instant(),
                    "UPDATE",
                    "COMPLETED",
                    "SteamCMDでPalworldサーバーを更新しました",
                ),
            )
            report
        } catch (exception: RuntimeException) {
            historyStore.append(
                PalworldOperationHistoryEntry(
                    Clock.systemUTC().instant(),
                    "UPDATE",
                    "FAILED",
                    exception.message ?: "Palworldサーバーの更新に失敗しました",
                ),
            )
            throw exception
        }
    }

    data class Command(
        val steamCmdPath: String,
        val installPath: String,
    )
}
