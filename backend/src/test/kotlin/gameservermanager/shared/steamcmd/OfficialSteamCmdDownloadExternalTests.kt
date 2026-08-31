package gameservermanager.shared.steamcmd

import gameservermanager.shared.steamcmd.PrepareSteamCmd
import gameservermanager.shared.configuration.StorageProperties
import gameservermanager.shared.preflight.WindowsManagedPathPolicy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

@EnabledIfSystemProperty(named = "steamcmd.external.test", matches = "true")
class OfficialSteamCmdDownloadExternalTests {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `Valve公式ZIPをダウンロードしてsteamcmd exeを展開する`() {
        val policy = WindowsManagedPathPolicy(StorageProperties(tempDir.toString()))
        val useCase = PrepareSteamCmd(
            policy,
            OfficialSteamCmdArchiveDownloader(),
            SafeSteamCmdZipExtractor(),
        )
        val installPath = tempDir.resolve("tools/steamcmd")

        val report = useCase.execute(PrepareSteamCmd.Command(installPath.toString()))

        assertThat(report.completed).isTrue()
        assertThat(report.sourceUrl).isEqualTo(OfficialSteamCmdArchiveDownloader.OFFICIAL_DOWNLOAD_URL)
        assertThat(report.downloadedBytes).isGreaterThan(0)
        assertThat(Path.of(report.executablePath)).isRegularFile()
    }
}
