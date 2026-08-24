package gameservermanager.application.construction

data class GameFirewallRuleCommand(
    val game: String,
    val serverId: String,
    val gamePort: Int,
    val remoteAddresses: List<String>,
)

interface GameFirewallManager {
    fun apply(command: GameFirewallRuleCommand): String
    fun remove(command: GameFirewallRuleCommand)
}
