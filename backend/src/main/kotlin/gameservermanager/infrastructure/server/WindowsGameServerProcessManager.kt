package gameservermanager.infrastructure.server

import gameservermanager.application.server.GameServerProcessCommand
import gameservermanager.application.server.GameServerProcessManager
import gameservermanager.application.server.GameServerProcessSnapshot
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.util.concurrent.TimeUnit

// Windows上で複数のゲームサーバープロセスを管理する。
@Component
class WindowsGameServerProcessManager : GameServerProcessManager {
    private val managedProcesses = mutableMapOf<String, ManagedProcess>()

    // serverIdごとにゲームサーバープロセスを起動する。
    @Synchronized
    override fun start(command: GameServerProcessCommand): GameServerProcessSnapshot {
        validate(command)
        require(managedProcesses[command.serverId]?.handle?.isAlive != true) {
            "${command.serverId}はすでに起動しています"
        }

        Files.createDirectories(requireNotNull(command.logPath.parent))
        val process = ProcessBuilder(listOf(command.executable.toString()) + command.arguments)
            .directory(command.workingDirectory.toFile())
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.appendTo(command.logPath.toFile()))
            .start()

        val managedProcess = ManagedProcess(
            process = process,
            handle = process.toHandle(),
            logPath = command.logPath.toString(),
        )
        managedProcesses[command.serverId] = managedProcess

        return snapshot(command.serverId, managedProcess)
    }

    // GSM起動前から動いているプロセスをserverIdへ再接続する。
    @Synchronized
    override fun attach(serverId: String, processId: Long, logPath: java.nio.file.Path): GameServerProcessSnapshot {
        require(serverId.matches(SERVER_ID_PATTERN)) { "サーバー識別子が不正です" }
        require(managedProcesses[serverId]?.handle?.isAlive != true) { "${serverId}はすでに起動しています" }
        val handle = ProcessHandle.of(processId).orElseThrow { IllegalArgumentException("プロセスが見つかりません") }
        require(handle.isAlive) { "プロセスは終了しています" }
        val managedProcess = ManagedProcess(null, handle, logPath.toString())
        managedProcesses[serverId] = managedProcess
        return snapshot(serverId, managedProcess)
    }

    // 指定したserverIdの現在のプロセス状態を返す。
    @Synchronized
    override fun current(serverId: String): GameServerProcessSnapshot? {
        return managedProcesses[serverId]?.let { snapshot(serverId, it) }
    }

    // 指定したserverIdのプロセスを停止し、使用していた入出力を閉じる。
    @Synchronized
    override fun stop(serverId: String) {
        val managed = managedProcesses[serverId] ?: return
        val process = managed.process
        val handle = managed.handle

        // 起動中なら通常終了を試し、時間内に終了しなければ強制終了する。
        if (handle.isAlive) {
            handle.destroy()
            try {
                handle.onExit().get(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            } catch (_: java.util.concurrent.TimeoutException) {
                handle.destroyForcibly()
                handle.onExit().get()
            }
        }

        // Windowsがログファイルを解放できるように入出力ストリームを閉じる。
        process?.outputStream?.close()
        process?.inputStream?.close()
        process?.errorStream?.close()
    }

    // 実行ファイル、作業フォルダー、serverIdを検証する。
    private fun validate(command: GameServerProcessCommand) {
        require(command.serverId.matches(SERVER_ID_PATTERN)) { "サーバー識別子が不正です" }
        require(Files.isRegularFile(command.executable)) { "サーバー実行ファイルが見つかりません" }
        require(Files.isDirectory(command.workingDirectory)) { "作業フォルダーが見つかりません" }
    }

    // Processから外部へ返す状態を作成する。
    private fun snapshot(
        serverId: String,
        managedProcess: ManagedProcess,
    ): GameServerProcessSnapshot {
        val alive = managedProcess.handle.isAlive

        return GameServerProcessSnapshot(
            serverId = serverId,
            processId = managedProcess.handle.pid(),
            alive = alive,
            exitCode = if (alive) null else managedProcess.process?.exitValue(),
            logPath = managedProcess.logPath,
        )
    }

    // Windowsプロセスとログ保存先を内部で保持する。
    private data class ManagedProcess(
        val process: Process?,
        val handle: ProcessHandle,
        val logPath: String,
    )

    companion object {
        private val SERVER_ID_PATTERN = Regex("[a-z0-9-]{1,64}")
        private const val STOP_TIMEOUT_SECONDS = 10L
    }
}
