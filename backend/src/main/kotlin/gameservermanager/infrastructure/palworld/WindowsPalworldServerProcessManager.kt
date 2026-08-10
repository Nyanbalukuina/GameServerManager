package gameservermanager.infrastructure.palworld

import gameservermanager.application.palworld.PalworldServerProcessCommand
import gameservermanager.application.palworld.PalworldServerProcessManager
import gameservermanager.application.palworld.PalworldServerProcessSnapshot
import org.springframework.stereotype.Component
import java.nio.file.Files

@Component
class WindowsPalworldServerProcessManager : PalworldServerProcessManager {
    private var managedProcess: ManagedProcess? = null

    @Synchronized
    override fun start(command: PalworldServerProcessCommand): PalworldServerProcessSnapshot {
        require(managedProcess?.process?.isAlive != true) {
            "Palworldサーバーはすでに起動しています"
        }
        Files.createDirectories(requireNotNull(command.logPath.parent))
        val process = ProcessBuilder(listOf(command.executable.toString()) + command.arguments)
            .directory(command.executable.parent.toFile())
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.appendTo(command.logPath.toFile()))
            .start()
        managedProcess = ManagedProcess(process, command.gamePort, command.logPath.toString())
        return snapshot(requireNotNull(managedProcess))
    }

    @Synchronized
    override fun current(): PalworldServerProcessSnapshot? {
        return managedProcess?.let(::snapshot)
    }

    @Synchronized
    override fun stop() {
        val process = managedProcess?.process ?: return
        if (process.isAlive) {
            process.destroy()
            if (!process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly()
            }
        }
    }

    private fun snapshot(managed: ManagedProcess): PalworldServerProcessSnapshot {
        val alive = managed.process.isAlive
        return PalworldServerProcessSnapshot(
            processId = managed.process.pid(),
            alive = alive,
            exitCode = if (alive) null else managed.process.exitValue(),
            gamePort = managed.gamePort,
            logPath = managed.logPath,
        )
    }

    private data class ManagedProcess(
        val process: Process,
        val gamePort: Int,
        val logPath: String,
    )
}
