package gameservermanager.shared.construction

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import gameservermanager.shared.construction.DemoServerConfigurationCommand
import gameservermanager.shared.server.GamePortAccess
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class FileSystemDemoServerConfiguratorTests {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `管理画面で確認できるようパスワードをデモ設定へ保存する`() {
        FileSystemDemoServerConfigurator(jacksonObjectMapper()).configure(
            DemoServerConfigurationCommand(
                workspacePath = tempDir.toString(),
                serverName = "Demo Server",
                gamePort = 8211,
                rconPort = 25575,
                maxPlayers = 3,
                serverPassword = "server-password",
                adminPassword = "admin-password",
                automationEnabled = true,
                shutdownTime = "04:00",
                startupTime = "09:00",
                gamePortAccess = GamePortAccess(),
            ),
        )

        val settings = Files.readString(
            tempDir.resolve("servers/palworld/main/runtime/Pal/Saved/Config/WindowsServer/PalWorldSettings.ini"),
        )
        val automation = Files.readString(tempDir.resolve("config/palworld-main-automation.demo.json"))
        assertThat(settings).contains(
            "OptionSettings=(",
            "ServerPassword=\"server-password\"",
            "AdminPassword=\"admin-password\"",
        )
        assertThat(tempDir.resolve("servers/palworld/main/runtime/DefaultPalWorldSettings.ini")).exists()
        assertThat(automation).contains(
            "\"enabled\" : true",
            "\"100.64.0.0/10\"",
        )
    }
}
