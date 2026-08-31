package gameservermanager.asa.application

import gameservermanager.shared.server.GameServerProcessManager
import gameservermanager.asa.domain.AsaServerState
import gameservermanager.asa.domain.AsaServerStatus
import org.springframework.stereotype.Service
import java.time.Duration

// RCONでワールドを保存してからASAを安全に停止する。
@Service
class StopAsaServer(
    private val managementClient: AsaManagementClient,
    private val processManager: GameServerProcessManager,
) {
    fun execute(command: Command): AsaServerStatus {
        require(processManager.current(StartAsaServer.SERVER_ID)?.alive == true) { "ASAサーバーは起動していません" }
        managementClient.execute(command.rconPort, command.adminPassword, "SaveWorld")
        managementClient.execute(command.rconPort, command.adminPassword, "DoExit")

        repeat(STOP_POLL_COUNT) {
            val current = processManager.current(StartAsaServer.SERVER_ID)
            if (current?.alive != true) {
                return AsaServerStatus(
                    AsaServerState.STOPPED, current?.processId, null, null, null, current?.logPath,
                    "ワールド保存後にASAサーバーを停止しました",
                )
            }
            Thread.sleep(STOP_POLL_INTERVAL.toMillis())
        }

        processManager.stop(StartAsaServer.SERVER_ID)
        val stopped = processManager.current(StartAsaServer.SERVER_ID)
        return AsaServerStatus(
            AsaServerState.STOPPED, stopped?.processId, null, null, null, stopped?.logPath,
            "ワールド保存後、終了待機がタイムアウトしたためASAプロセスを停止しました",
        )
    }

    data class Command(val rconPort: Int, val adminPassword: String)

    companion object {
        private const val STOP_POLL_COUNT = 30
        private val STOP_POLL_INTERVAL = Duration.ofSeconds(1)
    }
}
