package gameservermanager.shared.server

import java.time.Instant

data class GameServerRegistration(
    val game: String,
    val serverId: String,
    val mode: String,
    val state: String,
    val serverName: String,
    val installPath: String,
    val workspacePath: String,
    val gamePort: Int,
    val rconPort: Int,
    val createdAt: Instant,
    val gamePortAccess: GamePortAccess = GamePortAccess(),
    val peerPort: Int? = null,
    val queryPort: Int? = null,
    val map: String? = null,
    val maxPlayers: Int? = null,
)
