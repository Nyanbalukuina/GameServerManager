package gameservermanager.web.construction

import gameservermanager.application.construction.CreateServerConstructionPlan
import gameservermanager.application.construction.CreateGamePortAccess
import gameservermanager.domain.construction.ServerConstructionPlan
import gameservermanager.web.error.InvalidConstructionPlanException
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/server-construction-plans")
class ServerConstructionPlanController(
    private val createServerConstructionPlan: CreateServerConstructionPlan,
) {
    @PostMapping
    fun create(@Valid @RequestBody request: NewServerRequest): ServerConstructionPlan {
        if (request.gamePort == request.rconPort) {
            throw InvalidConstructionPlanException(
                mapOf("rconPort" to "RCONポートはゲームポートと異なる値にしてください"),
            )
        }

        return createServerConstructionPlan.execute(
            CreateServerConstructionPlan.Command(
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
