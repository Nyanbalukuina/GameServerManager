package gameservermanager.shared.steamcmd

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SafeSteamCmdZipExtractorTests {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `通常のZIPを指定フォルダーへ展開する`() {
        val archive = createArchive("steamcmd.exe", "steamcmd")
        val destination = tempDir.resolve("destination")

        SafeSteamCmdZipExtractor().extract(archive, destination)

        assertThat(destination.resolve("steamcmd.exe")).hasContent("steamcmd")
    }

    @Test
    fun `展開先の外へ出るZIPエントリーを拒否する`() {
        val archive = createArchive("../outside.exe", "invalid")
        val destination = tempDir.resolve("destination")

        assertThatThrownBy {
            SafeSteamCmdZipExtractor().extract(archive, destination)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("不正なパス")

        assertThat(tempDir.resolve("outside.exe")).doesNotExist()
    }

    private fun createArchive(entryName: String, content: String): Path {
        val archive = tempDir.resolve("archive-${entryName.hashCode()}.zip")
        ZipOutputStream(Files.newOutputStream(archive)).use { zip ->
            zip.putNextEntry(ZipEntry(entryName))
            zip.write(content.toByteArray())
            zip.closeEntry()
        }
        return archive
    }
}
