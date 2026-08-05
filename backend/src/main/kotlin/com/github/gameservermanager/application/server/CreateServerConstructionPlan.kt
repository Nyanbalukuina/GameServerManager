package com.github.gameservermanager.application.server

import com.github.gameservermanager.domain.server.ServerConstructionPlan
import org.springframework.stereotype.Service

@Service
class CreateServerConstructionPlan {
    fun execute(command: Command): ServerConstructionPlan =
        ServerConstructionPlan(
            serverName = command.serverName.trim(),
            installPath = command.installPath.trim(),
            steamCmdPath = command.steamCmdPath.trim(),
            gamePort = command.gamePort,
            rconPort = command.rconPort,
            maxPlayers = command.maxPlayers,
            serverPasswordConfigured = command.serverPassword.isNotBlank(),
            adminPasswordConfigured = command.adminPassword.isNotBlank(),
        )

    data class Command(
        val serverName: String,
        val installPath: String,
        val steamCmdPath: String,
        val gamePort: Int,
        val rconPort: Int,
        val maxPlayers: Int,
        val serverPassword: String,
        val adminPassword: String,
    )
}

