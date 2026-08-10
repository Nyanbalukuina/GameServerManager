package gameservermanager.application.palworld

import gameservermanager.application.preflight.ManagedPathPolicy
import gameservermanager.domain.palworld.PalworldOperationHistoryEntry
import gameservermanager.infrastructure.palworld.ZipPalworldBackupCreator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class BackupPalworldServerRetentionTests {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `バックアップは最新3個だけを保持する`() {
        val installPath = tempDir.resolve("servers/palworld/main/runtime")
        val savedPath = installPath.resolve("Pal/Saved")
        val backupPath = tempDir.resolve("backups/palworld-main/world")
        Files.createDirectories(savedPath)
        Files.createDirectories(backupPath)
        Files.writeString(savedPath.resolve("world.sav"), "world")
        repeat(3) { index ->
            Files.writeString(backupPath.resolve("palworld-world-2026010${index + 1}-000000-000.zip"), "old")
        }
        val useCase = BackupPalworldServer(
            ManagedPolicy(tempDir),
            StoppedProcessManager,
            ZipPalworldBackupCreator(),
            EmptyHistoryStore,
        )

        useCase.execute(BackupPalworldServer.Command(installPath.toString()))

        Files.list(backupPath).use { files ->
            assertThat(files.filter { it.toString().endsWith(".zip") }.count()).isEqualTo(3)
        }
        assertThat(backupPath.resolve("palworld-world-20260101-000000-000.zip")).doesNotExist()
    }

    private class ManagedPolicy(private val root: Path) : ManagedPathPolicy {
        override fun managedRoot(): String = root.toString()
        override fun isServerPathAllowed(path: String): Boolean = true
        override fun isToolPathAllowed(path: String): Boolean = true
    }

    private object StoppedProcessManager : PalworldServerProcessManager {
        override fun start(command: PalworldServerProcessCommand) = error("not used")
        override fun current(): PalworldServerProcessSnapshot? = null
        override fun stop() = Unit
    }

    private object EmptyHistoryStore : PalworldOperationHistoryStore {
        override fun append(entry: PalworldOperationHistoryEntry) = Unit
        override fun latest(limit: Int): List<PalworldOperationHistoryEntry> = emptyList()
    }
}
