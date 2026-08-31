package gameservermanager.palworld.web

import gameservermanager.palworld.application.ManageDemoGameServer
import gameservermanager.shared.configuration.FeatureProperties
import gameservermanager.shared.server.GameServerRegistration
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/servers/palworld")
class PalworldServerRegistryController(
    private val manageDemoGameServer: ManageDemoGameServer,
    private val features: FeatureProperties,
) {
    @GetMapping
    fun get(): GameServerRegistration {
        val server = manageDemoGameServer.getPalworld()
        require(features.demoEnabled || server.mode != "DEMO") { "Palworldサーバーは登録されていません" }
        return server
    }
}
