package gameservermanager.application.construction

import gameservermanager.domain.construction.ServerConstructionPlan
import org.springframework.stereotype.Service

@Service
class CreateServerConstructionPlan(
    private val createGamePortAccess: CreateGamePortAccess,
) {
    fun execute(command: Command): ServerConstructionPlan {
        require(!command.automationEnabled || command.shutdownTime != command.startupTime) {
            "停止時刻と起動時刻は別の時刻を指定してください"
        }
        return ServerConstructionPlan(
            serverName = command.serverName.trim(),
            installPath = command.installPath.trim(),
            steamCmdPath = command.steamCmdPath.trim(),
            gamePort = command.gamePort,
            rconPort = command.rconPort,
            maxPlayers = command.maxPlayers,
            serverPasswordConfigured = command.serverPassword.isNotBlank(),
            adminPasswordConfigured = command.adminPassword.isNotBlank(),
            automationEnabled = command.automationEnabled,
            shutdownTime = command.shutdownTime,
            startupTime = command.startupTime,
            backupAfterShutdown = command.backupAfterShutdown,
            backupRetentionCount = 3,
            gamePortAccess = createGamePortAccess.execute(command.gamePortAccess),
        )
    }

    data class Command(
        val serverName: String,
        val installPath: String,
        val steamCmdPath: String,
        val gamePort: Int,
        val rconPort: Int,
        val maxPlayers: Int,
        val serverPassword: String,
        val adminPassword: String,
        val automationEnabled: Boolean,
        val shutdownTime: String,
        val startupTime: String,
        val backupAfterShutdown: Boolean,
        val gamePortAccess: CreateGamePortAccess.Command,
    )
}
