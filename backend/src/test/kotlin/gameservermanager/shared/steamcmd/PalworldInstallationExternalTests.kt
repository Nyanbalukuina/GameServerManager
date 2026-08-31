package gameservermanager.shared.steamcmd

import gameservermanager.palworld.application.InstallPalworldServer
import gameservermanager.shared.preflight.ManagedPathPolicy
import gameservermanager.shared.steamcmd.PrepareSteamCmd
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@EnabledIfSystemProperty(named = "palworld.external.test", matches = "true")
class PalworldInstallationExternalTests {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `SteamCMDでPalworld Dedicated Serverをインストールする`() {
        val policy = TemporaryManagedPathPolicy(tempDir)
        val steamCmdPath = tempDir.resolve("tools/steamcmd")
        val installPath = tempDir.resolve("servers/palworld/main/runtime")
        val prepareSteamCmd = PrepareSteamCmd(
            policy,
            OfficialSteamCmdArchiveDownloader(),
            SafeSteamCmdZipExtractor(),
        )
        val installPalworldServer = InstallPalworldServer(
            policy,
            WindowsSteamCmdProcessRunner(),
        )

        prepareSteamCmd.execute(PrepareSteamCmd.Command(steamCmdPath.toString()))
        val logPath = installPath.resolve("steamcmd-install.log")
        try {
            val report = installPalworldServer.execute(
                InstallPalworldServer.Command(steamCmdPath.toString(), installPath.toString()),
            )

            assertThat(report.completed).isTrue()
            assertThat(report.exitCode).isZero()
            assertThat(Files.size(Path.of(report.executablePath))).isGreaterThan(0)
            assertThat(Path.of(report.logPath)).isNotEmptyFile()
        } finally {
            if (Files.isRegularFile(logPath)) {
                val diagnosticLog = Path.of("build/test-results/palworld-external.log")
                Files.createDirectories(requireNotNull(diagnosticLog.parent))
                Files.copy(logPath, diagnosticLog, StandardCopyOption.REPLACE_EXISTING)
            }
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
