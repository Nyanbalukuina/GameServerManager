package gameservermanager.domain

data class ServerConstructionPlan(
    val serverName: String,
    val installPath: String,
    val steamCmdPath: String,
    val gamePort: Int,
    val rconPort: Int,
    val maxPlayers: Int,
    val serverPasswordConfigured: Boolean,
    val adminPasswordConfigured: Boolean,
)

