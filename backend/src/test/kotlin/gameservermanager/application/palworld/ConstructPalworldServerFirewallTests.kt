package gameservermanager.application.palworld

import gameservermanager.application.construction.CreateGamePortAccess
import gameservermanager.application.construction.GameFirewallManager
import gameservermanager.application.construction.GameFirewallRuleCommand
import gameservermanager.application.server.GameServerRegistrationStore
import gameservermanager.application.steamcmd.PrepareSteamCmd
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.nio.file.Files
import java.nio.file.Path

class ConstructPalworldServerFirewallTests {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `実構築では選択した接続元でFirewall規則を追加する`() {
        val dependencies = dependencies()
        given(dependencies.firewall.apply(FIREWALL_COMMAND)).willReturn("rule-name")

        dependencies.useCase.execute(command())

        verify(dependencies.firewall).apply(FIREWALL_COMMAND)
    }

    @Test
    fun `Firewall設定後に起動失敗した場合は規則を解除する`() {
        val dependencies = dependencies()
        given(dependencies.firewall.apply(FIREWALL_COMMAND)).willReturn("rule-name")
        given(dependencies.start.execute(StartPalworldServer.Command(INSTALL_PATH, 8211, 3)))
            .willThrow(IllegalStateException("startup failed"))

        assertThatThrownBy { dependencies.useCase.execute(command()) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("startup failed")
        verify(dependencies.firewall).remove(FIREWALL_COMMAND)
    }

    private fun dependencies(): Dependencies {
        val prepare = mock(PrepareSteamCmd::class.java)
        val install = mock(InstallPalworldServer::class.java)
        val configure = mock(ConfigurePalworldServer::class.java)
        val automation = mock(ConfigurePalworldAutomation::class.java)
        val start = mock(StartPalworldServer::class.java)
        val registrations = mock(GameServerRegistrationStore::class.java)
        val firewall = mock(GameFirewallManager::class.java)
        Files.createDirectories(tempDir.resolve("steamcmd"))
        Files.writeString(tempDir.resolve("steamcmd/steamcmd.exe"), "test")
        return Dependencies(
            ConstructPalworldServer(
                prepare,
                install,
                configure,
                automation,
                start,
                registrations,
                CreateGamePortAccess(),
                firewall,
            ),
            start,
            firewall,
        )
    }

    private fun command() = ConstructPalworldServer.Command(
        serverName = "Palworld Server",
        installPath = INSTALL_PATH,
        steamCmdPath = tempDir.resolve("steamcmd").toString(),
        gamePort = 8211,
        rconPort = 25575,
        maxPlayers = 3,
        serverPassword = "",
        adminPassword = "admin-password",
        automationEnabled = false,
        shutdownTime = "04:00",
        startupTime = "09:00",
        gamePortAccess = CreateGamePortAccess.Command(true, true, "", false),
    )

    private data class Dependencies(
        val useCase: ConstructPalworldServer,
        val start: StartPalworldServer,
        val firewall: GameFirewallManager,
    )

    companion object {
        private const val INSTALL_PATH = "C:\\GameServerManager\\servers\\palworld\\main\\runtime"
        private val FIREWALL_COMMAND = GameFirewallRuleCommand(
            "PALWORLD",
            "palworld-main",
            8211,
            listOf("LocalSubnet", "100.64.0.0/10"),
        )
    }
}
