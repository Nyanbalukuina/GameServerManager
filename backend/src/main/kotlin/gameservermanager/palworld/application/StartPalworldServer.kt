package gameservermanager.palworld.application

import gameservermanager.shared.preflight.ManagedPathPolicy
import gameservermanager.shared.preflight.ServerEnvironmentInspector
import gameservermanager.palworld.domain.PalworldServerState
import gameservermanager.palworld.domain.PalworldServerStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Duration

@Service
class StartPalworldServer @Autowired constructor(
    private val managedPathPolicy: ManagedPathPolicy,
    private val environmentInspector: ServerEnvironmentInspector,
    private val processManager: PalworldServerProcessManager,
) {
    private var startupTimeout: Duration = DEFAULT_STARTUP_TIMEOUT
    private var pollInterval: Duration = DEFAULT_POLL_INTERVAL

    constructor(
        managedPathPolicy: ManagedPathPolicy,
        environmentInspector: ServerEnvironmentInspector,
        processManager: PalworldServerProcessManager,
        startupTimeout: Duration,
        pollInterval: Duration,
    ) : this(managedPathPolicy, environmentInspector, processManager) {
        this.startupTimeout = startupTimeout
        this.pollInterval = pollInterval
    }

    fun execute(command: Command): PalworldServerStatus {
        require(managedPathPolicy.isServerPathAllowed(command.installPath)) {
            "インストール先が管理範囲外です"
        }
        require(command.gamePort in 1..65535) {
            "ゲームポートは1から65535の範囲で指定してください"
        }
        require(command.maxPlayers in 1..32) {
            "最大プレイヤー数は1から32の範囲で指定してください"
        }
        require(processManager.current()?.alive != true) {
            "Palworldサーバーはすでに起動しています"
        }
        require(environmentInspector.isUdpPortAvailable(command.gamePort)) {
            "ゲームポート UDP ${command.gamePort} は使用中です"
        }

        val installPath = Path.of(command.installPath).toAbsolutePath().normalize()
        val executable = installPath.resolve("PalServer.exe")
        require(Files.isRegularFile(executable)) {
            "PalServer.exeが見つかりません。先にPalworldをインストールしてください"
        }

        val logPath = Path.of(managedPathPolicy.managedRoot())
            .resolve("logs/palworld-main.log")
        val snapshot = processManager.start(
            PalworldServerProcessCommand(
                executable = executable,
                arguments = listOf(
                    "-port=${command.gamePort}",
                    "-players=${command.maxPlayers}",
                ),
                logPath = logPath,
                gamePort = command.gamePort,
            ),
        )

        val deadline = Clock.systemUTC().instant().plus(startupTimeout)
        while (Clock.systemUTC().instant().isBefore(deadline)) {
            val current = processManager.current() ?: snapshot
            if (!current.alive) {
                throw IllegalStateException(
                    "Palworldサーバーが起動中に終了しました。終了コード: ${current.exitCode}、ログ: ${current.logPath}",
                )
            }
            if (!environmentInspector.isUdpPortAvailable(command.gamePort)) {
                return status(current, PalworldServerState.RUNNING, "Palworldサーバーが起動しました")
            }
            try {
                Thread.sleep(pollInterval.toMillis())
            } catch (exception: InterruptedException) {
                processManager.stop()
                Thread.currentThread().interrupt()
                throw IllegalStateException("Palworldサーバーの起動確認が中断されました", exception)
            }
        }

        processManager.stop()
        throw IllegalStateException(
            "Palworldサーバーの起動確認がタイムアウトしました。ログ: ${snapshot.logPath}",
        )
    }

    private fun status(
        snapshot: PalworldServerProcessSnapshot,
        state: PalworldServerState,
        message: String,
    ): PalworldServerStatus {
        return PalworldServerStatus(
            state = state,
            processId = snapshot.processId,
            gamePort = snapshot.gamePort,
            logPath = snapshot.logPath,
            message = message,
        )
    }

    data class Command(
        val installPath: String,
        val gamePort: Int,
        val maxPlayers: Int,
    )

    companion object {
        private val DEFAULT_STARTUP_TIMEOUT = Duration.ofSeconds(60)
        private val DEFAULT_POLL_INTERVAL = Duration.ofMillis(250)
    }
}
