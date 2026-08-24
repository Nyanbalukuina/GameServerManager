package gameservermanager.infrastructure.server

import gameservermanager.application.server.GameServerProcessCommand
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

// Windows上でプロセスを起動・確認・停止できることを確認する。
@EnabledOnOs(OS.WINDOWS)
class WindowsGameServerProcessManagerTests {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `serverIdごとにプロセスを起動して停止する`() {
        val manager = WindowsGameServerProcessManager()
        val executable = powershellExecutable()
        val logPath = tempDir.resolve("logs/test-server.log")

        // 長時間動作するテスト用PowerShellプロセスを起動する。
        val started = manager.start(
            GameServerProcessCommand(
                serverId = "test-server",
                executable = executable,
                workingDirectory = tempDir,
                arguments = listOf(
                    "-NoProfile",
                    "-Command",
                    "Start-Sleep -Seconds 30",
                ),
                logPath = logPath,
            ),
        )

        // 起動中の状態を確認する。
        assertThat(started.serverId).isEqualTo("test-server")
        assertThat(started.processId).isPositive()
        assertThat(started.alive).isTrue()
        assertThat(started.exitCode).isNull()
        assertThat(Files.isRegularFile(logPath)).isTrue()

        // プロセスを停止して終了状態を確認する。
        manager.stop("test-server")
        val stopped = requireNotNull(manager.current("test-server"))
        assertThat(stopped.alive).isFalse()
        assertThat(stopped.exitCode).isNotNull()
        deleteLogAfterRelease(logPath)
    }

    @Test
    fun `同じserverIdのプロセスを同時に起動できない`() {
        val manager = WindowsGameServerProcessManager()
        val command = GameServerProcessCommand(
            serverId = "test-server",
            executable = powershellExecutable(),
            workingDirectory = tempDir,
            arguments = listOf(
                "-NoProfile",
                "-Command",
                "Start-Sleep -Seconds 30",
            ),
            logPath = tempDir.resolve("logs/test-server.log"),
        )

        // 同じserverIdを2回起動すると拒否されることを確認する。
        manager.start(command)
        try {
            assertThatThrownBy { manager.start(command) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("test-serverはすでに起動しています")
        } finally {
            manager.stop("test-server")
            deleteLogAfterRelease(command.logPath)
        }
    }

    @Test
    fun `GSM外で起動した既存プロセスへPIDで再接続して停止する`() {
        val manager = WindowsGameServerProcessManager()
        val external = ProcessBuilder(
            powershellExecutable().toString(), "-NoProfile", "-Command", "Start-Sleep -Seconds 30",
        ).start()
        val logPath = tempDir.resolve("logs/recovered-server.log")

        try {
            // 既存PIDを取り込み、共通プロセス管理から状態を取得できることを確認する。
            val recovered = manager.attach("recovered-server", external.pid(), logPath)
            assertThat(recovered.processId).isEqualTo(external.pid())
            assertThat(recovered.alive).isTrue()
            assertThat(manager.current("recovered-server")?.alive).isTrue()

            // 再接続したプロセスも通常の管理対象と同じように停止できることを確認する。
            manager.stop("recovered-server")
            assertThat(external.isAlive).isFalse()
        } finally {
            if (external.isAlive) external.destroyForcibly().waitFor()
        }
    }

    // Windowsが終了直後のログを解放するまで待って削除する。
    private fun deleteLogAfterRelease(logPath: Path) {
        repeat(40) {
            try {
                Files.deleteIfExists(logPath)
                return
            } catch (_: java.io.IOException) {
                Thread.sleep(50)
            }
        }
        Files.deleteIfExists(logPath)
    }

    // Windows標準のPowerShell実行ファイルを取得する。
    private fun powershellExecutable(): Path {
        return Path.of(
            System.getenv("SystemRoot"),
            "System32",
            "WindowsPowerShell",
            "v1.0",
            "powershell.exe",
        )
    }
}
