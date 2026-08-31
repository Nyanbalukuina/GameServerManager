package gameservermanager.palworld.application

import gameservermanager.palworld.domain.PalworldServerState
import gameservermanager.palworld.domain.PalworldServerStatus
import org.springframework.stereotype.Service

@Service
class GetPalworldServerStatus(
    private val processManager: PalworldServerProcessManager,
) {
    fun execute(): PalworldServerStatus {
        val current = processManager.current()
            ?: return PalworldServerStatus(
                state = PalworldServerState.NOT_STARTED,
                processId = null,
                gamePort = null,
                logPath = null,
                message = "Palworldサーバーはまだ起動されていません",
            )

        val state = if (current.alive) {
            PalworldServerState.RUNNING
        } else {
            PalworldServerState.STOPPED
        }
        val message = if (current.alive) {
            "Palworldサーバーは実行中です"
        } else {
            "Palworldサーバーは停止しています。終了コード: ${current.exitCode}"
        }

        return PalworldServerStatus(
            state = state,
            processId = current.processId,
            gamePort = current.gamePort,
            logPath = current.logPath,
            message = message,
        )
    }
}
