package gameservermanager.application.construction

import gameservermanager.domain.server.GamePortAccess

data class DemoServerConfigurationCommand(
    val workspacePath: String,
    val serverName: String,
    val gamePort: Int,
    val rconPort: Int,
    val maxPlayers: Int,
    val serverPasswordConfigured: Boolean,
    val adminPasswordConfigured: Boolean,
    val automationEnabled: Boolean,
    val shutdownTime: String,
    val startupTime: String,
    val backupAfterShutdown: Boolean,
    val gamePortAccess: GamePortAccess,
)

interface DemoServerConfigurator {
    fun configure(command: DemoServerConfigurationCommand)
}
