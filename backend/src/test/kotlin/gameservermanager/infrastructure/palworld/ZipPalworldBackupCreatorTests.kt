package gameservermanager.infrastructure.palworld

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.zip.ZipFile

class ZipPalworldBackupCreatorTests {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `Saved配下を相対パスのZIPへ保存する`() {
        val source = tempDir.resolve("Saved")
        Files.createDirectories(source.resolve("SaveGames/world"))
        Files.createDirectories(source.resolve("Config/WindowsServer"))
        Files.writeString(source.resolve("SaveGames/world/Level.sav"), "world")
        Files.writeString(source.resolve("Config/WindowsServer/PalWorldSettings.ini"), "settings")
        val clock = Clock.fixed(Instant.parse("2026-08-10T04:00:00Z"), ZoneOffset.UTC)
        val creator = ZipPalworldBackupCreator(clock)

        val result = creator.create(source, tempDir.resolve("backups"))

        assertThat(result.fileCount).isEqualTo(2)
        assertThat(result.sizeBytes).isGreaterThan(0)
        assertThat(Path.of(result.backupPath)).hasFileName("palworld-world-20260810-040000-000.zip")
        ZipFile(result.backupPath).use { zip ->
            assertThat(zip.getEntry("SaveGames/world/Level.sav")).isNotNull()
            assertThat(zip.getEntry("Config/WindowsServer/PalWorldSettings.ini")).isNotNull()
        }
    }
}
