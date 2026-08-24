package gameservermanager.application.asa

import gameservermanager.application.preflight.ServerEnvironmentInspector
import gameservermanager.application.preflight.ManagedPathPolicy
import gameservermanager.application.server.GameServerProcessManager
import gameservermanager.application.server.GameServerProcessSnapshot
import gameservermanager.application.server.GameServerRegistrationStore
import org.springframework.stereotype.Service
import java.nio.file.Path

// GSM再起動後に登録済みASAの実行ファイルと一致するプロセスを再接続する。
@Service
class RecoverAsaServerProcess(
    private val registrationStore: GameServerRegistrationStore,
    private val processManager: GameServerProcessManager,
    private val environmentInspector: ServerEnvironmentInspector,
    private val managedPathPolicy: ManagedPathPolicy,
) {
    @Synchronized
    fun execute(): GameServerProcessSnapshot? {
        processManager.current(StartAsaServer.SERVER_ID)?.let { if (it.alive) return it }
        val server = registrationStore.findByGame("ASA") ?: return null
        if (server.mode != "REAL") return null
        val executable = Path.of(server.installPath)
            .resolve("ShooterGame/Binaries/Win64/ArkAscendedServer.exe")
            .toAbsolutePath().normalize()

        val matches = ProcessHandle.allProcesses().use { processes ->
            processes.filter { process ->
                process.isAlive && process.info().command().orElse("").let { command ->
                    command.isNotBlank() && runCatching {
                        Path.of(command).toAbsolutePath().normalize() == executable
                    }.getOrDefault(false)
                }
            }.toList()
        }
        if (matches.size != 1 || environmentInspector.isUdpPortAvailable(server.gamePort)) return null

        val logPath = Path.of(managedPathPolicy.managedRoot()).resolve("logs/asa-main.log")
        val recovered = processManager.attach(StartAsaServer.SERVER_ID, matches.single().pid(), logPath)
        if (server.state != "RUNNING") registrationStore.update(server.copy(state = "RUNNING"))
        return recovered
    }
}
