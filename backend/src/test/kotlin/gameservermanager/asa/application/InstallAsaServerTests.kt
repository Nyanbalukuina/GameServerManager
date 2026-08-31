package gameservermanager.asa.application

import gameservermanager.shared.preflight.ManagedPathPolicy
import gameservermanager.shared.steamcmd.SteamCmdProcessResult
import gameservermanager.shared.steamcmd.SteamCmdProcessRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class InstallAsaServerTests {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `ASAのApp IDでSteamCMDを実行して実行ファイルを確認する`() {
        val steamCmdPath = tempDir.resolve("tools/steamcmd")
        val installPath = tempDir.resolve("servers/asa/main/runtime")
        Files.createDirectories(steamCmdPath)
        Files.writeString(steamCmdPath.resolve("steamcmd.exe"), "test")

        val runner = RecordingProcessRunner()
        val installAsaServer = InstallAsaServer(
            TemporaryManagedPathPolicy(tempDir),
            runner,
        )

        val report = installAsaServer.execute(
            InstallAsaServer.Command(
                steamCmdPath = steamCmdPath.toString(),
                installPath = installPath.toString(),
            ),
        )

        assertThat(report.completed).isTrue()
        assertThat(report.appId).isEqualTo(2430930)
        assertThat(Path.of(report.executablePath).fileName.toString())
            .isEqualTo("ArkAscendedServer.exe")
        assertThat(runner.arguments).containsSubsequence(
            "+app_update",
            "2430930",
            "validate",
        )
    }

    private class RecordingProcessRunner : SteamCmdProcessRunner {
        lateinit var arguments: List<String>

        override fun run(
            executable: Path,
            arguments: List<String>,
            logPath: Path,
        ): SteamCmdProcessResult {
            this.arguments = arguments

            val installPath = Path.of(arguments[1])
            val gameExecutable = installPath
                .resolve("ShooterGame")
                .resolve("Binaries")
                .resolve("Win64")
                .resolve("ArkAscendedServer.exe")

            Files.createDirectories(requireNotNull(gameExecutable.parent))
            Files.writeString(gameExecutable, "test")
            Files.writeString(logPath, "test")

            return SteamCmdProcessResult(
                exitCode = 0,
                logPath = logPath.toString(),
            )
        }
    }

    private class TemporaryManagedPathPolicy(
        root: Path,
    ) : ManagedPathPolicy {
        private val normalizedRoot = root.toAbsolutePath().normalize()

        override fun isServerPathAllowed(path: String): Boolean {
            return Path.of(path)
                .toAbsolutePath()
                .normalize()
                .startsWith(normalizedRoot.resolve("servers"))
        }

        override fun isToolPathAllowed(path: String): Boolean {
            return Path.of(path)
                .toAbsolutePath()
                .normalize()
                .startsWith(normalizedRoot.resolve("tools"))
        }

        override fun managedRoot(): String {
            return normalizedRoot.toString()
        }
    }
}