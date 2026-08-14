package gameservermanager.web.palworld

import gameservermanager.application.palworld.ConstructPalworldServer
import gameservermanager.application.construction.CreateGamePortAccess
import gameservermanager.domain.construction.ServerConstructionReport
import gameservermanager.web.construction.NewServerRequest
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/palworld/constructions")
class PalworldConstructionController(
    private val constructPalworldServer: ConstructPalworldServer,
) {
    @PostMapping
    fun construct(@Valid @RequestBody request: NewServerRequest): ServerConstructionReport {
        return constructPalworldServer.execute(
            ConstructPalworldServer.Command(
                serverName = request.serverName,
                installPath = request.installPath,
                steamCmdPath = request.steamCmdPath,
                gamePort = requireNotNull(request.gamePort),
                rconPort = requireNotNull(request.rconPort),
                maxPlayers = requireNotNull(request.maxPlayers),
                serverPassword = request.serverPassword,
                adminPassword = request.adminPassword,
                automationEnabled = request.automationEnabled,
                shutdownTime = request.shutdownTime,
                startupTime = request.startupTime,
                backupAfterShutdown = request.backupAfterShutdown,
                gamePortAccess = CreateGamePortAccess.Command(
                    localSubnet = request.allowLocalSubnet,
                    tailscale = request.allowTailscale,
                    customRemoteAddresses = request.customRemoteAddresses,
                    allowAny = request.allowAnyRemoteAddress,
                ),
            ),
        )
    }
}
