package gameservermanager.palworld.web

import gameservermanager.palworld.application.ConstructPalworldServer
import gameservermanager.shared.construction.CreateGamePortAccess
import gameservermanager.shared.construction.ConstructionProgressSnapshot
import gameservermanager.shared.construction.ConstructionProgressTracker
import gameservermanager.shared.construction.ServerConstructionReport
import gameservermanager.shared.construction.NewServerRequest
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/palworld/constructions")
class PalworldConstructionController(
    private val constructPalworldServer: ConstructPalworldServer,
    private val progressTracker: ConstructionProgressTracker,
) {
    @GetMapping("/progress")
    fun progress(): ConstructionProgressSnapshot? = progressTracker.get(ConstructPalworldServer.GAME)

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
            ),
        )
    }
}
