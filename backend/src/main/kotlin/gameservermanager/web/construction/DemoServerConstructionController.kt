package gameservermanager.web.construction

import gameservermanager.application.construction.CreateGamePortAccess
import gameservermanager.application.construction.RunDemoServerConstruction
import gameservermanager.domain.construction.DemoConstructionReport
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/server-constructions/demo")
class DemoServerConstructionController(
    private val runDemoServerConstruction: RunDemoServerConstruction,
) {
    @PostMapping
    fun run(@Valid @RequestBody request: NewServerRequest): DemoConstructionReport {
        val command = RunDemoServerConstruction.Command(
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
            gamePortAccess = CreateGamePortAccess.Command(
                localSubnet = request.allowLocalSubnet,
                tailscale = request.allowTailscale,
                customRemoteAddresses = request.customRemoteAddresses,
                allowAny = request.allowAnyRemoteAddress,
            ),
        )

        return runDemoServerConstruction.execute(command)
    }
}
