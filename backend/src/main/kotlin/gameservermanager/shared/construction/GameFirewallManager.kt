package gameservermanager.shared.construction

data class GameFirewallRuleCommand(
    val game: String,
    val serverId: String,
    val gamePort: Int,
    val remoteAddresses: List<String>,
    val protocol: String = "UDP",
)

interface GameFirewallManager {
    fun apply(command: GameFirewallRuleCommand): String
    fun remove(command: GameFirewallRuleCommand)
}
