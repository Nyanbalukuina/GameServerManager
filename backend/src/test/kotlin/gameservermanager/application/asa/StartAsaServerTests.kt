package gameservermanager.application.asa

import gameservermanager.application.preflight.ManagedPathPolicy
import gameservermanager.application.preflight.PathInspection
import gameservermanager.application.preflight.ServerEnvironmentInspector
import gameservermanager.application.server.GameServerProcessCommand
import gameservermanager.application.server.GameServerProcessManager
import gameservermanager.application.server.GameServerProcessSnapshot
import gameservermanager.domain.asa.AsaServerState
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

// ASAの起動成功と起動中の異常終了を確認する。
class StartAsaServerTests {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `ASAがゲームポートを待ち受けたら実行中を返す`() {
        val inspector = TestEnvironmentInspector()
        val processManager = TestProcessManager(onStart = { inspector.gamePortAvailable = false })
        val startAsaServer = useCase(inspector, processManager)

        prepareExecutable()
        val status = startAsaServer.execute(command())

        assertThat(status.state).isEqualTo(AsaServerState.RUNNING)
        assertThat(status.processId).isEqualTo(123L)
        assertThat(status.gamePort).isEqualTo(7777)
        assertThat(status.peerPort).isEqualTo(7778)
        assertThat(status.queryPort).isEqualTo(27015)
        assertThat(processManager.startedCommand?.serverId).isEqualTo("asa-main")
    }

    @Test
    fun `ASAが待受前に終了したら失敗する`() {
        val inspector = TestEnvironmentInspector()
        val processManager = TestProcessManager(exitOnStart = true)
        val startAsaServer = useCase(inspector, processManager)

        prepareExecutable()
        assertThatThrownBy { startAsaServer.execute(command()) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("ASAサーバーが起動中に終了しました")
    }

    // テスト用の起動処理を作る。
    private fun useCase(
        inspector: TestEnvironmentInspector,
        processManager: TestProcessManager,
    ): StartAsaServer {
        val policy = TemporaryManagedPathPolicy(tempDir)
        return StartAsaServer(
            CreateAsaServerLaunchCommand(policy),
            inspector,
            processManager,
            Duration.ofMillis(100),
            Duration.ofMillis(1),
        )
    }

    // テスト用のArkAscendedServer.exeを作る。
    private fun prepareExecutable() {
        val executable = tempDir.resolve(
            "servers/asa/main/runtime/ShooterGame/Binaries/Win64/ArkAscendedServer.exe",
        )
        Files.createDirectories(requireNotNull(executable.parent))
        Files.writeString(executable, "test")
    }

    private fun command(): StartAsaServer.Command {
        return StartAsaServer.Command(
            installPath = tempDir.resolve("servers/asa/main/runtime").toString(),
            map = CreateAsaServerLaunchCommand.THE_ISLAND_MAP,
            gamePort = 7777,
            queryPort = 27015,
            maxPlayers = 20,
        )
    }

    // 指定したポートの利用可否をテスト内で切り替える。
    private class TestEnvironmentInspector : ServerEnvironmentInspector {
        var gamePortAvailable = true

        override fun inspectPath(path: String, executableName: String?): PathInspection {
            return PathInspection(true, true, false, true, true, true, 100L, true)
        }

        override fun isUdpPortAvailable(port: Int): Boolean {
            return if (port == 7777) gamePortAvailable else true
        }

        override fun isTcpPortAvailable(port: Int) = true
    }

    // 実プロセスを起動せず、共通プロセス管理の状態を再現する。
    private class TestProcessManager(
        private val onStart: () -> Unit = {},
        private val exitOnStart: Boolean = false,
    ) : GameServerProcessManager {
        var startedCommand: GameServerProcessCommand? = null
        private var snapshot: GameServerProcessSnapshot? = null

        override fun start(command: GameServerProcessCommand): GameServerProcessSnapshot {
            startedCommand = command
            onStart()
            snapshot = GameServerProcessSnapshot(
                serverId = command.serverId,
                processId = 123L,
                alive = !exitOnStart,
                exitCode = if (exitOnStart) 1 else null,
                logPath = command.logPath.toString(),
            )
            return requireNotNull(snapshot)
        }

        override fun current(serverId: String) = snapshot

        override fun stop(serverId: String) {
            snapshot = snapshot?.copy(alive = false, exitCode = 0)
        }
    }

    // テスト用ルート内だけを管理対象として許可する。
    private class TemporaryManagedPathPolicy(root: Path) : ManagedPathPolicy {
        private val normalizedRoot = root.toAbsolutePath().normalize()

        override fun isServerPathAllowed(path: String): Boolean {
            return Path.of(path).toAbsolutePath().normalize().startsWith(normalizedRoot.resolve("servers"))
        }

        override fun isToolPathAllowed(path: String): Boolean {
            return Path.of(path).toAbsolutePath().normalize().startsWith(normalizedRoot.resolve("tools"))
        }

        override fun managedRoot() = normalizedRoot.toString()
    }
}
