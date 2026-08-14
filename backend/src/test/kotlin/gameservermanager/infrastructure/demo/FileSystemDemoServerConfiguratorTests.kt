package gameservermanager.infrastructure.demo

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import gameservermanager.application.construction.DemoServerConfigurationCommand
import gameservermanager.domain.server.GamePortAccess
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class FileSystemDemoServerConfiguratorTests {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `パスワードを残さずデモ設定と自動運転設定を保存する`() {
        FileSystemDemoServerConfigurator(jacksonObjectMapper()).configure(
            DemoServerConfigurationCommand(
                workspacePath = tempDir.toString(),
                serverName = "Demo Server",
                gamePort = 8211,
                rconPort = 25575,
                maxPlayers = 3,
                serverPasswordConfigured = true,
                adminPasswordConfigured = true,
                automationEnabled = true,
                shutdownTime = "04:00",
                startupTime = "09:00",
                backupAfterShutdown = true,
                gamePortAccess = GamePortAccess(),
            ),
        )

        val settings = Files.readString(
            tempDir.resolve("servers/palworld/main/runtime/Pal/Saved/Config/WindowsServer/PalWorldSettings.demo.ini"),
        )
        val automation = Files.readString(tempDir.resolve("config/palworld-main-automation.demo.json"))
        assertThat(settings).contains("AdminPassword=<configured>").doesNotContain("admin-password")
        assertThat(automation).contains(
            "\"enabled\" : true",
            "\"backupRetentionCount\" : 3",
            "\"100.64.0.0/10\"",
        )
    }
}
