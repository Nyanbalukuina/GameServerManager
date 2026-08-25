package gameservermanager.web.asa

import gameservermanager.application.asa.RunDemoAsaServerConstruction
import gameservermanager.application.construction.CreateGamePortAccess
import gameservermanager.domain.construction.DemoConstructionReport
import jakarta.validation.Valid
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// ASAデモ構築APIを公開する。
@RestController
@RequestMapping("/api/asa/constructions/demo")
@ConditionalOnProperty(
    prefix = "game-server-manager.features",
    name = ["demo-enabled"],
    havingValue = "true",
)
class AsaDemoConstructionController(private val construction: RunDemoAsaServerConstruction) {
    @PostMapping
    fun construct(@Valid @RequestBody request: ConstructAsaRequest): DemoConstructionReport {
        return construction.execute(
            RunDemoAsaServerConstruction.Command(
                request.serverName, request.installPath, request.map, requireNotNull(request.gamePort),
                requireNotNull(request.queryPort), requireNotNull(request.rconPort), requireNotNull(request.maxPlayers),
                request.serverPassword, request.adminPassword,
                CreateGamePortAccess.Command(
                    request.allowLocalSubnet, request.allowTailscale,
                    request.customRemoteAddresses, request.allowAnyRemoteAddress,
                ),
                request.pveEnabled, request.xpMultiplier,
                request.tamingSpeedMultiplier, request.harvestAmountMultiplier,
                request.eggHatchSpeedMultiplier, request.babyMatureSpeedMultiplier,
            ),
        )
    }
}
