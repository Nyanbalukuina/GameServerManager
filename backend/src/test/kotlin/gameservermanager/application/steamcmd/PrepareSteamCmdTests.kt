package gameservermanager.application.steamcmd

import gameservermanager.configuration.StorageProperties
import gameservermanager.infrastructure.steamcmd.SafeSteamCmdZipExtractor
import gameservermanager.infrastructure.windows.WindowsManagedPathPolicy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PrepareSteamCmdTests {
    // テストで使用する一時フォルダーを受け取る。
    @TempDir
    lateinit var tempDir: Path

    // SteamCMDの準備処理を実行し、結果を検証する。
    @Test
    fun `公式ZIP相当のファイルを安全に展開して一時ファイルを削除する`() {
        val policy = WindowsManagedPathPolicy(StorageProperties(tempDir.toString()))
        val useCase = PrepareSteamCmd(
            policy,
            FakeSteamCmdArchiveDownloader(),
            SafeSteamCmdZipExtractor(),
        )
        val installPath = tempDir.resolve("tools/steamcmd")

        val report = useCase.execute(PrepareSteamCmd.Command(installPath.toString()))

        assertThat(report.completed).isTrue()
        assertThat(Path.of(report.executablePath)).isRegularFile()
        assertThat(Files.readString(Path.of(report.executablePath))).isEqualTo("demo steamcmd")
        Files.list(installPath.parent).use { paths ->
            assertThat(paths.map { it.fileName.toString() }.toList())
                .doesNotContainAnyElementsOf(listOf(".steamcmd-prepare-"))
        }
    }

    private class FakeSteamCmdArchiveDownloader : SteamCmdArchiveDownloader {
        override fun download(destination: Path): DownloadedSteamCmdArchive {
            ZipOutputStream(Files.newOutputStream(destination)).use { zip ->
                zip.putNextEntry(ZipEntry("steamcmd.exe"))
                zip.write("demo steamcmd".toByteArray())
                zip.closeEntry()
            }
            return DownloadedSteamCmdArchive(
                path = destination,
                sourceUrl = "https://example.test/steamcmd.zip",
                sizeBytes = Files.size(destination),
            )
        }
    }
}
