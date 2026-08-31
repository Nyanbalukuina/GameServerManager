package gameservermanager.shared.construction

import gameservermanager.shared.construction.SteamCmdInstallCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class DemoSteamCmdInstallerTests {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `一時フォルダへSteamCMDとPalworldのデモ構成を作成する`() {
        val installer = DemoSteamCmdInstaller(tempDir)
        val command = SteamCmdInstallCommand(
            steamCmdPath = "C:\\GameServerManager\\tools\\steamcmd",
            installPath = "C:\\GameServerManager\\servers\\palworld\\main\\runtime",
            appId = 2394010,
        )

        val result = installer.install(command)
        val workspace = Path.of(result.workspacePath)

        assertThat(Path.of(result.steamCmdExecutablePath)).isRegularFile()
        assertThat(Path.of(result.gameExecutablePath)).isRegularFile()
        assertThat(workspace.resolve("servers/palworld/main/runtime/Pal/Saved/Config/WindowsServer"))
            .isDirectory()
        assertThat(workspace.resolve("servers/palworld/main/runtime/Pal/Saved/SaveGames")).isDirectory()
        assertThat(workspace.resolve("servers/palworld/main/runtime/Pal/Saved/Logs")).isDirectory()
        assertThat(workspace.resolve("backups/palworld-main")).isDirectory()
        assertThat(workspace.resolve("logs")).isDirectory()
        assertThat(Files.readString(workspace.resolve("steamcmd-arguments.txt")))
            .contains("+force_install_dir", command.installPath, "+app_update", "2394010")
    }
}
