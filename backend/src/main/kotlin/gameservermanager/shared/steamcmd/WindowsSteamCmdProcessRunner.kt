package gameservermanager.shared.steamcmd

import gameservermanager.shared.steamcmd.SteamCmdProcessResult
import gameservermanager.shared.steamcmd.SteamCmdProcessRunner
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.TimeUnit

@Component
class WindowsSteamCmdProcessRunner : SteamCmdProcessRunner {
    // 複数のゲームがSteamCMDを同時に実行しないようにする。
    @Synchronized
    override fun run(
        executable: Path,
        arguments: List<String>,
        logPath: Path,
    ): SteamCmdProcessResult {
        Files.createDirectories(requireNotNull(logPath.parent))
        val process = ProcessBuilder(listOf(executable.toString()) + arguments)
            .directory(executable.parent.toFile())
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.appendTo(logPath.toFile()))
            .start()

        val completed = try {
            process.waitFor(TIMEOUT.toMinutes(), TimeUnit.MINUTES)
        } catch (exception: InterruptedException) {
            process.destroyForcibly()
            Thread.currentThread().interrupt()
            throw IllegalStateException("SteamCMDの実行が中断されました。ログ: $logPath", exception)
        }

        if (!completed) {
            process.destroyForcibly()
            process.waitFor()
            throw IllegalStateException("SteamCMDの実行がタイムアウトしました。ログ: $logPath")
        }

        return SteamCmdProcessResult(
            exitCode = process.exitValue(),
            logPath = logPath.toString(),
        )
    }

    companion object {
        private val TIMEOUT = Duration.ofMinutes(30)
    }
}
