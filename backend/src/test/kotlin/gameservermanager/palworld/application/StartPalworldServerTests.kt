package gameservermanager.palworld.application

import gameservermanager.shared.preflight.ManagedPathPolicy
import gameservermanager.shared.preflight.PathInspection
import gameservermanager.shared.preflight.ServerEnvironmentInspector
import gameservermanager.palworld.domain.PalworldServerState
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

class StartPalworldServerTests {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `プロセスが生存しUDPポートが使用中になれば起動成功とする`() {
        val manager = FakeProcessManager(alive = true)
        val useCase = useCase(FakeEnvironmentInspector(listOf(true, false)), manager)
        val installPath = preparePalServer()

        val status = useCase.execute(command(installPath))

        assertThat(status.state).isEqualTo(PalworldServerState.RUNNING)
        assertThat(status.processId).isEqualTo(1234)
        assertThat(manager.command?.arguments).containsExactly("-port=8211", "-players=3")
        assertThat(manager.command?.logPath).isEqualTo(tempDir.resolve("logs/palworld-main.log"))
    }

    @Test
    fun `起動中にプロセスが終了したら終了コードを報告する`() {
        val manager = FakeProcessManager(alive = false, exitCode = 7)
        val useCase = useCase(FakeEnvironmentInspector(listOf(true, true)), manager)
        val installPath = preparePalServer()

        assertThatThrownBy { useCase.execute(command(installPath)) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("終了コード: 7")
            .hasMessageContaining("palworld-main.log")
    }

    @Test
    fun `待受開始を確認できなければプロセスを停止する`() {
        val manager = FakeProcessManager(alive = true)
        val useCase = StartPalworldServer(
            TemporaryManagedPathPolicy(tempDir),
            FakeEnvironmentInspector(listOf(true)),
            manager,
            Duration.ofMillis(2),
            Duration.ofMillis(1),
        )
        val installPath = preparePalServer()

        assertThatThrownBy { useCase.execute(command(installPath)) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("タイムアウト")
        assertThat(manager.stopped).isTrue()
    }

    private fun useCase(
        inspector: ServerEnvironmentInspector,
        manager: PalworldServerProcessManager,
    ): StartPalworldServer {
        return StartPalworldServer(
            TemporaryManagedPathPolicy(tempDir),
            inspector,
            manager,
            Duration.ofMillis(50),
            Duration.ofMillis(1),
        )
    }

    private fun preparePalServer(): Path {
        val installPath = tempDir.resolve("servers/palworld/main/runtime")
        Files.createDirectories(installPath)
        Files.writeString(installPath.resolve("PalServer.exe"), "test")
        return installPath
    }

    private fun command(installPath: Path): StartPalworldServer.Command {
        return StartPalworldServer.Command(installPath.toString(), 8211, 3)
    }

    private class FakeEnvironmentInspector(
        private val portAvailability: List<Boolean>,
    ) : ServerEnvironmentInspector {
        private var index = 0

        override fun inspectPath(path: String, executableName: String?): PathInspection {
            error("not used")
        }

        override fun isUdpPortAvailable(port: Int): Boolean {
            val value = portAvailability.getOrElse(index) { portAvailability.last() }
            index += 1
            return value
        }

        override fun isTcpPortAvailable(port: Int): Boolean {
            error("not used")
        }
    }

    private class FakeProcessManager(
        private var alive: Boolean,
        private val exitCode: Int? = null,
    ) : PalworldServerProcessManager {
        var command: PalworldServerProcessCommand? = null
        var stopped = false

        override fun start(command: PalworldServerProcessCommand): PalworldServerProcessSnapshot {
            this.command = command
            return snapshot()
        }

        override fun current(): PalworldServerProcessSnapshot? {
            return if (command == null) null else snapshot()
        }

        override fun stop() {
            stopped = true
            alive = false
        }

        private fun snapshot(): PalworldServerProcessSnapshot {
            return PalworldServerProcessSnapshot(
                processId = 1234,
                alive = alive,
                exitCode = exitCode,
                gamePort = 8211,
                logPath = command?.logPath?.toString() ?: "palworld-main.log",
            )
        }
    }

    private class TemporaryManagedPathPolicy(root: Path) : ManagedPathPolicy {
        private val normalizedRoot = root.toAbsolutePath().normalize()

        override fun isServerPathAllowed(path: String): Boolean {
            return Path.of(path).toAbsolutePath().normalize().startsWith(normalizedRoot.resolve("servers"))
        }

        override fun isToolPathAllowed(path: String): Boolean {
            return Path.of(path).toAbsolutePath().normalize().startsWith(normalizedRoot.resolve("tools"))
        }

        override fun managedRoot(): String {
            return normalizedRoot.toString()
        }
    }
}
