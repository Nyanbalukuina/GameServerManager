package gameservermanager.application.construction

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CreateServerConstructionPlanTests {
    private val useCase = CreateServerConstructionPlan(CreateGamePortAccess())

    @Test
    fun `入力を整形して構築計画を作成する`() {
        val plan = useCase.execute(
            CreateServerConstructionPlan.Command(
                serverName = " Palworld Server ",
                installPath = " C:\\GameServers\\Palworld ",
                steamCmdPath = " C:\\GameServers\\SteamCMD ",
                gamePort = 8211,
                rconPort = 25575,
                maxPlayers = 3,
                serverPassword = "",
                adminPassword = "admin-password",
                automationEnabled = true,
                shutdownTime = "04:00",
                startupTime = "09:00",
                gamePortAccess = CreateGamePortAccess.Command(
                    localSubnet = true,
                    tailscale = true,
                    customRemoteAddresses = "10.8.0.0/24",
                    allowAny = false,
                ),
            ),
        )

        assertThat(plan.serverName).isEqualTo("Palworld Server")
        assertThat(plan.installPath).isEqualTo("C:\\GameServers\\Palworld")
        assertThat(plan.steamCmdPath).isEqualTo("C:\\GameServers\\SteamCMD")
        assertThat(plan.serverPasswordConfigured).isFalse()
        assertThat(plan.adminPasswordConfigured).isTrue()
        assertThat(plan.automationEnabled).isTrue()
        assertThat(plan.shutdownTime).isEqualTo("04:00")
        assertThat(plan.startupTime).isEqualTo("09:00")
        assertThat(plan.gamePortAccess.remoteAddresses())
            .containsExactly("LocalSubnet", "100.64.0.0/10", "10.8.0.0/24")
    }
}
