package gameservermanager.application.construction

import gameservermanager.domain.server.GamePortAccess

data class DemoServerConfigurationCommand(
    val workspacePath: String,
    val serverName: String,
    val gamePort: Int,
    val rconPort: Int,
    val maxPlayers: Int,
    val serverPassword: String,
    val adminPassword: String,
    val automationEnabled: Boolean,
    val shutdownTime: String,
    val startupTime: String,
    val gamePortAccess: GamePortAccess,
)

interface DemoServerConfigurator {
    fun configure(command: DemoServerConfigurationCommand)
}
