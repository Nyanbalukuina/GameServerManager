package gameservermanager.application.asa

import gameservermanager.domain.asa.AsaServerStatus
import org.springframework.stereotype.Service

// ASAを安全に停止してから同じ設定で起動する。
@Service
class RestartAsaServer(
    private val stopAsaServer: StopAsaServer,
    private val startAsaServer: StartAsaServer,
) {
    fun execute(command: Command): AsaServerStatus {
        stopAsaServer.execute(StopAsaServer.Command(command.rconPort, command.adminPassword))
        return startAsaServer.execute(
            StartAsaServer.Command(
                command.installPath, command.map, command.gamePort, command.queryPort, command.maxPlayers,
            ),
        )
    }

    data class Command(
        val installPath: String,
        val map: String,
        val gamePort: Int,
        val queryPort: Int,
        val maxPlayers: Int,
        val rconPort: Int,
        val adminPassword: String,
    )
}
