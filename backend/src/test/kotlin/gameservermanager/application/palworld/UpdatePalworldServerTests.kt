package gameservermanager.application.palworld

import gameservermanager.application.preflight.ManagedPathPolicy
import gameservermanager.application.steamcmd.SteamCmdProcessResult
import gameservermanager.application.steamcmd.SteamCmdProcessRunner
import gameservermanager.domain.palworld.PalworldOperationHistoryEntry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class UpdatePalworldServerTests {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `停止中に更新して成功履歴を残す`() {
        val history = MemoryHistoryStore()
        val installer = InstallPalworldServer(TemporaryPolicy(tempDir), SuccessfulRunner())
        val useCase = UpdatePalworldServer(FixedProcessManager(false), installer, history)
        val steamCmdPath = tempDir.resolve("tools/steamcmd")
        val installPath = tempDir.resolve("servers/palworld/main/runtime")
        Files.createDirectories(steamCmdPath)
        Files.writeString(steamCmdPath.resolve("steamcmd.exe"), "test")

        val report = useCase.execute(
            UpdatePalworldServer.Command(steamCmdPath.toString(), installPath.toString()),
        )

        assertThat(report.completed).isTrue()
        assertThat(history.entries.single().operation).isEqualTo("UPDATE")
        assertThat(history.entries.single().status).isEqualTo("COMPLETED")
    }

    @Test
    fun `実行中は更新しない`() {
        val history = MemoryHistoryStore()
        val installer = InstallPalworldServer(TemporaryPolicy(tempDir), SuccessfulRunner())
        val useCase = UpdatePalworldServer(FixedProcessManager(true), installer, history)

        assertThatThrownBy {
            useCase.execute(UpdatePalworldServer.Command("tools", "servers"))
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("停止してから更新")
        assertThat(history.entries).isEmpty()
    }

    private class SuccessfulRunner : SteamCmdProcessRunner {
        override fun run(
            executable: Path,
            arguments: List<String>,
            logPath: Path,
        ): SteamCmdProcessResult {
            val installPath = Path.of(arguments[1])
            Files.writeString(installPath.resolve("PalServer.exe"), "test")
            Files.writeString(logPath, "updated")
            return SteamCmdProcessResult(0, logPath.toString())
        }
    }

    private class FixedProcessManager(private val alive: Boolean) : PalworldServerProcessManager {
        override fun start(command: PalworldServerProcessCommand): PalworldServerProcessSnapshot {
            error("not used")
        }

        override fun current(): PalworldServerProcessSnapshot? {
            return if (alive) PalworldServerProcessSnapshot(1, true, null, 8211, "log") else null
        }

        override fun stop() {
        }
    }

    private class MemoryHistoryStore : PalworldOperationHistoryStore {
        val entries = mutableListOf<PalworldOperationHistoryEntry>()

        override fun append(entry: PalworldOperationHistoryEntry) {
            entries += entry
        }

        override fun latest(limit: Int): List<PalworldOperationHistoryEntry> {
            return entries.takeLast(limit).reversed()
        }
    }

    private class TemporaryPolicy(root: Path) : ManagedPathPolicy {
        private val root = root.toAbsolutePath().normalize()

        override fun isServerPathAllowed(path: String): Boolean {
            return Path.of(path).toAbsolutePath().normalize().startsWith(root.resolve("servers"))
        }

        override fun isToolPathAllowed(path: String): Boolean {
            return Path.of(path).toAbsolutePath().normalize().startsWith(root.resolve("tools"))
        }

        override fun managedRoot(): String {
            return root.toString()
        }
    }
}
