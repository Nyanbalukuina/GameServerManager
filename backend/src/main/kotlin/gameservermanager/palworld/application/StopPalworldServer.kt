package gameservermanager.palworld.application

import gameservermanager.palworld.application.PalworldServerProcessManager
import gameservermanager.palworld.domain.PalworldOperationReport
import gameservermanager.palworld.domain.PalworldOperationHistoryEntry
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration

@Service
class StopPalworldServer(
    private val managementClient: PalworldManagementClient,
    private val processManager: PalworldServerProcessManager,
    private val historyStore: PalworldOperationHistoryStore,
) {
    fun execute(command: Command): PalworldOperationReport {
        require(command.adminPassword.isNotBlank()) {
            "管理者パスワードを入力してください"
        }
        require(command.restApiPort in 1..65535) {
            "REST APIポートは1から65535の範囲で指定してください"
        }
        require(processManager.current()?.alive == true) {
            "Palworldサーバーは起動していません"
        }

        managementClient.save(command.restApiPort, command.adminPassword)
        managementClient.shutdown(
            command.restApiPort,
            command.adminPassword,
            SHUTDOWN_WAIT_SECONDS,
            "Game Server Managerから停止します",
        )

        val deadline = System.nanoTime() + STOP_TIMEOUT.toNanos()
        while (System.nanoTime() < deadline) {
            if (processManager.current()?.alive != true) {
                historyStore.append(
                    PalworldOperationHistoryEntry(
                        Clock.systemUTC().instant(),
                        "STOP",
                        "COMPLETED",
                        "保存後にPalworldサーバーを停止しました",
                    ),
                )
                return PalworldOperationReport(true, "STOP", "保存後にPalworldサーバーを停止しました")
            }
            try {
                Thread.sleep(POLL_INTERVAL.toMillis())
            } catch (exception: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IllegalStateException("Palworldサーバーの停止確認が中断されました", exception)
            }
        }

        processManager.stop()
        throw IllegalStateException("Palworldサーバーが時間内に終了しなかったため強制停止しました")
    }

    data class Command(
        val restApiPort: Int,
        val adminPassword: String,
    )

    companion object {
        private const val SHUTDOWN_WAIT_SECONDS = 1
        private val STOP_TIMEOUT = Duration.ofSeconds(30)
        private val POLL_INTERVAL = Duration.ofMillis(250)
    }
}
