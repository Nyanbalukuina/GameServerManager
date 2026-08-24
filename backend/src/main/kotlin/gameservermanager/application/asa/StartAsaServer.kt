package gameservermanager.application.asa

import gameservermanager.application.preflight.ServerEnvironmentInspector
import gameservermanager.application.server.GameServerProcessCommand
import gameservermanager.application.server.GameServerProcessManager
import gameservermanager.application.server.GameServerProcessSnapshot
import gameservermanager.domain.asa.AsaServerState
import gameservermanager.domain.asa.AsaServerStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.time.Clock
import java.time.Duration

// ASAプロセスを起動し、ゲームポートの待受開始まで確認する。
@Service
class StartAsaServer @Autowired constructor(
    private val createLaunchCommand: CreateAsaServerLaunchCommand,
    private val environmentInspector: ServerEnvironmentInspector,
    private val processManager: GameServerProcessManager,
) {
    private var startupTimeout: Duration = DEFAULT_STARTUP_TIMEOUT
    private var pollInterval: Duration = DEFAULT_POLL_INTERVAL

    // テストでは短い待機時間を指定できるようにする。
    constructor(
        createLaunchCommand: CreateAsaServerLaunchCommand,
        environmentInspector: ServerEnvironmentInspector,
        processManager: GameServerProcessManager,
        startupTimeout: Duration,
        pollInterval: Duration,
    ) : this(createLaunchCommand, environmentInspector, processManager) {
        this.startupTimeout = startupTimeout
        this.pollInterval = pollInterval
    }

    // ASAを起動し、正常な待受状態になったら実行中の状態を返す。
    fun execute(command: Command): AsaServerStatus {
        require(processManager.current(SERVER_ID)?.alive != true) { "ASAサーバーはすでに起動しています" }

        val launchCommand = createLaunchCommand.execute(
            CreateAsaServerLaunchCommand.Command(
                installPath = command.installPath,
                map = command.map,
                gamePort = command.gamePort,
                queryPort = command.queryPort,
                maxPlayers = command.maxPlayers,
            ),
        )

        require(Files.isRegularFile(launchCommand.executable)) {
            "ArkAscendedServer.exeが見つかりません。先にASAをインストールしてください"
        }

        // 起動前にASAが利用するUDPポートの重複を確認する。
        require(environmentInspector.isUdpPortAvailable(launchCommand.gamePort)) {
            "ゲームポート UDP ${launchCommand.gamePort} は使用中です"
        }
        require(environmentInspector.isUdpPortAvailable(launchCommand.peerPort)) {
            "Peerポート UDP ${launchCommand.peerPort} は使用中です"
        }
        require(environmentInspector.isUdpPortAvailable(launchCommand.queryPort)) {
            "Queryポート UDP ${launchCommand.queryPort} は使用中です"
        }

        val snapshot = processManager.start(
            GameServerProcessCommand(
                serverId = SERVER_ID,
                executable = launchCommand.executable,
                workingDirectory = requireNotNull(launchCommand.executable.parent),
                arguments = launchCommand.arguments,
                logPath = launchCommand.logPath,
            ),
        )

        // プロセスの生存とゲームポートの待受開始を確認する。
        val deadline = Clock.systemUTC().instant().plus(startupTimeout)
        while (Clock.systemUTC().instant().isBefore(deadline)) {
            val current = processManager.current(SERVER_ID) ?: snapshot
            if (!current.alive) {
                throw IllegalStateException(
                    "ASAサーバーが起動中に終了しました。終了コード: ${current.exitCode}、ログ: ${current.logPath}",
                )
            }
            if (!environmentInspector.isUdpPortAvailable(launchCommand.gamePort)) {
                return status(current, launchCommand, "ASAサーバーが起動しました")
            }

            try {
                Thread.sleep(pollInterval.toMillis())
            } catch (exception: InterruptedException) {
                processManager.stop(SERVER_ID)
                Thread.currentThread().interrupt()
                throw IllegalStateException("ASAサーバーの起動確認が中断されました", exception)
            }
        }

        processManager.stop(SERVER_ID)
        throw IllegalStateException(
            "ASAサーバーの起動確認がタイムアウトしました。ログ: ${snapshot.logPath}",
        )
    }

    // 共通プロセス状態をASAの状態へ変換する。
    private fun status(
        snapshot: GameServerProcessSnapshot,
        launchCommand: AsaServerLaunchCommand,
        message: String,
    ): AsaServerStatus {
        return AsaServerStatus(
            state = AsaServerState.RUNNING,
            processId = snapshot.processId,
            gamePort = launchCommand.gamePort,
            peerPort = launchCommand.peerPort,
            queryPort = launchCommand.queryPort,
            logPath = snapshot.logPath,
            message = message,
        )
    }

    data class Command(
        val installPath: String,
        val map: String,
        val gamePort: Int,
        val queryPort: Int,
        val maxPlayers: Int,
    )

    companion object {
        const val SERVER_ID = "asa-main"
        private val DEFAULT_STARTUP_TIMEOUT = Duration.ofMinutes(5)
        private val DEFAULT_POLL_INTERVAL = Duration.ofMillis(250)
    }
}
