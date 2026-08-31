package gameservermanager.asa.application

import gameservermanager.shared.server.GameServerProcessManager
import gameservermanager.asa.domain.AsaServerState
import gameservermanager.asa.domain.AsaServerStatus
import org.springframework.stereotype.Service

// 共通プロセス管理からASAの現在状態を取得する。
@Service
class GetAsaServerStatus(
    private val processManager: GameServerProcessManager,
    private val recoverProcess: RecoverAsaServerProcess,
    private val registrationStore: gameservermanager.shared.server.GameServerRegistrationStore,
) {
    fun execute(): AsaServerStatus {
        recoverProcess.execute()
        val registration = registrationStore.findByGame("ASA")
        val current = processManager.current(StartAsaServer.SERVER_ID)
            ?: return AsaServerStatus(
                AsaServerState.NOT_STARTED, null, null, null, null, null,
                "ASAサーバーはまだ起動されていません",
            )
        val state = if (current.alive) AsaServerState.RUNNING else AsaServerState.STOPPED
        val message = if (current.alive) {
            "ASAサーバーは実行中です"
        } else {
            "ASAサーバーは停止しています。終了コード: ${current.exitCode}"
        }
        return AsaServerStatus(
            state, current.processId, registration?.gamePort, registration?.peerPort,
            registration?.queryPort, current.logPath, message,
        )
    }
}
