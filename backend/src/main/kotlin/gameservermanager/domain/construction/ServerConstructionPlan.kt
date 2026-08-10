package gameservermanager.domain.construction

data class ServerConstructionPlan(
    val serverName: String,
    val installPath: String,
    val steamCmdPath: String,
    val gamePort: Int,
    val rconPort: Int,
    val maxPlayers: Int,
    val serverPasswordConfigured: Boolean,
    val adminPasswordConfigured: Boolean,
    val automationEnabled: Boolean,
    val shutdownTime: String,
    val startupTime: String,
    val backupAfterShutdown: Boolean,
    val backupRetentionCount: Int,
)
