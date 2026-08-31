package gameservermanager.asa.web

import gameservermanager.asa.application.ConstructAsaServer
import gameservermanager.shared.construction.CreateGamePortAccess
import gameservermanager.shared.construction.ConstructionProgressSnapshot
import gameservermanager.shared.construction.ConstructionProgressTracker
import gameservermanager.shared.construction.ServerConstructionReport
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// ASAの実構築APIを公開する。
@RestController
@RequestMapping("/api/asa/constructions")
class AsaConstructionController(
    private val constructAsaServer: ConstructAsaServer,
    private val progressTracker: ConstructionProgressTracker,
) {
    @GetMapping("/progress")
    fun progress(): ConstructionProgressSnapshot? = progressTracker.get(ConstructAsaServer.GAME)

    @PostMapping
    fun construct(@Valid @RequestBody request: ConstructAsaRequest): ServerConstructionReport {
        return constructAsaServer.execute(
            ConstructAsaServer.Command(
                request.serverName, request.installPath, request.steamCmdPath, request.map,
                requireNotNull(request.gamePort), requireNotNull(request.queryPort), requireNotNull(request.rconPort),
                requireNotNull(request.maxPlayers), request.serverPassword, request.adminPassword,
                CreateGamePortAccess.Command(
                    request.allowLocalSubnet, request.allowTailscale,
                    request.customRemoteAddresses, request.allowAnyRemoteAddress,
                ),
                request.pveEnabled, request.xpMultiplier,
                request.tamingSpeedMultiplier, request.harvestAmountMultiplier,
                request.eggHatchSpeedMultiplier, request.babyMatureSpeedMultiplier,
                request.useSingleplayerSettings,
            ),
        )
    }
}
