package gameservermanager.shared.steamcmd

import gameservermanager.shared.preflight.ManagedPathPolicy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class SteamCmdServerVersionReaderTests {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `manifestとSteamCMD出力から現在版と最新版を取得する`() {
        val steamCmd = tempDir.resolve("tools/steamcmd")
        val install = tempDir.resolve("servers/asa/main/runtime")
        Files.createDirectories(steamCmd.resolve("steamapps"))
        Files.createDirectories(install)
        Files.writeString(steamCmd.resolve("steamcmd.exe"), "test")
        Files.writeString(
            steamCmd.resolve("steamapps/appmanifest_2430930.acf"),
            "\"AppState\" { \"buildid\" \"100\" }",
        )
        val runner = LatestBuildRunner()
        val reader = SteamCmdServerVersionReader(TemporaryPathPolicy(tempDir), runner)

        assertThat(reader.currentBuildId("ASA", 2430930, steamCmd.toString(), install.toString())).isEqualTo("100")
        assertThat(reader.latestBuildId("ASA", 2430930, steamCmd.toString(), install.toString())).isEqualTo("101")
        assertThat(runner.arguments).containsSubsequence("+app_info_print", "2430930")
    }

    private class LatestBuildRunner : SteamCmdProcessRunner {
        lateinit var arguments: List<String>

        override fun run(executable: Path, arguments: List<String>, logPath: Path): SteamCmdProcessResult {
            this.arguments = arguments
            Files.createDirectories(requireNotNull(logPath.parent))
            Files.writeString(logPath, "\"branches\" { \"public\" { \"buildid\" \"101\" } }")
            return SteamCmdProcessResult(0, logPath.toString())
        }
    }

    private class TemporaryPathPolicy(root: Path) : ManagedPathPolicy {
        private val root = root.toAbsolutePath().normalize()
        override fun isServerPathAllowed(path: String) = Path.of(path).toAbsolutePath().normalize().startsWith(root.resolve("servers"))
        override fun isToolPathAllowed(path: String) = Path.of(path).toAbsolutePath().normalize().startsWith(root.resolve("tools"))
        override fun managedRoot() = root.toString()
    }
}
