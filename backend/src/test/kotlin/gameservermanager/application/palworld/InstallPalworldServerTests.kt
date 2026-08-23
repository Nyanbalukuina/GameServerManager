package gameservermanager.application.palworld

import gameservermanager.application.preflight.ManagedPathPolicy
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class InstallPalworldServerTests {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `固定App IDでSteamCMDを実行してPalServer exeを確認する`() {
        val runner = RecordingProcessRunner(createGameExecutable = true)
        val useCase = InstallPalworldServer(TemporaryManagedPathPolicy(tempDir), runner)
        val steamCmdPath = prepareSteamCmd()
        val installPath = tempDir.resolve("servers/palworld/main/runtime")

        val report = useCase.execute(
            InstallPalworldServer.Command(steamCmdPath.toString(), installPath.toString()),
        )

        assertThat(report.completed).isTrue()
        assertThat(report.appId).isEqualTo(2394010)
        assertThat(report.executablePath).isEqualTo(installPath.resolve("PalServer.exe").toString())
        assertThat(runner.arguments).containsExactly(
            "+force_install_dir",
            installPath.toAbsolutePath().normalize().toString(),
            "+login",
            "anonymous",
            "+app_update",
            "2394010",
            "validate",
            "+quit",
        )
    }

    @Test
    fun `SteamCMDの終了コードがゼロ以外ならログ位置を含めて失敗する`() {
        val runner = RecordingProcessRunner(exitCode = 8)
        val useCase = InstallPalworldServer(TemporaryManagedPathPolicy(tempDir), runner)
        val steamCmdPath = prepareSteamCmd()
        val installPath = tempDir.resolve("servers/palworld/main/runtime")

        assertThatThrownBy {
            useCase.execute(
                InstallPalworldServer.Command(steamCmdPath.toString(), installPath.toString()),
            )
        }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("終了コード8")
            .hasMessageContaining("steamcmd-install.log")
    }

    @Test
    fun `SteamCMDの初回自己更新後にインストールを再実行する`() {
        val runner = FirstLaunchUpdateRunner()
        val useCase = InstallPalworldServer(TemporaryManagedPathPolicy(tempDir), runner)
        val steamCmdPath = prepareSteamCmd()
        val installPath = tempDir.resolve("servers/palworld/main/runtime")

        val report = useCase.execute(
            InstallPalworldServer.Command(steamCmdPath.toString(), installPath.toString()),
        )

        assertThat(report.completed).isTrue()
        assertThat(runner.executionCount).isEqualTo(2)
    }

    @Test
    fun `PalServer exeがなければ正常終了扱いにしない`() {
        val runner = RecordingProcessRunner(createGameExecutable = false)
        val useCase = InstallPalworldServer(TemporaryManagedPathPolicy(tempDir), runner)
        val steamCmdPath = prepareSteamCmd()
        val installPath = tempDir.resolve("servers/palworld/main/runtime")

        assertThatThrownBy {
            useCase.execute(
                InstallPalworldServer.Command(steamCmdPath.toString(), installPath.toString()),
            )
        }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("PalServer.exe")
    }

    @Test
    fun `同じ保存先へ再実行できる`() {
        val runner = RecordingProcessRunner(createGameExecutable = true)
        val useCase = InstallPalworldServer(TemporaryManagedPathPolicy(tempDir), runner)
        val steamCmdPath = prepareSteamCmd()
        val installPath = tempDir.resolve("servers/palworld/main/runtime")
        val command = InstallPalworldServer.Command(steamCmdPath.toString(), installPath.toString())

        useCase.execute(command)
        val report = useCase.execute(command)

        assertThat(report.completed).isTrue()
        assertThat(runner.executionCount).isEqualTo(2)
    }

    private fun prepareSteamCmd(): Path {
        val steamCmdPath = tempDir.resolve("tools/steamcmd")
        Files.createDirectories(steamCmdPath)
        Files.writeString(steamCmdPath.resolve("steamcmd.exe"), "test")
        return steamCmdPath
    }

    private class RecordingProcessRunner(
        private val exitCode: Int = 0,
        private val createGameExecutable: Boolean = false,
    ) : SteamCmdProcessRunner {
        var arguments: List<String> = emptyList()
        var executionCount = 0

        override fun run(
            executable: Path,
            arguments: List<String>,
            logPath: Path,
        ): SteamCmdProcessResult {
            this.arguments = arguments
            executionCount += 1
            Files.writeString(logPath, "test log")
            if (createGameExecutable) {
                val installPath = Path.of(arguments[1])
                Files.writeString(installPath.resolve("PalServer.exe"), "test server")
            }
            return SteamCmdProcessResult(exitCode, logPath.toString())
        }
    }

    private class FirstLaunchUpdateRunner : SteamCmdProcessRunner {
        var executionCount = 0

        override fun run(
            executable: Path,
            arguments: List<String>,
            logPath: Path,
        ): SteamCmdProcessResult {
            executionCount += 1
            Files.writeString(logPath, "test log")
            if (executionCount == 2) {
                Files.writeString(Path.of(arguments[1]).resolve("PalServer.exe"), "test server")
            }
            return SteamCmdProcessResult(if (executionCount == 1) 7 else 0, logPath.toString())
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
