package gameservermanager.shared.construction

import gameservermanager.shared.construction.CreateGamePortAccess
import gameservermanager.shared.construction.RunDemoServerConstruction
import gameservermanager.shared.construction.DemoConstructionReport
import jakarta.validation.Valid
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/server-constructions/demo")
@ConditionalOnProperty(
    prefix = "game-server-manager.features",
    name = ["demo-enabled"],
    havingValue = "true",
)
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
            serverDescription = request.serverDescription,
            expRate = request.expRate, palCaptureRate = request.palCaptureRate,
            palSpawnRate = request.palSpawnRate, enemyDropRate = request.enemyDropRate,
            eggHatchingTime = request.eggHatchingTime, deathPenalty = request.deathPenalty,
            pvpEnabled = request.pvpEnabled, friendlyFireEnabled = request.friendlyFireEnabled,
            baseCampMaxNum = request.baseCampMaxNum, baseCampWorkerMaxNum = request.baseCampWorkerMaxNum,
        )

        return runDemoServerConstruction.execute(command)
    }
}
